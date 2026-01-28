package cz.jeme.cqd.renderer

import cz.jeme.cqd.util.ANSI

class BackgroundCharRenderer(
    name: String,
    val char: Char
) : CharRenderer(name) {
    override fun render(frame: StringBuilder, r: Int, g: Int, b: Int) {
        frame
            .append(ANSI.bgRgb(r, g, b))
            .append(char)
    }
}
