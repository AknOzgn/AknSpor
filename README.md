# AknSpor Android Uygulaması

AknSpor, Türkçe canlı skor arayüzünü Android `WebView` içinde çalıştıran bir Android Studio uygulamasıdır. HTML, CSS, JavaScript dosyaları ve uygulama simgeleri `app/src/main/assets/www` klasöründe bulunur; arayüzün açılması için ayrıca barındırılan bir internet sitesi gerekmez. Canlı maç verileri internet bağlantısı ve uygulamayı derleyen kişi tarafından yapılandırılan API anahtarını kullanabilir. Uygulamayı kullanan kişiler anahtar girmek zorunda değildir.

## Android Studio'da açma ve çalıştırma

1. Android Studio'yu ve Android SDK 35 platformunu yükleyin.
2. Android Studio'da **Aç** seçeneğine basıp bu depo klasörünü seçin.
3. Gradle eşitlemesinin tamamlanmasını bekleyin; ardından bir emülatör veya bağlı Android cihaz seçip **Çalıştır**'a basın.

Proje JDK 17 kullanır; Android Studio ile gelen JBR uygundur. Hata ayıklama APK'sını **Derle > Paketleri / APK'ları Derle > APK Derle** menüsünden veya `gradlew.bat assembleDebug` komutuyla oluşturabilirsiniz. APK şu konuma yazılır: `app/build/outputs/apk/debug/app-debug.apk`.

## Canlı veriler

Kullanıcılar uygulamaya API anahtarı girmez. Desteklenen büyük liglerin canlı skorları FotMob'un herkese açık veri akışını kullanır. FotMob'da bulunmayan müsabakalar ve API-Sports gerektiren diğer özellikler için API anahtarı, yerel `local.properties` dosyasına `aknsporApiKey=API_ANAHTARINIZ` biçiminde eklenebilir. Derleme sırasında anahtar APK'ya eklenir; kendisi Git deposuna gönderilmez. **APK'ya eklenen anahtarlar çıkarılıp başkaları tarafından kullanılabilir.** Uygulamayı herkese açık dağıtacaksanız anahtarı sunucu tarafında saklayan bir ara sunucu kullanın.

Süper Lig, Türkiye 1. Lig, Premier Lig, La Liga, Serie A, Bundesliga, Ligue 1, Şampiyonlar Ligi, Avrupa Ligi, Konferans Ligi, Eredivisie ve Liga Portugal için puan tabloları, günlük maçlar, canlı maçlar ve favori takım fikstürleri FotMob'un herkese açık veri akışından alınır; API-Sports günlük kullanım hakkını tüketmez ve API anahtarı gerektirmez. Desteklenen liglerin puan durumu, maçlar sürerken skorlarına göre geçici olarak hesaplanır ve uygulama açıkken 30 saniyede bir yenilenir. Maçlar tamamlanınca sağlayıcının son puan tablosu gösterilir. Uluslar Ligi ve Dünya Kupası elemeleri müsabaka listesinde bulunmaz. FotMob veri akışı resmî değildir; sınırsız kullanım garantisi yoktur ve akış değişebilir veya kendi sınırlarını uygulayabilir. WebView kaynaklı CORS kısıtlamalarını önlemek için FotMob istekleri Android'in yerel ağ katmanından gönderilir. FotMob veya API-Sports erişimini engelleyen DNS filtreleri, VPN ya da özel DNS ayarları canlı verilerin yüklenmesini önleyebilir.

