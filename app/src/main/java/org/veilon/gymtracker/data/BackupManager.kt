package org.veilon.gymtracker.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.veilon.gymtracker.ui.UserPreferences
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manual, on-demand backup/restore — not automatic cloud sync. Produces a
 * single .zip containing the Room database plus the DataStore preferences
 * that are genuinely meaningful to carry over (units, rest default, weekly
 * goal, selected theme, and gamification progress). Transient state (active
 * session, rest-ends-at) is deliberately excluded — importing gives you a
 * clean slate for "what's happening right now" while preserving everything
 * historical.
 *
 * Import is a full REPLACE, not a merge — it's meant for restoring onto a
 * fresh install on a new device, not combining two independent histories.
 *
 * Gamification progress (XP, prestige, PR count) is backed up EXPLICITLY,
 * not left to recompute itself from the restored workout history — letting
 * it recompute would silently erase prestige status, since prestiging isn't
 * something the workout history alone can reconstruct.
 */
object BackupManager {

    private const val DB_ENTRY_NAME = "gymtracker.db"
    private const val PREFS_ENTRY_NAME = "preferences.json"

    suspend fun exportBackup(context: Context, outputUri: Uri): Boolean {
        return try {
            // Checkpoint the WAL first — the same lesson from the original
            // phone-data-recovery episode. Without this, recent writes could
            // still be sitting in the -wal file, not yet in the main .db.
            val db = AppDatabase.getInstance(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use {
                it.moveToFirst()
            }

            val dbFile = context.getDatabasePath(DB_ENTRY_NAME)
            val prefsJson = buildPrefsJson(context)

            val out = context.contentResolver.openOutputStream(outputUri) ?: return false
            out.use { stream ->
                ZipOutputStream(stream).use { zip ->
                    zip.putNextEntry(ZipEntry(DB_ENTRY_NAME))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry(PREFS_ENTRY_NAME))
                    zip.write(prefsJson.toByteArray())
                    zip.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Returns true on success. Caller MUST prompt the user to restart the
     *  app afterward — every existing ViewModel holds a DAO reference bound
     *  to the connection this closes, and would be querying a dead
     *  connection otherwise. */
    suspend fun importBackup(context: Context, inputUri: Uri): Boolean {
        return try {
            var dbBytes: ByteArray? = null
            var prefsJsonStr: String? = null

            val input = context.contentResolver.openInputStream(inputUri) ?: return false
            input.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            DB_ENTRY_NAME -> dbBytes = zip.readBytes()
                            PREFS_ENTRY_NAME -> prefsJsonStr = String(zip.readBytes())
                        }
                        entry = zip.nextEntry
                    }
                }
            }

            val dbData = dbBytes ?: return false  // a backup without a DB isn't valid

            AppDatabase.closeInstance()

            val dbFile = context.getDatabasePath(DB_ENTRY_NAME)
            dbFile.parentFile?.mkdirs()
            dbFile.writeBytes(dbData)

            // Remove any leftover WAL/SHM from the OLD database — otherwise
            // Room could try to replay stale WAL data against the freshly
            // restored file the next time it's opened.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            prefsJsonStr?.let { restorePrefsJson(context, it) }

            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun buildPrefsJson(context: Context): String {
        val json = JSONObject()
        json.put("use_lbs", UserPreferences.useLbs(context).first())
        json.put("rest_seconds", UserPreferences.restSeconds(context).first())
        json.put("weekly_goal", UserPreferences.weeklyGoal(context).first())
        json.put("total_xp", UserPreferences.totalXp(context).first())
        json.put("prestige_level", UserPreferences.prestigeLevel(context).first())
        json.put("total_pr_count", UserPreferences.totalPrCount(context).first())
        json.put("last_known_streak", UserPreferences.lastKnownStreak(context).first())
        json.put("selected_theme", UserPreferences.selectedTheme(context).first())
        json.put("gamification_backfill_version", UserPreferences.gamificationBackfillVersion(context).first())
        return json.toString()
    }

    private suspend fun restorePrefsJson(context: Context, jsonStr: String) {
        val json = JSONObject(jsonStr)
        if (json.has("use_lbs")) UserPreferences.setUseLbs(context, json.getBoolean("use_lbs"))
        if (json.has("rest_seconds")) UserPreferences.setRestSeconds(context, json.getInt("rest_seconds"))
        if (json.has("weekly_goal")) UserPreferences.setWeeklyGoal(context, json.getInt("weekly_goal"))
        if (json.has("total_xp")) UserPreferences.setTotalXp(context, json.getLong("total_xp"))
        if (json.has("prestige_level")) UserPreferences.setPrestigeLevel(context, json.getInt("prestige_level"))
        if (json.has("total_pr_count")) UserPreferences.setTotalPrCount(context, json.getLong("total_pr_count"))
        if (json.has("last_known_streak")) UserPreferences.setLastKnownStreak(context, json.getInt("last_known_streak"))
        if (json.has("selected_theme")) UserPreferences.setSelectedTheme(context, json.getString("selected_theme"))
        if (json.has("gamification_backfill_version")) {
            UserPreferences.setGamificationBackfillVersion(context, json.getInt("gamification_backfill_version"))
        }
    }
}
