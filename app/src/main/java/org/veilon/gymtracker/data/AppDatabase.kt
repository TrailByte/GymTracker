package org.veilon.gymtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Exercise::class,
        WorkoutSession::class,
        ExerciseLog::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        SessionExerciseOrder::class,
        ExerciseRecord::class,
        UnlockedAchievement::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun templateDao(): TemplateDao
    abstract fun recordDao(): RecordDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Needed for backup restore: the live connection must be closed before
        // its underlying file is overwritten, or every ViewModel holding an
        // old DAO reference would be querying a closed database. The next
        // getInstance() call after this rebuilds fresh from whatever file
        // exists on disk at that point.
        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        // Defensive 1->2 migration. Because the DB version was never bumped while
        // columns were added during dev, different "version 1" databases exist.
        // We add each column only if it's actually missing.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "workout_sessions", "durationSeconds", "INTEGER")
                addColumnIfMissing(db, "exercises", "archived", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "exercise_logs", "completed", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        // New table for explicit exercise ordering within a workout session.
        // Nothing to migrate from — it's brand new, so no data copy needed.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_exercise_order` (
                        `sessionId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`, `exerciseId`)
                    )
                    """.trimIndent()
                )
            }
        }

        // New table for all-time per-exercise records (best weight, best volume).
        // Brand new table, nothing to copy — existing history is backfilled once
        // in code after the app opens (see HomeViewModel), not here.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_records` (
                        `exerciseId` INTEGER NOT NULL,
                        `maxWeightKg` REAL NOT NULL,
                        `maxWeightReps` INTEGER NOT NULL,
                        `maxWeightDate` INTEGER NOT NULL,
                        `maxVolumeKg` REAL NOT NULL,
                        `maxVolumeWeightKg` REAL NOT NULL,
                        `maxVolumeReps` INTEGER NOT NULL,
                        `maxVolumeDate` INTEGER NOT NULL,
                        PRIMARY KEY(`exerciseId`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `unlocked_achievements` (
                        `achievementId` TEXT NOT NULL,
                        `unlockedDate` INTEGER NOT NULL,
                        PRIMARY KEY(`achievementId`)
                    )
                    """.trimIndent()
                )
            }
        }

        // Equipment used to be hand-encoded into the exercise NAME (e.g.
        // "Lateral Raise (Dumbbell)") since there was nowhere else to put it.
        // Now it has a real field: rename+tag strips the equipment part of
        // the name and sets equipmentType instead. Parentheticals that
        // encode something ELSE (grip, unilateral, attachment) are
        // deliberately preserved — only pure-equipment suffixes are stripped.
        // Tested end-to-end against a real exported backup before shipping.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "exercises", "equipmentType", "TEXT NOT NULL DEFAULT 'Other'")

                val renameAndTag = mapOf(
                    "Bicep Curl (Barbell)" to ("Bicep Curl" to "Barbell"),
                    "Bicep Curl (Cable)" to ("Bicep Curl" to "Cable"),
                    "Bicep Curl (Dumbbell)" to ("Bicep Curl" to "Dumbbell"),
                    "Hammer Curl" to ("Hammer Curl" to "Dumbbell"),
                    "Hammer Curl (Cable)" to ("Hammer Curl" to "Cable"),
                    "Incline Curl (Dumbbell)" to ("Incline Curl" to "Dumbbell"),
                    "Preacher Curl (Machine)" to ("Preacher Curl" to "Machine"),
                    "Skull Crusher" to ("Skull Crusher" to "Barbell"),
                    "Tricep Pushdown" to ("Tricep Pushdown" to "Cable"),
                    "Tricep Pushdown (Cable - Rope - Vision)" to ("Tricep Pushdown (Rope)" to "Cable"),
                    "Triceps Extension" to ("Triceps Extension" to "Dumbbell"),
                    "Triceps Pushdown (Cable - Rope)" to ("Triceps Pushdown (Rope)" to "Cable"),
                    "Bent Over Row (Barbell)" to ("Bent Over Row" to "Barbell"),
                    "Bent Over Row - Underhand (Barbell)" to ("Bent Over Row - Underhand" to "Barbell"),
                    "Lat Pulldown" to ("Lat Pulldown" to "Cable"),
                    "Lat pulldown neutral grip (Cable)" to ("Lat pulldown neutral grip" to "Cable"),
                    "Pull-Up" to ("Pull-Up" to "Bodyweight"),
                    "Seated Row (Close Neutral Grip)" to ("Seated Row (Close Neutral Grip)" to "Cable"),
                    "Seated Row (Wide grip)" to ("Seated Row (Wide grip)" to "Cable"),
                    "Bench Press" to ("Bench Press" to "Barbell"),
                    "Bench Press (Smith)" to ("Bench Press" to "Smith Machine"),
                    "Cable Fly" to ("Cable Fly" to "Cable"),
                    "Chest Fly (Machine)" to ("Chest Fly" to "Machine"),
                    "Incline Bench Press" to ("Incline Bench Press" to "Barbell"),
                    "Incline Bench Press (Dumbbell)" to ("Incline Bench Press" to "Dumbbell"),
                    "Incline Bench Press (Smith)" to ("Incline Bench Press" to "Smith Machine"),
                    "Incline Chest Press (Machine)" to ("Incline Chest Press" to "Machine"),
                    "Incline Press" to ("Incline Press" to "Machine"),
                    "Iso-Lateral Chest Press (Machine)" to ("Iso-Lateral Chest Press" to "Machine"),
                    "Weighted Chest Dips" to ("Weighted Chest Dips" to "Bodyweight"),
                    "Cable Crunch" to ("Cable Crunch" to "Cable"),
                    "Crunch (Machine)" to ("Crunch" to "Machine"),
                    "Plank" to ("Plank" to "Bodyweight"),
                    "Weighted Planks" to ("Weighted Planks" to "Bodyweight"),
                    "Back Squat" to ("Back Squat" to "Barbell"),
                    "Deadlift" to ("Deadlift" to "Barbell"),
                    "Hip Thrust (Machine)" to ("Hip Thrust" to "Machine"),
                    "Leg Press" to ("Leg Press" to "Machine"),
                    "Romanian Deadlift" to ("Romanian Deadlift" to "Barbell"),
                    "Seated Calf Raise (Plate Loaded)" to ("Seated Calf Raise" to "Machine"),
                    "Seated Leg Curl (One leg)" to ("Seated Leg Curl (One leg)" to "Machine"),
                    "Squat" to ("Squat" to "Barbell"),
                    "Face Pull" to ("Face Pull" to "Cable"),
                    "Lateral Raise (Dumbbell)" to ("Lateral Raise" to "Dumbbell"),
                    "Lateral Raise (Machine)" to ("Lateral Raise" to "Machine"),
                    "Overhead Press (Barbell)" to ("Overhead Press" to "Barbell"),
                    "Overhead Press (Dumbbell)" to ("Overhead Press" to "Dumbbell"),
                    "Seated Overhead Press (Barbell)" to ("Seated Overhead Press" to "Barbell"),
                    "Seated Overhead Press (Dumbell)" to ("Seated Overhead Press" to "Dumbbell"),
                    "Shoulder Press (Machine)" to ("Shoulder Press" to "Machine"),
                    "Shrugs (Dumbbells)" to ("Shrugs" to "Dumbbell")
                )
                renameAndTag.forEach { (oldName, newNameAndEquipment) ->
                    val (newName, equipment) = newNameAndEquipment
                    db.execSQL(
                        "UPDATE exercises SET name = ?, equipmentType = ? WHERE name = ?",
                        arrayOf(newName, equipment, oldName)
                    )
                }

                // Plain "Shoulder Press" and "Shoulder Press (Machine)" become
                // an exact duplicate after the rename above (confirmed: the
                // plain one was never actually used) — merge rather than
                // leave two identical rows. Both are now named "Shoulder
                // Press", so distinguish them by equipmentType: the loser
                // was never touched by the rename map above and still holds
                // the column's default ('Other'); the survivor was just
                // explicitly set to 'Machine'. Matches what was verified
                // against the real exported backup before writing this.
                val loserId = queryExerciseIdByNameAndEquipment(db, "Shoulder Press", "Other")
                val survivorId = queryExerciseIdByNameAndEquipment(db, "Shoulder Press", "Machine")
                if (loserId != null && survivorId != null && loserId != survivorId) {
                    mergeExercises(db, loserId, survivorId)
                }
            }
        }

        /** Re-points all logged history, templates, and saved order from
         *  [loserId] to [survivorId], combines their PR records (keeping the
         *  better of each metric), then deletes the loser. Handles the case
         *  where session_exercise_order would otherwise hit a primary-key
         *  collision (both exercises logged in the same session). */
        private fun mergeExercises(db: SupportSQLiteDatabase, loserId: Long, survivorId: Long) {
            db.execSQL("UPDATE exercise_logs SET exerciseId = ? WHERE exerciseId = ?", arrayOf(survivorId, loserId))
            db.execSQL("UPDATE template_exercises SET exerciseId = ? WHERE exerciseId = ?", arrayOf(survivorId, loserId))

            // Composite primary key (sessionId, exerciseId) — if the survivor
            // already has an order row for a session, the loser's row for
            // that same session would collide. Drop those first.
            db.execSQL(
                """DELETE FROM session_exercise_order
                   WHERE exerciseId = ? AND sessionId IN (
                       SELECT sessionId FROM session_exercise_order WHERE exerciseId = ?
                   )""",
                arrayOf(loserId, survivorId)
            )
            db.execSQL("UPDATE session_exercise_order SET exerciseId = ? WHERE exerciseId = ?", arrayOf(survivorId, loserId))

            val loserRecord = queryRecord(db, loserId)
            if (loserRecord != null) {
                val survivorRecord = queryRecord(db, survivorId)
                val merged = if (survivorRecord == null) loserRecord else combineRecords(survivorRecord, loserRecord)
                db.execSQL("DELETE FROM exercise_records WHERE exerciseId IN (?, ?)", arrayOf(loserId, survivorId))
                db.execSQL(
                    """INSERT INTO exercise_records
                       (exerciseId, maxWeightKg, maxWeightReps, maxWeightDate, maxVolumeKg, maxVolumeWeightKg, maxVolumeReps, maxVolumeDate)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf<Any>(
                        survivorId, merged.maxWeightKg, merged.maxWeightReps, merged.maxWeightDate,
                        merged.maxVolumeKg, merged.maxVolumeWeightKg, merged.maxVolumeReps, merged.maxVolumeDate
                    )
                )
            }

            db.execSQL("DELETE FROM exercises WHERE id = ?", arrayOf(loserId))
        }

        private data class RecordRow(
            val maxWeightKg: Double, val maxWeightReps: Int, val maxWeightDate: Long,
            val maxVolumeKg: Double, val maxVolumeWeightKg: Double, val maxVolumeReps: Int, val maxVolumeDate: Long
        )

        private fun queryExerciseIdByNameAndEquipment(db: SupportSQLiteDatabase, name: String, equipment: String): Long? {
            val cursor = db.query("SELECT id FROM exercises WHERE name = ? AND equipmentType = ?", arrayOf(name, equipment))
            val result = if (cursor.moveToFirst()) cursor.getLong(0) else null
            cursor.close()
            return result
        }

        private fun queryRecord(db: SupportSQLiteDatabase, exerciseId: Long): RecordRow? {
            val cursor = db.query(
                "SELECT maxWeightKg, maxWeightReps, maxWeightDate, maxVolumeKg, maxVolumeWeightKg, maxVolumeReps, maxVolumeDate FROM exercise_records WHERE exerciseId = ?",
                arrayOf(exerciseId)
            )
            val result = if (cursor.moveToFirst()) {
                RecordRow(
                    cursor.getDouble(0), cursor.getInt(1), cursor.getLong(2),
                    cursor.getDouble(3), cursor.getDouble(4), cursor.getInt(5), cursor.getLong(6)
                )
            } else null
            cursor.close()
            return result
        }

        private fun combineRecords(a: RecordRow, b: RecordRow): RecordRow {
            val useAForWeight = a.maxWeightKg >= b.maxWeightKg
            val useAForVolume = a.maxVolumeKg >= b.maxVolumeKg
            return RecordRow(
                maxWeightKg = if (useAForWeight) a.maxWeightKg else b.maxWeightKg,
                maxWeightReps = if (useAForWeight) a.maxWeightReps else b.maxWeightReps,
                maxWeightDate = if (useAForWeight) a.maxWeightDate else b.maxWeightDate,
                maxVolumeKg = if (useAForVolume) a.maxVolumeKg else b.maxVolumeKg,
                maxVolumeWeightKg = if (useAForVolume) a.maxVolumeWeightKg else b.maxVolumeWeightKg,
                maxVolumeReps = if (useAForVolume) a.maxVolumeReps else b.maxVolumeReps,
                maxVolumeDate = if (useAForVolume) a.maxVolumeDate else b.maxVolumeDate
            )
        }



        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            type: String
        ) {
            val cursor = db.query("PRAGMA table_info(`$table`)")
            var exists = false
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIdx) == column) { exists = true; break }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $type")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gymtracker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.exerciseDao()?.let { dao ->
                                    seedExercises.forEach { dao.insert(it) }
                                }
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val seedExercises = listOf(
            Exercise(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"),
            Exercise(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Smith Machine"),
            Exercise(name = "Cable Fly", muscleGroup = "Chest", equipmentType = "Cable"),
            Exercise(name = "Incline Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"),
            Exercise(name = "Incline Bench Press", muscleGroup = "Chest", equipmentType = "Dumbbell"),
            Exercise(name = "Incline Bench Press", muscleGroup = "Chest", equipmentType = "Smith Machine"),
            Exercise(name = "Incline Chest Press", muscleGroup = "Chest", equipmentType = "Machine"),
            Exercise(name = "Incline Press", muscleGroup = "Chest", equipmentType = "Machine"),
            Exercise(name = "Iso-Lateral Chest Press", muscleGroup = "Chest", equipmentType = "Machine"),
            Exercise(name = "Bent Over Row", muscleGroup = "Back", equipmentType = "Barbell"),
            Exercise(name = "Bent Over Row - Underhand", muscleGroup = "Back", equipmentType = "Barbell"),
            Exercise(name = "Lat Pulldown", muscleGroup = "Back", equipmentType = "Cable"),
            Exercise(name = "Pull-Up", muscleGroup = "Back", equipmentType = "Bodyweight"),
            Exercise(name = "Seated Row", muscleGroup = "Back", equipmentType = "Cable"),
            Exercise(name = "Face Pull", muscleGroup = "Shoulders", equipmentType = "Cable"),
            Exercise(name = "Lateral Raise", muscleGroup = "Shoulders", equipmentType = "Dumbbell"),
            Exercise(name = "Overhead Press", muscleGroup = "Shoulders", equipmentType = "Barbell"),
            Exercise(name = "Overhead Press", muscleGroup = "Shoulders", equipmentType = "Dumbbell"),
            Exercise(name = "Seated Overhead Press", muscleGroup = "Shoulders", equipmentType = "Barbell"),
            Exercise(name = "Seated Overhead Press", muscleGroup = "Shoulders", equipmentType = "Dumbbell"),
            Exercise(name = "Shoulder Press", muscleGroup = "Shoulders", equipmentType = "Machine"),
            Exercise(name = "Deadlift", muscleGroup = "Legs", equipmentType = "Barbell"),
            Exercise(name = "Leg Curl", muscleGroup = "Legs", equipmentType = "Machine"),
            Exercise(name = "Leg Press", muscleGroup = "Legs", equipmentType = "Machine"),
            Exercise(name = "Romanian Deadlift", muscleGroup = "Legs", equipmentType = "Barbell"),
            Exercise(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"),
            Exercise(name = "Bicep Curl", muscleGroup = "Arms", equipmentType = "Barbell"),
            Exercise(name = "Bicep Curl", muscleGroup = "Arms", equipmentType = "Cable"),
            Exercise(name = "Bicep Curl", muscleGroup = "Arms", equipmentType = "Dumbbell"),
            Exercise(name = "Hammer Curl", muscleGroup = "Arms", equipmentType = "Dumbbell"),
            Exercise(name = "Incline Curl", muscleGroup = "Arms", equipmentType = "Dumbbell"),
            Exercise(name = "Preacher Curl", muscleGroup = "Arms", equipmentType = "Machine"),
            Exercise(name = "Skull Crusher", muscleGroup = "Arms", equipmentType = "Barbell"),
            Exercise(name = "Tricep Pushdown", muscleGroup = "Arms", equipmentType = "Cable"),
            Exercise(name = "Triceps Extension", muscleGroup = "Arms", equipmentType = "Dumbbell"),
            Exercise(name = "Triceps Pushdown (Rope)", muscleGroup = "Arms", equipmentType = "Cable"),
            Exercise(name = "Cable Crunch", muscleGroup = "Core", equipmentType = "Cable"),
            Exercise(name = "Plank", muscleGroup = "Core", equipmentType = "Bodyweight"),
            Exercise(name = "Weighted Planks", muscleGroup = "Core", equipmentType = "Bodyweight"),
        )
    }
}