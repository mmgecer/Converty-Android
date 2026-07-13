# Yayınlama ve README HANDOFF

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur. README, lisans, uygulama kimliği, signing, sürümleme, GitHub Releases, store metadata veya F-Droid hazırlığı değiştiğinde bu belge aynı çalışmada güncellenmelidir.

## README dosya haritası — 2026-07-13

- `README.md`: İngilizce ana ve en ayrıntılı kaynak.
- `README.tr.md`: Türkçe.
- `README.de.md`: Almanca.
- `README.zh-CN.md`: Basitleştirilmiş Çince.
- `README.ar.md`: Arapça.
- `README.pt.md`: genel Portekizce.
- `README.fr.md`: Fransızca.
- `README.ru.md`: Rusça.

Her README'nin ilk bölümünde aynı sekiz dil sırası vardır; mevcut dil düz metin, diğer yedi dil göreli bağlantıdır. Apache-2.0, conditional production signing ve güncel F-Droid blocker'ları 2026-07-13 tarihinde sekiz dosyanın tümünde eşlendi. Otomatik kontrolde sekiz dosyanın tüm bağlantı hedefleri bulundu, her dosyada yedi benzersiz README bağlantısı doğrulandı, UTF-8 okuması geçti ve eski “lisans/signing yok” iddiası kalmadı. İngilizce README 180 satırdır; özellik, format matrisi, gizlilik, sınırlar, build, mimari ve yayın checklist'i içerir. Çeviriler pazarlama özeti değildir; aynı teknik iddiaları ve yayın blocker'larını korur.

## README'de korunması gereken ürün doğruları

- Paket `com.converty.app`, minSdk 23, compile/target SDK 37'dir.
- Uygulama güncel kaynakta `INTERNET` izni istemez; reklam/analytics SDK bağımlılığı yoktur. Dönüşüm SAF ile seçilen kaynak ve hedeflerde cihaz üstünde çalışır. Bu iddia yeni izin, ağ veya telemetry eklendiğinde yeniden denetlenmelidir.
- PDF→PPTX sayfaları slaytlara görsel olarak koyar; metin düzenlenebilir değildir.
- PPTX→PDF temel metin/görsel/dolgu/çizgi/şekil desteğidir; tam PowerPoint sadakati iddia edilmez.
- DOCX/XLSX/ODT/ODS/ODP→PDF on-device sınırlı renderer'dır; karmaşık Office sadakati garanti edilmez.
- Aktif format matrisi `ConversionDirection.kt` ile eş tutulmalıdır. Yeni yön eklendiğinde sekiz README birlikte güncellenmelidir.
- Son doğrulama README'lerde 80 JVM + 7 cihaz testi olarak yazılıdır; kalite kapısı değişirse bu sayılar birlikte güncellenmelidir.

## GitHub yayın durumu

Kökte standart Apache License 2.0 metnini taşıyan `LICENSE` vardır. Kaynak `https://github.com/mmgecer/Converty-Android` üzerinde açık kaynak olarak yayımlandı. Kullanıcının açık talebiyle yerel `main` başlatıldı; GitHub'daki mevcut `2ec3ebe` başlangıç commit'i silinmeden `8c9b016` merge commit'iyle bağlandı ve tam kaynak `origin/main` dalına push edildi. Force-push yapılmadı; sonraki AI fetch etmeden uzak geçmiş varsaymamalıdır.

Kamuya açılmadan önce ayrıca:

1. Özgün `ICON` varlıklarının Apache-2.0 kapsamında yayımlandığı açıkça belirtilmeli; AOSP HEIC/AVIF test fixture'ları için kesin upstream commit/telif/lisans kaydı eklenmeli.
2. Release dependency taramasında AndroidX, Kotlin/kotlinx, Okio, Guava `listenablefuture`, JetBrains annotations ve JSpecify dışında runtime aile bulunmadı. Bunlar FLOSS'tur; Firebase/GMS/reklam/analytics/telemetry yoktur. Yeni bağımlılık eklendiğinde lisans taraması tekrarlanmalı.
3. `CONTRIBUTING.md`, davranış kuralları, security/issue kanalı ve gizlilik açıklaması hazırlanmalı.
4. Ekran görüntüleri ve sürüm notları oluşturulmalı.
5. Her yayın için `versionCode` artırılmalı ve public source revision bir release tag ile sabitlenmeli.

Kökteki `.gitignore`; `local.properties`, `.gradle`, `.kotlin`, tüm generated `build` klasörleri, IDE dosyaları, `output`, APK/AAB, loglar, `key.properties`, `*.jks` ve `*.keystore` dosyalarını public source kapsamı dışında tutar. Signing keystore veya parola dosyası hiçbir zaman repoya eklenmemelidir. `ICON`, `HANDOFF`, Gradle wrapper ve gerçek kaynaklar bilinçli olarak dışlanmaz.

## Signing ve APK dağıtımı

