package com.testsoposiciones.app.monetization

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.testsoposiciones.app.R

/** Banner adaptativo anclado. Solo en pantallas de consulta, nunca en formularios a medio rellenar. */
@Composable
fun AdBanner(state: MonetizationState, modifier: Modifier = Modifier) {
    if (!state.showAds) return
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adView = remember(widthDp) {
        AdView(context).apply {
            adUnitId = AdUnits.BANNER
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    Box(modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}

/** Botón de la barra superior que abre la pantalla Premium; desaparece si ya es Premium. */
@Composable
fun PremiumAction(state: MonetizationState, onClick: () -> Unit) {
    if (state.isPremium) return
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.premium_title))
    }
}

/**
 * Muestra [content] si la función está desbloqueada; si no, una tarjeta para desbloquearla
 * 24 h con un anuncio recompensado o para siempre con Premium.
 */
@Composable
fun LockedFeature(
    state: MonetizationState,
    monetization: Monetization,
    title: String,
    description: String,
    onGoPremium: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (state.isUnlocked()) {
        content()
        return
    }
    val activity = LocalActivity.current
    var rewardFailed by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (state.adsReady) {
                OutlinedButton(
                    onClick = {
                        if (activity != null) {
                            monetization.unlockWithReward(activity) { earned -> rewardFailed = !earned }
                        }
                    },
                    enabled = state.rewardedAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.locked_watch_ad)) }
            }
            if (rewardFailed) {
                Text(
                    stringResource(R.string.rewarded_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = onGoPremium, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.locked_go_premium))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    state: MonetizationState,
    monetization: Monetization,
    features: List<String>,
    onBack: () -> Unit,
) {
    val activity: Activity? = LocalActivity.current
    var storeUnavailable by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.premium_back))
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                stringResource(if (state.isPremium) R.string.premium_active else R.string.premium_headline),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                (listOf(stringResource(R.string.premium_benefit_no_ads)) + features).forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(feature, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (!state.isPremium) {
                val buy = { option: PremiumOption ->
                    storeUnavailable = activity == null || !monetization.buy(activity, option)
                }
                Button(
                    onClick = { buy(PremiumOption.MONTHLY) },
                    enabled = state.monthlyPrice != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(state.monthlyPrice?.let { stringResource(R.string.premium_monthly, it) } ?: stringResource(R.string.premium_loading))
                }
                OutlinedButton(
                    onClick = { buy(PremiumOption.LIFETIME) },
                    enabled = state.lifetimePrice != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(state.lifetimePrice?.let { stringResource(R.string.premium_lifetime, it) } ?: stringResource(R.string.premium_loading))
                }
                if (storeUnavailable) {
                    Text(
                        stringResource(R.string.premium_store_unavailable),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = monetization::restorePurchases) { Text(stringResource(R.string.premium_restore)) }
                Text(
                    stringResource(R.string.premium_legal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
