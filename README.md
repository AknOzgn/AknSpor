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

## Kaynak kodlar ne yapıyor?

### Android uygulaması (Kotlin)

| Dosya | Görevi |
|---|---|
| `app/src/main/java/com/aknozgn/aknspor/MainActivity.kt` | Android uygulamasının başlangıç ekranı. Yerel HTML arayüzünü WebView içinde açar, internet isteklerini FotMob/API-Sports/Google Haberler'e güvenli biçimde yönlendirir ve JavaScript ile Android arasındaki bildirim, metin okuma ve haber bağlantısı köprüsünü sağlar. |
| `app/src/main/java/com/aknozgn/aknspor/FavoriteGoalWorker.kt` | Android arka plandayken WorkManager ile yaklaşık 15 dakikada bir çalışan görev. Favori takımların gollerini, takip edilen maçların başlangıç/bitişini ve seçilen oyuncuların gollerini kontrol edip Android bildirimi gönderir. |
| `app/src/main/AndroidManifest.xml` | Uygulama adını, simgesini, açılış etkinliğini ve internet/bildirim gibi Android izinlerini tanımlar. |
| `app/src/main/res/values/strings.xml` | Android'de gösterilen uygulama adını tutar. |
| `app/src/main/res/values/styles.xml` | Açılış teması ve sistem çubuklarının temel görünümünü tanımlar. |
| `app/src/main/res/drawable-nodpi/ic_launcher.png` | Android ana ekranındaki uygulama simgesi. |

`MainActivity.kt` içindeki önemli bölümler:

- `onCreate`: WebView'ı, yerel asset yükleyicisini ve Türkçe metin-okuma motorunu hazırlar; ana `index.html` dosyasını açar.
- `shouldInterceptRequest` ve `proxyRequest`: Web arayüzünün `/proxy/...` isteklerini kontrol eder ve yalnızca izin verilen servis uçlarına iletir. Böylece WebView'ın CORS kısıtları aşılır ve API anahtarı istek başlığına Android tarafından eklenir.
- `fotmobRequest`, `newsRequest` ve `proxyError`: FotMob, haber ve hata yanıtlarını WebView'a uygun biçimde döndürür.
- `FavoriteAlertsBridge`: JavaScript'ten gelen bildirim tercihlerini alır; Android bildirim iznini ister, maç bildirimi gönderir, oyuncu adını seslendirir ve haber bağlantısını açar.
- `onBackPressed`, `onSaveInstanceState` ve `onDestroy`: Geri gezinmeyi, ekran durumu kaydını ve ekran kapanırken kaynakların temizlenmesini yönetir.

`FavoriteGoalWorker.kt` içindeki önemli bölümler:

- `FavoriteGoalAlerts.configure`: Takip ayarlarını yerel Android tercih alanına kaydeder ve gerektiğinde periyodik işi başlatır veya durdurur.
- `FavoriteGoalWorker.doWork`: Takip edilen takımların ve maçların verisini alır, önceki skor/durumla karşılaştırır ve yeni olaylarda bildirim üretir.
- `fetchFixtures`: API-Sports'tan maç listesini alır; API anahtarı istekte kullanılır.
- `checkMatchAlerts`: Maç başladı/bitti geçişlerini ve izlenen oyuncuların yeni gollerini belirler.
- `FavoriteGoalNotification.post`: Android bildirim kanalını oluşturur ve ses/titreşimli bildirimi gösterir.

### Web arayüzü (HTML, CSS ve JavaScript)

| Dosya | Görevi |
|---|---|
| `app/src/main/assets/www/index.html` | Uygulamanın ana arayüzü ve istemci kodu. Sayfa yapısı, stiller, maç/puan durumu ekranları, servis istekleri ve etkileşimler tek dosyadadır. |
| `app/src/main/assets/www/manifest.webmanifest` | Web arayüzünün adı, teması ve PWA simge bilgileri. |
| `app/src/main/assets/www/sw.js` | Web servis çalışanı dosyası; Android uygulamasındaki asıl ekran ve veri işlemleri bu dosyada değildir. |
| `app/src/main/assets/www/icon-192.png`, `icon-512.png`, `icon-maskable-512.png` | Web uygulaması için farklı boyut ve maskeleme biçimlerindeki simgeler. |

