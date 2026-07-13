# Doğrulama ve Yol Haritası HANDOFF

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur; aktif faz, test sonucu, blocker veya kalan iş değiştiğinde bu belge güncellenir.

## Fazlar

1. **Envanter ve temel kararlar — tamamlandı**
   - Eski dönüşüm motorlarını ve riskleri incele.
   - Resmi M3 Expressive sürüm/API setini doğrula. **Tamamlandı: 1.5.0-alpha23.**
   - Android/JDK/SDK/Gradle ortamını doğrula.
   - HANDOFF sistemini kur. **Tamamlandı.**
2. **Çalışan Android iskeleti — tamamlandı**
   - Gradle proje dosyaları, manifest, Compose giriş noktası, M3E tema ve navigation.
   - 8 dil kaynak dosyası ve locale config.
3. **Dosya ve seçim çekirdeği + PDF→PPTX — tamamlandı (cihaz smoke testi hariç)**
   - SAF tekli/çoklu seçim, belge meta verisi, aralık ayrıştırıcı ve testleri.
   - Sayfa sayfa PDF render, OOXML PPTX paketleme, ilerleme/iptal.
4. **PPTX→PDF + kalıcı işler — temel kapsam tamamlandı (ileri PowerPoint özellikleri uyarılı/unsupported)**
   - OOXML ayrıştırma/render destek matrisi.
   - WorkManager foreground işler, çıktı, aç/paylaş ve hata yönetimi.
5. **Geçmiş ve ürün cilası — temel kapsam tamamlandı**
   - Room, ayarlar/DataStore, tema/dil, arama/filtre/yeniden deneme.
   - Adaptive/RTL/TalkBack ve performans iyileştirmeleri.
6. **Teslim doğrulaması — kısmen tamamlandı**
   - Birim, entegrasyon ve Compose UI testleri; APK derleme/lint.
   - Gerçek örnek PDF/PPTX round-trip ve büyük dosya testleri.

## Kabul ölçütleri — özet

- En az bir gerçek PDF seçilip seçilen sayfalarla açılabilir PPTX üretilebilmeli.
- En az temel metin/görsel/şekil içeren gerçek PPTX seçilip açılabilir PDF üretilebilmeli; desteklenmeyen içerik uyarılmalı.
- Çoklu işlerde ilerleme ve dosya bazlı hata; iptal sonrası kısmi dosya/geçici cache bırakmama.
- Ayrık aralıklar ve ilk/son N seçimleri sınır ve tekrarları doğru ele almalı.
- Geçmişten çıktı aç/paylaş/yeniden çalıştır; kayıp izin/çıktı durumu anlaşılır olmalı.
- Tüm kullanıcı metinleri 8 dilde kaynakta bulunmalı; Arapça RTL ve açık/koyu/dinamik temalar kullanılabilir olmalı.
- M3 Expressive kullanımı gerçek resmi API veya belgelenmiş stabil M3 fallback olmalı.

## Güncel doğrulama durumu

- Bilgi grafiği indekslemesi başarılı; eski Python kodu 11 fonksiyon/4 kaynak dosyası olarak görüldü.
- İzole OpenJDK 17.0.19, Gradle wrapper 9.4.1, Android SDK Platform 37.0/Build Tools 37.0.0 ile tam Kotlin compile başarılıdır.
- `testDebugUnitTest`: 61/61 başarılı. Kapsam: 23 selection; 13 conversion writer/parser/security/contracts; 15 worker selection/ad/output/error/quality/fit; 6 model invariant; 2 Room mapper; 1 sekiz-locale key/placeholder/plural parity; 1 gündüz/gece AppCompat başlangıç tema sözleşmesi.
- `lintDebug`: başarılı. Önce bulunan 3 hata baseline kullanılmadan düzeltildi: API 27 tema attribute işareti, custom WorkManager initializer kaldırma.
- `assembleDebug`: başarılı; `app/build/outputs/apk/debug/app-debug.apk` boyutu 22,573,677 bayt; SHA-256 `C1E1A3AA56A563F929CDF0AE0168DBDCF3C6E22DAB484B2904E521A0697C4932`.
- `apkanalyzer`: applicationId `com.converty.app`, minSdk 23, targetSdk 37. `apksigner`: debug APK v1 ve v2 şemalarıyla doğrulandı.
- Başlangıç crash düzeltmesi için `aapt2 dump resources`, paketlenmiş gündüz/gece `Theme.Converty` parent'ını `Theme.AppCompat.DayNight.NoActionBar` olarak doğruladı. `MainActivityStartupTest` Activity + Compose root smoke testi olarak eklendi ve Kotlin derlemesi başarılı oldu.
- `adb devices` bağlı cihaz göstermedi; bu nedenle açılış testi ve gerçek PDF/PPTX, foreground worker, SAF provider, RTL ve büyük dosya smoke/instrumentation testleri çalıştırılmadı. Test APK'sının son Java paketleme adımı OneDrive içindeki generated `R.jar` dosyasında ortam kaynaklı `AccessDeniedException` ile durdu; asıl APK'nın unit/lint/assemble turu başarılıdır.
- Bilgi grafiği ilk eski-kod indeksinde başarılıydı. Yeni Android kaynakları sonrası önceki denemeler MCP transport kapanmasıyla; başlangıç crash düzeltmesi sonrasındaki 2026-07-13 `moderate` ve `fast` denemeleri ise kaynak klasörü mevcut olmasına rağmen genel `Pipeline failed` cevabıyla başarısız oldu. Kod keşfinde mevcut grafik kullanıldı, yetersiz kaldığı noktalar dosya/config aramasıyla doğrulandı; sonraki AI servis düzeldiğinde yeniden indekslemelidir.

