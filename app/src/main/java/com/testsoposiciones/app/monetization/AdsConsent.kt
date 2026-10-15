package com.testsoposiciones.app.monetization

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pide el consentimiento (RGPD) con UMP antes de iniciar AdMob.
 * Nunca se solicitan anuncios antes de que [onReady] se llame.
 */
object AdsConsent {
    private val initialized = AtomicBoolean(false)

    fun gather(activity: Activity, onReady: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (info.canRequestAds()) initialize(activity, onReady)
                }
            },
            { if (info.canRequestAds()) initialize(activity, onReady) },
        )
        // Consentimiento de una sesión anterior: no hace falta esperar.
        if (info.canRequestAds()) initialize(activity, onReady)
    }

    fun isPrivacyOptionsRequired(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
    }

    private fun initialize(activity: Activity, onReady: () -> Unit) {
        if (initialized.getAndSet(true)) {
            onReady()
            return
        }
        MobileAds.initialize(activity.applicationContext) {}
        onReady()
    }
}
