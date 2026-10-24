package io.github.filipembcruz.rumo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.filipembcruz.rumo.core.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** Local backups: a single JSON file the user saves anywhere (Files, Google Drive…) or shares. */
object Backup {
    fun fileName() = "rumo-backup-${LocalDate.now()}.json"

    fun export(context: Context): String {
        val root = JSONObject()
        root.put("app", "rumo")
        root.put("version", 2)
        root.put("created", System.currentTimeMillis())
        root.put("db", Db.get(context).exportJson())
        val prefs = JSONObject()
        for ((k, v) in Prefs.get(context).exportMap()) when (v) {
            is Set<*> -> prefs.put(k, JSONObject().put("set", JSONArray(v.map { it.toString() })))
            null -> {}
            else -> prefs.put(k, v)
        }
        root.put("prefs", prefs)
        return root.toString()
    }

    fun writeTo(context: Context, uri: Uri) {
        val json = export(context)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
    }

    fun shareIntent(context: Context): Intent {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val f = File(dir, fileName())
        f.writeText(export(context))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Returns true when the file was a valid Rumo backup. */
    fun importFrom(context: Context, uri: Uri): Boolean {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return false
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (root.optString("app") != "rumo") return false
        root.optJSONObject("db")?.let { Db.get(context).importJson(it) }
        root.optJSONObject("prefs")?.let { p ->
            val map = HashMap<String, Any?>()
            for (k in p.keys()) {
                val v = p.get(k)
                map[k] = when (v) {
                    is JSONObject -> v.optJSONArray("set")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    is Int -> if (k in LONG_KEYS) v.toLong() else v
                    else -> v
                }
            }
            Prefs.get(context).importMap(map)
        }
        Categorizer.invalidate()
        return true
    }

    private val LONG_KEYS = setOf("focus_start", "focus_until", "focus_goal")
}
