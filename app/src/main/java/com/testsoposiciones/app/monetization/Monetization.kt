package com.testsoposiciones.app.monetization

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonetizationState(
    val isPremium: Boolean = false,
    val rewardedUntilMs: Long = 0,
    val adsReady: Boolean = false,
    val rewardedAvailable: Boolean = false,
    val monthlyPrice: String? = null,
    val lifetimePrice: String? = null,
) {
    val showAds: Boolean get() = adsReady && !isPremium

    /** Las funciones extra se abren con Premium o durante 24 h tras ver un anuncio recompensado. */
    fun isUnlocked(nowMs: Long = System.currentTimeMillis()): Boolean = isPremium || nowMs < rewardedUntilMs
}

/** Punto único para anuncios y Premium. Se crea una vez en la Application. */
class Monetization(private val app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = MonetizationStore(app.monetizationDataStore)
    private val _state = MutableStateFlow(MonetizationState())
    val state: StateFlow<MonetizationState> = _state.asStateFlow()

    private val ads = AdsManager(app, store, scope) { _state.value.isPremium }
    private val billing = BillingRepository(
        context = app,
        onPremiumChanged = { premium ->
            _state.update { it.copy(isPremium = premium) }
            scope.launch { store.setPremium(premium) }
            if (!premium) ads.preload()
        },
        onPricesLoaded = { monthly, lifetime ->
            _state.update { it.copy(monthlyPrice = monthly ?: it.monthlyPrice, lifetimePrice = lifetime ?: it.lifetimePrice) }
        },
    )

    init {
        scope.launch {
            store.data.collect { stored ->
                _state.update { it.copy(isPremium = stored.premium, rewardedUntilMs = stored.rewardedUntilMs) }
            }
        }
        scope.launch { ads.rewardedReady.collect { ready -> _state.update { it.copy(rewardedAvailable = ready) } } }
        billing.start()
    }

    /** En el onCreate de la actividad principal. */
    fun gatherConsent(activity: Activity) {
        AdsConsent.gather(activity) {
            _state.update { it.copy(adsReady = true) }
            ads.onSdkReady()
        }
    }

    /** En una pausa natural; [onDone] se llama siempre, haya anuncio o no. */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit = {}) = ads.maybeShowInterstitial(activity, onDone)

    fun unlockWithReward(activity: Activity, onClosed: (Boolean) -> Unit = {}) {
        ads.showRewarded(
            activity,
            onReward = {
                val until = System.currentTimeMillis() + REWARD_UNLOCK_MS
                _state.update { it.copy(rewardedUntilMs = until) }
                scope.launch { store.setRewardedUntil(until) }
            },
            onClosed = onClosed,
        )
    }

    fun buy(activity: Activity, option: PremiumOption): Boolean = billing.launchPurchase(activity, option)

    fun restorePurchases() = billing.refreshPurchases()

    fun isPrivacyOptionsRequired(activity: Activity) = AdsConsent.isPrivacyOptionsRequired(activity)

    fun showPrivacyOptions(activity: Activity) = AdsConsent.showPrivacyOptions(activity)

    private companion object {
        const val REWARD_UNLOCK_MS = 24 * 60 * 60 * 1000L
    }
}
