package com.testsoposiciones.app.monetization

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Anuncios a pantalla completa con límites de frecuencia:
 * - Intersticial: solo en pausas naturales que decide cada pantalla, como mucho cada [INTERSTITIAL_GAP_MS].
 * - App Open: al volver a la app desde segundo plano (nunca en el primer arranque, que es cuando sale el consentimiento).
 * - Recompensado: solo cuando el usuario lo pide para desbloquear algo.
 * Con Premium no se carga ni se muestra nada.
 */
class AdsManager(
    private val app: Application,
    private val store: MonetizationStore,
    private val scope: CoroutineScope,
    private val isPremium: () -> Boolean,
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private var sdkReady = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var loadingAppOpen = false
    private var showingFullScreen = false
    private var lastFullScreenAt = 0L
    private var currentActivity: Activity? = null
    private var firstStart = true

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch { lastFullScreenAt = store.lastFullScreenAt() }
    }

    fun onSdkReady() {
        if (sdkReady) return
        sdkReady = true
        preload()
    }

    fun preload() {
        if (!sdkReady || isPremium()) return
        loadInterstitial()
        loadRewarded()
        loadAppOpen()
    }

    /** Llamar en una pausa natural (fin de un test, tras un cálculo…). [onDone] se llama siempre. */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null || !canShowFullScreen(INTERSTITIAL_GAP_MS)) {
            loadInterstitial()
            onDone()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = callback(onDone) { loadInterstitial() }
        markShown()
        ad.show(activity)
    }

    /** [onClosed] recibe si el usuario ganó la recompensa. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onClosed: (Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null || !sdkReady || showingFullScreen) {
            loadRewarded()
            onClosed(false)
            return
        }
        rewarded = null
        _rewardedReady.value = false
        var earned = false
        ad.fullScreenContentCallback = callback({ onClosed(earned) }) { loadRewarded() }
        markShown()
        ad.show(activity) {
            earned = true
            onReward()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (firstStart) {
            firstStart = false
            return
        }
        showAppOpenIfAvailable()
    }

    private fun showAppOpenIfAvailable() {
        val activity = currentActivity ?: return
        val ad = appOpen
        if (ad == null || now() - appOpenLoadedAt > APP_OPEN_EXPIRY_MS) {
            appOpen = null
            loadAppOpen()
            return
        }
        if (!canShowFullScreen(APP_OPEN_GAP_MS)) return
        appOpen = null
        ad.fullScreenContentCallback = callback({}) { loadAppOpen() }
        markShown()
        ad.show(activity)
    }

    private fun loadInterstitial() {
        if (!canLoad() || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            app,
            AdUnits.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                }
            },
        )
    }

    private fun loadRewarded() {
        if (!sdkReady || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            app,
            AdUnits.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    loadingRewarded = false
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                }
            },
        )
    }

    private fun loadAppOpen() {
        if (!canLoad() || appOpen != null || loadingAppOpen) return
        loadingAppOpen = true
        AppOpenAd.load(
            app,
            AdUnits.APP_OPEN,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpen = ad
                    appOpenLoadedAt = now()
                    loadingAppOpen = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingAppOpen = false
                }
            },
        )
    }

    private fun callback(onDone: () -> Unit, reload: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() {
            showingFullScreen = true
        }

        override fun onAdDismissedFullScreenContent() {
            showingFullScreen = false
            reload()
            onDone()
        }

        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            showingFullScreen = false
            reload()
            onDone()
        }
    }

    private fun canLoad() = sdkReady && !isPremium()

    private fun canShowFullScreen(gapMs: Long) =
        canLoad() && !showingFullScreen && now() - lastFullScreenAt >= gapMs

    private fun markShown() {
        lastFullScreenAt = now()
        val shownAt = lastFullScreenAt
        scope.launch { store.setLastFullScreenAt(shownAt) }
    }

    private fun now() = System.currentTimeMillis()

    private fun Activity.isAdActivity() = javaClass.name.startsWith("com.google.android.gms.ads")

    override fun onActivityStarted(activity: Activity) {
        if (!activity.isAdActivity()) currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        if (!activity.isAdActivity()) currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    private companion object {
        const val INTERSTITIAL_GAP_MS = 3 * 60 * 1000L
        const val APP_OPEN_GAP_MS = 5 * 60 * 1000L
        const val APP_OPEN_EXPIRY_MS = 4 * 60 * 60 * 1000L
    }
}
