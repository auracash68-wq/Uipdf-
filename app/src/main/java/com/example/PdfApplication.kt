package com.example

import android.app.Application
import com.example.data.AdManager
import com.example.data.BillingManager
import com.example.data.RecentPdfRepository
import com.example.data.SettingsRepository
import com.example.data.db.AppDatabase
import com.example.engine.FileUtils
import com.example.engine.PdfProcessor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var recentPdfRepository: RecentPdfRepository
        private set
    lateinit var billingManager: BillingManager
        private set
    lateinit var adManager: AdManager
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var pdfProcessor: PdfProcessor
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Room DB & Repositories
        database = AppDatabase.getDatabase(this)
        recentPdfRepository = RecentPdfRepository(database.pdfDao())
        billingManager = BillingManager(this)
        adManager = AdManager(this, billingManager)
        settingsRepository = SettingsRepository(this)
        pdfProcessor = PdfProcessor(this)

        // Clean up any stale temporary processing files from prior sessions
        FileUtils.cleanTempFiles(this)
    }
}