Bir maç kartına dokunarak **Özet**, **İstatistik**, **Kadro**, **Oyuncu**, **Anlatım**, **İkili rekabet**, **Puan durumu** ve **Haberler** bölümlerinden oluşan maç merkezini açabilirsiniz. Veri sağlayıcıların sunduğu maç bilgileri, olaylar ve anlatım, ilk 11'ler, oyuncu puanları ve istatistikleri, iki takımın geçmiş maçları, canlı maç eğilim grafiği ve varsa beklenen gol (xG) değerlerini içeren şut haritası gösterilir. Kadro ekranında FotMob'un oyuncu konum verilerinden hesaplanan ortalama konumlar ve yedek oyuncuları gösterme seçeneği bulunur. FotMob maçın en iyi oyuncusunu belirlediyse bu oyuncu vurgulanır. Canlı maçlarda diziliş ekranındaki oyuncu fotoğrafları, forma numaraları, golleri, asistleri ve yıldızla gösterilen FotMob puanları maç verileriyle yenilenir. Gösterilen puanlar FotMob tarafından sağlanır; uygulama tarafından hesaplanmaz. FotMob'da fotoğrafı bulunan oyuncular profil, kadro ve diziliş ekranlarında fotoğrafıyla gösterilir; fotoğraf yoksa isim baş harfleri kullanılır. Kadroda, oyuncu istatistiklerinde veya takım kadrosunda bir oyuncuya dokunarak maç performansını ve mevcutsa kariyer maçlarını, gollerini, asistlerini, kulüp armalarını ve sezonlarını görebilirsiniz. **Adını dinle** özelliği, cihazda Türkçe ses bulunduğunda oyuncunun adını seslendirir. Takım ayrıntı sayfasından takımı doğrudan favorilere ekleyebilir, maç kartından iki takımı birden favori yapabilirsiniz. Haber başlıkları uygulamada Google Haberler RSS akışıyla listelenir; başlığa dokunulduğunda yayıncının yazısı açılır. Maç ayrıntılarından maçın başlaması ve bitmesi için bildirim açılabilir; oyuncu profilinden o oyuncunun golleri takip edilebilir. Veri sağlayıcı milliyet bilgisi sunuyorsa oyuncu profilinde ülke ve bayrağı da gösterilir. Uygulama açıkken bildirim kontrolleri çalışır; Android arka plan denetimleri en az 15 dakikalık aralıklarla çalışabilir. Puan durumunda bir takıma veya takım adına dokunarak güncel sezon performansını, son maçlarını, gelecek fikstürünü, kadrosunu ve teknik direktörünü görebilirsiniz. **Takımlarım** sekmesinde favori takımların geçmiş ve gelecek maçları listelenir; maç ayrıntısından iki takım puan tablosunda vurgulanabilir. Takım armaları, kadrolar, maç dizilişleri, fikstürler, oyuncu kariyerleri ve puan tabloları FotMob'un herkese açık veri akışlarını kullanır. Lig verileri uygulama açık olduğu sürece bellekte saklanır.

Ayarlar'daki **Favori takım gol uyarısı** seçeneğini etkinleştirerek sesli ve titreşimli Android bildirimleri alabilirsiniz. Uygulama açıkken favori takım skorları yaklaşık 30 saniyede bir kontrol edilir; bu sık aralıklı sorgulamadır, anında bildirim garantisi değildir. API-Sports erişimi yapılandırılmışsa Android uygulama arka plandayken yaklaşık 15 dakikada bir kontrol yapabilir. Ağ bağlantısı, pil tasarrufu ayarları ve bildirim izni bu kontrolleri etkileyebileceğinden arka plan gol bildirimleri anında ulaşmayabilir. Android 13 ve üzeri sürümlerde bildirim izni gerekir. Bildirimlerde cihazın varsayılan sesi kullanılır.

Maç anlatımı, ev sahibi takım solda ve deplasman takımı sağda olacak şekilde orta çizgide zaman sırasına göre gösterilir. Veri kaynağı sunuyorsa golü atan oyuncu, gol anındaki skor ve asist bilgisi ayrı ayrı belirtilir. Maç tamamlandıktan sonra FotMob'un maçın oyuncusu seçimi gösterilir; resmî seçim yoksa puanı bulunan oyuncular arasından en yüksek puanlı oyuncu öne çıkarılır.

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

Maç bölümleri tüm sayfayı yeniden oluşturmadan kendi içeriklerini günceller. Böylece kullanıcı sayfayı kaydırırken ve maç verileri arka planda yenilenirken sabit sekme çubuğu yerinde kalır.

## Komut satırından derleme

Windows'ta `.\gradlew.bat testDebugUnitTest assembleDebug` komutunu çalıştırın. Hata ayıklama APK'sı `app/build/outputs/apk/debug/app-debug.apk` konumunda oluşturulur.
