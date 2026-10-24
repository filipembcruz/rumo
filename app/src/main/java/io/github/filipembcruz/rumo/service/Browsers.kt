package io.github.filipembcruz.rumo.service

import android.view.accessibility.AccessibilityNodeInfo

/** Reads the address bar of common Android browsers. */
object Browsers {
    private val urlBarIds: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("url_bar"),
        "com.chrome.beta" to listOf("url_bar"),
        "com.chrome.dev" to listOf("url_bar"),
        "com.brave.browser" to listOf("url_bar"),
        "com.brave.browser_beta" to listOf("url_bar"),
        "com.microsoft.emmx" to listOf("url_bar"),
        "com.kiwibrowser.browser" to listOf("url_bar"),
        "com.vivaldi.browser" to listOf("url_bar"),
        "org.bromite.bromite" to listOf("url_bar"),
        "app.vanadium.browser" to listOf("url_bar"),
        "com.ecosia.android" to listOf("url_bar"),
        "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
        "org.mozilla.firefox_beta" to listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
        "org.mozilla.focus" to listOf("mozac_browser_toolbar_url_view", "display_url"),
        "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text", "custom_tab_toolbar_url_bar_text"),
        "com.opera.browser" to listOf("url_field"),
        "com.opera.mini.native" to listOf("url_field"),
        "com.opera.gx" to listOf("url_field"),
        "com.duckduckgo.mobile.android" to listOf("omnibarTextInput"),
        "com.yandex.browser" to listOf("bro_omnibar_address_title_text"),
        "com.mi.globalbrowser" to listOf("url"),
        "com.UCMobile.intl" to listOf("titlebar_url"),
    )

    fun isSupported(pkg: String) = pkg in urlBarIds

    /** Returns the current domain, or null when it can't be read (or the user is typing in the bar). */
    fun readDomain(root: AccessibilityNodeInfo?, pkg: String): String? {
        root ?: return null
        val ids = urlBarIds[pkg] ?: return null
        for (id in ids) {
            val nodes = runCatching { root.findAccessibilityNodeInfosByViewId("$pkg:id/$id") }.getOrNull().orEmpty()
            for (n in nodes) {
                if (n.isFocused) return null // typing a new address: not a visit yet
                val text = n.text?.toString() ?: n.contentDescription?.toString()
                val d = domainOf(text)
                if (d != null) return d
            }
        }
        return null
    }

    private val domainRegex = Regex("^(?:[a-z][a-z0-9+.-]*://)?([^/:?#\\s]+)")

    fun domainOf(raw: String?): String? {
        val text = raw?.trim()?.lowercase() ?: return null
        if (text.isEmpty() || text.contains(' ')) return null
        val host = domainRegex.find(text)?.groupValues?.getOrNull(1) ?: return null
        if (!host.contains('.') || host.startsWith('.') || host.endsWith('.')) return null
        val clean = host.removePrefix("www.").removePrefix("m.").removePrefix("mobile.")
        if (clean.length < 4 || clean.all { it.isDigit() || it == '.' }) return null
        return clean
    }
}
