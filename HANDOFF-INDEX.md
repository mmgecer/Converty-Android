# Converty Android — HANDOFF Index

Bu dosya projenin kalıcı ana devretme belgesidir. **Bu projede çalışan her yapay zekâ işe başlamadan önce bu dosyanın tamamını ve göreviyle ilgili alt HANDOFF dosyalarını okumalı; yaptığı anlamlı kod, mimari, bağımlılık, davranış, hata düzeltmesi ve doğrulama değişikliğinden sonra ilgili belgeleri güncellemelidir.**

## Değişmez çalışma kuralları

- Bu proje mevcut Windows klasöründe çalışır. Kullanıcı 2026-07-13 tarihinde Git kullanımına, commit ve GitHub push'una açıkça izin verdi. `origin` doğrulanmış `https://github.com/mmgecer/Converty-Android.git` adresidir; destructive/force Git işlemleri yasaktır.
- Kod keşfinde önce `codebase-memory-mcp` bilgi grafiği araçları kullanılır; metin/config araması veya yetersiz grafik sonucu dışında dosya taramasına dönülmez.
- HANDOFF belgeleri ne yüzeysel ne de günlük dökümü kadar şişkin olmalıdır. Bir sonraki geliştiricinin neden-sonuç ilişkisini anlayacağı orta ayrıntı korunur.
- Yeni veya değişen her önemli dosya için: görevi, değişiklik nedeni, önemli tasarım kararı, bilinen risk ve doğrulama sonucu ilgili alt belgede tutulur.
- Bir bug düzeltildiğinde yalnızca sonuç değil; görülen belirti, kök neden ve uygulanan çözüm de yazılır.
- Bir alt belgenin kapsamı değişirse bu indeks de aynı çalışmada güncellenir.

## Alt HANDOFF haritası

| Belge | Kapsam |
|---|---|
| [HANDOFF/00-PROJECT-RULES.md](HANDOFF/00-PROJECT-RULES.md) | Ürün hedefi, kapsam sınırları, çalışma ve belge güncelleme protokolü |
| [HANDOFF/01-ARCHITECTURE.md](HANDOFF/01-ARCHITECTURE.md) | Android modülleri/katmanları, temel dosyalar, bağımlılık yönleri ve mimari kararlar |
| [HANDOFF/02-CONVERSION-ENGINES.md](HANDOFF/02-CONVERSION-ENGINES.md) | PDF/PPTX, raster/SVG ve Office/ODF motorlarının davranışı, güvenlik sınırları ve gerçek çıktı testleri |
| [HANDOFF/03-UX-M3E-I18N.md](HANDOFF/03-UX-M3E-I18N.md) | Material 3 Expressive tasarım sistemi, ekranlar, erişilebilirlik, tema ve 8 dil altyapısı |
| [HANDOFF/04-DATA-FILES-HISTORY.md](HANDOFF/04-DATA-FILES-HISTORY.md) | SAF dosya erişimi, çoklu seçim/kuyruk, aralık-kırpma modeli, çıktı, paylaşım ve Room geçmişi |
| [HANDOFF/05-DECISIONS-CHANGELOG.md](HANDOFF/05-DECISIONS-CHANGELOG.md) | Tarihli önemli kararlar, yapılan değişiklikler, gerekçeler ve bug kayıtları |
| [HANDOFF/06-VERIFICATION-ROADMAP.md](HANDOFF/06-VERIFICATION-ROADMAP.md) | Faz planı, kabul ölçütleri, test/derleme durumu, bilinen riskler ve sıradaki işler |
| [HANDOFF/07-PUBLISHING.md](HANDOFF/07-PUBLISHING.md) | Çok dilli README seti, GitHub/F-Droid yayın hazırlığı, lisans, imza ve store metadata gereksinimleri |

## Güncel durum — 2026-07-13

