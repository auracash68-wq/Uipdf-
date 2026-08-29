package com.example.data

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.model.UserEntitlement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Standardized lifecycle and readiness state for all Google AdMob ad units.
 */
enum class AdState {
    IDLE,
    LOADING,
    READY,
    SHOWING,
    FAILED,
    EXPIRED,
    DISPOSED
}

/**
 * Performance diagnostics instrumentation for measuring
 * initialization, network loading, and perceived UI display latency.
 */
object AdPerformanceTracker {
    private const val TAG = "AdMobPerformance"

    var sdkInitDurationMs: Long = -1L
        private set

    fun logSdkInit(durationMs: Long) {
        sdkInitDurationMs = durationMs
        Log.d(TAG, "[SDK_INIT] Initialized in ${durationMs}ms")
    }

    fun logAdRequestStart(adType: String, adUnitId: String) {
        Log.d(TAG, "[$adType] Request started | AdUnit: $adUnitId")
    }

    fun logAdLoaded(adType: String, durationMs: Long) {
        Log.d(TAG, "[$adType] Ad loaded successfully in ${durationMs}ms")
    }

    fun logAdFailed(adType: String, durationMs: Long, error: LoadAdError) {
        Log.w(
            TAG,
            "[$adType] Ad load failed after ${durationMs}ms | Code: ${error.code} | Message: ${error.message}"
        )
    }

    fun logAdDisplayPerceivedTime(adType: String, perceivedWaitMs: Long, wasPreloaded: Boolean) {
        Log.d(
            TAG,
            "[$adType] Displayed to user | Perceived wait: ${perceivedWaitMs}ms | Was preloaded: $wasPreloaded"
        )
    }
}

/**
 * Manages Interstitial Ads with background preloading, expiration tracking,
 * exponential backoff retry, and Google-compliant frequency capping.
 */
class InterstitialAdManager(private val appContext: Context) {
    companion object {
        private const val AD_EXPIRATION_MILLIS = 3600_000L // 1 hour AdMob expiration
    }

    private val _adState = MutableStateFlow(AdState.IDLE)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var adLoadedTimestamp = 0L
    private var requestStartTime = 0L
    private val isAdLoading = AtomicBoolean(false)
    private val isAdShowing = AtomicBoolean(false)
    private var retryAttempt = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isAdReady(): Boolean {
        val ad = interstitialAd ?: return false
        val isExpired = (SystemClock.elapsedRealtime() - adLoadedTimestamp) > AD_EXPIRATION_MILLIS
        if (isExpired) {
            interstitialAd = null
            _adState.value = AdState.EXPIRED
            preload()
            return false
        }
        return _adState.value == AdState.READY
    }

    fun preload() {
        if (AdManager.getInstance(appContext).isPremium()) {
            _adState.value = AdState.DISPOSED
            return
        }

        if (isAdReady()) {
            return
        }

        if (!isAdLoading.compareAndSet(false, true)) {
            return
        }

        _adState.value = AdState.LOADING
        requestStartTime = SystemClock.elapsedRealtime()
        AdPerformanceTracker.logAdRequestStart("Interstitial", AdManager.INTERSTITIAL_TEST_ID)

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                appContext,
                AdManager.INTERSTITIAL_TEST_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        val duration = SystemClock.elapsedRealtime() - requestStartTime
                        interstitialAd = ad
                        adLoadedTimestamp = SystemClock.elapsedRealtime()
                        isAdLoading.set(false)
                        _adState.value = AdState.READY
                        retryAttempt = 0
                        AdPerformanceTracker.logAdLoaded("Interstitial", duration)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        val duration = SystemClock.elapsedRealtime() - requestStartTime
                        interstitialAd = null
                        isAdLoading.set(false)
                        _adState.value = AdState.FAILED
                        AdPerformanceTracker.logAdFailed("Interstitial", duration, error)

                        if (retryAttempt < 3) {
                            retryAttempt++
                            val backoffDelay = retryAttempt * 5000L
                            mainHandler.postDelayed({ preload() }, backoffDelay)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            isAdLoading.set(false)
            _adState.value = AdState.FAILED
        }
    }

