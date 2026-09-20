package com.moneyfamily.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class PremiumBilling(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit
) {
    companion object { const val PRODUCT_ID = "moneyfamily_premium" }

    private val prefs = context.getSharedPreferences("moneyfamily_premium", Context.MODE_PRIVATE)
    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> handlePurchases(result, purchases) }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var productDetails: ProductDetails? = null

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    queryOwned()
                }
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun isPremium(): Boolean = prefs.getBoolean("premium", false)

    fun launchPurchase(activity: Activity): Boolean {
        val product = productDetails ?: return false
        val offer = product.oneTimePurchaseOfferDetailsList.firstOrNull() ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offer.offerToken)
            .build()
        billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(params))
                .build()
        )
        return true
    }

    fun price(): String? =
        productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice

    fun close() {
        billingClient.endConnection()
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        ) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = details.productDetailsList.firstOrNull()
            }
        }
    }

    private fun queryOwned() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(result, purchases)
        }
    }

    private fun handlePurchases(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return
        purchases.orEmpty().filter { it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                if (!purchase.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { ack ->
                        if (ack.responseCode == BillingClient.BillingResponseCode.OK) setPremium()
                    }
                } else {
                    setPremium()
                }
            }
    }

    private fun setPremium() {
        prefs.edit().putBoolean("premium", true).apply()
        onPremiumChanged(true)
    }
}
