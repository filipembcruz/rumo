package io.github.filipembcruz.rumo.data

import android.content.Context
import android.content.pm.ApplicationInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Category of an app or website. [score]: +1 productive, 0 neutral, -1 distracting.
 */
enum class Cat(val id: String, val color: Long, val score: Int) {
    SOCIAL("social", 0xFFFF5C8A, -1),
    VIDEO("video", 0xFFFF7A45, -1),
    GAMES("games", 0xFFFFB020, -1),
    SHOPPING("shopping", 0xFFE8C547, -1),
    ADULT("adult", 0xFFD9466F, -1),
    NEWS("news", 0xFF4FB3FF, 0),
    COMMUNICATION("communication", 0xFF22C3A6, 0),
    MUSIC("music", 0xFF2ED47A, 0),
    BROWSER("browser", 0xFF7D8BFF, 0),
    UTILITIES("utilities", 0xFF9AA0B8, 0),
    FINANCE("finance", 0xFF14B8C4, 0),
    OTHER("other", 0xFFB9BDD0, 0),
    PRODUCTIVITY("productivity", 0xFF6B4EFF, 1),
    EDUCATION("education", 0xFF8BD63A, 1),
    READING("reading", 0xFFA77BFF, 1),
    HEALTH("health", 0xFF3CCB8E, 1),
    CREATIVITY("creativity", 0xFFD16BFF, 1);

    val distracting get() = score < 0
    val productive get() = score > 0

