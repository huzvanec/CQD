package cz.jeme.cqd

import cz.jeme.cqd.util.ANSI
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegLogCallback
import org.bytedeco.javacv.FrameGrabber
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Attributes
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.io.File
import java.nio.ByteBuffer

lateinit var terminal: Terminal
var initialAttributes: Attributes? = null
val reader: LineReader by lazy { LineReaderBuilder.builder().terminal(terminal).build() }

fun main() {
    terminal = TerminalBuilder.builder().system(true).build()

    // Disable FFmpeg logging
    FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET)

    try {
        run()
    } finally {
        terminal.puts(InfoCmp.Capability.exit_ca_mode)
        terminal.puts(InfoCmp.Capability.cursor_visible)
        initialAttributes?.let { terminal.attributes = it }
        terminal.flush()
        terminal.close()
    }
}

private const val CHARS =
    " ´`.·-¯¨¸',_¬:ºª°;~\"!¡=¹^+¦÷×²³>l<r|L?¿/\\TFJv(*iz±íIfìjt«)»[]cY47xu}1kîy{ïnZsÍoÌCÝ2eEúVù6Ï5h3¤Þçý9aÎPXSüóAò£UµûÿédÈÉèKñHbqÇöOõpàôá#Gëw¼mÁÊD¢ÀðËÚêÙg©¾M¥½âäþßãæøR®Ä0ÃÓ"

fun run() {
    val input = readInputPath()

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

    val start = System.currentTimeMillis()

    var sizeChange = false
    terminal.handle(Terminal.Signal.WINCH) { sizeChange = true }

    FFmpegFrameGrabber(input).use { grabber ->
        grabber.start()
        grabber.imageMode = FrameGrabber.ImageMode.COLOR
        val originalWidth = grabber.imageWidth
        val originalHeight = grabber.imageHeight

        var barEnabled = true

        var w: Int = -1
        var h: Int = -1
        var padLeft = 0
        var padTop = 0
        fun resizeGrabber() {
            val termWidth = terminal.width
            // height lowered by 1 because some terminals glitch when the last row is used
            val termHeight = terminal.height - 1 - if (barEnabled) 1 else 0
            val scaleX = termWidth / (originalWidth.toDouble() * 2)
            val scaleY = termHeight / originalHeight.toDouble()
            val minScale = minOf(scaleX, scaleY)
            w = (originalWidth * minScale * 2).toInt()
            h = (originalHeight * minScale).toInt()
            padLeft = (termWidth - w) / 2
            padTop = (termHeight - h) / 2
            grabber.imageWidth = w
            grabber.imageHeight = h
        }
        resizeGrabber()

        assert(w > 0 && h > 0) { "Invalid width and height: $w x $h" }

        val fps = grabber.frameRate
        val fpsStr = String.format("%05.2f", fps)
        var frameNum = 0
        val frameBuilder = StringBuilder(
            h * (w * (19 /* max color ansi length */ + 1 /* 2 pixel chars */) + 1 /* newline */) + 6 /* goto home length */
        )
        var frameStamp: Long = 0
        var bytesWrite: Int = 0
        var lastSyncStamp: Long = 0
        while (true) {
            val computedFrame = ((System.currentTimeMillis() - start) / 1000.0 * fps).toInt()
            if (sizeChange) {
                resizeGrabber()
                terminal.puts(InfoCmp.Capability.clear_screen)
                terminal.flush()
                sizeChange = false
            }
            if (computedFrame < frameNum) continue
            if (computedFrame - frameNum > 5) { // 5 frames behind, sync
                grabber.frameNumber = computedFrame
                frameNum = computedFrame
                lastSyncStamp = System.currentTimeMillis()
            }
            val lastFrame = frameStamp
            frameStamp = System.currentTimeMillis()
            val frame = grabber.grabImage() ?: break
            frameNum++
            val stridePad = frame.imageStride - w * 3
            val buff = frame.image[0] as ByteBuffer
            buff.rewind()
            frameBuilder.setLength(0)
            frameBuilder.append(ANSI.GOTO_HOME)
            if (barEnabled) {
                frameBuilder.append(ANSI.CLEAR_LINE)
                val now = System.currentTimeMillis()
                if (now - lastSyncStamp < 1000) {
                    frameBuilder.append(ANSI.fgRgb(255, 0, 0)).append("LAG")
                } else {
                    frameBuilder.append(ANSI.fgRgb(0, 255, 0)).append("OK")
                }
                frameBuilder.append(ANSI.fgRgb(255, 255, 255))
                    .append(" | ")
                val currentFps = 1.0 / ((now - lastFrame) / 1000.0)
                frameBuilder
                    .append(String.format("%05.2f", currentFps))
                    .append('/')
                    .append(fpsStr)
                    .append(" fps | ")
                    .append(String.format("%05.2f", ((bytesWrite * currentFps * 8) / 1_000_000)))
                    .append(" Mbps | ")

                frameBuilder.append('\n')
            }
            repeat(padTop) { frameBuilder.append('\n') }
            repeat(h) {
                repeat(padLeft) { frameBuilder.append(' ') }
                repeat(w) {
                    val b = buff.get().toInt() and 0xFF
                    val g = buff.get().toInt() and 0xFF
                    val r = buff.get().toInt() and 0xFF
                    val l = 0.299 * r + 0.587 * g + 0.114 * b
                    val lNorm = l / 255.0
                    val char = CHARS[(lNorm * (CHARS.length - 1)).toInt()]
                    frameBuilder.append(ANSI.fgRgb(r, g, b)).append(char)
                }
                buff.position(buff.position() + stridePad)
                frameBuilder.append('\n')
            }
            val frameStr = frameBuilder.toString()
            bytesWrite = frameStr.toByteArray().size
            print(frameStr)
        }
    }
}

fun readInputPath(): File {
    while (true) {
        val path = reader.readLine("video path> ")
        val file = File(path)
        if (!file.exists()) {
            terminal.puts(InfoCmp.Capability.bell)
            println("File does not exist")
            continue
        }
        if (!file.isFile) {
            terminal.puts(InfoCmp.Capability.bell)
            println("Not a file")
            continue
        }
        if (!file.canRead()) {
            terminal.puts(InfoCmp.Capability.bell)
            println("File unreadable")
            continue
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
            continue
        }
        return file
    }
}