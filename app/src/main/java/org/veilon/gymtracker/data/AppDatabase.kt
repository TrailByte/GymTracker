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
    version = 5,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
            Exercise(name = "Bench Press", muscleGroup = "Chest"),
            Exercise(name = "Bench Press (Smith)", muscleGroup = "Chest"),
            Exercise(name = "Cable Fly", muscleGroup = "Chest"),
            Exercise(name = "Incline Bench Press", muscleGroup = "Chest"),
            Exercise(name = "Incline Bench Press (Dumbbell)", muscleGroup = "Chest"),
            Exercise(name = "Incline Bench Press (Smith)", muscleGroup = "Chest"),
            Exercise(name = "Incline Chest Press (Machine)", muscleGroup = "Chest"),
            Exercise(name = "Incline Dumbbell Press", muscleGroup = "Chest"),
            Exercise(name = "Incline Press", muscleGroup = "Chest"),
            Exercise(name = "Iso-Lateral Chest Press (Machine)", muscleGroup = "Chest"),
            Exercise(name = "Bent Over Row (Barbell)", muscleGroup = "Back"),
            Exercise(name = "Bent Over Row - Underhand (Barbell)", muscleGroup = "Back"),
            Exercise(name = "Lat Pulldown", muscleGroup = "Back"),
            Exercise(name = "Pull-Up", muscleGroup = "Back"),
            Exercise(name = "Seated Row", muscleGroup = "Back"),
            Exercise(name = "Face Pull", muscleGroup = "Shoulders"),
            Exercise(name = "Lateral Raise", muscleGroup = "Shoulders"),
            Exercise(name = "Overhead Press (Barbell)", muscleGroup = "Shoulders"),
            Exercise(name = "Overhead Press (Dumbbell)", muscleGroup = "Shoulders"),
            Exercise(name = "Seated Overhead Press (Barbell)", muscleGroup = "Shoulders"),
            Exercise(name = "Seated Overhead Press (Dumbell)", muscleGroup = "Shoulders"),
            Exercise(name = "Shoulder Press", muscleGroup = "Shoulders"),
            Exercise(name = "Shoulder Press (Machine)", muscleGroup = "Shoulders"),
            Exercise(name = "Deadlift", muscleGroup = "Legs"),
            Exercise(name = "Leg Curl", muscleGroup = "Legs"),
            Exercise(name = "Leg Press", muscleGroup = "Legs"),
            Exercise(name = "Romanian Deadlift", muscleGroup = "Legs"),
            Exercise(name = "Squat", muscleGroup = "Legs"),
            Exercise(name = "Bicep Curl (Barbell)", muscleGroup = "Arms"),
            Exercise(name = "Bicep Curl (Cable)", muscleGroup = "Arms"),
            Exercise(name = "Bicep Curl (Dumbbell)", muscleGroup = "Arms"),
            Exercise(name = "Hammer Curl", muscleGroup = "Arms"),
            Exercise(name = "Incline Curl (Dumbbell)", muscleGroup = "Arms"),
            Exercise(name = "Preacher Curl (Machine)", muscleGroup = "Arms"),
            Exercise(name = "Skull Crusher", muscleGroup = "Arms"),
            Exercise(name = "Tricep Pushdown", muscleGroup = "Arms"),
            Exercise(name = "Triceps Extension", muscleGroup = "Arms"),
            Exercise(name = "Triceps Pushdown (Cable - Rope)", muscleGroup = "Arms"),
            Exercise(name = "Cable Crunch", muscleGroup = "Core"),
            Exercise(name = "Plank", muscleGroup = "Core"),
            Exercise(name = "Weighted Planks", muscleGroup = "Core"),
        )
    }
}