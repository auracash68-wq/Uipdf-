package com.example

import android.app.Application
import com.example.data.AdManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Application class initializing on-device PDF engine dependencies
 * and warming up the Google Mobile Ads SDK asynchronously at cold start.
 */
class PdfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(this)
        } catch (_: Throwable) {
            // Handled gracefully in engine
        }

        try {
            // Asynchronously initialize Google Mobile Ads SDK early
            AdManager.initialize(this)
        } catch (_: Throwable) {
            // Graceful fallback for non-GMS environments
        }
    }
}
