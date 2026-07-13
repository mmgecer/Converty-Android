# Converty

[English](README.md) | Türkçe | [Deutsch](README.de.md) | [简体中文](README.zh-CN.md) | [العربية](README.ar.md) | [Português](README.pt.md) | [Français](README.fr.md) | [Русский](README.ru.md)

Converty; cihaz üzerinde çalışan dönüşüm motorları, Android Storage Access Framework ve Material 3 Expressive arayüzü üzerine kurulmuş bir Android dosya dönüştürme uygulamasıdır. Tekli ve çoklu dosya seçimi, sayfa veya slayt bazlı seçim, kalıcı işlem geçmişi, arka plan işleri ve üretilen dosyaları doğrudan açma veya paylaşma desteği sunar.

Proje aktif geliştirme aşamasındadır. Dönüşüm motorları format sınırlarını açıkça belirtir. PDF'ten PPTX'e dönüşüm, işlenen sayfaları slaytlara görsel olarak yerleştirerek görünümü korur; ortaya çıkan metin düzenlenebilir değildir. PPTX'ten PDF'e ve Office'ten PDF'e yollar, belge içeriğinin pratik bir alt kümesini destekler ve eksiksiz Microsoft Office render sadakati iddiasında bulunmaz.

## Güncel durum

- Android uygulama kimliği: `com.converty.app`
- En düşük Android sürümü: Android 6.0, API 23
- Hedef ve derleme SDK'sı: API 37
- Arayüz: Material 3 Expressive API'leriyle Jetpack Compose
- İşleme modeli: yerel, cihaz üzerinde dönüşüm
- Güncel doğrulama: 80 JVM testi ve 7 Android cihaz instrumentation testi
- Kamu dağıtımı durumu: Apache-2.0 kapsamında kaynak yayınına hazır ve yerel imzalı release üretildi; F-Droid hazırlığı henüz tamamlanmadı. [Sürüm ve dağıtım durumu](#sürüm-ve-dağıtım-durumu) bölümüne bakın.

## Temel özellikler

- Tek bir akışta bir veya birden çok dosya seçme.
- WorkManager ile dosya ve iş seviyesinde ilerleme gösteren dönüşümler.
- Tamamlanan, başarısız olan ve iptal edilen işler için yerel geçmiş.
- Geçmiş ekranından her çıktıyı ayrı ayrı açma veya paylaşma.
- Android sistem dosya seçicisiyle çıktı klasörü belirleme.
- Tüm sayfa/slaytları, ilk veya son N öğeyi, özel aralıkları ya da `1,3-5,9` gibi ayrık seçimleri işleme.
- Kaynak belgeyi değiştirmeden belirli aralıkları tutma veya çıkarma.
- Uygun yönlerde görüntü kalitesi, DPI, WebP lossless ve PDF'ten PPTX'e görüntü yerleşimi seçenekleri.
- Sistem, açık ve koyu tema; desteklenen Android sürümlerinde dinamik renk.
- İngilizce, Türkçe, Almanca, Basitleştirilmiş Çince, Arapça, Portekizce, Fransızca ve Rusça arayüz.
- Desteklenen launcher'larda adaptive ve Android 13 temalı uygulama ikonu.

## Desteklenen dönüşümler

### Belgeler ve sunumlar

| Kaynak | Hedef | Davranış |
|---|---|---|
| PDF | PPTX | Seçilen her PDF sayfasını render eder ve bir slayda yerleştirir. Görünüm görsel olarak korunur; metin düzenlenebilir değildir. |
| PPTX | PDF | Desteklenen slayt metinlerini, görselleri, dolgu ve çizgileri, temel şekilleri render eder. Eksik desteklenen sunum özellikleri uyarı üretebilir. |
| DOCX | PDF | Desteklenen belge içeriğini cihaz üzerinde dönüştürür. Karmaşık Word düzeni Microsoft Word'den farklı olabilir. |
| XLSX | PDF | Desteklenen çalışma sayfası içeriğini dönüştürür. İleri düzen ve hesaplama davranışı sınırlıdır. |
| ODT | PDF | Desteklenen OpenDocument metin içeriğini dönüştürür. |
| ODS | PDF | Desteklenen OpenDocument hesap tablosu içeriğini dönüştürür. |
| ODP | PDF | Desteklenen OpenDocument sunum içeriğini dönüştürür. |

### PDF görüntü çıktıları

| Kaynak | Hedef | Not |
|---|---|---|
| PDF | PNG | Seçilen her sayfa için kayıpsız PNG üretir. |
| PDF | JPG | Yapılandırılabilir kalite ve DPI ile her seçilen sayfa için JPEG üretir. |
| PDF | TIFF | Seçilen sayfalar için TIFF çıktısı üretir. |
| PDF | Thumbnail | Küçük bir JPEG önizleme üretir. |

### Görsel dönüşümleri

- PNG'den JPG ve WebP'ye
- JPG'den PNG ve WebP'ye
- WebP'den PNG ve JPG'ye
- TIFF'ten PNG ve JPG'ye
- BMP'den PNG ve JPG'ye
- HEIC veya HEIF'ten PNG ve JPG'ye
- SVG'den PNG'ye
- AVIF'ten PNG, JPG ve WebP'ye

Özellikle HEIC, HEIF, AVIF ve TIFF için codec kullanılabilirliği Android sürümüne ve cihaz uygulamasına göre değişebilir.

## Seçim ve çıktı denetimleri

PDF ve PPTX girdilerinde tüm sayfa/slaytlar, ilk N, son N, tutulacak aralıklar, çıkarılacak aralıklar veya tek işlemde birden fazla ayrı aralık seçilebilir.

PDF'ten PPTX'e dönüşüm contain, cover ve stretch yerleşimini destekler. PDF raster dönüşümünde yön destekliyorsa 72 ile 600 arasında DPI seçilebilir. Çıktı adları güvenli hale getirilir, Unicode adlar korunur ve dosya çakışmaları seçilen çıktı politikasına göre yönetilir.

## Gizlilik ve depolama

Converty'nin güncel manifesti Android `INTERNET` izni istemez; uygulamada reklam veya analiz SDK'sı yoktur. Dönüşüm cihaz üzerinde yapılır. Uygulama yalnızca Android sistem belge seçicisi üzerinden kullanıcının seçtiği dosya ve çıktı klasörlerine erişir.

İşlem geçmişi ve ayarlar Room ile DataStore kullanılarak yerel olarak saklanır. Uygulama verileri, geçmiş ve seçilen dosya metadatası Android bulut yedeği ile cihaz aktarım yedeğine dahil edilmez. Cleartext ağ trafiği uygulama yapılandırmasında kapalıdır.

Bu ifadeler güncel kaynak ağacını açıklar; izin, bağımlılık, telemetri, crash reporting veya ağ özelliği eklendiğinde yeniden incelenmelidir.

## Material 3 Expressive arayüz

Arayüz Material 3 Expressive bileşenlerini, hareket sistemini, tonal renkleri ve adaptive navigasyonu kullanır. Kaynak/hedef format seçicisi, expressive ilerleme göstergeleri, tema ve palet seçenekleri, Arapça için RTL yerleşim, sekiz dilde kaynak tabanlı metinler ve katmanlı launcher ikonları içerir.

## Bilinen sınırlar

- PDF'ten PPTX'e yol görsel tabanlı slaytlar üretir; metin ve sayfa nesneleri düzenlenebilir PowerPoint öğelerine dönüştürülmez.
- PPTX'ten PDF'e motoru Microsoft PowerPoint, LibreOffice veya eksiksiz bir OOXML render motorunun yerine geçmez. SmartArt, grafik, tablo, gruplu nesne, medya, animasyon, karmaşık tema, master düzeni, font ikamesi ve gelişmiş efektler eksik veya desteklenmiyor olabilir.
- DOCX, XLSX, ODT, ODS ve ODP dönüşümü sınırlı cihaz üstü render modeline sahiptir. Karmaşık belgelerde sayfalama ve görsel düzen birebir korunmayabilir.
- Parolalı, şifreli, bozuk, aşırı büyük veya kötü niyetli belgeler reddedilebilir.
- Büyük belgeler yüksek bellek ve uzun işlem süresi gerektirebilir.
- Cihaz codec farkları HEIC, HEIF, AVIF, TIFF ve WebP sonuçlarını etkileyebilir.
- Android 13 ve sonrasında foreground iş bildirimleri bildirim izni ister. İzni reddetmek uygulama içindeki kuyruk ekranını kapatmaz.

## Kaynaktan derleme

### Gereksinimler

- JDK 17
- Android SDK Platform 37
- Android Build Tools 37.0.0
- Projeyle gelen Gradle wrapper

Temel araç sürümleri AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.10, Compose BOM 2026.06.00 ve Material 3 1.5.0-alpha23'tür.

Linux veya macOS:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Release varyantı şu komutla üretilir:

```sh
./gradlew assembleRelease
```

Production imzalama, yerel `key.properties` dosyası ve özel release keystore üzerinden yapılandırılmıştır. İki dosya da `.gitignore` kapsamındadır; repoya eklenmemeli, dokümantasyona kopyalanmamalı, loglarda veya issue kayıtlarında paylaşılmamalıdır. Bu yerel dosyalar varken `assembleRelease` imzalı APK üretir. Temiz bir herkese açık kaynak kopyası özel imza malzemesi içermez ve imzasız release üretebilir; dağıttığı ikiliyi kendisi imzalayan F-Droid için doğru kaynak derleme yolu budur.

## Proje yapısı

- `app/src/main/java/com/converty/app/core`: dönüşüm sözleşmeleri, seçim mantığı ve motorlar.
- `app/src/main/java/com/converty/app/data`: SAF dosya erişimi, Room geçmişi ve DataStore ayarları.
- `app/src/main/java/com/converty/app/work`: WorkManager, foreground ilerleme, iptal ve worker adaptörleri.
- `app/src/main/java/com/converty/app/feature`: Compose ekranları, uygulama durumu ve kullanıcı eylemleri.
- `app/src/main/java/com/converty/app/ui`: Material 3 Expressive tema ve ortak arayüz bileşenleri.
- `app/src/main/res`: sekiz dil, temalar, launcher ikonları ve Android yapılandırması.
- `app/src/test`: JVM sözleşme ve regresyon testleri.
- `app/src/androidTest`: cihaz açılışı, veritabanı migration ve dönüşüm smoke testleri.
- `HANDOFF`: mimari, karar, doğrulama ve devam notları.

## Sürüm ve dağıtım durumu

Kaynak ağacı Apache License 2.0 kapsamında GitHub'da yayımlanmaya hazırlanmıştır. Özel production anahtarı yalnızca yerelde yapılandırılmış ve imzalı bir release APK üretilmiştir. Özel imza dosyaları kasıtlı olarak herkese açık kaynak ağacının dışında tutulur.

Mevcut imzalı APK, F-Droid'e hazır paket değil yerel release adayı olarak görülmelidir. F-Droid başvurusu veya kalıcı kamu paketi yayını öncesinde şu işler tamamlanmalıdır:

1. `com.converty.app`, kalıcı ve dünya çapında benzersiz bir application ID ile değiştirilmelidir. Bu kimlik ilgisiz başka bir uygulama tarafından zaten kullanıldığı için mevcut haliyle yayınlamak kimlik çakışmasına yol açar.
2. Herkese açık `main` dalı [GitHub'da](https://github.com/mmgecer/Converty-Android) yayımlanmıştır. Gönderilen kaynakla birebir eşleşen `v0.1.0` tag'i tanımlanıp push edilmelidir.
3. Sürüm eşlemesi ve tamamen kaynaktan derleme tarifi dahil F-Droid build metadatası eklenmelidir.
4. Temiz bir Linux checkout'undan F-Droid benzeri ortamda yeniden derleme yapılmalı; F-Droid scanner ve reproducibility kontrolleri geçilmelidir.
5. Varlıkların ve bağımlılık lisanslarının son incelemesi tamamlanmalı; gerekli olduğu belirlenen bildirim ve atıflar eklenmelidir.
6. Her yayında `versionCode` artırılmalı; sürüm notları, checksum'lar, ekran görüntüleri, gizlilik açıklaması ve destek/issue kanalı yayımlanmalıdır.

Apache-2.0 lisansı, yerel production imzalama ve `main` yayını tamamlanmıştır. Application ID çakışması, `v0.1.0` release tag'i, F-Droid metadatası ve temiz Linux/F-Droid derleme ve scanner doğrulaması engel olmaya devam etmektedir.

## Katkı

Henüz katkı politikası ve davranış kuralları kabul edilmemiştir. Dış katkı alınmadan önce `CONTRIBUTING.md`, davranış kuralları, issue şablonları ve katkıların proje lisansı kapsamında yeniden dağıtılabilmesini doğrulayan bir süreç eklenmelidir.

## Lisans

Converty, Apache License 2.0 kapsamında lisanslanmıştır. Tam koşullar için kökteki [`LICENSE`](LICENSE) dosyasına bakın.