## Öncelikli sıradaki iş

Bağlı gerçek cihaz/emülatörde PDF→PPTX ve temel PPTX→PDF fixture smoke testi; farklı SAF provider, foreground iptal, RTL/büyük font ve büyük/bozuk dosya instrumentation testleri. SmartArt/chart/table/group/tema-master sadakati ayrı motor genişletme fazıdır.

## Son UI cilası doğrulaması — 2026-07-13

- Son kaynak durumunda `testDebugUnitTest` 13 suite / 64 test / 0 failure / 0 error / 0 skipped ile geçti. Yeni regresyonlar ayar/setup `MAXIMUM` varsayılanını, dört paleti, tek seferlik kalite migration'ını ve Activity recreation kullanmayan locale sözleşmesini kapsar.
- `lintDebug` baseline veya suppression ile hata gizlemeden başarılıdır. `assembleDebug` başarılıdır.
- Sekiz locale dosyasının her biri 125 string ve 2 plural taşır; mevcut parity testi anahtar, placeholder ve plural eşliğini korur.
- Güncel `app/build/outputs/apk/debug/app-debug.apk` 23,371,172 bayttır; SHA-256 `1D93DFC7F2E5A72AB49D16A2E6A2BB09D4C3ABAD903D6F9631B480F5B5321849`. `apksigner` v1/v2 şemalarını geçerli doğruladı. Manifest kimliği/min/target önceki paket turunda `com.converty.app`/23/37 idi ve bu çalışmada build config/manifest değişmedi.
- Kullanıcı Android kaynakları için Codebase Memory indeksini yeniden oluşturduğunu belirtti; bu oturumda MCP transport kapalı kaldı. Sonraki AI grafik erişimi geldiğinde `search_graph` ile Android sınıflarının döndüğünü kısa bir kontrolle doğrulamalı, başarısızsa gereksiz yeniden kurulum döngüsüne girmeden doğrudan kaynak fallback'ini belgelemelidir.
- Bağlı cihaz/emülatör bulunmadığından sabit Convert footer, palette görünümü, sekiz dilde taşma/RTL ve gerçek dönüşüm smoke testi fiziksel cihazda hâlâ yapılmalıdır.

## İkinci açılış regresyonu doğrulaması — 2026-07-13

- Özel Compose locale-context sarmalaması kaldırıldı; public AppCompat locale API'si + manifest `locale|layoutDirection` handling kullanılıyor. Paketlenmiş APK manifestinde `MainActivity configChanges=0x00002004` doğrulandı.
- Settings Flow artık başlangıçta DataStore migration yazısı yapmıyor; legacy kalite salt-okunur biçimde `MAXIMUM` çözülüyor. Home'un ilk frame kataloğu FlowRow ağırlığı yerine veri tabanlı ikili Row kullanıyor.
- Son tur: 13 suite / 64 test / 0 failure / 0 error; `lintDebug` ve `assembleDebug` başarılı. APK boyutu 23,371,172 bayt, SHA-256 `F93B8BF7EB7643E43638ECE2EADFBB300632A4C1180ABF71EE3C605DB35D0BAA`, `apksigner` exit 0.
- Cihaz/emülatör bağlı olmadığı için bu regresyonun gerçek cihazda kapandığını doğrulama yetkisi kullanıcıdadır. Yeni APK da kapanırsa tahminle yeni UI değişikliği yapılmadan `adb logcat` veya Android crash stacktrace'i alınmalıdır.

