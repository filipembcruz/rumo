package io.github.filipembcruz.rumo.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Per-app usage for one day. */
data class AppDay(var ms: Long = 0, var opens: Int = 0)

/**
 * Local-only storage of daily usage. Days are stored as ISO strings (yyyy-MM-dd),
 * which sort correctly as text.
 */
class UsageDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "rumo.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE usage (day TEXT NOT NULL, pkg TEXT NOT NULL, ms INTEGER NOT NULL, " +
                "opens INTEGER NOT NULL, PRIMARY KEY(day, pkg))"
        )
        db.execSQL("CREATE TABLE daily (day TEXT PRIMARY KEY, unlocks INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun replaceDay(day: String, apps: Map<String, AppDay>, unlocks: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("usage", "day = ?", arrayOf(day))
            for ((pkg, d) in apps) {
                if (d.ms <= 0) continue
                db.insert("usage", null, ContentValues().apply {
                    put("day", day); put("pkg", pkg); put("ms", d.ms); put("opens", d.opens)
                })
            }
            db.insertWithOnConflict("daily", null, ContentValues().apply {
                put("day", day); put("unlocks", unlocks)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun hasDay(day: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM daily WHERE day = ?", arrayOf(day)).use { it.moveToFirst() }

    fun totalsByDay(from: String, to: String): Map<String, Long> =
        readableDatabase.rawQuery(
            "SELECT day, SUM(ms) FROM usage WHERE day BETWEEN ? AND ? GROUP BY day",
            arrayOf(from, to)
        ).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getLong(1)) }
        }

    fun topApps(from: String, to: String, limit: Int = 50): List<Pair<String, Long>> =
        readableDatabase.rawQuery(
            "SELECT pkg, SUM(ms) AS t FROM usage WHERE day BETWEEN ? AND ? GROUP BY pkg ORDER BY t DESC LIMIT $limit",
            arrayOf(from, to)
        ).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) }
        }

    fun opensByApp(from: String, to: String): Map<String, Int> =
        readableDatabase.rawQuery(
            "SELECT pkg, SUM(opens) FROM usage WHERE day BETWEEN ? AND ? GROUP BY pkg",
            arrayOf(from, to)
        ).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
        }

    fun unlocks(from: String, to: String): Int =
        readableDatabase.rawQuery(
            "SELECT SUM(unlocks) FROM daily WHERE day BETWEEN ? AND ?", arrayOf(from, to)
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun firstDay(): String? =
        readableDatabase.rawQuery("SELECT MIN(day) FROM daily", null)
            .use { c -> if (c.moveToFirst()) c.getString(0) else null }

    companion object {
        @Volatile private var instance: UsageDb? = null
        fun get(context: Context): UsageDb =
            instance ?: synchronized(this) { instance ?: UsageDb(context).also { instance = it } }
    }
}