- `converty-release.keystore` ve `key.properties` ile kalıcı production signing kuruldu. İkisi de gitignored'dur; hiçbir parola README, HANDOFF veya loga yazılmadı. Bu iki dosya, özellikle keystore, güvenli ve tercihen offline bir yerde birlikte yedeklenmelidir.
- `app/build.gradle.kts`, `key.properties` varsa production config'i kullanır; dosya yoksa release signing config oluşturmaz. Böylece maintainer build'i imzalı, public/F-Droid source build'i anahtarsız olabilir.
- Canonical yerel teslim `output/Converty-Android-v0.1.0-release.apk`; kısa yol `output/release.apk` aynı dosya içeriğidir. APK SHA-256 `FE4CC780F48A83D1ACADF674BE5CC4DF9EDAE12212609CCBE8420CC8C09CF47A`, production sertifika SHA-256 `A3C87F2AD05364D20AC49864D1BA33DCF1D5F6D359A3455C62D4EFF74B388F7A`dır. RSA-4096, v1/v2 doğrulaması geçti.
- Daha önce debug sertifikasıyla kurulan `com.converty.app` production APK ile aynı imzayı taşımaz; ilk production kurulumu için eski paket bir kez kaldırılmalıdır. Bundan sonraki tüm upstream güncellemeler aynı production keystore ile imzalanmalıdır.
- Public release sayfasında sürüm adı, versionCode, APK SHA-256 ve sertifika SHA-256 yayımlanmalı; keystore/parola hiçbir zaman eklenmemelidir.

## F-Droid uygunluk durumu

Ana F-Droid deposu için şu an **hazır değil**. Apache-2.0 lisans, conditional signing ve FLOSS dependency tabanı uygun; kalan başlıca işler:

- **Kritik kimlik çakışması:** `com.converty.app`, Google Play'de 10K+ indirmeli ilgisiz bir Converty e-ticaret uygulaması tarafından kullanılıyor. F-Droid farklı ve benzersiz Application ID ister; ilk public/F-Droid sürümünden önce kullanıcıya ait namespace seçilmelidir. Kullanıcının GitHub adı bilinmediği için otomatik paket değişikliği yapılmadı.
- Public Git upstream ve eksiksiz `main` source artık vardır; her resmî sürüm için tag (`v0.1.0`) ve F-Droid metadata'sında tam commit hash hâlâ gerekir.
- Anahtarsız temiz Linux/F-Droid build; `fdroid scanner`, `fdroid lint` ve izole `fdroid build` kanıtı. Windows production `assembleRelease` başarılıdır fakat bu F-Droid ortamı kanıtı değildir.
- `fastlane/metadata/android/<locale>` altında kısa/uzun açıklama, ikon, ekran görüntüleri ve versionCode changelog'u.
- `fdroiddata` metadata dosyası; lisans, source URL, issue tracker, build recipe, currentVersion/currentVersionCode ve gerekirse allowed signing key.
- `app/src/androidTest/assets` altındaki HEIC/AVIF fixture'ları release APK'ye girmez; yine de kesin AOSP upstream commit/telif/Apache-2.0 provenance kaydı eklenmeli veya F-Droid recipe'de `scandelete` kullanılmalıdır.
- Özgün ikon/varlıkların proje sahibi tarafından Apache-2.0 altında sunulduğu açıkça kaydedilmelidir.
- F-Droid'in kendi anahtarıyla derleyip imzaladığı standart yol veya upstream imzayı korumak için kanıtlanmış reproducible-build akışı.

F-Droid başvurusuna geçmeden önce applicationId çözülmeli; temiz Linux/container build'i denenmeli ve metadata yeni kimlik üzerinden hazırlanmalıdır. Standart F-Droid imzası seçilirse GitHub APK'sıyla karşılıklı update olmaz; aynı upstream imzası istenirse reproducible build + `Binaries`/`AllowedAPKSigningKeys` akışı gerekir.

Resmî referanslar: GitHub Docs “Licensing a repository”; F-Droid “Inclusion Policy”, “Submitting to F-Droid Quick Start Guide”, “Build Metadata Reference” ve “Reproducible Builds”. Gereksinimler zamanla değişebileceği için submission turunda bu sayfalar yeniden kontrol edilmelidir.

## Sonraki AI için güncelleme protokolü

- README'ye özellik eklemeden önce Codebase Memory ile gerçek sınıf/yön bulunmalı; format iddiası yalnız UI metnine dayanarak eklenmemelidir.
- İngilizce README canonical içeriktir fakat aynı değişiklikte diğer yedi dil de güncellenmelidir.
- README metninde genel amaçlı üretim fiilinin kullanıcı tarafından yasaklanan İngilizce/Türkçe veya yerelleştirilmiş doğrudan karşılıkları kullanılmaz; bağlama özgü üretme, ekleme, hazırlama, yapılandırma ve sonuca yol açma fiilleri seçilir.
- Dil navigasyonunda dosya adları değiştirilirse sekiz dosyanın tüm bağlantıları otomatik kontrol edilmelidir.
- Sekiz README Apache-2.0/signing durumuyla eş tutulmalı; applicationId ve diğer blocker'lar çözülmeden “F-Droid'e hazır” denmemelidir.
- Signing key, applicationId, versionName/versionCode veya store durumu değiştiğinde `HANDOFF-INDEX.md`, bu belge ve `06-VERIFICATION-ROADMAP.md` birlikte güncellenmelidir.
