package org.veilon.gymtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Exercise::class, WorkoutSession::class, ExerciseLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gymtracker.db"
                )
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
            Exercise(name = "Incline Dumbbell Press", muscleGroup = "Chest"),
            Exercise(name = "Cable Fly", muscleGroup = "Chest"),
            Exercise(name = "Overhead Press", muscleGroup = "Shoulders"),
            Exercise(name = "Lateral Raise", muscleGroup = "Shoulders"),
            Exercise(name = "Face Pull", muscleGroup = "Shoulders"),
            Exercise(name = "Barbell Row", muscleGroup = "Back"),
            Exercise(name = "Lat Pulldown", muscleGroup = "Back"),
            Exercise(name = "Pull-Up", muscleGroup = "Back"),
            Exercise(name = "Seated Row", muscleGroup = "Back"),
            Exercise(name = "Squat", muscleGroup = "Legs"),
            Exercise(name = "Deadlift", muscleGroup = "Legs"),
            Exercise(name = "Leg Press", muscleGroup = "Legs"),
            Exercise(name = "Romanian Deadlift", muscleGroup = "Legs"),
            Exercise(name = "Leg Curl", muscleGroup = "Legs"),
            Exercise(name = "Bicep Curl", muscleGroup = "Arms"),
            Exercise(name = "Hammer Curl", muscleGroup = "Arms"),
            Exercise(name = "Tricep Pushdown", muscleGroup = "Arms"),
            Exercise(name = "Skull Crusher", muscleGroup = "Arms"),
            Exercise(name = "Plank", muscleGroup = "Core"),
            Exercise(name = "Cable Crunch", muscleGroup = "Core"),
        )
    }
}