package org.veilon.gymtracker.ui

import org.veilon.gymtracker.data.ExerciseLog
import org.veilon.gymtracker.data.ExerciseRecord

/** What kind of PR (weight, volume, or both) an exercise hit in a session,
 *  plus the record itself so the UI can show the actual numbers. */
data class ExercisePrInfo(
    val isWeightPr: Boolean,
    val isVolumePr: Boolean,
    val record: ExerciseRecord
)

/**
 * Which exercises (by id) had their CURRENT best (weight or volume) set during
 * the given session's time window, and which kind of PR each one was.
 *
 * Honest limitation: this only reflects the record currently in force. If a
 * PR set in an older session was later broken again, that older session will
 * no longer show as a PR — we only store the current best per exercise, not
 * a full history of every time a record was broken. For "did I just PR"
 * (checked right after finishing), this is always correct, since nothing
 * could have superseded it yet.
 */
fun exercisesPrdInSession(
    sessionLogs: List<ExerciseLog>,
    sessionStart: Long,
    sessionEnd: Long,
    records: List<ExerciseRecord>
): Map<Long, ExercisePrInfo> {
    val recordByExercise = records.associateBy { it.exerciseId }
    val loggedExerciseIds = sessionLogs.map { it.exerciseId }.distinct()
    val result = mutableMapOf<Long, ExercisePrInfo>()
    loggedExerciseIds.forEach { exId ->
        val record = recordByExercise[exId] ?: return@forEach
        val weightInRange = record.maxWeightDate in sessionStart..sessionEnd
        val volumeInRange = record.maxVolumeDate in sessionStart..sessionEnd
        if (weightInRange || volumeInRange) {
            result[exId] = ExercisePrInfo(
                isWeightPr = weightInRange,
                isVolumePr = volumeInRange,
                record = record
            )
        }
    }
    return result
}
