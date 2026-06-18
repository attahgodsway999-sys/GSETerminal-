package com.gseterminal.app

import android.app.Application
import android.webkit.WebView

/**
 * GSEApplication — Application subclass.
 * Registered in AndroidManifest via android:name=".GSEApplication"
 */
class GSEApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable WebView debugging in debug builds only
        // Inspect via chrome://inspect in desktop Chrome
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
