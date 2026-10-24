package io.github.filipembcruz.rumo.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/** Per-app usage for one day while collecting. */
class AppDay(var ms: Long = 0, var opens: Int = 0)

/** Local-only storage. Days are ISO strings (yyyy-MM-dd) so they sort as text. */
class Db private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "rumo.db", null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        TABLES.forEach { db.execSQL(it) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf("usage", "daily", "hourly", "web", "limits", "schedules", "focus", "activity", "goals", "overrides", "pause_apps")
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    private inline fun <T> q(sql: String, args: Array<String> = emptyArray(), f: (Cursor) -> T): T =
        readableDatabase.rawQuery(sql, args).use(f)

    private fun Cursor.longs(): List<Long> = buildList { while (moveToNext()) add(getLong(0)) }

    // ------------------------------------------------------------------ usage
    fun replaceDay(day: String, apps: Map<String, AppDay>, hourly: Map<String, LongArray>, unlocks: Int, firstUse: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("usage", "day = ?", arrayOf(day))
            db.delete("hourly", "day = ?", arrayOf(day))
            for ((pkg, d) in apps) {
                if (d.ms <= 0 && d.opens <= 0) continue
                db.insert("usage", null, ContentValues().apply {
                    put("day", day); put("pkg", pkg); put("ms", d.ms); put("opens", d.opens)
                })
            }
            for ((pkg, arr) in hourly) for (h in 0 until 24) if (arr[h] > 0) {
                db.insert("hourly", null, ContentValues().apply {
                    put("day", day); put("hour", h); put("pkg", pkg); put("ms", arr[h])
                })
            }
            db.insertWithOnConflict("daily", null, ContentValues().apply {
                put("day", day); put("unlocks", unlocks); put("first_use", firstUse)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun hasDay(day: String): Boolean = q("SELECT 1 FROM daily WHERE day = ?", arrayOf(day)) { it.moveToFirst() }

    fun firstDay(): String? = q("SELECT MIN(day) FROM daily") { if (it.moveToFirst()) it.getString(0) else null }

    fun totalsByDay(from: String, to: String): Map<String, Long> =
        q("SELECT day, SUM(ms) FROM usage WHERE day BETWEEN ? AND ? GROUP BY day", arrayOf(from, to)) { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getLong(1)) }
        }

    fun appTotals(from: String, to: String): List<Triple<String, Long, Int>> =
        q("SELECT pkg, SUM(ms) t, SUM(opens) FROM usage WHERE day BETWEEN ? AND ? GROUP BY pkg ORDER BY t DESC", arrayOf(from, to)) { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getLong(1), c.getInt(2))) }
        }

    /** day -> (pkg -> ms) for a range. */
    fun appByDay(from: String, to: String): Map<String, Map<String, Long>> =
        q("SELECT day, pkg, ms FROM usage WHERE day BETWEEN ? AND ?", arrayOf(from, to)) { c ->
            val out = HashMap<String, HashMap<String, Long>>()
            while (c.moveToNext()) out.getOrPut(c.getString(0)) { HashMap() }[c.getString(1)] = c.getLong(2)
            out
        }

    fun appDaily(pkg: String, from: String, to: String): Map<String, Pair<Long, Int>> =
        q("SELECT day, ms, opens FROM usage WHERE pkg = ? AND day BETWEEN ? AND ?", arrayOf(pkg, from, to)) { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getLong(1) to c.getInt(2)) }
        }

    /** day, hour, ms summed over all apps (or one app when [pkg] is given). */
    fun hourly(from: String, to: String, pkg: String? = null): List<Triple<String, Int, Long>> {
        val sql = if (pkg == null) "SELECT day, hour, SUM(ms) FROM hourly WHERE day BETWEEN ? AND ? GROUP BY day, hour"
        else "SELECT day, hour, SUM(ms) FROM hourly WHERE day BETWEEN ? AND ? AND pkg = ? GROUP BY day, hour"
        val args = if (pkg == null) arrayOf(from, to) else arrayOf(from, to, pkg)
        return q(sql, args) { c -> buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getInt(1), c.getLong(2))) } }
    }

    fun unlocks(from: String, to: String): Int =
        q("SELECT SUM(unlocks) FROM daily WHERE day BETWEEN ? AND ?", arrayOf(from, to)) { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun firstUse(day: String): Long? =
        q<Long?>("SELECT first_use FROM daily WHERE day = ?", arrayOf(day)) { c -> if (c.moveToFirst() && c.getLong(0) > 0) c.getLong(0) else null }

    // ------------------------------------------------------------------ web
    fun addWeb(day: String, domain: String, ms: Long, visit: Boolean) {
        val db = writableDatabase
        // UPSERT syntax needs SQLite 3.24 (Android 11+), so update-then-insert instead.
        db.execSQL(
            "UPDATE web SET ms = ms + ?, visits = visits + ? WHERE day = ? AND domain = ?",
            arrayOf<Any>(ms, if (visit) 1 else 0, day, domain)
        )
        db.execSQL(
            "INSERT OR IGNORE INTO web(day, domain, ms, visits) VALUES(?, ?, ?, ?)",
            arrayOf<Any>(day, domain, ms, if (visit) 1 else 0)
        )
    }

    fun webTotals(from: String, to: String): List<Triple<String, Long, Int>> =
        q("SELECT domain, SUM(ms) t, SUM(visits) FROM web WHERE day BETWEEN ? AND ? GROUP BY domain ORDER BY t DESC", arrayOf(from, to)) { c ->
            buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getLong(1), c.getInt(2))) }
        }

    fun webDaily(domain: String, from: String, to: String): Map<String, Pair<Long, Int>> =
        q("SELECT day, ms, visits FROM web WHERE domain = ? AND day BETWEEN ? AND ?", arrayOf(domain, from, to)) { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getLong(1) to c.getInt(2)) }
        }

    fun webByDay(from: String, to: String): Map<String, Map<String, Long>> =
        q("SELECT day, domain, ms FROM web WHERE day BETWEEN ? AND ?", arrayOf(from, to)) { c ->
            val out = HashMap<String, HashMap<String, Long>>()
            while (c.moveToNext()) out.getOrPut(c.getString(0)) { HashMap() }[c.getString(1)] = c.getLong(2)
            out
        }

    fun hasAnyWeb(): Boolean = q("SELECT 1 FROM web LIMIT 1") { it.moveToFirst() }

    // ------------------------------------------------------------------ limits
    fun limits(): List<Limit> = q("SELECT id, kind, target, minutes, block, enabled FROM limits ORDER BY id") { c ->
        buildList {
            while (c.moveToNext()) add(
                Limit(c.getLong(0), TargetKind.of(c.getString(1)), c.getString(2), c.getInt(3), c.getInt(4) == 1, c.getInt(5) == 1)
            )
        }
    }

    fun saveLimit(l: Limit): Long {
        val cv = ContentValues().apply {
            put("kind", l.kind.id); put("target", l.target); put("minutes", l.minutes)
            put("block", if (l.block) 1 else 0); put("enabled", if (l.enabled) 1 else 0)
        }
        val db = writableDatabase
        return if (l.id > 0) { db.update("limits", cv, "id = ?", arrayOf(l.id.toString())); l.id }
        else {
            db.delete("limits", "kind = ? AND target = ?", arrayOf(l.kind.id, l.target))
            db.insert("limits", null, cv)
        }
    }

    fun deleteLimit(id: Long) { writableDatabase.delete("limits", "id = ?", arrayOf(id.toString())) }

    // ------------------------------------------------------------------ schedules
    fun schedules(): List<Schedule> =
        q("SELECT id, name, start_min, end_min, days, cats, apps, enabled, icon FROM schedules ORDER BY start_min") { c ->
            buildList {
                while (c.moveToNext()) add(
                    Schedule(
                        c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3), c.getInt(4),
                        c.getString(5).split(',').filter { it.isNotBlank() }.toSet(),
                        c.getString(6).split(',').filter { it.isNotBlank() }.toSet(),
                        c.getInt(7) == 1, c.getString(8) ?: "moon",
                    )
                )
            }
        }

    fun saveSchedule(s: Schedule): Long {
        val cv = ContentValues().apply {
            put("name", s.name); put("start_min", s.startMin); put("end_min", s.endMin); put("days", s.days)
            put("cats", s.cats.joinToString(",")); put("apps", s.apps.joinToString(","))
            put("enabled", if (s.enabled) 1 else 0); put("icon", s.icon)
        }
        val db = writableDatabase
        return if (s.id > 0) { db.update("schedules", cv, "id = ?", arrayOf(s.id.toString())); s.id }
        else db.insert("schedules", null, cv)
    }

    fun deleteSchedule(id: Long) { writableDatabase.delete("schedules", "id = ?", arrayOf(id.toString())) }

    // ------------------------------------------------------------------ focus
    fun startFocus(start: Long, plannedMin: Int, goalId: Long?): Long = writableDatabase.insert("focus", null, ContentValues().apply {
        put("start", start); put("end", 0L); put("planned_min", plannedMin); put("completed", 0)
        if (goalId != null && goalId > 0) put("goal_id", goalId)
    })

    fun endFocus(end: Long, completed: Boolean) {
        writableDatabase.execSQL(
            "UPDATE focus SET end = ?, completed = ? WHERE id = (SELECT MAX(id) FROM focus) AND end = 0",
            arrayOf<Any>(end, if (completed) 1 else 0)
        )
    }

    fun focusSessions(fromMs: Long, toMs: Long): List<FocusSession> =
        q("SELECT id, start, end, planned_min, completed, goal_id FROM focus WHERE start BETWEEN ? AND ? ORDER BY start",
            arrayOf(fromMs.toString(), toMs.toString())) { c ->
            buildList {
                while (c.moveToNext()) add(
                    FocusSession(c.getLong(0), c.getLong(1), c.getLong(2), c.getInt(3), c.getInt(4) == 1,
                        if (c.isNull(5)) null else c.getLong(5))
                )
            }
        }

    // ------------------------------------------------------------------ goals & activities
    fun goals(): List<Goal> = q("SELECT id, title, preset, target_hours, created FROM goals ORDER BY id") { c ->
        buildList { while (c.moveToNext()) add(Goal(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getLong(4))) }
    }

    fun saveGoal(g: Goal): Long {
        val cv = ContentValues().apply {
            put("title", g.title); put("preset", g.preset); put("target_hours", g.targetHours); put("created", g.created)
        }
        val db = writableDatabase
        return if (g.id > 0) { db.update("goals", cv, "id = ?", arrayOf(g.id.toString())); g.id }
        else db.insert("goals", null, cv)
    }

    fun deleteGoal(id: Long) { writableDatabase.delete("goals", "id = ?", arrayOf(id.toString())) }

    fun addActivity(a: ActivityLog): Long = writableDatabase.insert("activity", null, ContentValues().apply {
        put("day", a.day); if (a.goalId != null) put("goal_id", a.goalId); put("label", a.label)
        put("minutes", a.minutes); put("ts", a.ts)
    })

    fun deleteActivity(id: Long) { writableDatabase.delete("activity", "id = ?", arrayOf(id.toString())) }

    fun activities(from: String, to: String): List<ActivityLog> =
        q("SELECT id, day, goal_id, label, minutes, ts FROM activity WHERE day BETWEEN ? AND ? ORDER BY ts DESC", arrayOf(from, to)) { c ->
            buildList {
                while (c.moveToNext()) add(
                    ActivityLog(c.getLong(0), c.getString(1), if (c.isNull(2)) null else c.getLong(2), c.getString(3), c.getInt(4), c.getLong(5))
                )
            }
        }

    // ------------------------------------------------------------------ overrides & pause
    fun overrides(): Map<String, String> = q("SELECT kind, target, cat FROM overrides") { c ->
        buildMap { while (c.moveToNext()) put("${c.getString(0)}:${c.getString(1)}", c.getString(2)) }
    }

    fun setOverride(kind: String, target: String, cat: String) {
        writableDatabase.insertWithOnConflict("overrides", null, ContentValues().apply {
            put("kind", kind); put("target", target); put("cat", cat)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun pauseApps(): Set<String> = q("SELECT pkg FROM pause_apps") { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }

    fun setPause(pkg: String, on: Boolean) {
        if (on) writableDatabase.insertWithOnConflict("pause_apps", null, ContentValues().apply { put("pkg", pkg) }, SQLiteDatabase.CONFLICT_IGNORE)
        else writableDatabase.delete("pause_apps", "pkg = ?", arrayOf(pkg))
    }

    // ------------------------------------------------------------------ backup
    fun exportJson(): JSONObject {
        val root = JSONObject()
        for (t in listOf("usage", "daily", "hourly", "web", "limits", "schedules", "focus", "activity", "goals", "overrides", "pause_apps")) {
            val arr = JSONArray()
            readableDatabase.rawQuery("SELECT * FROM $t", null).use { c ->
                while (c.moveToNext()) {
                    val o = JSONObject()
                    for (i in 0 until c.columnCount) {
                        when (c.getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> o.put(c.getColumnName(i), c.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> o.put(c.getColumnName(i), c.getDouble(i))
                            Cursor.FIELD_TYPE_STRING -> o.put(c.getColumnName(i), c.getString(i))
                            else -> {}
                        }
                    }
                    arr.put(o)
                }
            }
            root.put(t, arr)
        }
        return root
    }

    fun importJson(root: JSONObject) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (t in root.keys()) {
                val arr = root.optJSONArray(t) ?: continue
                db.delete(t, null, null)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val cv = ContentValues()
                    for (k in o.keys()) when (val v = o.get(k)) {
                        is Int -> cv.put(k, v.toLong())
                        is Long -> cv.put(k, v)
                        is Double -> cv.put(k, v)
                        is String -> cv.put(k, v)
                        else -> cv.put(k, v.toString())
                    }
                    db.insertWithOnConflict(t, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun wipe() {
        val db = writableDatabase
        listOf("usage", "daily", "hourly", "web", "limits", "schedules", "focus", "activity", "goals", "overrides", "pause_apps")
            .forEach { db.delete(it, null, null) }
    }

    companion object {
        private const val VERSION = 2
        private val TABLES = listOf(
            "CREATE TABLE usage (day TEXT NOT NULL, pkg TEXT NOT NULL, ms INTEGER NOT NULL, opens INTEGER NOT NULL, PRIMARY KEY(day, pkg))",
            "CREATE TABLE hourly (day TEXT NOT NULL, hour INTEGER NOT NULL, pkg TEXT NOT NULL, ms INTEGER NOT NULL, PRIMARY KEY(day, hour, pkg))",
            "CREATE TABLE daily (day TEXT PRIMARY KEY, unlocks INTEGER NOT NULL, first_use INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE web (day TEXT NOT NULL, domain TEXT NOT NULL, ms INTEGER NOT NULL, visits INTEGER NOT NULL, PRIMARY KEY(day, domain))",
            "CREATE TABLE limits (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, target TEXT NOT NULL, minutes INTEGER NOT NULL, block INTEGER NOT NULL, enabled INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE schedules (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, start_min INTEGER NOT NULL, end_min INTEGER NOT NULL, days INTEGER NOT NULL, cats TEXT NOT NULL, apps TEXT NOT NULL, enabled INTEGER NOT NULL, icon TEXT)",
            "CREATE TABLE focus (id INTEGER PRIMARY KEY AUTOINCREMENT, start INTEGER NOT NULL, end INTEGER NOT NULL, planned_min INTEGER NOT NULL, completed INTEGER NOT NULL, goal_id INTEGER)",
            "CREATE TABLE activity (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, goal_id INTEGER, label TEXT NOT NULL, minutes INTEGER NOT NULL, ts INTEGER NOT NULL)",
            "CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, preset TEXT, target_hours REAL NOT NULL, created INTEGER NOT NULL)",
            "CREATE TABLE overrides (kind TEXT NOT NULL, target TEXT NOT NULL, cat TEXT NOT NULL, PRIMARY KEY(kind, target))",
            "CREATE TABLE pause_apps (pkg TEXT PRIMARY KEY)",
        )

        @Volatile private var instance: Db? = null
        fun get(context: Context): Db =
            instance ?: synchronized(this) { instance ?: Db(context).also { instance = it } }
    }
}
