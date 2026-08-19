package com.example.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.example.model.UserEntitlement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_PREMIUM = "premium_lifetime"
        const val PRICE_DISPLAY = "₹29"
        private const val PREFS_NAME = "universal_pdf_billing_prefs"
        private const val KEY_IS_PREMIUM = "is_premium_cached"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _entitlement = MutableStateFlow<UserEntitlement>(
        if (prefs.getBoolean(KEY_IS_PREMIUM, false)) UserEntitlement.PREMIUM else UserEntitlement.FREE
    )
    val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

    private val _billingMessage = MutableSharedFlow<String>()
    val billingMessage: SharedFlow<String> = _billingMessage.asSharedFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startBillingConnection()
    }

    fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Connection will be re-attempted on demand or app foreground
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = queryProductDetailsResult.firstOrNull { it.productId == PRODUCT_ID_PREMIUM }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails
        if (details != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        } else {
            // Re-attempt query and inform user
            queryProductDetails()
            scope.launch {
                _billingMessage.emit("Connecting to Google Play Store. Please try again in a moment.")
            }
        }
    }

    fun restorePurchases() {
        queryPurchases(isUserInitiatedRestore = true)
    }

    private fun queryPurchases(isUserInitiatedRestore: Boolean = false) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        scope.launch {
            val purchasesResult = billingClient.queryPurchasesAsync(params)
            if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchasesResult.purchasesList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PREMIUM) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (hasPremium) {
                    setPremiumActive(true)
                    purchasesResult.purchasesList.forEach { handlePurchase(it) }
                    if (isUserInitiatedRestore) {
                        _billingMessage.emit("Purchase restored successfully! Premium is active.")
                    }
                } else {
                    if (isUserInitiatedRestore) {
                        _billingMessage.emit("No previous purchases found for this Google account.")
                    }
                }
            } else if (isUserInitiatedRestore) {
                _billingMessage.emit("Unable to reach Google Play Store. Check connection.")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // User intentionally cancelled the purchase dialog
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                setPremiumActive(true)
                scope.launch {
                    _billingMessage.emit("You already own Premium! Restoring access.")
                }
            }
            else -> {
                scope.launch {
                    _billingMessage.emit("Purchase could not be completed.")
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(PRODUCT_ID_PREMIUM)) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                setPremiumActive(true)

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            setPremiumActive(true)
                        }
                    }
                }
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                scope.launch {
                    _billingMessage.emit("Your purchase is pending confirmation.")
                }
            }
        }
    }

    private fun setPremiumActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, active).apply()
        _entitlement.value = if (active) UserEntitlement.PREMIUM else UserEntitlement.FREE
    }
}
