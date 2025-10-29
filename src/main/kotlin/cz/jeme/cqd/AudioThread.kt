package cz.jeme.cqd

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class AudioThread(
    val sampleRate: Float,
    val audioChannels: Int,
    val queueCapacity: Int = 100
) : Thread() {
    val queue = ArrayBlockingQueue<ByteArray>(queueCapacity)
    
    val format = AudioFormat(
        sampleRate,
        16,
        audioChannels,
        true,
        false
    )

    val info = DataLine.Info(SourceDataLine::class.java, format)

    @Volatile
    var running = true

    override fun run() {
        (AudioSystem.getLine(info) as SourceDataLine).use { audioLine ->
            audioLine.open(format)
            audioLine.start()

            while (running) {
                queue.poll(50, TimeUnit.MILLISECONDS)?.let { buf ->
                    audioLine.write(buf, 0, buf.size)
                }
            }

            audioLine.drain()
            audioLine.stop()
        }
    }
}