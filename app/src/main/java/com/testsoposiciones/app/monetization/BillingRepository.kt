package com.testsoposiciones.app.monetization

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

enum class PremiumOption { MONTHLY, LIFETIME }

/**
 * Compras de Google Play: suscripción mensual [MONTHLY_ID] y compra única [LIFETIME_ID].
 * La tienda manda: al conectar se consultan las compras y, si la suscripción caducó, vuelven los anuncios.
 */
class BillingRepository(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit,
    private val onPricesLoaded: (monthly: String?, lifetime: String?) -> Unit,
) {
    private var monthly: ProductDetails? = null
    private var lifetime: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                handle(purchases)
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    refreshPurchases()
                }
            }

            // Con enableAutoServiceReconnection la librería reconecta sola.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Devuelve false si los productos aún no están cargados (sin conexión con la tienda). */
    fun launchPurchase(activity: Activity, option: PremiumOption): Boolean {
        val details = (if (option == PremiumOption.MONTHLY) monthly else lifetime) ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { details.subscriptionOfferDetails?.firstOrNull()?.let { setOfferToken(it.offerToken) } }
            .build()
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build(),
        )
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /** "Restaurar compras": vuelve a preguntar a la tienda. */
    fun refreshPurchases() {
        if (!client.isReady) return
        client.queryPurchasesAsync(query(ProductType.SUBS)) { subsResult, subs ->
            client.queryPurchasesAsync(query(ProductType.INAPP)) { inAppResult, inApp ->
                val ok = BillingClient.BillingResponseCode.OK
                // Sin respuesta completa de la tienda se mantiene lo guardado.
                if (subsResult.responseCode == ok && inAppResult.responseCode == ok) {
                    val all = subs + inApp
                    acknowledge(all)
                    onPremiumChanged(all.any { it.ownsPremium() })
                }
            }
        }
    }

    private fun handle(purchases: List<Purchase>) {
        acknowledge(purchases)
        if (purchases.any { it.ownsPremium() }) onPremiumChanged(true)
    }

    private fun acknowledge(purchases: List<Purchase>) {
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { purchase ->
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                ) {}
            }
    }

    private fun queryProducts() {
        client.queryProductDetailsAsync(productQuery(MONTHLY_ID, ProductType.SUBS)) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                monthly = details.productDetailsList.firstOrNull()
                publishPrices()
            }
        }
        client.queryProductDetailsAsync(productQuery(LIFETIME_ID, ProductType.INAPP)) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                lifetime = details.productDetailsList.firstOrNull()
                publishPrices()
            }
        }
    }

    private fun publishPrices() {
        val monthlyPrice = monthly?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        @Suppress("DEPRECATION")
        val lifetimePrice = lifetime?.oneTimePurchaseOfferDetails?.formattedPrice
        onPricesLoaded(monthlyPrice, lifetimePrice)
    }

    private fun Purchase.ownsPremium() =
        purchaseState == Purchase.PurchaseState.PURCHASED && products.any { it == MONTHLY_ID || it == LIFETIME_ID }

    private fun query(type: String) = QueryPurchasesParams.newBuilder().setProductType(type).build()

    private fun productQuery(id: String, type: String) = QueryProductDetailsParams.newBuilder()
        .setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()),
        )
        .build()

    companion object {
        /** Crear en Play Console con estos IDs: suscripción mensual y producto único. */
        const val MONTHLY_ID = "premium_monthly"
        const val LIFETIME_ID = "premium_lifetime"
    }
}