`index.html` içindeki JavaScript görevleri:

- **Uygulama durumu ve yardımcılar:** `S` ekran verilerini tutar; `LS` favori/ayar verilerini cihazda saklar. `friendly`, `applyTheme`, `statusLabel` ve tarih/ülke yardımcıları ekrana uygun metin ve biçimleri üretir.
- **Veri kaynakları:** `af` API-Sports isteklerini; `fotmobData` ve `fotmobLeague` FotMob isteklerini yapar. `getFx`, `getSt`, `favoriteTeamFixtures` maç, puan tablosu ve favori takım fikstürünü hazırlar.
- **Veri dönüştürme ve canlı puan:** `fotmobStandings`, `fotmobFixture`, `normalizeMatchStatus` ve `normalizeFixtureStatus` servis verisini uygulama biçimine dönüştürür. `calculateLiveStandings` ve `updateLiveStandings`, oynanan maçların skoruna göre geçici canlı puan sıralaması hesaplar.
- **Maç ve takım detayları:** `openFixture` maç merkezini yükler; `loadTeam` takım sayfasını, `loadHeadToHead` ikili maç geçmişini, `loadMatchTable` ilgili puan durumunu, `loadMatchNews` haberleri getirir. `lineupFrom` ve `lineupFromApiSports` kadroları ortak biçime çevirir.
- **Maç olayları ve istatistikler:** `matchEvents`, `matchEventList` ve `translateMatchEventDetail` anlatım olaylarını hazırlar ve Türkçeleştirir. `momentumChart`, `matchFormSummary`, `fotmobMatchStats`, `matchShotMap` ve `ratedMatchPlayers` ilgili grafik, form, şut ve oyuncu istatistiklerini üretir.
- **Oyuncu ve saha:** `playerProfilePage` ve `loadPlayerCareer` oyuncu profilini/kariyerini gösterir. `lineupPlayer`, `averagePositionFor`, `averagePositionPitch` ve `lineupCard` oyuncu bilgilerini ve iki ayrı saha görünümünü oluşturur.
- **Favoriler ve bildirimler:** `toggleFav`, `toggleTeamFavorite`, `toggleMatchAlert` ve `togglePlayerAlert` seçimleri yönetir. `pollFavoriteGoals` uygulama açıkken kontrol yapar; `syncBackgroundAlerts` tercihleri Kotlin köprüsüne iletir.
- **Ekran çizimi ve etkileşim:** `matches`, `standings`, `myTeams`, `teamPage`, `fixturePage` ve `playerProfilePage` ekran içeriğini üretir. `render` ana ekranı çizer; `renderFixtureDetail` maç ayrıntısında yalnızca gereken bölümü günceller. `matchTabChanged` sekme verisini yükler; tıklama/değişiklik dinleyicileri butonları ve seçimleri işler.

### Gradle ve uygulama yapılandırması

- `app/build.gradle.kts`: Android modülünü, bağımlılıkları ve derleme ayarlarını tanımlar. `local.properties` içindeki `aknsporApiKey` değerini derleme sırasında APK'ya aktarır; bu dosya Git'e eklenmemelidir.
- `build.gradle.kts` ve `settings.gradle.kts`: Gradle eklentilerini ve projedeki modülleri tanımlar.
- `gradle.properties`: Gradle derleme ayarlarını içerir.
- `gradle/wrapper/`: Projenin kullandığı Gradle sürümünü ve Windows/Linux başlatıcılarını içerir.
- `.gitignore`: Yerel ayarları, API anahtarının bulunduğu `local.properties` dosyasını ve derleme çıktılarını Git dışında tutar.

Match sections update their own content without rebuilding the entire page. This preserves the sticky match-tab bar while the user scrolls and while match data refreshes in the background.

## Build from the command line

On Windows, run `.\gradlew.bat testDebugUnitTest assembleDebug`. The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