    companion object {
        fun of(id: String?): Cat = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

object KnownCategories {
    val browsers = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.brave.browser", "com.brave.browser_beta",
        "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.focus", "com.microsoft.emmx",
        "com.sec.android.app.sbrowser", "com.opera.browser", "com.opera.mini.native", "com.opera.gx",
        "com.duckduckgo.mobile.android", "com.vivaldi.browser", "com.kiwibrowser.browser", "com.yandex.browser",
        "com.mi.globalbrowser", "com.UCMobile.intl", "app.vanadium.browser", "org.bromite.bromite",
        "com.ecosia.android",
    )

    val apps: Map<String, Cat> = mapOf(
        "com.instagram.android" to Cat.SOCIAL, "com.instagram.barcelona" to Cat.SOCIAL,
        "com.facebook.katana" to Cat.SOCIAL, "com.facebook.lite" to Cat.SOCIAL,
        "com.twitter.android" to Cat.SOCIAL, "com.reddit.frontpage" to Cat.SOCIAL,
        "com.zhiliaoapp.musically" to Cat.SOCIAL, "com.ss.android.ugc.trill" to Cat.SOCIAL,
        "com.snapchat.android" to Cat.SOCIAL, "com.pinterest" to Cat.SOCIAL,
        "com.linkedin.android" to Cat.SOCIAL, "com.tumblr" to Cat.SOCIAL, "com.bereal.ft" to Cat.SOCIAL,
        "com.kwai.video" to Cat.SOCIAL, "com.kwai.bulldog" to Cat.SOCIAL, "xyz.blueskyweb.app" to Cat.SOCIAL,
        "com.google.android.youtube" to Cat.VIDEO, "com.google.android.apps.youtube.music" to Cat.MUSIC,
        "com.netflix.mediaclient" to Cat.VIDEO, "com.amazon.avod.thirdpartyclient" to Cat.VIDEO,
        "com.disney.disneyplus" to Cat.VIDEO, "com.hbo.hbonow" to Cat.VIDEO, "com.wbd.stream" to Cat.VIDEO,
        "tv.twitch.android.app" to Cat.VIDEO, "com.globo.globotv" to Cat.VIDEO, "br.com.globo.globoplay" to Cat.VIDEO,
        "com.crunchyroll.crunchyroid" to Cat.VIDEO, "com.google.android.apps.youtube.kids" to Cat.VIDEO,
        "com.spotify.music" to Cat.MUSIC, "deezer.android.app" to Cat.MUSIC, "com.soundcloud.android" to Cat.MUSIC,
        "com.whatsapp" to Cat.COMMUNICATION, "com.whatsapp.w4b" to Cat.COMMUNICATION,
        "org.telegram.messenger" to Cat.COMMUNICATION, "com.discord" to Cat.COMMUNICATION,
        "com.facebook.orca" to Cat.COMMUNICATION, "org.thoughtcrime.securesms" to Cat.COMMUNICATION,
        "com.google.android.gm" to Cat.COMMUNICATION, "com.microsoft.office.outlook" to Cat.COMMUNICATION,
        "com.google.android.apps.messaging" to Cat.COMMUNICATION, "com.Slack" to Cat.PRODUCTIVITY,
        "com.microsoft.teams" to Cat.PRODUCTIVITY, "us.zoom.videomeetings" to Cat.COMMUNICATION,
        "com.google.android.dialer" to Cat.COMMUNICATION, "com.samsung.android.dialer" to Cat.COMMUNICATION,
        "com.duolingo" to Cat.EDUCATION, "com.babbel.mobile.android.en" to Cat.EDUCATION,
        "org.coursera.android" to Cat.EDUCATION, "com.udemy.android" to Cat.EDUCATION,
        "org.khanacademy.android" to Cat.EDUCATION, "com.memrise.android.memrisecompanionapp" to Cat.EDUCATION,
        "com.ichi2.anki" to Cat.EDUCATION, "com.quizlet.quizletandroid" to Cat.EDUCATION,
        "com.amazon.kindle" to Cat.READING, "com.google.android.apps.books" to Cat.READING,
        "com.medium.reader" to Cat.READING, "com.audible.application" to Cat.READING,
        "com.getpocket.android" to Cat.READING, "com.blinkslabs.blinkist.android" to Cat.READING,
        "notion.id" to Cat.PRODUCTIVITY, "com.todoist" to Cat.PRODUCTIVITY, "com.ticktick.task" to Cat.PRODUCTIVITY,
        "com.google.android.keep" to Cat.PRODUCTIVITY, "com.google.android.apps.docs" to Cat.PRODUCTIVITY,
        "com.google.android.apps.docs.editors.docs" to Cat.PRODUCTIVITY,
        "com.google.android.apps.docs.editors.sheets" to Cat.PRODUCTIVITY,
        "com.google.android.calendar" to Cat.PRODUCTIVITY, "com.microsoft.office.word" to Cat.PRODUCTIVITY,
        "md.obsidian" to Cat.PRODUCTIVITY, "com.evernote" to Cat.PRODUCTIVITY, "com.trello" to Cat.PRODUCTIVITY,
        "com.github.android" to Cat.PRODUCTIVITY, "com.openai.chatgpt" to Cat.PRODUCTIVITY,
        "com.anthropic.claude" to Cat.PRODUCTIVITY, "com.google.android.apps.bard" to Cat.PRODUCTIVITY,
        "com.amazon.mShop.android.shopping" to Cat.SHOPPING, "com.mercadolibre" to Cat.SHOPPING,
        "com.shopee.br" to Cat.SHOPPING, "com.alibaba.aliexpresshd" to Cat.SHOPPING, "com.einnovation.temu" to Cat.SHOPPING,
        "com.zzkko" to Cat.SHOPPING, "com.magazineluiza.app" to Cat.SHOPPING, "br.com.americanas" to Cat.SHOPPING,
        "com.nu.production" to Cat.FINANCE, "com.itau" to Cat.FINANCE, "br.com.intermedium" to Cat.FINANCE,
        "com.mercadopago.wallet" to Cat.FINANCE, "com.binance.dev" to Cat.FINANCE, "com.picpay" to Cat.FINANCE,
        "com.strava" to Cat.HEALTH, "com.google.android.apps.fitness" to Cat.HEALTH,
        "com.calm.android" to Cat.HEALTH, "com.getsomeheadspace.android" to Cat.HEALTH,
        "com.myfitnesspal.android" to Cat.HEALTH, "com.nike.ntc" to Cat.HEALTH,
        "com.google.android.apps.maps" to Cat.UTILITIES, "com.waze" to Cat.UTILITIES,
        "com.ubercab" to Cat.UTILITIES, "com.google.android.apps.photos" to Cat.CREATIVITY,
        "com.canva.editor" to Cat.CREATIVITY, "com.adobe.lrmobile" to Cat.CREATIVITY,
        "com.google.android.googlequicksearchbox" to Cat.UTILITIES, "com.android.settings" to Cat.UTILITIES,
        "com.google.android.deskclock" to Cat.UTILITIES, "com.android.vending" to Cat.UTILITIES,
        "com.google.android.apps.translate" to Cat.EDUCATION, "com.ifood.webapp" to Cat.SHOPPING,
        "br.com.brainweb.ifood" to Cat.SHOPPING,
    )

    /** Domain suffix → category. Matched against the registrable domain and its parents. */
    val sites: Map<String, Cat> = mapOf(
        "instagram.com" to Cat.SOCIAL, "facebook.com" to Cat.SOCIAL, "x.com" to Cat.SOCIAL, "twitter.com" to Cat.SOCIAL,
        "reddit.com" to Cat.SOCIAL, "tiktok.com" to Cat.SOCIAL, "linkedin.com" to Cat.SOCIAL, "pinterest.com" to Cat.SOCIAL,
        "tumblr.com" to Cat.SOCIAL, "threads.net" to Cat.SOCIAL, "bsky.app" to Cat.SOCIAL, "9gag.com" to Cat.SOCIAL,
        "quora.com" to Cat.SOCIAL, "snapchat.com" to Cat.SOCIAL, "kwai.com" to Cat.SOCIAL,
        "youtube.com" to Cat.VIDEO, "youtu.be" to Cat.VIDEO, "netflix.com" to Cat.VIDEO, "twitch.tv" to Cat.VIDEO,
        "primevideo.com" to Cat.VIDEO, "disneyplus.com" to Cat.VIDEO, "max.com" to Cat.VIDEO, "vimeo.com" to Cat.VIDEO,
        "globoplay.globo.com" to Cat.VIDEO, "crunchyroll.com" to Cat.VIDEO, "dailymotion.com" to Cat.VIDEO,
        "spotify.com" to Cat.MUSIC, "deezer.com" to Cat.MUSIC, "soundcloud.com" to Cat.MUSIC,
        "steampowered.com" to Cat.GAMES, "epicgames.com" to Cat.GAMES, "roblox.com" to Cat.GAMES, "chess.com" to Cat.GAMES,
        "lichess.org" to Cat.GAMES, "poki.com" to Cat.GAMES, "miniclip.com" to Cat.GAMES, "ign.com" to Cat.GAMES,
        "amazon.com" to Cat.SHOPPING, "amazon.com.br" to Cat.SHOPPING, "mercadolivre.com.br" to Cat.SHOPPING,
        "mercadolibre.com" to Cat.SHOPPING, "shopee.com.br" to Cat.SHOPPING, "aliexpress.com" to Cat.SHOPPING,
        "temu.com" to Cat.SHOPPING, "shein.com" to Cat.SHOPPING, "ebay.com" to Cat.SHOPPING, "magazineluiza.com.br" to Cat.SHOPPING,
        "americanas.com.br" to Cat.SHOPPING, "etsy.com" to Cat.SHOPPING, "olx.com.br" to Cat.SHOPPING, "ifood.com.br" to Cat.SHOPPING,
        "g1.globo.com" to Cat.NEWS, "globo.com" to Cat.NEWS, "uol.com.br" to Cat.NEWS, "folha.uol.com.br" to Cat.NEWS,
        "estadao.com.br" to Cat.NEWS, "cnn.com" to Cat.NEWS, "cnnbrasil.com.br" to Cat.NEWS, "bbc.com" to Cat.NEWS,
        "bbc.co.uk" to Cat.NEWS, "nytimes.com" to Cat.NEWS, "theguardian.com" to Cat.NEWS, "reuters.com" to Cat.NEWS,
        "news.google.com" to Cat.NEWS, "terra.com.br" to Cat.NEWS, "r7.com" to Cat.NEWS, "ge.globo.com" to Cat.NEWS,
        "news.ycombinator.com" to Cat.NEWS, "theverge.com" to Cat.NEWS, "techcrunch.com" to Cat.NEWS, "tecmundo.com.br" to Cat.NEWS,
        "whatsapp.com" to Cat.COMMUNICATION, "web.whatsapp.com" to Cat.COMMUNICATION, "mail.google.com" to Cat.COMMUNICATION,
        "outlook.live.com" to Cat.COMMUNICATION, "outlook.office.com" to Cat.COMMUNICATION, "discord.com" to Cat.COMMUNICATION,
        "telegram.org" to Cat.COMMUNICATION, "web.telegram.org" to Cat.COMMUNICATION, "messenger.com" to Cat.COMMUNICATION,
        "github.com" to Cat.PRODUCTIVITY, "gitlab.com" to Cat.PRODUCTIVITY, "stackoverflow.com" to Cat.PRODUCTIVITY,
        "notion.so" to Cat.PRODUCTIVITY, "docs.google.com" to Cat.PRODUCTIVITY, "drive.google.com" to Cat.PRODUCTIVITY,
        "calendar.google.com" to Cat.PRODUCTIVITY, "figma.com" to Cat.CREATIVITY, "trello.com" to Cat.PRODUCTIVITY,
        "slack.com" to Cat.PRODUCTIVITY, "chatgpt.com" to Cat.PRODUCTIVITY, "claude.ai" to Cat.PRODUCTIVITY,
        "gemini.google.com" to Cat.PRODUCTIVITY, "office.com" to Cat.PRODUCTIVITY, "atlassian.net" to Cat.PRODUCTIVITY,
        "developer.android.com" to Cat.PRODUCTIVITY, "developer.mozilla.org" to Cat.PRODUCTIVITY, "vercel.com" to Cat.PRODUCTIVITY,
        "wikipedia.org" to Cat.EDUCATION, "duolingo.com" to Cat.EDUCATION, "coursera.org" to Cat.EDUCATION,
        "udemy.com" to Cat.EDUCATION, "khanacademy.org" to Cat.EDUCATION, "edx.org" to Cat.EDUCATION,
        "alura.com.br" to Cat.EDUCATION, "brasilescola.uol.com.br" to Cat.EDUCATION, "scholar.google.com" to Cat.EDUCATION,
        "translate.google.com" to Cat.EDUCATION, "deepl.com" to Cat.EDUCATION, "w3schools.com" to Cat.EDUCATION,
        "medium.com" to Cat.READING, "substack.com" to Cat.READING, "goodreads.com" to Cat.READING, "kindle.amazon.com" to Cat.READING,
        "nubank.com.br" to Cat.FINANCE, "itau.com.br" to Cat.FINANCE, "binance.com" to Cat.FINANCE, "investing.com" to Cat.FINANCE,
        "tradingview.com" to Cat.FINANCE, "infomoney.com.br" to Cat.FINANCE,
        "google.com" to Cat.UTILITIES, "google.com.br" to Cat.UTILITIES, "bing.com" to Cat.UTILITIES,
        "duckduckgo.com" to Cat.UTILITIES, "maps.google.com" to Cat.UTILITIES,
        "strava.com" to Cat.HEALTH, "myfitnesspal.com" to Cat.HEALTH,
    )

    /** Well-known adult (+18) sites; matched on the domain and its parents. */
    private val adultSites = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com", "youporn.com", "tube8.com", "spankbang.com",
        "brazzers.com", "onlyfans.com", "fansly.com", "chaturbate.com", "stripchat.com", "bongacams.com", "livejasmin.com",
        "cam4.com", "camsoda.com", "myfreecams.com", "eporner.com", "txxx.com", "hqporner.com", "porntrex.com", "beeg.com",
        "tnaflix.com", "motherless.com", "rule34.xxx", "e-hentai.org", "nhentai.net", "hentaihaven.xxx", "hanime.tv",
        "manyvids.com", "clips4sale.com", "erome.com", "fapello.com", "thothub.to", "sxyprn.com", "porn.com", "xxx.com",
        "sex.com", "adultfriendfinder.com", "literotica.com", "f95zone.to", "mrdeepfakes.com", "camwhores.tv", "redgifs.com",
        "privacy.com.br", "xvideos.red", "pornpics.com", "imagefap.com", "4tube.com", "drtuber.com", "sunporno.com",
    )
    private val adultTlds = setOf("xxx", "porn", "sex", "adult")
    private val adultWords = listOf("porn", "xvideo", "xnxx", "hentai", "xhamster", "camgirl", "nsfw", "onlyfans", "putaria", "sexcam")