- Başlangıçta bulunan `OLD BAD CODE` daha sonra kullanıcı tarafından silindi; aktif ürün yalnız Android kaynak ağacıdır.
- Kullanıcı Android kaynakları için Codebase Memory indeksini yeniden oluşturduğunu belirtti. Bu çalışma oturumunda MCP transport kapalı kaldığı için son UI keşfi doğrudan kaynak okumayla yapıldı; servis erişilebilir olduğunda grafik sonucu Android paketleriyle kontrol edilmelidir.
- Android uygulaması; cihaz üstü PDF→PPTX ve temel görsel/metin/şekil PPTX→PDF motoru, SAF, Room, DataStore, WorkManager, ölçeklenebilir dönüşüm kataloğu, dört M3E ana ekranı, dört marka paleti ve 8 dil kaynağı içerir.
- Sistem PATH üzerinde araç yok; doğrulama için izole OpenJDK 17.0.19 (`C:\tmp\converty-jdk17\jdk-17.0.19+10`), Android SDK 37.0 (`C:\tmp\converty-android-sdk`) ve proje Gradle wrapper 9.4.1 kullanıldı.
- Resmî M3 Expressive sürüm/API doğrulaması tamamlandı: ana pin `material3:1.5.0-alpha23`; build hedefi AGP 9.2.1, Gradle 9.4.1, compile/target SDK 37 ve JDK 17. SDK paket adı `platforms;android-37.0` olarak doğrulandı.
- İlk APK'nın simgeye dokunulduktan sonra UI göstermeden kapanmasının kök nedeni, `AppCompatActivity` ile platform `android:style/Theme.Material.*` başlangıç temasının uyumsuzluğuydu. Gündüz/gece `Theme.Converty`, `Theme.AppCompat.DayNight.NoActionBar` soyuna taşındı; paketlenmiş APK kaynağı `aapt2` ile doğrulandı ve regresyon testleri eklendi.
- Son doğrulanan temel: Kotlin compile, 64/64 unit test, `lintDebug` ve `assembleDebug` başarılı. Sekiz locale'in her biri 125 string + 2 plural kaynağı taşır. APK applicationId `com.converty.app`, minSdk 23, targetSdk 37; v1/v2 debug imzası doğrulandı.
- Son UI cilası APK'sı bazı cihazlarda yeniden UI göstermeden kapanma regresyonu üretti. Özel localized `Context` sarmalaması kaldırıldı; locale artık AppCompat public API + Activity `locale|layoutDirection` config handling ile uygulanır, kalite varsayılanı açılışta DataStore yazısı olmadan çözülür ve Home kataloğu güvenli satır ölçümüne geçti.
- Güncel debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 23,371,172 bayt, SHA-256 `F93B8BF7EB7643E43638ECE2EADFBB300632A4C1180ABF71EE3C605DB35D0BAA`.
- Bağlı cihaz/emülatör olmadığı için gerçek cihaz smoke testi yapılmadı. Sıradaki öncelik gerçek PDF/PPTX + farklı DocumentsProvider/RTL/büyük dosya instrumentation testidir.
- Kullanıcı aynı cihazda UI oluşmadan kapanmanın sürdüğünü yeniden bildirdi. Stacktrace alınamadığı için tek bir kök neden kesinleştirilmedi; ilk kareden önce çalışan risk yüzeyi daraltıldı: WorkManager başlatması ilk gerçek iş çağrısına ertelendi, Room/DataStore akış hataları güvenli Home state'ine düşürülüp loglanıyor, locale ve bildirim kanalı hataları process'i sonlandırmıyor. Bu korumalar yerel geçmişi silmez.
- Güncel kaynak; iki taraflı M3E format seçici, tek çoklu dosya seçici, genişletilmiş format matrisi, DPI/lossless seçenekleri ve çoklu çıktı destekli Room v3 şemasını içerir. Ayrıntılar `02`, `03` ve `04` alt HANDOFF belgelerindedir.
- Güncel debug APK `app/build/outputs/apk/debug/app-debug.apk`: 23.345.524 bayt, SHA-256 `A97519F2AECDAA1B16B034FB66B9B5479C43C2F3D1CEE5CDBD309DF3444DC189`. 65/65 JVM testi, `lintDebug`, `assembleDebug` ve v1/v2 imza doğrulaması başarılıdır. Cihaz bağlı olmadığından açılışın gerçek cihazda kapandığı henüz kanıtlanmış değildir.

