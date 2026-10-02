package com.moneyfamily.app

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import com.android.billingclient.api.*

class PremiumBilling(
    context: Context,
    private val verifyPurchase: suspend (String) -> Boolean,
    private val onPremiumChanged: (Boolean) -> Unit,
    private val onPurchaseDetected: () -> Unit = {},
    private val onVerificationError: (String) -> Unit = {}
) {
    companion object { const val PRODUCT_ID = "moneyfamily_premium" }

    private val prefs = context.getSharedPreferences("moneyfamily_premium", Context.MODE_PRIVATE)
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> safe { handlePurchases(result, purchases) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var productDetails: ProductDetails? = null
    private var selectedOffer: ProductDetails.OneTimePurchaseOfferDetails? = null
    private var connected = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun connect() {
        safe {
            if (billingClient.isReady) {
                connected = true
                queryProduct()
                queryOwned()
            } else {
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        safe {
                            connected = result.responseCode == BillingClient.BillingResponseCode.OK
                            if (connected) { queryProduct(); queryOwned() }
                        }
                    }
                    override fun onBillingServiceDisconnected() { connected = false }
                })
            }
        }
    }

    fun isPremium(): Boolean =
        prefs.getBoolean("premium", false) ||
        (BuildConfig.INTERNAL_PREMIUM_TEST && prefs.getBoolean("internal_test_premium", false))

    fun enableInternalTestPremium() {
        if (!BuildConfig.INTERNAL_PREMIUM_TEST) return
        safe {
            prefs.edit().putBoolean("internal_test_premium", true).putBoolean("premium", true).apply()
            onPremiumChanged(true)
        }
    }

    fun isPremiumSetupRequired(): Boolean = prefs.getBoolean("premium_setup_required", false)

    fun recheckOwnedPurchases() {
        safe {
            if (!connected) { connect(); return@safe }
            prefs.getString("pending_purchase_token", null)?.let { verifyAndSetPremium(it) }
            queryOwned()
        }
    }

    fun launchPurchase(activity: Activity): Boolean? = safeResult {
        if (!connected || !billingClient.isReady) { connect(); return@safeResult false }
        val product = productDetails ?: return@safeResult false
        val offer = selectedOffer ?: product.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: return@safeResult false
        val offerToken = offer.offerToken ?: return@safeResult false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product).setOfferToken(offerToken).build()
        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build()
        ).responseCode == BillingClient.BillingResponseCode.OK
    }

    fun price(): String? = selectedOffer?.formattedPrice

    fun close() {
        safe { billingClient.endConnection() }
        connected = false
        scope.cancel()
    }

    private fun queryProduct() {
        safe {
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            ) { result, details ->
                safe {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        productDetails = details.productDetailsList.firstOrNull()
                        selectedOffer = productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                    } else { productDetails = null; selectedOffer = null }
                }
            }
        }
    }

    private fun queryOwned() {
        safe {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            ) { result, purchases ->
                safe { if (result.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(result, purchases) }
            }
        }
    }

    private fun handlePurchases(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases.orEmpty()
            .filter { it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                safe {
                    prefs.edit().putBoolean("premium_setup_required", true)
                        .putString("pending_purchase_token", purchase.purchaseToken).apply()
                    onPurchaseDetected()
                    if (!purchase.isAcknowledged) {
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        ) { ack -> safe { if (ack.responseCode == BillingClient.BillingResponseCode.OK) verifyAndSetPremium(purchase.purchaseToken) } }
                    } else verifyAndSetPremium(purchase.purchaseToken)
                }
            }
    }

    private fun verifyAndSetPremium(token: String) {
        scope.launch {
            runCatching { verifyPurchase(token) }
                .onSuccess { verified ->
                    if (verified) safe {
                        prefs.edit().putBoolean("premium", true).putBoolean("premium_setup_required", false)
                            .remove("pending_purchase_token").apply()
                        onPremiumChanged(true)
                    }
                }
                .onFailure { error ->
                    safe { onVerificationError(error.message ?: "Verifica Premium non completata.") }
                }
        }
    }

    private inline fun safe(block: () -> Unit) { runCatching { block() } }
    private inline fun <T> safeResult(block: () -> T): T? = runCatching { block() }.getOrNull()
}