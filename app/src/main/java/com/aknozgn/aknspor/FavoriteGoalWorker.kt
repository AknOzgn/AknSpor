package com.aknozgn.aknspor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

internal object FavoriteGoalAlerts {
    private const val WORK_NAME = "favorite-goal-alerts"
    private const val PREFERENCES = "favorite_goal_alerts"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TEAM_IDS = "team_ids"
    private const val KEY_MATCH_IDS = "match_ids"
    private const val KEY_PLAYER_WATCHES = "player_watches"
    private const val KEY_APP_FOREGROUND = "app_foreground"

    fun configure(
        context: Context,
        enabled: Boolean,
        teamIds: Set<String>,
        matchIds: Set<String> = emptySet(),
        playerWatches: Set<String> = emptySet()
    ) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val active = enabled &&
            (teamIds.isNotEmpty() || matchIds.isNotEmpty() || playerWatches.isNotEmpty()) &&
            BuildConfig.API_SPORTS_KEY.isNotBlank()
        preferences.edit()
            .putBoolean(KEY_ENABLED, active)
            .putStringSet(KEY_TEAM_IDS, teamIds.toSet())
            .putStringSet(KEY_MATCH_IDS, matchIds.toSet())
            .putStringSet(KEY_PLAYER_WATCHES, playerWatches.toSet())
            .apply()

        val workManager = WorkManager.getInstance(context)
        if (!active) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<FavoriteGoalWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    internal fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun notify(context: Context, notificationId: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        FavoriteGoalNotification.post(context, notificationId, title, text)
    }

    fun setForeground(context: Context, foreground: Boolean) {
        preferences(context).edit().putBoolean(KEY_APP_FOREGROUND, foreground).apply()
    }

    internal const val ENABLED_KEY = KEY_ENABLED
    internal const val TEAM_IDS_KEY = KEY_TEAM_IDS
    internal const val MATCH_IDS_KEY = KEY_MATCH_IDS
    internal const val PLAYER_WATCHES_KEY = KEY_PLAYER_WATCHES
}

class FavoriteGoalWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        if (BuildConfig.API_SPORTS_KEY.isBlank()) return Result.success()
        val preferences = FavoriteGoalAlerts.preferences(applicationContext)
        if (!preferences.getBoolean(FavoriteGoalAlerts.ENABLED_KEY, false)) return Result.success()
        val teamIds = preferences.getStringSet(FavoriteGoalAlerts.TEAM_IDS_KEY, emptySet())
            .orEmpty()
        val matchIds = preferences.getStringSet(FavoriteGoalAlerts.MATCH_IDS_KEY, emptySet())
            .orEmpty()
        val playerWatches = preferences.getStringSet(FavoriteGoalAlerts.PLAYER_WATCHES_KEY, emptySet())
            .orEmpty()
        if (teamIds.isEmpty() && matchIds.isEmpty() && playerWatches.isEmpty()) return Result.success()

        return try {
            val fixtures = fetchLiveFixtures()
            val trackedFixtures = if (matchIds.isEmpty() && playerWatches.isEmpty()) {
                emptyList()
            } else {
                fetchTrackedFixtures((matchIds + playerWatches.mapNotNull { it.substringBefore('|').takeIf(String::isNotBlank) })
                    .filter { it.matches(Regex("\\d{1,12}")) }
                    .distinct()
                    .take(MAX_TRACKED_MATCHES))
            }
            val editor = preferences.edit()
            val allFixtures = (fixtures + trackedFixtures).distinctBy { it.optJSONObject("fixture")?.optLong("id") }
            allFixtures.forEach { fixture ->
                val fixtureId = fixture.optJSONObject("fixture")?.optLong("id") ?: return@forEach
                val teams = fixture.optJSONObject("teams") ?: return@forEach
                val goals = fixture.optJSONObject("goals") ?: return@forEach
                val home = teams.optJSONObject("home") ?: return@forEach
                val away = teams.optJSONObject("away") ?: return@forEach
                val homeId = home.optLong("id").toString()
                val awayId = away.optLong("id").toString()
                val fixtureIdText = fixtureId.toString()
                if (homeId !in teamIds && awayId !in teamIds &&
                    fixtureIdText !in matchIds &&
                    playerWatches.none { it.startsWith("$fixtureIdText|") }
                ) return@forEach
                val homeScore = goals.optionalScore("home")
                val awayScore = goals.optionalScore("away")
                val scoreKey = "fixture_$fixtureId"
                val previous = preferences.getString(scoreKey, null)
                if (previous != null) {
                    val oldScores = previous.split(':')
                    val oldHome = oldScores.getOrNull(0)?.toIntOrNull()
                    val oldAway = oldScores.getOrNull(1)?.toIntOrNull()
                    val homeScored = homeId in teamIds &&
                        homeScore != null && oldHome != null && homeScore > oldHome
                    val awayScored = awayId in teamIds &&
                        awayScore != null && oldAway != null && awayScore > oldAway
                    if (homeScored) {
                        notifyGoal(
                            "goal_${fixtureId}_$homeId".hashCode(),
                            home.optString("name", "Takım"),
                            homeScore,
                            awayScore,
                            away.optString("name", "Rakip")
                        )
                    }
                    if (awayScored) {
                        notifyGoal(
                            "goal_${fixtureId}_$awayId".hashCode(),
                            away.optString("name", "Takım"),
                            awayScore,
                            homeScore,
                            home.optString("name", "Rakip")
                        )
                    }
                }
                editor.putString(scoreKey, "${homeScore ?: "?"}:${awayScore ?: "?"}")
                checkMatchAlerts(fixture, fixtureIdText, matchIds, playerWatches, preferences, editor)
            }
            editor.apply()
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: org.json.JSONException) {
            Result.retry()
        }
    }

    private fun checkMatchAlerts(
        fixture: JSONObject,
        fixtureId: String,
        matchIds: Set<String>,
        playerWatches: Set<String>,
        preferences: android.content.SharedPreferences,
        editor: android.content.SharedPreferences.Editor
    ) {
        val watchedPlayers = playerWatches.mapNotNull { entry ->
            val parts = entry.split('|', limit = 2)
            if (parts.size == 2 && parts[0] == fixtureId) parts[1] else null
        }.toSet()
        if (fixtureId !in matchIds && watchedPlayers.isEmpty()) return

        val status = fixture.optJSONObject("fixture")?.optJSONObject("status")
        val currentStatus = status?.optString("short").orEmpty()
        val previousStatus = preferences.getString("status_$fixtureId", null)
        if (fixtureId in matchIds && previousStatus != null) {
            if (previousStatus in NOT_STARTED && currentStatus in LIVE) {
                notify("match_${fixtureId}_start", "Maç başladı", matchDescription(fixture))
            } else if (previousStatus in LIVE && currentStatus in FINISHED) {
                notify("match_${fixtureId}_finish", "Maç sona erdi", matchDescription(fixture))
            }
        }
        if (currentStatus.isNotBlank()) editor.putString("status_$fixtureId", currentStatus)

        if (watchedPlayers.isEmpty()) return
        val events = fixture.optJSONArray("events") ?: return
        val seen = preferences.getStringSet("events_$fixtureId", emptySet()).orEmpty().toMutableSet()
        val isBaseline = !preferences.getBoolean("events_initialized_$fixtureId", false)
        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            if (event.optString("type") != "Goal") continue
            val playerName = event.optJSONObject("player")?.optString("name").orEmpty()
            val time = event.optJSONObject("time")?.optInt("elapsed", -1)?.takeIf { it >= 0 }
                ?: event.optString("time", "")
            val eventKey = "$time|$playerName"
            val isNewEvent = seen.add(eventKey)
            if (!isBaseline && isNewEvent && playerName in watchedPlayers) {
                notify(
                    "player_${fixtureId}_$eventKey",
                    "Gol! $playerName",
                    matchDescription(fixture)
                )
            }
        }
        editor.putBoolean("events_initialized_$fixtureId", true)
        if (seen.size > MAX_SEEN_EVENTS) {
            val trimmed = seen.toList().takeLast(MAX_SEEN_EVENTS).toSet()
            editor.putStringSet("events_$fixtureId", trimmed)
        } else {
            editor.putStringSet("events_$fixtureId", seen)
        }
    }

    private fun matchDescription(fixture: JSONObject): String {
        val teams = fixture.optJSONObject("teams")
        val goals = fixture.optJSONObject("goals")
        val home = teams?.optJSONObject("home")?.optString("name").orEmpty()
        val away = teams?.optJSONObject("away")?.optString("name").orEmpty()
        return "$home ${goals?.optionalScore("home") ?: "?"}–${goals?.optionalScore("away") ?: "?"} $away"
    }

    private fun fetchLiveFixtures(): List<JSONObject> {
        return fetchFixtures("live=all&timezone=Europe%2FIstanbul")
    }

    private fun fetchTrackedFixtures(ids: List<String>): List<JSONObject> {
        if (ids.isEmpty()) return emptyList()
        return fetchFixtures("ids=${ids.joinToString("-")}")
    }

    private fun fetchFixtures(query: String): List<JSONObject> {
        val url = URL("https://v3.football.api-sports.io/fixtures?$query")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = REQUEST_TIMEOUT_MS
            connection.readTimeout = REQUEST_TIMEOUT_MS
            connection.setRequestProperty("x-apisports-key", BuildConfig.API_SPORTS_KEY)
            connection.setRequestProperty("Accept", "application/json")
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                if (statusCode >= 500 || statusCode == 429) throw IOException("HTTP $statusCode")
                return emptyList()
            }
            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                JSONObject(it.readText())
            }
            val response = json.optJSONArray("response") ?: return emptyList()
            return List(response.length()) { index -> response.getJSONObject(index) }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optionalScore(key: String): Int? =
        if (isNull(key)) null else optInt(key, Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE }

    private fun notifyGoal(
        notificationId: Int,
        teamName: String,
        teamScore: Int,
        opponentScore: Int?,
        opponentName: String
    ) {
        notify(notificationId, "Gol! $teamName", "$teamName $teamScore–${opponentScore ?: "?"} $opponentName")
    }

    private fun notify(key: String, title: String, text: String) {
        notify(key.hashCode(), title, text)
    }

    private fun notify(notificationId: Int, title: String, text: String) {
        if (FavoriteGoalAlerts.preferences(applicationContext)
                .getBoolean("app_foreground", false)
        ) return
        FavoriteGoalAlerts.notify(applicationContext, notificationId, title, text)
    }
}

internal object FavoriteGoalNotification {
    fun post(context: Context, notificationId: Int, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Maç bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Takip edilen maç ve oyuncular için bildirim gönderir."
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()
        manager.notify(notificationId, notification)
    }

}

private val NOT_STARTED = setOf("NS", "TBD")
private val LIVE = setOf("1H", "HT", "2H", "ET", "BT", "P", "LIVE", "SUSP", "INT")
private val FINISHED = setOf("FT", "AET", "PEN")
private const val CHANNEL_ID = "match-alerts"
private const val REQUEST_TIMEOUT_MS = 20_000
private const val MAX_TRACKED_MATCHES = 20
private const val MAX_SEEN_EVENTS = 300
