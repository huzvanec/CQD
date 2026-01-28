package cz.jeme.cqd.renderer

abstract class CharRenderer(val name: String) {
    abstract fun render(frame: StringBuilder, r: Int, g: Int, b: Int)
}
