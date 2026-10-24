package io.github.filipembcruz.rumo.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import io.github.filipembcruz.rumo.core.Prefs
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Website icons, downloaded directly from each site (never through a third-party service)
 * and cached on the phone. Can be turned off in Settings.
 */
object SiteIcons {
    private const val PX = 128
    private const val RETRY_MS = 3L * 24 * 3600_000L
    private val mem = LruCache<String, Bitmap>(80)
    private val missing = ConcurrentHashMap<String, Long>()

    private fun key(domain: String) = domain.lowercase().removePrefix("www.").removePrefix("m.").removePrefix("mobile.")
    private fun dir(ctx: Context) = File(ctx.cacheDir, "site_icons").apply { mkdirs() }
    private fun file(ctx: Context, k: String) = File(dir(ctx), k.replace(Regex("[^a-z0-9.-]"), "_") + ".png")
    private fun missFile(ctx: Context, k: String) = File(dir(ctx), k.replace(Regex("[^a-z0-9.-]"), "_") + ".none")

    /** Cached icon only — never touches the network. Safe on any thread. */
    fun cached(ctx: Context, domain: String): Bitmap? {
        val k = key(domain)
        mem.get(k)?.let { return it }
        val f = file(ctx, k)
        if (!f.exists()) return null
        return runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull()?.also { mem.put(k, it) }
    }

    /** Cached icon, or downloads it from the site. Call off the main thread. */
    fun get(ctx: Context, domain: String): Bitmap? {
        cached(ctx, domain)?.let { return it }
        if (!Prefs.get(ctx).siteIcons) return null
        val k = key(domain)
        val now = System.currentTimeMillis()
        if ((missing[k] ?: 0L) > now) return null
        val mf = missFile(ctx, k)
        if (mf.exists() && now - mf.lastModified() < RETRY_MS) { missing[k] = mf.lastModified() + RETRY_MS; return null }
        val bmp = runCatching { fetch(k) }.getOrNull()
        if (bmp == null) {
            missing[k] = now + RETRY_MS
            runCatching { mf.writeText("") ; mf.setLastModified(now) }
            return null
        }
        val scaled = Bitmap.createScaledBitmap(bmp, PX, PX, true)
        runCatching { file(ctx, k).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        mf.delete()
        mem.put(k, scaled)
        return scaled
    }

    fun clear(ctx: Context) {
        mem.evictAll(); missing.clear()
        dir(ctx).listFiles()?.forEach { it.delete() }
    }

    private fun fetch(host: String): Bitmap? {
        val hosts = buildList {
            add(host)
            val parts = host.split('.')
            if (parts.size > 2) add(parts.takeLast(if (parts[parts.size - 2].length <= 3 && parts.last().length == 2) 3 else 2).joinToString("."))
        }.distinct()
        for (h in hosts) {
            val base = "https://$h"
            // 1. icon declared in the page
            val html = runCatching { download("$base/", 256 * 1024)?.toString(Charsets.UTF_8) }.getOrNull()
            if (html != null) {
                for (href in iconLinks(html)) {
                    val url = resolve(base, href) ?: continue
                    decode(url)?.let { return it }
                }
            }
            // 2. conventional locations
            for (p in listOf("/apple-touch-icon.png", "/favicon.ico", "/favicon.png")) {
                decode("$base$p")?.let { return it }
            }
        }
        return null
    }

    private fun decode(url: String): Bitmap? {
        val bytes = runCatching { download(url, 512 * 1024) }.getOrNull() ?: return null
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return if (b.width >= 16 && b.height >= 16) b else null
    }

    private val linkRe = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val relRe = Regex("rel\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val hrefRe = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val sizesRe = Regex("sizes\\s*=\\s*[\"'](\\d+)x\\d+", RegexOption.IGNORE_CASE)

    /** Icon hrefs from the page, best first (apple-touch-icon, then largest declared size). */
    private fun iconLinks(html: String): List<String> =
        linkRe.findAll(html.take(200_000)).mapNotNull { m ->
            val tag = m.value
            val rel = relRe.find(tag)?.groupValues?.get(1)?.lowercase() ?: return@mapNotNull null
            if ("icon" !in rel) return@mapNotNull null
            val href = hrefRe.find(tag)?.groupValues?.get(1) ?: return@mapNotNull null
            if (href.endsWith(".svg", true) || href.startsWith("data:")) return@mapNotNull null
            val size = sizesRe.find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 32
            val score = (if ("apple-touch" in rel) 10_000 else 0) + size.coerceAtMost(512)
            score to href.replace("&amp;", "&")
        }.sortedByDescending { it.first }.map { it.second }.distinct().take(4).toList()

    private fun resolve(base: String, href: String): String? = runCatching {
        when {
            href.startsWith("//") -> "https:$href"
            href.startsWith("http://") || href.startsWith("https://") -> href
            else -> URL(URL("$base/"), href).toString()
        }
    }.getOrNull()

    private fun download(url: String, max: Int, redirects: Int = 4): ByteArray? {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 6000; c.readTimeout = 6000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36")
            c.setRequestProperty("Accept", "*/*")
            val code = c.responseCode
            if (code in 300..399 && redirects > 0) {
                val loc = c.getHeaderField("Location") ?: return null
                val next = URL(URL(url), loc).toString()
                if (!next.startsWith("https://")) return null
                return download(next, max, redirects - 1)
            }
            if (code != 200) return null
            c.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf); if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > max) break
                }
                return out.toByteArray()
            }
        } finally {
            c.disconnect()
        }
    }
}
