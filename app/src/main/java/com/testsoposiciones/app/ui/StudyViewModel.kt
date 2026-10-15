package com.testsoposiciones.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.testsoposiciones.app.TestsOposicionesApp
import com.testsoposiciones.app.data.ProgressRepository
import com.testsoposiciones.app.data.QuestionRepository
import com.testsoposiciones.app.data.StoredProgress
import com.testsoposiciones.core.Question
import com.testsoposiciones.core.QuestionBank
import com.testsoposiciones.core.QuestionBankFormatException
import com.testsoposiciones.core.QuizBuilder
import com.testsoposiciones.core.QuizMode
import com.testsoposiciones.core.QuizResult
import com.testsoposiciones.core.QuizSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

data class FinishedQuiz(val session: QuizSession, val result: QuizResult)

data class StudyState(
    val bank: QuestionBank? = null,
    val loadError: Boolean = false,
    val stored: StoredProgress = StoredProgress(),
    val session: QuizSession? = null,
    /** Hora límite del simulacro en milisegundos; null en práctica. */
    val examDeadlineMs: Long? = null,
    val finished: FinishedQuiz? = null,
)

class StudyViewModel(
    private val questions: QuestionRepository,
    private val progress: ProgressRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StudyState())
    val state: StateFlow<StudyState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val bank = questions.bank()
                _state.update { it.copy(bank = bank) }
            } catch (e: IOException) {
                _state.update { it.copy(loadError = true) }
            } catch (e: QuestionBankFormatException) {
                _state.update { it.copy(loadError = true) }
            }
        }
        viewModelScope.launch { progress.data.collect { stored -> _state.update { it.copy(stored = stored) } } }
    }

    fun startTopic(topicId: String) {
        val bank = _state.value.bank ?: return
        // Primero las que aún no se han acertado, luego al azar.
        start(_state.value.stored.progress.pendingFirst(bank.questionsOf(topicId).shuffled()).take(PRACTICE_SIZE), QuizMode.PRACTICE)
    }

    fun startQuick() {
        val bank = _state.value.bank ?: return
        start(bank.questions, QuizMode.PRACTICE, PRACTICE_SIZE)
    }

    fun startFailed() {
        val bank = _state.value.bank ?: return
        start(_state.value.stored.progress.failedQuestions(bank), QuizMode.PRACTICE, PRACTICE_SIZE)
    }

    fun startExam() {
        val bank = _state.value.bank ?: return
        start(bank.questions, QuizMode.EXAM, EXAM_SIZE, deadline = System.currentTimeMillis() + EXAM_MINUTES * 60_000L)
    }

    private fun start(pool: List<Question>, mode: QuizMode, count: Int = pool.size, deadline: Long? = null) {
        if (pool.isEmpty()) return
        _state.update { it.copy(session = QuizBuilder.build(pool, count, mode), examDeadlineMs = deadline, finished = null) }
    }

    fun answer(option: Int) = updateSession { it.answer(option) }

    fun next() = updateSession { it.next() }

    fun previous() = updateSession { it.goTo(it.current - 1) }

    fun goTo(index: Int) = updateSession { it.goTo(index) }

    private fun updateSession(change: (QuizSession) -> QuizSession) {
        _state.update { state -> state.session?.let { state.copy(session = change(it)) } ?: state }
    }

    fun finish() {
        val session = _state.value.session ?: return
        val result = session.result()
        _state.update { it.copy(session = null, examDeadlineMs = null, finished = FinishedQuiz(session, result)) }
        viewModelScope.launch {
            progress.record(
                outcomes = session.outcomes(),
                examScore = if (session.mode == QuizMode.EXAM) result.score else null,
                today = LocalDate.now(),
            )
        }
    }

    fun quit() = _state.update { it.copy(session = null, examDeadlineMs = null) }

    fun closeResult() = _state.update { it.copy(finished = null) }

    fun resetProgress() {
        viewModelScope.launch { progress.reset() }
    }

    companion object {
        const val PRACTICE_SIZE = 10
        const val EXAM_SIZE = 30
        const val EXAM_MINUTES = 30

        fun factory(app: TestsOposicionesApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { StudyViewModel(app.questions, app.progress) }
        }
    }
}
