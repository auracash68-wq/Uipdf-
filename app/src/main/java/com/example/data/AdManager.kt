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

        // Frequency capping: at least 2 operations and 60 seconds between interstitials
        private const val MIN_OPERATIONS_BETWEEN_ADS = 2
        private const val MIN_INTERVAL_MILLIS = 60_000L
    }

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = AtomicBoolean(false)
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
     * when an eligible PDF operation finishes.
     */
    fun preloadInterstitial() {
        if (isPremium() || interstitialAd != null) return
        if (!isAdLoading.compareAndSet(false, true)) return

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
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
     * Call after a PDF operation completes successfully.
     * Checks frequency capping and presents interstitial if eligible.
     */
    fun onOperationCompleted(activity: Activity?, onAdDismissedOrSkipped: () -> Unit) {
        completedOperationsCount++

        if (isPremium() || activity == null) {
            onAdDismissedOrSkipped()
            return
        }

        val currentTime = System.currentTimeMillis()
        val isIntervalPassed = (currentTime - lastAdShownTimestamp) >= MIN_INTERVAL_MILLIS
        val isCountEligible = completedOperationsCount >= MIN_OPERATIONS_BETWEEN_ADS

        val ad = interstitialAd
        if (ad != null && isCountEligible && isIntervalPassed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastAdShownTimestamp = System.currentTimeMillis()
                    completedOperationsCount = 0
                    preloadInterstitial()
                    onAdDismissedOrSkipped()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    preloadInterstitial()
                    onAdDismissedOrSkipped()
                }
            }
            ad.show(activity)
        } else {
            // If ad is not yet cached, request preload immediately
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