## En son doğrulanmış durum — 2026-07-13

- Kaynak ve cihazdaki kalıcı paket kimliği `com.converty.app` olarak doğrulandı. Ayrı test paketi veya `com.crownguard.converty` kullanılmıyor.
- Samsung SM-S721B / Android 15 üzerinde `MainActivityStartupTest`, `DatabaseMigrationSmokeTest` ve beş `ConversionEngineSmokeTest` vakası çalıştı: **7/7 geçti**. PDF→PPTX→PDF, PDF→PNG/JPG/TIFF/thumbnail, PNG/JPG/WebP/BMP/SVG/TIFF, HEIC/HEIF→JPG/PNG, AVIF→PNG/JPG/WebP ve DOCX/XLSX/ODT/ODS/ODP→PDF yolları cihazda gerçek dosya/imza kontrolleriyle doğrulandı.
- Android/Harmony parser'ın masaüstü JAXP güvenlik özelliklerini desteklememesi cihaz testinde yakalandı. `SecurePackageXml.kt`; desteklenen feature'ları kullanır, ayrıca UTF-8/UTF-16 DOCTYPE/ENTITY taraması ve harici entity'leri reddeden resolver uygular. XXE koruması kaldırılmadan Office/ODF ve PPTX cihaz uyumluluğu düzeltildi.
- Güncel tam kalite kapısı: **79/79 JVM testi**, `lintDebug`, `assembleDebug` ve **7/7 cihaz testi** başarılı. Sekiz locale 152 string + 2 plural parity sözleşmesini korur.
- Güncel APK: `app/build/outputs/apk/debug/app-debug.apk`, 23.345.524 bayt, SHA-256 `D90ED9AD01C0A74B9D8E64B1F21DCBF7AEF22D542EEBA4A5B9767D89952C2E90`. Bu APK cihaza yeniden kuruldu; ADB soğuk açılış `Status: ok`, 1.669 ms, çalışan PID ve boş crash filtresi verdi.
- OneDrive, AndroidTest `R.jar` paketlemesinde dosya kilidi ürettiği için son cihaz testleri güncel kaynakların `C:\tmp\converty-androidtest-20260713-8` temiz kopyasında yürütüldü. Bu klasör kaynak değildir. Yükseltilmiş derlemenin farklı debug anahtarı seçmesi, geçici keystore kopyası + Gradle signing override ile giderildi; APK sertifika SHA-256 değeri `29787975C9F52F33395B7E13A48B629E4200EA3D023125BECAF81B73F32EC313` olarak eşleştirildi.
- Codebase Memory MCP bu turda yine `Transport closed` döndürdü; kaynak keşfinde belgelenmiş doğrudan okuma fallback'i kullanıldı. Servis geldiğinde Android indeksini `search_graph` ile kontrol etmek hâlâ gereklidir.
- Kalan gerçek-cihaz kapsamı: farklı SAF provider'ları, foreground iptal, RTL/büyük font ve büyük/bozuk gerçek belge stres testleri. Format matrisindeki codec/motor yolları gerçek cihaz fixture'larıyla doğrulandı; ileri PowerPoint sadakati warning/unsupported kapsamındadır.
- Codebase Memory MCP sonraki ikon/release turunda yeniden çalıştı ve Android manifest/kaynak/test sembollerini döndürdü; kod keşfinde grafik önceliği aktiftir.
- `ICON` tasarımı altı katmanlı master SVG ile ayrı background/foreground/monochrome Android vektörlerinden oluşur ve M3E ürün diline uygundur. Uygulama artık minSdk 23 legacy, Android 8+ adaptive ve Android 13+ themed launcher icon kaynaklarını kullanır; yeni sözleşme testi toplam JVM testini 80/80'e çıkardı.
- Production release signing kuruldu. Gitignored `converty-release.keystore` ve `key.properties` yerelde tutulur; parola hiçbir belgeye/loga yazılmaz. `key.properties` olmadığında release signing tanımlanmadığı için public/F-Droid kaynak ağacı anahtarsız derlenebilir.
- Güncel production teslimi `output/Converty-Android-v0.1.0-release.apk` ve aynı içeriğin kısa adı `output/release.apk`: `com.converty.app` 0.1.0 (1), 15.691.403 bayt, APK SHA-256 `FE4CC780F48A83D1ACADF674BE5CC4DF9EDAE12212609CCBE8420CC8C09CF47A`; v1/v2 imza doğrulandı. RSA-4096 production sertifika SHA-256 değeri `A3C87F2AD05364D20AC49864D1BA33DCF1D5F6D359A3455C62D4EFF74B388F7A`dır.
- GitHub için İngilizce ana `README.md` ile Türkçe, Almanca, Basitleştirilmiş Çince, Arapça, Portekizce, Fransızca ve Rusça README dosyaları eklendi. Her dosya diğer yedi dile bağlanır; README'ler emojisiz, ciddi ve kaynak kodun gerçek format/sadakat sınırlarıyla uyumludur.
- Kökte standart Apache-2.0 `LICENSE` vardır; release bağımlılık taramasında yalnız FLOSS AndroidX/Kotlin/kotlinx/Okio/Guava-listenablefuture/annotation aileleri bulundu, reklam/analytics/Firebase/GMS ve `INTERNET` izni yoktur. Kaynak GitHub'da Apache-2.0 olarak yayımlanabilir.
- Ana F-Droid deposu için henüz hazır değildir. En kritik engel, `com.converty.app` kimliğinin Google Play'deki ilgisiz bir Converty uygulamasıyla çakışmasıdır. Ayrıca public Git remote/tag, fastlane + fdroiddata metadata, HEIC/AVIF test fixture provenance/scandelete kararı ve temiz Linux/F-Droid build/scanner kanıtı gerekir. Ayrıntı `HANDOFF/07-PUBLISHING.md` içindedir.
- Yerel `main` başlangıç commit'i `22f3990` oluşturuldu. GitHub'daki önceki `2ec3ebe` başlangıç commit'i silinmeden `8c9b016` merge commit'iyle geçmişe bağlandı ve tam kaynak `origin/main` dalına push edildi. Henüz `v0.1.0` etiketi oluşturulmadı.
- Sekiz README'de genel amaçlı üretim fiilinin kullanıcı tarafından yasaklanan İngilizce/Türkçe ve yerelleştirilmiş karşılıkları kaldırıldı. Bundan sonra bağlama göre “generate”, “produce”, “add”, “prepare”, “configure” gibi kesin fiiller ve her dildeki doğal karşılıkları kullanılmalıdır.

## Hızlı başlangıç — sonraki yapay zekâ

1. Bu indeksin tamamını oku.
2. Görevinle ilişkili alt HANDOFF belgelerini oku.
3. Dosya/fonksiyon keşfi için önce bilgi grafiğini kullan; yeni kodlardan sonra gerekirse indeksi tazele.
4. Git kullanılırsa destructive/force komut çalıştırma; `origin/main` geçmişini koru ve push öncesi fetch/status ile uzak değişiklikleri doğrula.
5. Uygulamayı değiştir ve orantılı test et.
6. Aynı çalışmada ilgili alt HANDOFF dosyalarını ve gerekiyorsa bu indeksin güncel durumunu güncelle.
