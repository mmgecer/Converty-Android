# Android Mimari HANDOFF

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur; mimari veya önemli dosya sorumlulukları değiştiğinde bu belge güncellenir.

## Başlangıç durumu

2026-07-13 itibarıyla Android kaynak ağacı oluşturulmuştur. Proje Kotlin ve Jetpack Compose ile geliştirilir. Tam Expressive API seti için `androidx.compose.material3:material3:1.5.0-alpha23` sabitlenmiştir. Build temeli AGP 9.2.1 + Gradle 9.4.1 + compile/target SDK 37 + JDK 17; AGP built-in Kotlin 2.3.10 ve eşleşen Compose compiler plugin yaklaşımıdır.

## Hedef katmanlar

- `app`: Android giriş noktası, Compose navigation, ekran bağlama, manifest ve platform adaptörleri.
- `core/model`: dönüşüm yönü, kaynak belge, sayfa/slayt seçimi, iş durumu, çıktı ve hata modelleri.
- `core/domain`: aralık ayrıştırma/normalleştirme, çıktı adı üretimi, dönüşüm orkestrasyonu ve use-case'ler.
- `core/files`: SAF `Uri` erişimi, geçici dosya yaşam döngüsü, MIME tespiti, açma/paylaşma intentleri.
- `core/conversion`: arayüzler ve PDF→PPTX / PPTX→PDF motorları; UI/Room bağımlılığı olmamalı.
- `core/data`: Room geçmişi, ayarlar/DataStore ve repository uygulamaları.
- `feature/*`: ana sayfa, iş yapılandırma, ilerleme/kuyruk, geçmiş, çıktı ayrıntısı ve ayarlar.
- `ui/theme`: M3 Expressive tema, renk, tipografi, şekil, hareket ve adaptive düzen.

İlk teslimatta derleme karmaşıklığını düşürmek için fiziksel olarak tek `app` Gradle modülü içinde paketlere ayrılabilir. Sınırlar oturunca çok-modüllü yapıya geçiş ayrı bir karar olmalıdır.

## Bağımlılık yönü

UI → domain/use-case → repository/engine arayüzleri. Android/SAF/Room ve somut dönüştürücüler dış katmandadır. Dönüşüm işi doğrudan Composable içinde çalışmaz; WorkManager/foreground iş katmanından yönetilir.

## Çözülmüş temel kararlar ve kalan doğrulamalar

- Sistem PATH üzerinde araç yoktur; doğrulama için izole JDK `C:\tmp\converty-jdk17\jdk-17.0.19+10`, SDK `C:\tmp\converty-android-sdk` ve proje Gradle wrapper'ı kullanılır. Bu yollar makineye özeldir.
- Harici Office/POI bağımlılığı yerine `core/conversion` içinde küçük, güvenlik limitli OOXML writer/parser uygulandı; üçüncü taraf format kütüphanesi eklenmedi.
- Destek matrisi minSdk 23, compile/target SDK 37'dir. M3E 1.5.0-alpha23 AAR'ları compileSdk 37 gerektirir; SDK paket adı `platforms;android-37.0`dır.
- Kalan doğrulama: gerçek cihazda PdfRenderer/PdfDocument, foreground bildirim/iptal, farklı DocumentsProvider'lar, RTL/büyük font ve büyük/bozuk dosya testleri.

## Sürüm ve API kararı — 2026-07-13

- `material3:1.5.0-alpha23` (2026-07-01) tam M3 Expressive için kullanılır. Stable 1.4.0, 1.4 beta sürecinde deneysel Expressive public API'leri çıkardığı için kullanıcının tam M3E şartını karşılamaz.
- Compose BOM `2026.06.00`; Material 3 sürümü BOM üstüne açıkça override edilir.
- Tema giriş noktası `MaterialExpressiveTheme`; hareket `MotionScheme.expressive()`. Expressive API ve opt-in'ler `ui/theme`/design-system wrapper'larında merkezileştirilir.
- Gerçek M3E bileşenleri arasında `ShortNavigationBar`, `WideNavigationRail`, flexible app bar/toolbars, shape-morphing `ButtonShapes`, `ButtonGroup`/`SplitButton`, `LinearWavyProgressIndicator` ve `CircularWavyProgressIndicator` vardır. `ExpressiveButton` adlı API yoktur.
- `LoadingIndicator`/`ContainedLoadingIndicator` hâlâ deneysel olduğundan yalnız merkezi wrapper içinden kullanılır; stabil wavy progress önceliklidir.

## Uygulama ve arka plan iş bileşenleri — 2026-07-13

Sonraki AI bu haritayı okumalı; application container, WorkManager sözleşmesi veya worker yaşam döngüsünü değiştirirse bu belgeyi, veri akışı için `04-DATA-FILES-HISTORY.md` dosyasını ve anlamlı karar/bug düzeltmeleri için `05-DECISIONS-CHANGELOG.md` dosyasını güncellemelidir.

