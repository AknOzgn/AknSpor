# AknSpor Android

AknSpor is an Android Studio app that packages the Turkish live-score interface in an Android `WebView`. The HTML, CSS, JavaScript and app icons are bundled under `app/src/main/assets/www`, so the interface itself does not depend on a hosted website. Live match data uses the maintainer-configured API key and requires an internet connection; app users do not enter credentials.

## Open and run

1. Install Android Studio and an Android SDK platform (API 35).
2. In Android Studio, choose **Open** and select this repository folder.
3. Allow Gradle sync to finish, then choose an emulator or connected Android device and press **Run**.

The project uses JDK 17 (Android Studio's bundled JBR is suitable). A debug APK can be built with **Build > Build Bundle(s) / APK(s) > Build APK(s)**, or with `gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Live data

Users do not enter an API key in the app. Live scores for supported major leagues use FotMob's public feed; the API-Sports key, configured locally in `local.properties` as `aknsporApiKey=YOUR_API_KEY`, is used only for competitions not covered by FotMob and other API-Sports-backed features. The build inserts it into the APK, while the key itself stays out of Git. **Keys embedded in an APK can be extracted and used by others**; use a backend proxy before distributing the app publicly.

League tables, daily match lists, live match lists, and favorite-team schedules for Süper Lig, Türkiye 1. Lig, Premier League, La Liga, Serie A, Bundesliga, Ligue 1, Champions League, Europa League, Conference League, Eredivisie and Liga Portugal use FotMob's public feed and do not consume the API-Sports daily quota or require an API key. The standings screen provisionally recalculates those supported leagues from in-progress match scores and refreshes automatically every 30 seconds while the app is open; final standings return to the data provider's table after matches finish. Nations League and World Cup qualifiers are not included in the competition list. FotMob's feed is unofficial and has no documented unlimited-use guarantee; it may change or impose its own limits. Android requests FotMob through its native network layer to avoid WebView CORS restrictions. A network DNS filter, VPN or private-DNS setting that blocks FotMob or API-Sports can prevent live data from loading.

Tap a match card to open a match center with **Özet**, **İstatistik**, **Kadro**, **Oyuncu**, **Anlatım**, **H2H**, **Puan durumu**, and **Haberler** sections. Match facts, events/commentary, starting XIs, player ratings/statistics, head-to-head history, a live momentum graph, and (where supplied) a shot map with expected goals (xG) are shown when the data providers publish them. Match lineups also include average player positions derived from FotMob's player heatmap points, with an option to include substitutes; the match's best-rated player is highlighted when FotMob identifies one. During live matches, the formation view shows compact player photos, shirt numbers, goals, assists, and a clear star-marked FotMob rating; these update with live match details. Ratings shown are FotMob ratings, not independently calculated scores. Player photos appear on profiles, squad lists, and lineups when FotMob has an image; initials are shown if no photo is available. Tap a player in a lineup, player-stat table, or team squad to see the match performance and available career appearances, goals, assists, club crests, and seasons; **Adını dinle** uses the device's Turkish text-to-speech voice when available. The team detail page also has a direct favorite toggle; match cards can favorite both teams at once. News headlines are listed in the app through Google News RSS; selecting a headline opens the publisher's article, and the feed is attributed to Google News. Match detail can enable start/end notifications, and a player profile can opt into that player's goals. Player profiles also show the player's country and flag when the data provider supplies nationality information. Notifications work while the app is active and through Android's periodic background checks (minimum interval 15 minutes). Tap a team or its name in the standings to see its current-season record, recent results, upcoming fixtures, full squad, and coach. The **Takımlarım** tab lists recent and upcoming fixtures for favorited teams, and a match detail can highlight both teams in the standings. Team crests, squads, match lineups, schedules, player careers, and tables use FotMob's public data feeds; league data is cached in memory while the app is open.

Enable **Favori takım gol uyarısı** in Settings to receive Android notifications with sound and vibration. While the app is open, favorite-team scores are checked approximately every 30 seconds; this is near-real-time polling, not an instant push guarantee. In the background, Android can check at approximately 15-minute intervals only when API-Sports access is configured (also subject to network availability, battery optimizations, and notification permission), so background goal alerts are not instant. Android 13 and later require notification permission. Notifications use the device's default notification sound.

Match commentary is displayed as a chronological center-line timeline, with the home team on the left and away team on the right. Goal scorers, the score at the time, and assists are shown separately when provided by the data source. After full time, the match center shows FotMob's selected player of the match or, when no official selection is supplied, the highest-rated player with an available rating.

## Project structure

- `app/src/main/java/com/aknozgn/aknspor/MainActivity.kt` hosts the bundled web interface in Android WebView and provides the native network proxy and notification bridge.
- `app/src/main/java/com/aknozgn/aknspor/FavoriteGoalWorker.kt` handles periodic background checks for favorite-team goals when API-Sports is configured.
- `app/src/main/assets/www/` contains the app's HTML, CSS, JavaScript, and icons. No hosted website is required to display the interface.
- `app/build.gradle.kts` reads the optional API key from the untracked local `local.properties`; do not commit API keys.

Match sections update their own content without rebuilding the entire page. This preserves the sticky match-tab bar while the user scrolls and while match data refreshes in the background.

## Build from the command line

On Windows, run `.\gradlew.bat testDebugUnitTest assembleDebug`. The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