## Tekrarlayan açılış kapanması ve genişletilmiş build — 2026-07-13

- Codebase Memory tekrar `Transport closed`; `adb devices -l` bağlı cihaz göstermedi. Kesin runtime exception ve Activity instrumentation sonucu yoktur.
- `StartupResilienceContractTest`: Room/DataStore Flow catch+fallback, locale guard, WorkManager lazy init ve notification channel guard sözleşmelerini korur.
- Güncel tam tur: 14 suite / 65 test / 0 failure / 0 error; `lintDebug` ve `assembleDebug` başarılı. Sekiz locale'in her biri 152 string + 2 plural ile parity testini geçti.
- Paket: `app/build/outputs/apk/debug/app-debug.apk`, 23.345.524 bayt, SHA-256 `A97519F2AECDAA1B16B034FB66B9B5479C43C2F3D1CEE5CDBD309DF3444DC189`; v1/v2 debug imza geçerli, applicationId/min/target `com.converty.app`/23/37.
- En yakın gerekli kanıt: bu APK'yı gerçek cihaza kurup Home'un açıldığını gözlemek. Hâlâ kapanırsa aynı paketin `FATAL EXCEPTION`/`AndroidRuntime` logcat stacktrace'i alınmadan başka tahmine dayalı açılış değişikliği yapılmamalıdır.
- Genişletilmiş görsel/PDF/Office motorları derlenmiş fakat platform codec/renderer ve gerçek çıktı dosyaları cihaz üstünde doğrulanmamıştır; teslim yol haritası hâlâ kısmi tamamlandıdır.

## Bağlı Samsung cihaz doğrulaması — 2026-07-13

- Cihaz: Samsung SM-S721B, Android 15; ADB durumu `device`. Kalıcı applicationId `com.converty.app`.
- `connectedDebugAndroidTest`: **7 test / 0 failure**. `MainActivityStartupTest` Compose root açılışını; `DatabaseMigrationSmokeTest` v1→v2→v3 SQLite migration zincirini; `ConversionEngineSmokeTest` içindeki beş vaka gerçek PDF/PPTX/raster/SVG/HEIC/AVIF/Office/ODF çıktıları ve dosya imzalarını doğrular.
- İlk iki cihaz turu boşa gitmedi: Android parser uyumsuzluğunu sırasıyla `isXIncludeAware` ve `FEATURE_SECURE_PROCESSING` hatalarıyla ortaya çıkardı. `SecurePackageXml.kt` sonrasında temel tur 6/6; AOSP codec fixture'ları eklendikten sonra son tur 7/7 geçti.
- OneDrive AndroidTest `R.jar` kilidi nedeniyle kaynak-of-truth workspace'tir, fakat son connected test güncel kaynakların `C:\tmp\converty-androidtest-20260713-8` temiz aynasında çalıştırıldı. Temiz kopyaya `.git`, `.gradle`, build klasörleri ve `local.properties` taşınmadı; Git kullanılmadı. Yükseltilmiş derleme için workspace debug keystore geçici build dosyası üzerinden signing override ile verildi ve sertifika eşleşmesi ölçüldü.
- Test altyapısının hedef paketi tur sonunda kaldırması normal davranış olarak gözlendi. Son tam build APK'sı ADB `install -r` ile tekrar kuruldu; soğuk açılış `Status: ok`, 1.669 ms, çalışan PID ve crash filtresinde sıfır sonuç verdi.
- Son kalite kapısı: `testDebugUnitTest` **79/79**, `lintDebug` başarılı, `assembleDebug` başarılı. APK `app/build/outputs/apk/debug/app-debug.apk`, 23.345.524 bayt, SHA-256 `D90ED9AD01C0A74B9D8E64B1F21DCBF7AEF22D542EEBA4A5B9767D89952C2E90`.
- Kalan genişletilmiş kalite kanıtları: farklı DocumentsProvider/SAF izin yenileme; foreground iptal ve yarım çıktı temizliği; Arapça RTL/büyük font; büyük/bozuk/şifreli gerçek dosya stres turu. Chart/table/SmartArt/group/tema-master tam sadakati warning/unsupported kapsamındadır.
- Codebase Memory MCP bu turda `Transport closed` kaldı. Sonraki AI servis erişilebilir olduğunda önce `list_projects`/`search_graph` ile Android indeksini doğrulamalı; başarısızsa doğrudan kaynak fallback'ini ve nedeni yine belgelemelidir.

