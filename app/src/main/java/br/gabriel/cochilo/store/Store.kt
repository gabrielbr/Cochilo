package br.gabriel.cochilo.store

import java.net.URLEncoder

/**
 * Links to the Tatá Slings shop (docs/tata-slings/in-app-marketing.md). Each one opens the browser
 * through an intent, so the app itself needs no internet, and carries UTM tags, so the shop (not the
 * app) can count what the app brings in. The app records nothing about it.
 */
object Store {
    const val HOME = "https://tataslings.com.br/"
    private const val SOURCE = "utm_source=cochilo&utm_medium=app"

    /** The shop's home, for Settings › About (campaign "about"). */
    val about: String = "$HOME?$SOURCE&utm_campaign=about"

    /**
     * Her guide to using the sling safely (positions, the chin-off-the-chest check, videos), for
     * the T.I.C.K.S. sheet's "Tatá's guides": a guide page, never a product list, because safety
     * never sells (in-app-marketing.md §2); campaign "ticks_guide".
     */
    val slingGuide: String = "${HOME}tudo-sobre-o-tata-facil-sling/?$SOURCE&utm_campaign=ticks_guide"

    /**
     * The sling in the fabric [fabricName] (her product name, e.g. "Rosa Chá"), as the shop's own
     * search for "sling <name>", so a renamed or restocked product is still found; campaign
     * "fabric_<key>".
     */
    fun fabric(fabricName: String, key: String): String =
        "${HOME}search/?q=" + URLEncoder.encode("sling $fabricName", "UTF-8") + "&$SOURCE&utm_campaign=fabric_$key"
}
