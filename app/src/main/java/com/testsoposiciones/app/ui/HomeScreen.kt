package com.testsoposiciones.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testsoposiciones.app.R
import com.testsoposiciones.app.monetization.AdBanner
import com.testsoposiciones.app.monetization.LockedFeature
import com.testsoposiciones.app.monetization.Monetization
import com.testsoposiciones.app.monetization.MonetizationState
import com.testsoposiciones.app.monetization.PremiumAction
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: StudyState,
    monetization: Monetization,
    monetizationState: MonetizationState,
    onStartTopic: (String) -> Unit,
    onStartQuick: () -> Unit,
    onStartFailed: () -> Unit,
    onStartExam: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    PremiumAction(monetizationState, onOpenPremium)
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        bottomBar = { AdBanner(monetizationState) },
    ) { padding ->
        val bank = state.bank
        if (bank == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loadError) Text(stringResource(R.string.load_error)) else CircularProgressIndicator()
            }
            return@Scaffold
        }
        val progress = state.stored.progress
        val overall = progress.stats(bank)
        val failed = progress.failedQuestions(bank).size
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(bank.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.mastery, overall.correct, overall.total))
                        LinearProgressIndicator(progress = { overall.mastery }, modifier = Modifier.fillMaxWidth())
                        val streak = state.stored.streak.current(LocalDate.now())
                        Text(
                            if (streak > 0) pluralStringResource(R.plurals.streak, streak, streak) else stringResource(R.string.streak_none),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        state.stored.bestExamScore?.let {
                            Text(stringResource(R.string.best_exam, String.format(Locale.getDefault(), "%.2f", it)))
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStartQuick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.quick_test, StudyViewModel.PRACTICE_SIZE))
                    }
                    OutlinedButton(onClick = onStartFailed, enabled = failed > 0, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.review_failed, failed))
                    }
                }
            }
            item {
                LockedFeature(
                    state = monetizationState,
                    monetization = monetization,
                    title = stringResource(R.string.exam_title),
                    description = stringResource(R.string.exam_locked, StudyViewModel.EXAM_SIZE, StudyViewModel.EXAM_MINUTES),
                    onGoPremium = onOpenPremium,
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.exam_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.exam_description, StudyViewModel.EXAM_SIZE, StudyViewModel.EXAM_MINUTES))
                            Button(onClick = onStartExam, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.exam_start))
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.topics), style = MaterialTheme.typography.titleMedium) }
            items(bank.topics, key = { it.id }) { topic ->
                val stats = progress.stats(bank, topic.id)
                Card(onClick = { onStartTopic(topic.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(topic.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(progress = { stats.mastery }, modifier = Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.topic_stats, stats.total, (stats.mastery * 100).roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    bank.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
