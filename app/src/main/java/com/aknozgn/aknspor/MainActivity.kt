package com.aknozgn.aknspor

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.net.Uri
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.FilterInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    private var textToSpeech: TextToSpeech? = null
    @Volatile private var textToSpeechStatus = "loading"
    private val fotmobClient by lazy {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val network = connectivity.allNetworks.firstOrNull { candidate ->
            connectivity.getNetworkCapabilities(candidate)?.let { capabilities ->
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } == true
        } ?: connectivity.activeNetwork

        network?.let {
            OkHttpClient.Builder()
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        if (hostname == FOTMOB_HOST) it.getAllByName(hostname).toList()
                        else Dns.SYSTEM.lookup(hostname)
                })
                .socketFactory(it.socketFactory)
                .connectTimeout(PROXY_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(PROXY_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 61, 46)
        window.navigationBarColor = Color.rgb(11, 61, 46)
        window.decorView.systemUiVisibility = 0

        textToSpeech = TextToSpeech(this) { status ->
            textToSpeechStatus = if (status == TextToSpeech.SUCCESS) {
                val languageStatus = textToSpeech?.setLanguage(Locale("tr", "TR"))
                if (languageStatus == TextToSpeech.LANG_MISSING_DATA ||
                    languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
                ) "unavailable" else "ready"
            } else {
                "unavailable"
            }
        }

        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(238, 241, 238))
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            addJavascriptInterface(FavoriteAlertsBridge(), "AknSpor")
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    if (request.url.host == "appassets.androidplatform.net" &&
                        request.url.encodedPath?.startsWith("/proxy/") == true
                    ) {
                        return proxyRequest(request.url.encodedPath.orEmpty(), request.url.encodedQuery)
                    }
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }
        }

        setContentView(webView)
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl("$APP_ASSETS_URL/www/index.html")
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        FavoriteGoalAlerts.setForeground(this, true)
    }

    override fun onPause() {
        FavoriteGoalAlerts.setForeground(this, false)
        super.onPause()
    }

    @Deprecated("Deprecated in Android; back navigation remains supported by WebView.")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun proxyRequest(path: String, query: String?): WebResourceResponse {
        val upstreamUrl = when {
            path.startsWith(FOTMOB_PROXY_PREFIX) -> {
                val endpoint = path.removePrefix(FOTMOB_PROXY_PREFIX)
                val heatmapMatch = FOTMOB_HEATMAP_ENDPOINT_PATTERN.matchEntire(endpoint)
                val allowedHeatmapRequest = heatmapMatch?.groupValues?.get(1)?.let { matchId ->
                    query == "heatmapUrl=https%3A%2F%2Fpub.fotmob.com%2Fprod%2Fdb%2Fapi%2Fheatmap%2Fmatch%2F$matchId"
                } == true
                if ((endpoint !in FOTMOB_ENDPOINTS && !allowedHeatmapRequest) ||
                    query == null || (!allowedHeatmapRequest && !query.matches(FOTMOB_QUERY_PATTERN))
                ) {
                    return proxyError(400, "Geçersiz puan durumu isteği.")
                }

                if (endpoint == FOTMOB_PLAYER_PAGE_ENDPOINT) {
                    val playerId = query.removePrefix("playerId=")
                    "https://www.fotmob.com/players/$playerId"
                } else {
                    "https://www.fotmob.com/api/$endpoint?$query"
                }
            }

            path.startsWith(API_SPORTS_PROXY_PREFIX) -> {
                if (BuildConfig.API_SPORTS_KEY.isBlank()) {
                    return proxyError(503, "API anahtarı bu uygulama derlemesinde ayarlanmamış.")
                }
                val endpoint = path.removePrefix(API_SPORTS_PROXY_PREFIX)
                if (endpoint !in API_SPORTS_ENDPOINTS ||
                    (query != null && !query.matches(API_SPORTS_QUERY_PATTERN))
                ) {
                    return proxyError(400, "Geçersiz maç servisi isteği.")
                }
                "https://v3.football.api-sports.io/$endpoint" +
                    (query?.let { "?$it" } ?: "")
            }

            path.startsWith(NEWS_PROXY_PREFIX) -> {
                if (path.removePrefix(NEWS_PROXY_PREFIX) != "rss/search" ||
                    query == null || !query.matches(NEWS_QUERY_PATTERN)
                ) {
                    return proxyError(400, "Geçersiz haber araması.")
                }
                "https://news.google.com/rss/search?$query&hl=tr&gl=TR&ceid=TR:tr"
            }

            else -> return proxyError(404, "Servis bulunamadı.")
        }

        if (path.startsWith(FOTMOB_PROXY_PREFIX)) {
            return fotmobRequest(
                upstreamUrl,
                if (path.endsWith(FOTMOB_PLAYER_PAGE_ENDPOINT)) "text/html" else "application/json"
            )
        }

        if (path.startsWith(NEWS_PROXY_PREFIX)) {
            return newsRequest(upstreamUrl)
        }

        val connection = try {
            (URL(upstreamUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = PROXY_TIMEOUT_MS
                readTimeout = PROXY_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", PROXY_USER_AGENT)
                if (path.startsWith(API_SPORTS_PROXY_PREFIX)) {
                    setRequestProperty("x-apisports-key", BuildConfig.API_SPORTS_KEY)
                }
                connect()
            }
        } catch (error: IOException) {
            return proxyError(502, "Futbol servisine bağlanılamadı: ${error.message.orEmpty()}")
        }

        return try {
            val statusCode = connection.responseCode
            val stream = if (statusCode >= 400) connection.errorStream else connection.inputStream
            val body = stream ?: return proxyError(statusCode, connection.responseMessage.orEmpty())
            WebResourceResponse(
                "application/json",
                "UTF-8",
                statusCode,
                connection.responseMessage?.takeIf { it.isNotBlank() }
                    ?: if (statusCode >= 400) "Request Error" else "OK",
                proxyHeaders(),
                body
            )
        } catch (error: IOException) {
            connection.disconnect()
            proxyError(502, "Futbol servisinden yanıt alınamadı: ${error.message.orEmpty()}")
        }
    }

    private inner class FavoriteAlertsBridge {
        @JavascriptInterface
        fun syncFavoriteAlerts(enabled: Boolean, teamIds: String) {
            val ids = teamIds.split(',')
                .map(String::trim)
                .filter { it.matches(Regex("\\d{1,12}")) }
                .take(MAX_FAVORITE_ALERT_TEAMS)
                .toSet()
            val shouldEnable = enabled && ids.isNotEmpty() && BuildConfig.API_SPORTS_KEY.isNotBlank()
            FavoriteGoalAlerts.configure(this@MainActivity, shouldEnable, ids)
            if (shouldEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                runOnUiThread {
                    val preferences = getSharedPreferences(
                        NOTIFICATION_PREFERENCES,
                        MODE_PRIVATE
                    )
                    if (!preferences.getBoolean(NOTIFICATION_PERMISSION_ASKED, false)) {
                        preferences.edit().putBoolean(NOTIFICATION_PERMISSION_ASKED, true).apply()
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST
                        )
                    }

                }
            }
        }

        @JavascriptInterface
        fun syncDetailedAlerts(configuration: String) {
            val json = try {
                org.json.JSONObject(configuration)
            } catch (_: org.json.JSONException) {
                return
            }
            val enabled = json.optBoolean("enabled", false)
            val teams = json.optJSONArray("teams").toStringSet(MAX_FAVORITE_ALERT_TEAMS) {
                it.matches(Regex("\\d{1,12}"))
            }
            val matches = json.optJSONArray("matches").toStringSet(MAX_TRACKED_MATCHES) {
                it.matches(Regex("\\d{1,12}"))
            }
            val players = json.optJSONArray("players").toStringSet(MAX_TRACKED_PLAYER_ALERTS) {
                val parts = it.split('|', limit = 2)
                parts.size == 2 &&
                    parts[0].matches(Regex("\\d{1,12}")) &&
                    parts[1].length in 1..MAX_PLAYER_NAME_LENGTH
            }
            FavoriteGoalAlerts.configure(
                this@MainActivity,
                enabled,
                teams,
                matches,
                players
            )
            requestNotificationPermissionIfNeeded(enabled)
        }

        @JavascriptInterface
        fun notifyMatchEvent(key: String, title: String, text: String) {
            val safeKey = key.take(120).filter { it.isLetterOrDigit() || it in "_-" }
            val safeTitle = title.take(100)
            val safeText = text.take(240)
            if (safeKey.isBlank() || safeTitle.isBlank()) return
            runOnUiThread {
                FavoriteGoalAlerts.notify(
                    this@MainActivity,
                    safeKey.hashCode(),
                    safeTitle,
                    safeText
                )
            }
        }

        @JavascriptInterface
        fun speakPlayerName(name: String): String {
            if (name.isBlank() || name.length > 120) return "unavailable"
            if (textToSpeechStatus != "ready") return textToSpeechStatus
            val speech = textToSpeech ?: return "unavailable"
            runOnUiThread {
                speech.speak(name.trim(), TextToSpeech.QUEUE_FLUSH, null, "aknspor-player")
            }
            return "ready"
        }

        @JavascriptInterface
        fun openNewsArticle(articleUrl: String) {
            val uri = try {
                Uri.parse(articleUrl)
            } catch (_: IllegalArgumentException) {
                return
            }
            if (uri.scheme != "https" || uri.host != "news.google.com") return
            runOnUiThread {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        private fun requestNotificationPermissionIfNeeded(enabled: Boolean) {
            if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) return
            runOnUiThread {
                val preferences = getSharedPreferences(NOTIFICATION_PREFERENCES, MODE_PRIVATE)
                if (preferences.getBoolean(NOTIFICATION_PERMISSION_ASKED, false)) return@runOnUiThread
                preferences.edit().putBoolean(NOTIFICATION_PERMISSION_ASKED, true).apply()
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun org.json.JSONArray?.toStringSet(limit: Int, valid: (String) -> Boolean): Set<String> {
        if (this == null) return emptySet()
        return buildSet {
            for (index in 0 until minOf(length(), limit)) {
                val value = optString(index).trim()
                if (valid(value)) add(value)
            }
        }
    }

    private fun newsRequest(upstreamUrl: String): WebResourceResponse {
        val connection = try {
            (URL(upstreamUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = PROXY_TIMEOUT_MS
                readTimeout = PROXY_TIMEOUT_MS
                setRequestProperty("Accept", "application/rss+xml, application/xml")
                setRequestProperty("User-Agent", PROXY_USER_AGENT)
                connect()
            }
        } catch (error: IOException) {
            return proxyError(502, "Haber kaynağına bağlanılamadı: ${error.message.orEmpty()}")
        }
        return try {
            val statusCode = connection.responseCode
            val stream = if (statusCode >= 400) connection.errorStream else connection.inputStream
            val body = stream ?: return proxyError(statusCode, connection.responseMessage.orEmpty())
            WebResourceResponse(
                "application/rss+xml",
                "UTF-8",
                statusCode,
                connection.responseMessage?.takeIf { it.isNotBlank() }
                    ?: if (statusCode >= 400) "Request Error" else "OK",
                proxyHeaders("application/rss+xml; charset=utf-8"),
                body
            )
        } catch (error: IOException) {
            connection.disconnect()
            proxyError(502, "Haber yanıtı alınamadı: ${error.message.orEmpty()}")
        }
    }

    private fun fotmobRequest(upstreamUrl: String, accept: String): WebResourceResponse {
        val client = fotmobClient
            ?: return proxyError(503, "İnternet bağlantısı bulunamadı.")
        val response = try {
            client.newCall(
                Request.Builder()
                    .url(upstreamUrl)
                    .header("Accept", accept)
                    .header("Referer", FOTMOB_REFERER)
                    .header("User-Agent", FOTMOB_USER_AGENT)
                    .build()
            ).execute()
        } catch (error: IOException) {
            return proxyError(502, "Puan durumu servisine bağlanılamadı: ${error.message.orEmpty()}")
        }

        val body = response.body
        if (body == null) {
            response.close()
            return proxyError(response.code, response.message)
        }
        val contentType = body.contentType()
        return WebResourceResponse(
            contentType?.let { "${it.type}/${it.subtype}" } ?: "application/json",
            contentType?.charset(Charsets.UTF_8)?.name() ?: "UTF-8",
            response.code,
            response.message.ifBlank { if (response.isSuccessful) "OK" else "Request Error" },
            proxyHeaders(contentType?.toString() ?: "$accept; charset=utf-8"),
            object : FilterInputStream(body.byteStream()) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        response.close()
                    }
                }
            }
        )
    }

    private fun proxyError(statusCode: Int, message: String): WebResourceResponse {
        val json = """{"error":"${message.replace("\\", "\\\\").replace("\"", "\\\"")}"}"""
        return WebResourceResponse(
            "application/json",
            "UTF-8",
            statusCode,
            if (statusCode >= 500) "Bad Gateway" else "Request Error",
            proxyHeaders(),
            json.byteInputStream(Charsets.UTF_8)
        )
    }

    private fun proxyHeaders(contentType: String = "application/json; charset=utf-8") = mapOf(
        "Access-Control-Allow-Origin" to "*",
        "Cache-Control" to "no-store",
        "Content-Type" to contentType
    )

    private companion object {
        const val APP_ASSETS_URL = "https://appassets.androidplatform.net/assets"
        const val FOTMOB_PROXY_PREFIX = "/proxy/fotmob/"
        const val API_SPORTS_PROXY_PREFIX = "/proxy/apisports/"
        const val NEWS_PROXY_PREFIX = "/proxy/news/"
        const val PROXY_TIMEOUT_MS = 20_000
        const val MAX_FAVORITE_ALERT_TEAMS = 50
        const val MAX_TRACKED_MATCHES = 20
        const val MAX_TRACKED_PLAYER_ALERTS = 100
        const val MAX_PLAYER_NAME_LENGTH = 80
        const val NOTIFICATION_PERMISSION_REQUEST = 1401
        const val NOTIFICATION_PREFERENCES = "notification_permissions"
        const val NOTIFICATION_PERMISSION_ASKED = "notifications_asked"
        const val PROXY_USER_AGENT = "AknSpor-Android/1.0"
        const val FOTMOB_HOST = "www.fotmob.com"
        const val FOTMOB_REFERER = "https://www.fotmob.com/"
        const val FOTMOB_PLAYER_PAGE_ENDPOINT = "playerPage"
        val FOTMOB_HEATMAP_ENDPOINT_PATTERN = Regex("data/heatmap/match/(\\d{1,10})/heatmaps")
        const val FOTMOB_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
        val FOTMOB_ENDPOINTS = setOf("data/leagues", "data/teams", "data/matchDetails", FOTMOB_PLAYER_PAGE_ENDPOINT)
        val FOTMOB_QUERY_PATTERN =
            Regex(
                "(?:id=(?:47&ccode3=ENG|71&ccode3=TUR|87&ccode3=ESP|55&ccode3=ITA|" +
                    "54&ccode3=GER|53&ccode3=FRA|57&ccode3=NED|61&ccode3=POR|" +
                    "42&ccode3=INT|73&ccode3=INT)|" +
                    "playerId=\\d{1,10}|"+
                    "id=\\d{1,8}&ccode3=(?:TUR|ENG|ESP|ITA|GER|FRA|NED|POR|INT)|matchId=\\d{1,10})"
            )
        val API_SPORTS_ENDPOINTS = setOf("status", "fixtures", "fixtures/headtohead", "fixtures/players", "standings")
        val API_SPORTS_QUERY_PATTERN = Regex("[A-Za-z0-9_./=&:%+,-]+")
        val NEWS_QUERY_PATTERN = Regex("q=[A-Za-z0-9._~%+-]{1,500}")
    }
}
