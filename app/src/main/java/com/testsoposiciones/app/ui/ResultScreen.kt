package com.testsoposiciones.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testsoposiciones.app.R
import com.testsoposiciones.app.monetization.AdBanner
import com.testsoposiciones.app.monetization.MonetizationState
import com.testsoposiciones.app.ui.theme.AnswerColors
import com.testsoposiciones.core.QuizMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(finished: FinishedQuiz, monetizationState: MonetizationState, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val result = finished.result
    val session = finished.session
    val mistakes = session.items.indices.filter { session.isCorrect(it) == false }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.results)) }) },
        bottomBar = { AdBanner(monetizationState) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.score, String.format(Locale.getDefault(), "%.2f", result.score)),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result.score >= 5) AnswerColors.correct else AnswerColors.wrong,
                        )
                        Text(stringResource(R.string.result_counts, result.correct, result.wrong, result.blank))
                        if (session.mode == QuizMode.EXAM) {
                            Text(
                                stringResource(R.string.result_penalty, String.format(Locale.getDefault(), "%.2f", result.netCorrect)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item { Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back_home)) } }
            if (mistakes.isNotEmpty()) {
                item { Text(stringResource(R.string.your_mistakes), style = MaterialTheme.typography.titleMedium) }
            }
            items(mistakes) { index ->
                val item = session.items[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        finished.bank.topics.firstOrNull { it.id == item.question.topicId }?.let {
                            Text(it.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.question.text, style = MaterialTheme.typography.bodyLarge)
                        session.answers[index]?.let {
                            Text(stringResource(R.string.your_answer, item.options[it]), color = AnswerColors.wrong)
                        }
                        Text(stringResource(R.string.correct_answer, item.options[item.correctIndex]), color = AnswerColors.correct)
                        Text(item.question.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
