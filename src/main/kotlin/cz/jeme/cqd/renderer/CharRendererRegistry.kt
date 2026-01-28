package cz.jeme.cqd.renderer

object CharRendererRegistry {
    private val _renderers = mutableListOf<CharRenderer>()
    val renderers: List<CharRenderer> = _renderers

    private inline fun <T : CharRenderer> renderer(init: () -> T): T {
        return init().also { _renderers += it }
    }

    val ASCII_FULL = renderer {
        LumaCharRenderer(
            "ASCII Full",
            " ´`.·-¯¨¸',_¬:ºª°;~\"!¡=¹^+¦÷×²³>l<r|L?¿/\\TFJv(*iz±íIfìjt«)»[]cY47xu}1kîy{ïnZsÍoÌCÝ2eEúVù6Ï5h3¤Þçý9aÎPXSüóAò£UµûÿédÈÉèKñHbqÇöOõpàôá#Gëw¼mÁÊD¢ÀðËÚêÙg©¾M¥½âäþßãæøR®Ä0ÃÓ"
        )
    }

    val ASCII_CLEAN = renderer {
        LumaCharRenderer(
            "ASCII Clean",
            $$" ._-:!?71iIca234db56O089$W%#@Ñ"
        )
    }

    val STRIPED = renderer { ForegroundCharRenderer("Striped", '\u25A0') }

    val PIXELATED = renderer { BackgroundCharRenderer("Pixelated", ' ') }

    val PIXELATED_STAR = renderer { BackgroundCharRenderer("Pixelated Star", '*') }

    val PIXELATED_STAR_ADAPTIVE = renderer {
        DarkenedMultiCharRenderer(
            "Pixelated Star Adaptive",
            '*',
            100,
            DarkenedMultiCharRenderer.Mode.STATIC
        )
    }

    val PIXELATED_STAR_ADAPTIVE_AGRESSIVE = renderer {
        DarkenedMultiCharRenderer(
            "Pixelated Star Adaptive Agressive",
            '*',
            40,
            DarkenedMultiCharRenderer.Mode.STATIC
        )
    }

    val PIXELATED_STAR_ADAPTIVE_DYNAMIC = renderer {
        DarkenedMultiCharRenderer(
            "Pixelated Star Adaptive Luma",
            '*',
            100,
            DarkenedMultiCharRenderer.Mode.LUMA
        )
    }
}
