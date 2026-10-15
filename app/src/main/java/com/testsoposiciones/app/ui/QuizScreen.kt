package com.testsoposiciones.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testsoposiciones.app.R
import com.testsoposiciones.app.ui.theme.AnswerColors
import com.testsoposiciones.core.QuizMode
import com.testsoposiciones.core.QuizSession
import kotlinx.coroutines.delay

/** Pantalla de un test. Sin banner: no se ponen anuncios mientras se contesta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    session: QuizSession,
    deadlineMs: Long?,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onGoTo: (Int) -> Unit,
    onFinish: () -> Unit,
    onQuit: () -> Unit,
) {
    var confirmQuit by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    BackHandler { confirmQuit = true }
    val remainingMs = deadlineMs?.let { rememberRemaining(it, onFinish) }
    val isExam = session.mode == QuizMode.EXAM
    val item = session.currentItem
    val chosen = session.answers[session.current]
    val reveal = !isExam && chosen != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.question_of, session.current + 1, session.items.size)) },
                navigationIcon = {
                    IconButton(onClick = { confirmQuit = true }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.quit_test))
                    }
                },
                actions = {
                    if (remainingMs != null) {
                        val seconds = remainingMs / 1000
                        Text(
                            "%d:%02d".format(seconds / 60, seconds % 60),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (seconds < 60) AnswerColors.wrong else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LinearProgressIndicator(
                progress = { (session.current + 1f) / session.items.size },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isExam) QuestionDots(session, onGoTo)
            Text(item.question.text, style = MaterialTheme.typography.titleLarge)
            item.options.forEachIndexed { index, option ->
                val color = when {
                    reveal && index == item.correctIndex -> AnswerColors.correct
                    reveal && index == chosen -> AnswerColors.wrong
                    !reveal && index == chosen -> MaterialTheme.colorScheme.primary
                    else -> null
                }
                OutlinedCard(
                    onClick = { onAnswer(index) },
                    enabled = !reveal,
                    border = BorderStroke(if (color != null) 2.dp else 1.dp, color ?: MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = color?.copy(alpha = 0.10f) ?: Color.Transparent,
                        disabledContainerColor = color?.copy(alpha = 0.10f) ?: Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "${'A' + index}) $option",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (reveal) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(if (chosen == item.correctIndex) R.string.answer_right else R.string.answer_wrong),
                            fontWeight = FontWeight.Bold,
                            color = if (chosen == item.correctIndex) AnswerColors.correct else AnswerColors.wrong,
                        )
                        Text(item.question.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isExam) {
                    OutlinedButton(onClick = onPrevious, enabled = session.current > 0, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.previous))
                    }
                }
                if (session.isLast) {
                    Button(
                        onClick = { if (isExam) confirmFinish = true else onFinish() },
                        enabled = isExam || reveal,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.finish)) }
                } else {
                    Button(onClick = onNext, enabled = isExam || reveal, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.next))
                    }
                }
            }
            if (isExam && !session.isLast) {
                TextButton(onClick = { confirmFinish = true }) { Text(stringResource(R.string.finish_exam_now)) }
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.quit_test)) },
            text = { Text(stringResource(R.string.quit_test_text)) },
            confirmButton = { TextButton(onClick = onQuit) { Text(stringResource(R.string.quit)) } },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text(stringResource(R.string.keep_going)) } },
        )
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text(stringResource(R.string.finish_exam_now)) },
            text = { Text(stringResource(R.string.finish_exam_text, session.answeredCount, session.items.size)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    onFinish()
                }) { Text(stringResource(R.string.finish)) }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text(stringResource(R.string.keep_going)) } },
        )
    }
}

/** Milisegundos que quedan del simulacro; al llegar a cero lo termina. */
@Composable
private fun rememberRemaining(deadlineMs: Long, onTimeUp: () -> Unit): Long {
    var remaining by remember(deadlineMs) { mutableLongStateOf((deadlineMs - System.currentTimeMillis()).coerceAtLeast(0)) }
    val currentOnTimeUp by rememberUpdatedState(onTimeUp)
    LaunchedEffect(deadlineMs) {
        while (true) {
            remaining = (deadlineMs - System.currentTimeMillis()).coerceAtLeast(0)
            if (remaining == 0L) {
                currentOnTimeUp()
                break
            }
            delay(1_000)
        }
    }
    return remaining
}

/** Mapa de preguntas del simulacro: contestadas en color, la actual con borde. */
@Composable
private fun QuestionDots(session: QuizSession, onGoTo: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        session.items.indices.forEach { index ->
            val answered = session.answers[index] != null
            OutlinedCard(
                onClick = { onGoTo(index) },
                shape = CircleShape,
                border = BorderStroke(
                    if (index == session.current) 2.dp else 1.dp,
                    if (index == session.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (answered) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                ),
                modifier = Modifier.size(36.dp),
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
