package com.gseterminal.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.gseterminal.app.databinding.ActivityMainBinding

/**
 * MainActivity — hosts the GSE Terminal WebView.
 *
 * Features:
 *  • Hardware-accelerated WebView (smooth 60 fps scrolling)
 *  • JavaScript + localStorage + DOM storage enabled
 *  • Custom WebViewClient that intercepts external links
 *  • Pull-to-refresh reloads the page
 *  • Offline / no-network error page
 *  • JS → Native bridge (WebAppInterface)
 *  • Back-button handled correctly (WebView history first)
 *  • Safe Browsing via AndroidX WebKit
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ── lifecycle ──────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureWebView()
        setupSwipeRefresh()
        handleBackPress()

        if (isNetworkAvailable()) {
            loadApp()
        } else {
            showOfflineError()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onDestroy() {
        // Prevent WebView memory leaks
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    // ── WebView configuration ──────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        binding.webView.apply {

            // ── Settings ──
            settings.apply {
                javaScriptEnabled      = true          // Required — app is JS-driven
                domStorageEnabled      = true          // localStorage (chat, portfolio)
                databaseEnabled        = true
                allowFileAccess        = false         // No file:// access needed
                allowContentAccess     = false
                mixedContentMode       = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode              = WebSettings.LOAD_DEFAULT
                useWideViewPort        = true
                loadWithOverviewMode   = true
                setSupportZoom(false)                  // GSE UI handles its own zoom
                builtInZoomControls    = false
                displayZoomControls    = false
                mediaPlaybackRequiresUserGesture = false
                userAgentString        = userAgentString + " GSETerminal/1.0"
            }

            // ── Safe Browsing (API 26+) ──
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, true)
            }

            // ── JavaScript → Native bridge ──
            addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidBridge")

            // ── WebViewClient ──
            webViewClient = GSEWebViewClient()

            // ── WebChromeClient (JS alerts, console logs) ──
            webChromeClient = GSEWebChromeClient()
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    private fun loadApp() {
        binding.errorLayout.visibility = View.GONE
        binding.webView.visibility     = View.VISIBLE
        // Load the bundled HTML from assets — no server needed
        binding.webView.loadUrl("file:///android_asset/index.html")
    }

    // ── SwipeRefresh ───────────────────────────────────────────────────────────

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(
                R.color.gse_green,
                R.color.gse_amber,
                R.color.gse_blue
            )
            setOnRefreshListener {
                if (isNetworkAvailable()) {
                    binding.webView.reload()
                } else {
                    isRefreshing = false
                    showOfflineError()
                }
            }
        }
    }

    // ── Back press ────────────────────────────────────────────────────────────

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.webView.canGoBack() -> binding.webView.goBack()
                    else -> finish()
                }
            }
        })
    }

    // ── Offline error UI ─────────────────────────────────────────────────────

    private fun showOfflineError() {
        binding.webView.visibility  = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false
    }

    fun retryLoad() {   // called by XML onClick
        if (isNetworkAvailable()) {
            loadApp()
        } else {
            Toast.makeText(this, "Still offline. Check your connection.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Network check ─────────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner: WebViewClient
    // ═════════════════════════════════════════════════════════════════════════

    private inner class GSEWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressBar.visibility    = View.VISIBLE
            binding.swipeRefresh.isRefreshing = false
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            binding.progressBar.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            // Only show error page for main-frame failures, not sub-resources
            if (request.isForMainFrame) {
                showOfflineError()
            }
        }

        /**
         * Intercept URL loads.
         * • file:// (our bundled HTML) → let WebView handle normally
         * • External HTTPS links (e.g. news article) → open in system browser
         * • Everything else → block
         */
        override fun shouldOverrideUrlLoading(
            view: WebView, request: WebResourceRequest
        ): Boolean {
            val url = request.url.toString()
            return when {
                url.startsWith("file://") || url.startsWith("about:") -> false
                url.startsWith("https://") || url.startsWith("http://") -> {
                    openInBrowser(url)
                    true
                }
                else -> true  // block unknown schemes
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner: WebChromeClient
    // ═════════════════════════════════════════════════════════════════════════

    private inner class GSEWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            binding.progressBar.progress = newProgress
            if (newProgress == 100) {
                binding.progressBar.visibility = View.GONE
            }
        }

        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            // Forward JS console logs to Logcat for easier debugging
            android.util.Log.d("GSE_JS", "[${message.sourceId()}:${message.lineNumber()}] ${message.message()}")
            return true
        }

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            result.confirm()
            return true
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helper
    // ═════════════════════════════════════════════════════════════════════════

    private fun openInBrowser(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }
}