## Katmanlı ikon ve release teslimi — 2026-07-13

- Codebase Memory MCP bu turda çalıştı; proje `C-Users-sagla-OneDrive-Belgeler-converty-android` altında manifest ve ikon/test sembolleri `search_graph` ile bulundu. Kod keşfinde grafik önceliği yeniden uygulanabilir durumdadır.
- `LauncherIconContractTest` ile toplam `testDebugUnitTest` sonucu **80/80**, 0 failure, 0 error. Android kaynak derlemesi legacy, v26 adaptive ve v33 themed icon varyantlarını paketledi; `aapt` application icon ve launcher Activity'yi doğruladı.
- Release variant üretildi ve `output/release.apk` olarak imzalandı. Paket `com.converty.app`, sürüm `0.1.0`/1, minSdk 23, targetSdk 37; v1/v2/v3 signature doğrulaması başarılıdır. Boyut 15.683.394 bayt, SHA-256 `3C8381DEEF9D54F829CB5B72D6282909AFF18DC7724522BDDD894B0E2BF713C0`.
- Teslim imzası yerel debug sertifikası `A981F888…E569`dur ve mağaza imzası değildir. Önceki kurulu APK'nın `29787975…EC313` sertifikasıyla eşleşmediğinden update kurulumu yapılamaz; kullanıcı mevcut paketi kaldırmalıdır. Sonraki kalıcı production release öncesi kullanıcıya ait release keystore güvenli biçimde oluşturulup yedeklenmeli ve versionCode artırılmalıdır.

## Kamu yayın kapısı — 2026-07-13

- Sekiz dilli README seti bağlantı ve emoji kontrolünden geçti. İngilizce canonical dosya; ürün davranışı, format matrisi, gizlilik, sınırlamalar, build ve yayın checklist'ini içerir.
- GitHub public source paylaşımı teknik olarak mümkün; açık kaynak yayını için `LICENSE`, asset/dependency hak denetimi, katkı/security politikaları ve kalıcı production signing gerekir.
- Ana F-Droid deposuna başvuru henüz hazır değildir. Kalan kapılar: FLOSS lisansı, public tagged source, temiz source build, fdroiddata build recipe, localized fastlane metadata/screenshots, non-free/anti-feature denetimi ve signing/reproducible-build kararı.
- Ayrıntılı checklist ve güncelleme protokolü `HANDOFF/07-PUBLISHING.md` içindedir.

## Production release ve F-Droid audit — 2026-07-13

- `LICENSE` canonical Apache-2.0 metni olarak doğrulandı. `.gitignore`, `key.properties`, `*.jks` ve `*.keystore` kalıplarını kapsar; secret-value taraması `key.properties` dışında sıfır kopya buldu.
- Incremental `assembleRelease` 69 saniyede `BUILD SUCCESSFUL` tamamlandı; `validateSigningRelease`, `lintVitalRelease`, `packageRelease` ve `assembleRelease` geçti. Tam unit/device test turu bu yayın adımında gereksiz yere tekrarlanmadı; önceki 80/80 JVM + 7/7 cihaz kanıtı geçerlidir.
- Doğrulanan APK: `output/Converty-Android-v0.1.0-release.apk`, 15.691.403 bayt, package/version `com.converty.app` / `0.1.0` (1), min/target 23/37, APK SHA-256 `FE4CC780F48A83D1ACADF674BE5CC4DF9EDAE12212609CCBE8420CC8C09CF47A`.
- `apksigner verify --verbose --print-certs`: v1=true, v2=true; RSA-4096 production sertifika SHA-256 `A3C87F2AD05364D20AC49864D1BA33DCF1D5F6D359A3455C62D4EFF74B388F7A`. Signer DN production kimliğidir, Android Debug değildir.
- Release dependency/manifest auditinde proprietary SDK, reklam, analytics, Firebase/GMS, network client veya `INTERNET` izni bulunmadı. AndroidX kaynaklı iki `.so` Apache-2.0 ve güvenilir Google Maven bağımlılıklarıdır; vendored `.aar/.jar/.so` yoktur.
- F-Droid FAIL nedenleri: `com.converty.app` başka Google Play uygulamasıyla çakışıyor; public Git remote/tag kanıtı, fastlane/fdroiddata metadata, HEIC/AVIF test fixture provenance veya `scandelete`, temiz Linux/F-Droid scanner/lint/build henüz yoktur. Paket kimliği kullanıcıya ait namespace belirlenmeden otomatik değiştirilmemiştir.
