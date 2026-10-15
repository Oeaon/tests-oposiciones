package com.testsoposiciones.app.monetization

import com.testsoposiciones.app.BuildConfig

/** IDs de AdMob: los de prueba de Google en debug y los de `admob.properties` en release. */
object AdUnits {
    val BANNER: String = BuildConfig.AD_BANNER
    val INTERSTITIAL: String = BuildConfig.AD_INTERSTITIAL
    val APP_OPEN: String = BuildConfig.AD_APP_OPEN
    val REWARDED: String = BuildConfig.AD_REWARDED
}
