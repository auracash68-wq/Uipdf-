package com.example.data

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.model.UserEntitlement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

class AdManager(
    private val context: Context,
    private val billingManager: BillingManager
) {
    companion object {
        // Standard Google Mobile Ads test IDs for development
        const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
        const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
        const val MEDIUM_RECTANGLE_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

        // Frequency capping: at least 2 full operations between interstitials
        private const val MIN_OPERATIONS_BETWEEN_ADS = 3
        // Minimum cooldown: 120 seconds between successfully shown interstitials
        private const val MIN_INTERVAL_MILLIS = 120_000L
    }

    private var interstitialAd: InterstitialAd? = null
    private val isAdLoading = AtomicBoolean(false)
    private val isAdShowing = AtomicBoolean(false)
    private var completedOperationsCount = 0
    private var lastAdShownTimestamp = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var retryAttempt = 0

    init {
        try {
            MobileAds.initialize(context) {
                mainHandler.post {
                    preloadInterstitial()
                }
            }
        } catch (_: Exception) {}
    }

    fun isPremium(): Boolean {
        return billingManager.entitlement.value == UserEntitlement.PREMIUM
    }

    /**
     * Preloads an interstitial ad proactively so that it is instantly available
     * without blocking or making the user wait when an eligible transition occurs.
     */
    fun preloadInterstitial() {
        if (isPremium() || interstitialAd != null) return
        if (!isAdLoading.compareAndSet(false, true)) return

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context.applicationContext,
                INTERSTITIAL_TEST_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isAdLoading.set(false)
                        retryAttempt = 0
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isAdLoading.set(false)
                        // Exponential backoff retry (max 3 retries, compliant with Google AdMob policy)
                        if (retryAttempt < 3) {
                            retryAttempt++
                            val delay = (retryAttempt * 5000L)
                            mainHandler.postDelayed({
                                preloadInterstitial()
                            }, delay)
                        }
                    }
                }
            )
        } catch (_: Exception) {
            isAdLoading.set(false)
        }
    }

    /**
     * Call ONLY after a PDF operation completes successfully at a natural transition point.
     * Evaluates strict Google AdMob policy safeguards:
     * 1. User is not Premium
     * 2. Activity is valid, active, not finishing or destroyed
     * 3. No other Interstitial is currently showing
     * 4. Preloaded Interstitial is ready immediately (no waiting / no blocking)
     * 5. Meaningful-action protection (at least 2 operations completed between impressions)
     * 6. Minimum 120-second cooldown since last successfully shown impression
     */
    fun onOperationCompleted(activity: Activity?, onAdDismissedOrSkipped: () -> Unit) {
        completedOperationsCount++

        // 1. Premium & Validity Checks
        if (isPremium() || activity == null || activity.isFinishing || activity.isDestroyed) {
            onAdDismissedOrSkipped()
            return
        }

        // 2. Concurrency check: Ensure no duplicate or concurrent ad presentation
        if (isAdShowing.get()) {
            onAdDismissedOrSkipped()
            return
        }

        val currentTime = System.currentTimeMillis()
        val isIntervalPassed = (currentTime - lastAdShownTimestamp) >= MIN_INTERVAL_MILLIS
        val isCountEligible = completedOperationsCount >= MIN_OPERATIONS_BETWEEN_ADS

        val ad = interstitialAd
        if (ad != null && isCountEligible && isIntervalPassed) {
            if (!isAdShowing.compareAndSet(false, true)) {
                onAdDismissedOrSkipped()
                return
            }

            var callbackTriggered = false
            fun triggerCallbackOnce() {
                if (!callbackTriggered) {
                    callbackTriggered = true
                    isAdShowing.set(false)
                    interstitialAd = null
                    preloadInterstitial()
                    mainHandler.post {
                        onAdDismissedOrSkipped()
                    }
                }
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // Update timestamp ONLY when the ad is actually shown
                    lastAdShownTimestamp = System.currentTimeMillis()
                    completedOperationsCount = 0
                }

                override fun onAdDismissedFullScreenContent() {
                    triggerCallbackOnce()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    triggerCallbackOnce()
                }
            }

            try {
                ad.show(activity)
            } catch (_: Exception) {
                triggerCallbackOnce()
            }
        } else {
            // Ad not ready or not eligible yet: Never wait or block the user
            if (interstitialAd == null) {
                preloadInterstitial()
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
