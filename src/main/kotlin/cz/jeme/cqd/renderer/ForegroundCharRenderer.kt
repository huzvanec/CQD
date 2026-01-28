package cz.jeme.cqd.renderer

import cz.jeme.cqd.util.ANSI

class ForegroundCharRenderer(
    name: String,
    val char: Char
) : CharRenderer(name) {
    override fun render(frame: StringBuilder, r: Int, g: Int, b: Int) {
        frame
            .append(ANSI.fgRgb(r, g, b))
            .append(char)
    }
}