- `com.converty.app.ConvertyApplication`: WorkManager `Configuration.Provider`; uygulama-scoped container'ı lazy kurar ve notification channel'ı oluşturur. Lazy kurulum, Startup provider `Application.onCreate` öncesi konfigürasyon isterse de güvenlidir.
- `di/AppContainer.kt`: Room repository, Settings DataStore repository, SAF gateway, iki conversion engine, scheduler ve custom worker factory için küçük service locator. UI erişimi `(application as ConvertyApplication).appContainer` şeklindedir.
- `work/ConversionWorkerFactory.kt`: WorkManager'ın üç parametreli `ConversionWorker` kurucusuna `AppContainer` enjekte eder.
- `work/ConversionScheduler.kt`: job başına unique work; `enqueue(jobId)` KEEP, `retry(jobId)` REPLACE, `cancel(jobId)` ve `observe(jobId): Flow<WorkInfo?>`. WorkRequest input data yalnız `jobId` taşır; gerçek iş verisi Room'dan okunur.
- `work/ConversionWorker.kt`: Room snapshot → engine inspection → selection resolve → SAF output → yön bazlı engine → Room aggregate status akışı. Senkron engine çağrıları `Dispatchers.IO`, ilerleme yazımları conflated channel üzerindedir.
- `work/ConversionWorkerAdapters.kt`: domain selection/quality/fit/error modellerini engine sözleşmesine dönüştürür ve güvenli çıktı görünen adını üretir.
- `work/ConversionNotifications.kt`: data-sync foreground bildirimi, ilerleme ve WorkManager cancel pending intent'i.
- Manifest `ConvertyApplication`, `POST_NOTIFICATIONS`, foreground/data-sync izinleri ve WorkManager `SystemForegroundService` dataSync merge kaydını içerir. Custom factory için varsayılan `WorkManagerInitializer` metadata'sı kaldırılmıştır. Android 13+ bildirim runtime izni UI katmanının sorumluluğudur.
- `MainActivity` bir `AppCompatActivity` olduğu için manifestte miras aldığı XML pencere teması mutlaka AppCompat soyundan gelmelidir. `res/values/themes.xml` ve `res/values-night/themes.xml` içindeki `Theme.Converty` parent'ı `Theme.AppCompat.DayNight.NoActionBar`dır; platform `android:style/Theme.Material.*` parent'ına dönmek ilk `setContent` sırasında, Compose root oluşmadan runtime çökmesi üretir. Ürün görünümü yine Compose `ConvertyTheme`/`MaterialExpressiveTheme` tarafından çizilir.
- `AppCompatThemeContractTest.kt` gündüz/gece parent sözleşmesini saf JVM'de korur. `MainActivityStartupTest.kt` ise bağlı cihaz/emülatörde Activity'nin başlayıp Compose semantics root'u oluşturduğunu doğrulamak için eklenmiştir.

## UI ayarları ve locale mimarisi — 2026-07-13 ürün cilası

- `core/settings/AppSettings.kt`, kalıcı `ThemePalette` (`OCEAN`, `VIOLET`, `FOREST`, `SUNSET`) seçimini taşır. `PreferencesSettingsRepository` bunu `theme_palette` anahtarında saklar; palette seçimi dinamik rengi kapatarak kullanıcı seçimini hemen görünür kılar.
- Varsayılan dönüşüm kalitesi hem kalıcı ayarlarda hem yeni `ConversionSetupState` içinde `MAXIMUM`dur. Eski `quality_default_version` bulunmadığında repository legacy `BALANCED` kaydını bellekte `MAXIMUM` olarak yorumlar; açılış Flow'u içinde `DataStore.edit` çalıştırmaz. Sonraki kullanıcı ayarı normal `encode` yolunda sürüm 1'i yazar ve açık seçimi korunur.
- Dil değişimi `ApplyAppLocale` içinden `AppCompatDelegate.setApplicationLocales` public API'siyle yapılır. `MainActivity` manifestte `android:configChanges="locale|layoutDirection"` ilan ettiği için Activity yıkılıp yeniden kurulmaz; Compose güncel kaynak/configuration ile yeniden çizilir. Çağrı, settings ilk kez gerçekten yüklendikten sonra ve yalnız etiket değiştiğinde yapılır.
- Önceki `ProvideAppLocale` yaklaşımı bütün Compose ağacına Activity olmayan `createConfigurationContext` sonucu veriyordu. Son UI APK'sında açılış regresyonu bildirildikten sonra bu özel sarmalama kaldırıldı; tekrar eklenmemelidir. Sözleşme `LocaleSwitchContractTest` ile korunur.

## Açılış dayanıklılığı ve genişletilmiş motor kayıtları — 2026-07-13

- `ConversionScheduler` artık `WorkManager.getInstance` çağrısını constructor içinde yapmaz; senkronize lazy alan ilk gerçek scheduler çağrısında başlatır. Böylece WorkManager başlangıç uyumsuzluğu Home'un ilk karesini engellemez.
- `ConvertyViewModel` ayar ve Room geçmiş Flow'larını ayrı `catch` sınırlarıyla toplar. Başlangıç okuması hata verirse `ConvertyStartup` etiketiyle loglanır; `AppSettings()` ve boş geçmiş fallback'iyle Home açılır. Veriler silinmez.
- `AppLocaleController` locale uygulamasını, `ConvertyApplication` bildirim kanalı oluşturmayı `runCatching` sınırında tutar. `StartupResilienceContractTest.kt` bu dört açılış sınırını korur; gerçek Activity testi cihaz/emülatör gerektirir.
- `core/model/ConversionDirection.kt` izinli kaynak→hedef çiftlerinin tek doğruluk kaynağıdır; aynı format çifti kayıtlı değildir. `AppContainer` görsel, PDF raster/TIFF/thumbnail ve Office/ODF motorlarını yön/format kayıtlarıyla sağlar.
- `ConversionContracts.kt` tek hedefli motor yanında sayfa başına çoklu hedef üreten `MultiOutputConversionEngine` sözleşmesini taşır. Worker çoklu SAF hedeflerini hazırlar ve çıktı listesini Room'a yazar.