    fun show(
        activity: Activity,
        onAdDismissed: () -> Unit
    ): Boolean {
        if (!isAdReady() || isAdShowing.get()) {
            return false
        }

        val ad = interstitialAd ?: return false
        if (!isAdShowing.compareAndSet(false, true)) {
            return false
        }

        _adState.value = AdState.SHOWING
        var callbackTriggered = false

        fun finishPresentation() {
            if (!callbackTriggered) {
                callbackTriggered = true
                isAdShowing.set(false)
                interstitialAd = null
                _adState.value = AdState.IDLE
                preload()
                mainHandler.post { onAdDismissed() }
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdPerformanceTracker.logAdDisplayPerceivedTime("Interstitial", 0L, wasPreloaded = true)
            }

            override fun onAdDismissedFullScreenContent() {
                finishPresentation()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w("AdMobPerformance", "[Interstitial] Failed to show: ${error.message}")
                finishPresentation()
            }
        }

        try {
            ad.show(activity)
            return true
        } catch (e: Exception) {
            finishPresentation()
            return false
        }
    }
}

/**
 * Centralized, lifecycle-aware AdManager orchestrating all Google AdMob
 * operations, preloading pipelines, and policy enforcement across Sweet PDF.
 */
class AdManager private constructor(private val appContext: Context) {

    companion object {
        // Standard Google Mobile Ads test IDs for development
        const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
        const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
        const val MEDIUM_RECTANGLE_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

        // Frequency capping: at least 3 completed operations between interstitials
        private const val MIN_OPERATIONS_BETWEEN_ADS = 3
        // Minimum cooldown: 120 seconds between successfully shown interstitials
        private const val MIN_INTERVAL_MILLIS = 120_000L

        @Volatile
        private var INSTANCE: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val isInitialized = AtomicBoolean(false)

        /**
         * Asynchronously initializes the Google Mobile Ads SDK as early as possible
         * at Application launch without blocking the UI thread.
         */
        fun initialize(context: Context) {
            if (isInitialized.compareAndSet(false, true)) {
                val startTime = SystemClock.elapsedRealtime()
                try {
                    val requestConfig = RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                        .build()
                    MobileAds.setRequestConfiguration(requestConfig)

                    MobileAds.initialize(context.applicationContext) { status ->
                        val duration = SystemClock.elapsedRealtime() - startTime
                        AdPerformanceTracker.logSdkInit(duration)

                        val instance = getInstance(context.applicationContext)
                        instance.interstitialAdManager.preload()
                    }
                } catch (e: Exception) {
                    Log.e("AdMobPerformance", "Error initializing Mobile Ads SDK: ${e.message}")
                }
            }
        }
    }

    private val billingManager = BillingManager(appContext)
    val interstitialAdManager = InterstitialAdManager(appContext)

    private var completedOperationsCount = 0
    private var lastAdShownTimestamp = 0L

    fun isPremium(): Boolean {
        return billingManager.entitlement.value == UserEntitlement.PREMIUM
    }

    /**
     * Call ONLY after a PDF operation completes successfully at a natural transition point.
     * Evaluates strict Google AdMob policy safeguards:
     * 1. User is not Premium
     * 2. Activity is valid, active, not finishing or destroyed
     * 3. Preloaded Interstitial is ready immediately (no waiting / no UI blocking)
     * 4. Meaningful-action protection (at least 3 operations completed between impressions)
     * 5. Minimum 120-second cooldown since last successfully shown impression
     */
    fun onOperationCompleted(activity: Activity?, onAdDismissedOrSkipped: () -> Unit) {
        completedOperationsCount++

        // 1. Premium & Validity Checks
        if (isPremium() || activity == null || activity.isFinishing || activity.isDestroyed) {
            onAdDismissedOrSkipped()
            return
        }

        val currentTime = SystemClock.elapsedRealtime()
        val isIntervalPassed = (currentTime - lastAdShownTimestamp) >= MIN_INTERVAL_MILLIS
        val isCountEligible = completedOperationsCount >= MIN_OPERATIONS_BETWEEN_ADS

        if (interstitialAdManager.isAdReady() && isCountEligible && isIntervalPassed) {
            val shown = interstitialAdManager.show(activity) {
                lastAdShownTimestamp = SystemClock.elapsedRealtime()
                completedOperationsCount = 0
                onAdDismissedOrSkipped()
            }
            if (!shown) {
                onAdDismissedOrSkipped()
            }
        } else {
            if (!interstitialAdManager.isAdReady()) {
                interstitialAdManager.preload()
            }
            onAdDismissedOrSkipped()
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onUserRewarded: () -> Unit,
        onAdUnavailable: () -> Unit
    ) {
        if (isPremium()) {
            onUserRewarded()
            return
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            REWARDED_TEST_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {}
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            onAdUnavailable()
                        }
                    }
                    ad.show(activity) {
                        onUserRewarded()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdUnavailable()
                }
            }
        )
    }
}
