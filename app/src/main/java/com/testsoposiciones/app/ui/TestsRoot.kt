package com.testsoposiciones.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.testsoposiciones.app.R
import com.testsoposiciones.app.monetization.Monetization
import com.testsoposiciones.app.monetization.MonetizationState
import com.testsoposiciones.app.monetization.PremiumScreen

private enum class Screen { HOME, SETTINGS, PREMIUM }

@Composable
fun TestsRoot(
    state: StudyState,
    viewModel: StudyViewModel,
    monetization: Monetization,
    monetizationState: MonetizationState,
) {
    val activity = LocalActivity.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val session = state.session
    val finished = state.finished

    when {
        session != null -> QuizScreen(
            session = session,
            deadlineMs = state.examDeadlineMs,
            onAnswer = viewModel::answer,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onGoTo = viewModel::goTo,
            onFinish = {
                // Terminar un test es la pausa natural para un intersticial (con límite de frecuencia).
                viewModel.finish()
                activity?.let { monetization.maybeShowInterstitial(it) }
            },
            onQuit = viewModel::quit,
        )
        finished != null -> ResultScreen(finished, monetizationState, onClose = viewModel::closeResult)
        screen == Screen.PREMIUM -> {
            BackHandler { screen = Screen.HOME }
            PremiumScreen(
                state = monetizationState,
                monetization = monetization,
                features = listOf(stringResource(R.string.premium_feature_exam)),
                onBack = { screen = Screen.HOME },
            )
        }
        screen == Screen.SETTINGS -> {
            BackHandler { screen = Screen.HOME }
            SettingsScreen(
                isPremium = monetizationState.isPremium,
                showPrivacyOptions = remember { activity?.let(monetization::isPrivacyOptionsRequired) ?: false },
                onPrivacyOptions = { activity?.let(monetization::showPrivacyOptions) },
                onOpenPremium = { screen = Screen.PREMIUM },
                onResetProgress = viewModel::resetProgress,
                onBack = { screen = Screen.HOME },
            )
        }
        else -> HomeScreen(
            state = state,
            monetization = monetization,
            monetizationState = monetizationState,
            onSelectBank = viewModel::selectBank,
            onStartTopic = viewModel::startTopic,
            onStartQuick = viewModel::startQuick,
            onStartFailed = viewModel::startFailed,
            onStartExam = viewModel::startExam,
            onOpenSettings = { screen = Screen.SETTINGS },
            onOpenPremium = { screen = Screen.PREMIUM },
        )
    }
}
