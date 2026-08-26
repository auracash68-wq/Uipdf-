package com.example

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Application class initializing on-device PDF engine dependencies.
 */
class PdfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(this)
        } catch (_: Throwable) {
            // Handled gracefully in engine
        }
    }
}