    fun isAdult(domain: String): Boolean {
        val d = domain.lowercase()
        if (d.substringAfterLast('.') in adultTlds) return true
        if (adultWords.any { d.contains(it) }) return true
        var x = d
        while (true) {
            if (x in adultSites) return true
            val dot = x.indexOf('.')
            if (dot < 0 || x.indexOf('.', dot + 1) < 0) return false
            x = x.substring(dot + 1)
        }
    }

    fun siteCat(domain: String): Cat? {
        if (isAdult(domain)) return Cat.ADULT
        var d = domain
        while (true) {
            sites[d]?.let { return it }
            val dot = d.indexOf('.')
            if (dot < 0 || d.indexOf('.', dot + 1) < 0) return null
            d = d.substring(dot + 1)
        }
    }
}

/** Resolves categories with user overrides on top of the built-in tables. */
object Categorizer {
    private val appCache = ConcurrentHashMap<String, Cat>()
    private val siteCache = ConcurrentHashMap<String, Cat>()
    @Volatile private var overrides: Map<String, String>? = null

    fun invalidate() {
        appCache.clear(); siteCache.clear(); overrides = null
    }

    private fun ov(context: Context): Map<String, String> =
        overrides ?: Db.get(context).overrides().also { overrides = it }

    fun app(context: Context, pkg: String): Cat = appCache.getOrPut(pkg) {
        ov(context)["app:$pkg"]?.let { return@getOrPut Cat.of(it) }
        if (pkg in KnownCategories.browsers) return@getOrPut Cat.BROWSER
        KnownCategories.apps[pkg]?.let { return@getOrPut it }
        val info = runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.getOrNull()
        when (info?.category) {
            ApplicationInfo.CATEGORY_GAME -> Cat.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> Cat.MUSIC
            ApplicationInfo.CATEGORY_VIDEO -> Cat.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> Cat.CREATIVITY
            ApplicationInfo.CATEGORY_SOCIAL -> Cat.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> Cat.NEWS
            ApplicationInfo.CATEGORY_MAPS -> Cat.UTILITIES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Cat.PRODUCTIVITY
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> Cat.UTILITIES
            else -> if (info != null && (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) Cat.UTILITIES else Cat.OTHER
        }
    }

    fun site(context: Context, domain: String): Cat = siteCache.getOrPut(domain) {
        ov(context)["site:$domain"]?.let { return@getOrPut Cat.of(it) }
        KnownCategories.siteCat(domain) ?: Cat.OTHER
    }

    fun isBrowser(pkg: String) = pkg in KnownCategories.browsers
}
