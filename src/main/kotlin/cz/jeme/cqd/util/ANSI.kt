package cz.jeme.cqd.util

object ANSI {
    const val ESC = 27.toChar()

    const val ENABLE_ALTERNATE_SCREEN_BUFFER = "$ESC[?1049h"
    const val DISABLE_ALTERNATE_SCREEN_BUFFER = "$ESC[?1049l"

    fun goto(x: Int, y: Int): String = "${ESC}[${x};${y}H"
}