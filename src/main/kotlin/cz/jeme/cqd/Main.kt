package cz.jeme.cqd

import cz.jeme.cqd.renderer.CharRenderer
import cz.jeme.cqd.renderer.CharRendererRegistry
import cz.jeme.cqd.util.ANSI
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegLogCallback
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Attributes
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.io.File
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread
import kotlin.experimental.and
import kotlin.math.log10
import kotlin.math.roundToInt

lateinit var terminal: Terminal
var initialAttributes: Attributes? = null
val reader: LineReader by lazy { LineReaderBuilder.builder().terminal(terminal).build() }

fun main(args: Array<String>) {
    terminal = TerminalBuilder.builder().system(true).build()

    // Disable FFmpeg logging
    FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET)

    try {
        terminal.puts(InfoCmp.Capability.cursor_visible)
        run(args)
    } finally {
        terminal.puts(InfoCmp.Capability.exit_ca_mode)
        terminal.puts(InfoCmp.Capability.cursor_visible)
        initialAttributes?.let { terminal.attributes = it }
        terminal.flush()
        terminal.close()
    }
}

private const val CHAR_ASPECT_RATIO: Double = 1.0 / 2.0 // chars are approx twice as high as wide
private const val MAX_FPS: Double = 25.0

@OptIn(ExperimentalAtomicApi::class)
fun run(args: Array<String>) {
    val input = when {
        args.isNotEmpty() -> {
            val f = File(args[0])
            if (!validateInputFile(f)) return
            f
        }

        else -> askForInputFile()
    }

    val colors = System.getenv("COLORTERM")?.lowercase()
    val trueColor = colors == "truecolor" || colors == "24bit"
    if (!trueColor) println("This terminal environment does not support truecolor!\nYour experience will be very limited.")

    print("Play video file? (Y/n): ")
    val key = terminal.reader().read().toChar()
    when (key) {
        'Y', 'y', '\n', '\r' -> {}
        else -> return
    }
    terminal.puts(InfoCmp.Capability.enter_ca_mode)
    terminal.puts(InfoCmp.Capability.cursor_invisible)
    initialAttributes = terminal.enterRawMode()
    terminal.flush()

    val startStamp = System.currentTimeMillis()

    val sizeChange = AtomicBoolean(false)
    terminal.handle(Terminal.Signal.WINCH) { sizeChange.store(true) }

    FFmpegFrameGrabber(input).use { grabber ->
        grabber.start()
        grabber.imageMode = FrameGrabber.ImageMode.COLOR

        val originalWidth = grabber.imageWidth
        val originalHeight = grabber.imageHeight

        var infoBarsEnabled = true

        var w: Int = -1
        var h: Int = -1
        var padLeft = 0
        var padTop = 0

        fun resizeGrabber() {
            val termWidth = terminal.width
            // height lowered by 1 because some terminals glitch when the last row is used
            val termHeight = terminal.height - 1 - if (infoBarsEnabled) 2 /* bars take up 2 rows */ else 0
            val scaleX = termWidth / (originalWidth.toDouble() * 2)
            val scaleY = termHeight / originalHeight.toDouble()
            val minScale = minOf(scaleX, scaleY)
            w = ((originalWidth * minScale) / CHAR_ASPECT_RATIO).toInt()
            h = (originalHeight * minScale).toInt()
            padLeft = (termWidth - w) / 2
            padTop = (termHeight - h) / 2
            grabber.imageWidth = w
            grabber.imageHeight = h
        }
        resizeGrabber()

        assert(w > 0 && h > 0) { "Invalid width and height: $w x $h" }

        val sourceFps = grabber.frameRate
        val fps = minOf(sourceFps, MAX_FPS)
        val fpsStr = String.format("%05.2f", fps)
        val frameSkip: Double = if (fps == sourceFps) 1.0 else sourceFps / fps
        var videoFrameSkipCounter: Double = 0.0

        var fpsSnapshot: Double = 0.0
        var fpsSnapshotStamp: Long = 0
        var frameNumSnapshot: Int = 0

        var frameNum = 0
        val frameBuilder = StringBuilder(
            h * (w * (19 /* max color ansi length */ + 1 /* pixel charx */) + 1 /* newline */) + 6 /* goto home length */
        )
        var lastFrameStamp: Long = 0
        var lastBytesWritten: Int = 0
        var lastSyncStamp: Long = 0

        val frameDigits = log10(grabber.lengthInVideoFrames.toDouble()).toInt()
        val frameNumberFmt = "%0${frameDigits}d"
        val durationMs = grabber.lengthInTime / 1000.0

        val audioThread = AudioThread(
            grabber.sampleRate.toFloat(),
            grabber.audioChannels
        ).apply { start() }

        var renderer: CharRenderer = CharRendererRegistry.PIXELATED_STAR_ADAPTIVE_DYNAMIC

        val running = AtomicBoolean(true)

        val listenerThread = thread(start = true, isDaemon = true) {
            val reader = terminal.reader()
            while (running.load()) {
                val ch = reader.read().toChar()
                when (ch) {
                    'q', 'Q' -> running.store(false)
                    'f', 'F' -> {
                        // toggle info bars
                        infoBarsEnabled = !infoBarsEnabled
                        // and update video dimensions
                        sizeChange.store(true)
                    }

                    'r', 'R' -> {
                        val renderers = CharRendererRegistry.renderers
                        val index = renderers.indexOf(renderer)
                        renderer = renderers[(index + 1) % renderers.size]
                    }
                }
            }
        }

        while (running.load()) {
            if (sizeChange.load()) {
                resizeGrabber()
                terminal.puts(InfoCmp.Capability.clear_screen)
                terminal.flush()
                sizeChange.store(false)
            }
            val frame = grabber.grabFrame(
                true, true, true, false, false
            ) ?: break // end of file

            // AUDIO
            if (frame.type == Frame.Type.AUDIO) {
                // planar audio should never appear here as it's encoded
                val audioBuf = (frame.samples[0] as ShortBuffer).rewind()
                val audioData = ByteArray(audioBuf.remaining() * 2)
                var i = 0
                while (audioBuf.hasRemaining()) {
                    val sample: Short = audioBuf.get()
                    // 2 bytes per sample (16-bit PCM)
                    val lower = (sample and 0xFF).toByte()
                    val upper = ((sample.toInt() shr 8) and 0xFF).toByte()
                    // FFmpeg runs in native-endian (little-endian on x86 and x86-64)
                    audioData[i] = lower
                    audioData[i + 1] = upper
                    i += 2
                }
                audioThread.queue.offer(audioData)
            }

            // VIDEO
            val image = frame.image
            if (image != null) {
                videoFrameSkipCounter += 1.0
                if (frameSkip > 1 && videoFrameSkipCounter < frameSkip) continue // skip frames to match custom fps
                val computedFrame = ((System.currentTimeMillis() - startStamp) / 1000.0 * fps).toInt()
                if (computedFrame < frameNum) {
                    val frameDiff = frameNum - computedFrame
                    Thread.sleep((frameDiff / fps * 1000.0).toLong())
                }
                if (computedFrame - frameNum > 5) { // 5 frames behind, force sync
                    grabber.frameNumber = (computedFrame * frameSkip).roundToInt()
                    frameNum = computedFrame
                    lastSyncStamp = System.currentTimeMillis()
                } else {
                    frameNum++
                    videoFrameSkipCounter -= frameSkip
                }

                val prevFrameStamp = lastFrameStamp
                lastFrameStamp = System.currentTimeMillis()

                val stridePadding = frame.imageStride - w * 3

                frameBuilder.setLength(0) // reset frame
                frameBuilder.append(ANSI.GOTO_HOME)

                // Upper info bar
                if (infoBarsEnabled) {
                    val sinceFpsSnapshotUpdate = System.currentTimeMillis() - fpsSnapshotStamp
                    if (sinceFpsSnapshotUpdate >= 1000) {
                        val framesPlayed = frameNum - frameNumSnapshot
                        fpsSnapshot = framesPlayed / (sinceFpsSnapshotUpdate / 1000.0)
                        fpsSnapshotStamp = System.currentTimeMillis()
                        frameNumSnapshot = frameNum
                    }
                    frameBuilder
                        .append(ANSI.CLEAR_LINE)
                        .append(ANSI.FG_WHITE)
                        .append(" CQD | ")
                    val now = System.currentTimeMillis()
                    if (now - lastSyncStamp < 1000) {
                        frameBuilder.append(ANSI.FG_RED).append("BEHIND!")
                    } else {
                        frameBuilder.append(ANSI.FG_GREEN).append("SYNCED")
                    }

                    frameBuilder.append(ANSI.FG_WHITE)
                        .append(" | delta ")
                        .append(String.format("%02d", now - prevFrameStamp))
                        .append(" ms | ")
                        .append(String.format("%05.2f", fpsSnapshot))
                        .append('/')
                        .append(fpsStr)
                        .append(" fps | ")
                        .append(String.format("%05.2f", ((lastBytesWritten * fpsSnapshot * 8) / 1_000_000)))
                        .append(" Mbps | ")
                        .append(w)
                        .append('×')
                        .append(h)
                        .append(" ch")

                    frameBuilder.append('\n')
                }

                frameBuilder.append(ANSI.FG_BLACK)

                // take only the first video buffer, planar formats are unsupported
                val videoBuf = (image[0] as ByteBuffer).rewind()
                repeat(padTop) { frameBuilder.append('\n') }
                repeat(h) {
                    frameBuilder.append(ANSI.BG_RESET)
                    repeat(padLeft) { frameBuilder.append(' ') }
                    repeat(w) {
                        val blue = videoBuf.get().toInt() and 0xFF
                        val green = videoBuf.get().toInt() and 0xFF
                        val red = videoBuf.get().toInt() and 0xFF

                        renderer.render(frameBuilder, red, green, blue)
                    }
                    videoBuf.position(videoBuf.position() + stridePadding)
                    frameBuilder.append('\n')
                }

                // Lower info bar
                if (infoBarsEnabled) {
                    val now = System.currentTimeMillis()
                    val progress = (now - startStamp) / durationMs

                    frameBuilder
                        .append(ANSI.BG_RESET)
                        .append('\n')
                        .append(ANSI.CLEAR_LINE)
                        .append(ANSI.FG_WHITE)
                        .append(" #")
                        .append(String.format(frameNumberFmt, frameNum))
                        .append(" | ")
                        .append(String.format("%05.2f", progress * 100.0))
                        .append(" % | renderer: ")
                        .append(renderer.name)
                }

                val frameStr = frameBuilder.toString()
                lastBytesWritten = frameStr.toByteArray().size
                print(frameStr)
            }
        }
        if (running.load()) running.store(false)
        audioThread.running = false
        audioThread.join()
        listenerThread.join()
        grabber.stop()
    }
}

private fun validateInputFile(file: File): Boolean {
    if (!file.exists()) {
        terminal.puts(InfoCmp.Capability.bell)
        println("File does not exist")
        return false
    }
    if (!file.isFile) {
        terminal.puts(InfoCmp.Capability.bell)
        println("Not a file")
        return false
    }
    if (!file.canRead()) {
        terminal.puts(InfoCmp.Capability.bell)
        println("File unreadable")
        return false
    }
    println("Reading file...")
    try {
        FFmpegFrameGrabber(file).use { grabber ->
            grabber.start()
            grabber.stop()
        }
    } catch (_: Exception) {
        terminal.puts(InfoCmp.Capability.bell)
        println("Invalid file format (not a video file)")
        return false
    }
    return true
}

fun askForInputFile(): File {
    while (true) {
        val path = reader.readLine("video path> ")
        val file = File(path)
        if (validateInputFile(file)) return file
    }
}