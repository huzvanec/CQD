package cz.jeme.cqd.util

object ANSI {
    val sb = StringBuilder(32) // THREAD UNSAFE

    val GOTO_HOME = goto(0, 0)

    fun goto(x: Int, y: Int): String {
        sb.setLength(0)
        sb.append("\u001B[")
            .append(y + 1).append(";")
            .append(x + 1).append("H")
        return sb.toString()
    }
    
    val RESET_COLOR = "\u001B[0m"
    val CLEAR_LINE = "\u001B[2K"

    fun fgRgb(r: Int, g: Int, b: Int): String {
        sb.setLength(0)
        sb.append("\u001B[38;2;")
            .append(r).append(';')
            .append(g).append(';')
            .append(b).append('m')
        return sb.toString()
    }
}