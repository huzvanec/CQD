package cz.jeme.cqd.util

object ANSI {
    private val sb = StringBuilder(32) // THREAD UNSAFE

    val GOTO_HOME = goto(0, 0)

    fun goto(x: Int, y: Int): String {
        sb.setLength(0)
        sb.append("\u001B[")
            .append(y + 1).append(";")
            .append(x + 1).append("H")
        return sb.toString()
    }

    const val RESET_COLOR = "\u001B[0m"
    const val CLEAR_LINE = "\u001B[2K"

    fun fgRgb(r: Int, g: Int, b: Int): String {
        sb.setLength(0)
        sb.append("\u001B[38;2;")
            .append(r).append(';')
            .append(g).append(';')
            .append(b).append('m')
        return sb.toString()
    }

    val FG_RESET = "\u001B[39m"
    
    val FG_RED = fgRgb(255, 0, 0)
    val FG_GREEN = fgRgb(0, 255, 0)
    val FG_YELLOW = fgRgb(255, 255, 0)
    val FG_BLUE = fgRgb(0, 0, 255)
    val FG_MAGENTA = fgRgb(255, 0, 255)
    val FG_CYAN = fgRgb(0, 255, 255)
    val FG_WHITE = fgRgb(255, 255, 255)
    val FG_BLACK = fgRgb(0, 0, 0)

    fun bgRgb(r: Int, g: Int, b: Int): String {
        sb.setLength(0)
        sb.append("\u001B[48;2;")
            .append(r).append(';')
            .append(g).append(';')
            .append(b).append('m')
        return sb.toString()
    }
    
    val BG_RESET = "\u001B[49m"
}