package cz.jeme.cqd.renderer

import cz.jeme.cqd.util.ANSI

class LumaCharRenderer(
    name: String,
    val chars: String
) : CharRenderer(name) {
    private val maxIndex = chars.length - 1

    override fun render(frame: StringBuilder, r: Int, g: Int, b: Int) {
        val luma = ANSI.lumaFromRgb(r, g, b) / 255.0
        val index = (luma * maxIndex).toInt()
        val char = chars[index]

        frame
            .append(ANSI.fgRgb(r, g, b))
            .append(char)
    }
}
