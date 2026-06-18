package com.gseterminal.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * WebAppInterface — exposes native Android functions to JavaScript.
 *
 * Usage in your HTML/JS:
 *   window.AndroidBridge.showToast("Hello from JS!")
 *   window.AndroidBridge.openUrl("https://example.com")
 *   window.AndroidBridge.shareText("MTNGH is at ₵6.39")
 *   window.AndroidBridge.isAndroid()  → returns true
 *
 * ⚠️  Every method annotated with @JavascriptInterface runs on a
 *     background thread — do NOT touch UI from here directly.
 *     Use runOnUiThread{} for any UI calls.
 */
class WebAppInterface(private val context: Context) {

    /** Show a native Android Toast from JS */
    @JavascriptInterface
    fun showToast(message: String) {
        (context as? MainActivity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Open a URL in the system browser from JS */
    @JavascriptInterface
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Could not open link")
        }
    }

    /** Share text (e.g. a stock price or ticker) via the system share sheet */
    @JavascriptInterface
    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Lets your JS detect it's running inside the Android app.
     * Usage: if (window.AndroidBridge && window.AndroidBridge.isAndroid()) { ... }
     */
    @JavascriptInterface
    fun isAndroid(): Boolean = true

    /** Returns the app version name (e.g. "1.0.0") */
    @JavascriptInterface
    fun getAppVersion(): String = BuildConfig.VERSION_NAME
}
