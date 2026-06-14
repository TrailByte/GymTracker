package org.veilon.gymtracker.ui

// A personal record for one exercise
data class ExercisePR(
    val exerciseName: String,
    val muscleGroup: String,
    val maxWeightKg: Double,       // heaviest single set
    val maxVolumeKg: Double,       // best weight × reps in a single set
    val maxVolumeReps: Int,        // the reps that produced maxVolume
    val maxVolumeWeightKg: Double  // the weight that produced maxVolume
)

data class HomeStats(
    val workoutsThisWeek: Int,
    val weekStreak: Int,
    val totalVolumeKg: Double,
    val prs: List<ExercisePR>
)