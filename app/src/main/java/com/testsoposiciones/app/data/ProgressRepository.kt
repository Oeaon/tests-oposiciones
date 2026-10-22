package com.testsoposiciones.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.testsoposiciones.core.Progress
import com.testsoposiciones.core.StudyStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

data class StoredProgress(
    val progress: Progress = Progress(),
    val streak: StudyStreak = StudyStreak(),
    /** Mejor nota de simulacro por id de temario. */
    val bestExamScores: Map<String, Double> = emptyMap(),
    /** Temario elegido; null hasta que el usuario cambia el primero. */
    val selectedBankId: String? = null,
)

class ProgressRepository(private val store: DataStore<Preferences>) {
    val data: Flow<StoredProgress> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            StoredProgress(
                progress = Progress(prefs[CORRECT].orEmpty(), prefs[WRONG].orEmpty()),
                streak = StudyStreak(
                    days = prefs[STREAK_DAYS] ?: 0,
                    lastDay = prefs[STREAK_LAST]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                ),
                bestExamScores = prefs.asMap()
                    .filterKeys { it.name.startsWith(BEST_EXAM_PREFIX) }
                    .mapNotNull { (key, value) -> (value as? Double)?.let { key.name.removePrefix(BEST_EXAM_PREFIX) to it } }
                    .toMap(),
                selectedBankId = prefs[SELECTED_BANK],
            )
        }

    /** Guarda el resultado de un test y suma el día a la racha. */
    suspend fun record(outcomes: Map<String, Boolean>, bankId: String, examScore: Double?, today: LocalDate) {
        store.edit { prefs ->
            val updated = Progress(prefs[CORRECT].orEmpty(), prefs[WRONG].orEmpty()).record(outcomes)
            prefs[CORRECT] = updated.correctIds
            prefs[WRONG] = updated.wrongIds
            val streak = StudyStreak(
                prefs[STREAK_DAYS] ?: 0,
                prefs[STREAK_LAST]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            ).studiedOn(today)
            prefs[STREAK_DAYS] = streak.days
            prefs[STREAK_LAST] = today.toString()
            val bestKey = doublePreferencesKey(BEST_EXAM_PREFIX + bankId)
            if (examScore != null && examScore > (prefs[bestKey] ?: -1.0)) prefs[bestKey] = examScore
        }
    }

    suspend fun selectBank(bankId: String) {
        store.edit { it[SELECTED_BANK] = bankId }
    }

    /** Borra el progreso pero mantiene el temario elegido. */
    suspend fun reset() {
        store.edit { prefs ->
            val selected = prefs[SELECTED_BANK]
            prefs.clear()
            if (selected != null) prefs[SELECTED_BANK] = selected
        }
    }

    private companion object {
        val CORRECT = stringSetPreferencesKey("correct_ids")
        val WRONG = stringSetPreferencesKey("wrong_ids")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val STREAK_LAST = stringPreferencesKey("streak_last")
        const val BEST_EXAM_PREFIX = "best_exam_"
        val SELECTED_BANK = stringPreferencesKey("selected_bank")
    }
}
