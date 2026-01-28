package cz.jeme.cqd.renderer

import cz.jeme.cqd.util.ANSI

class DarkenedMultiCharRenderer(
    name: String,
    val char: Char,
    val darkness: Int,
    val mode: Mode
) : CharRenderer(name) {
    val isLuma = when (mode) {
        // exhaustive
        Mode.LUMA -> true
        Mode.STATIC -> false
    }

    override fun render(frame: StringBuilder, r: Int, g: Int, b: Int) {
        val multiplier = if (isLuma) ANSI.normLumaFromRgb(r, g, b) else 1.0

        val darken = (darkness * multiplier).toInt()

        frame
            .append(ANSI.bgRgb(r, g, b))
            .append(
                ANSI.fgRgb(
                    (r - darken).coerceIn(0, 255),
                    (g - darken).coerceIn(0, 255),
                    (b - darken).coerceIn(0, 255)
                )
            )
            .append(char)
    }

    enum class Mode {
        LUMA,
        STATIC
    }
}