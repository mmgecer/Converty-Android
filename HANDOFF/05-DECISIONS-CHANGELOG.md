# Kararlar ve Değişiklik Günlüğü

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur. Önemli karar, kod değişikliği, bağımlılık değişimi veya bug düzeltmesi tarih ve gerekçeyle buraya eklenir. Küçük biçimlendirme değişiklikleriyle belge şişirilmez.

## 2026-07-13 — Başlangıç envanteri ve belge sistemi

- **Değişiklik:** `HANDOFF-INDEX.md` ve altı konu bazlı alt HANDOFF belgesi oluşturuldu.
- **Neden:** Proje uzun süreli AI devriyle ilerleyecek; kararların ve dosya sorumluluklarının kaybolmaması gerekiyor.
- **Kural:** Her sonraki AI indeks + ilgili belgeleri okumalı ve kendi anlamlı değişikliklerinden sonra güncellemelidir. Git/GitHub kesinlikle kullanılmaz.

## 2026-07-13 — Android için yeniden yazım kararı

- **Karar:** Eski Python GUI/backend'i gömülmeyecek; ürün Kotlin + Jetpack Compose ile cihaz üstü Android uygulaması olarak yeniden yazılacak.
- **Neden:** Eski kod `tkinter`, Poppler/pdf2image, PowerPoint COM ve masaüstü LibreOffice süreçlerine bağlı. Bunlar Android runtime modeliyle uyumsuzdur.
- **Korunan mantık:** PDF sayfasını görüntü olarak render etme ve oranı bozmadan boş slayda ortalama; tekli/çoklu seçim fikri; iş geçmişi; opsiyonel toplu çıktı yaklaşımı.
- **İyileştirme:** Motor/UI ayrımı, akış tabanlı SAF erişimi, sayfa sayfa bellek kullanımı, yapılandırılmış hata/uyarı, iş kuyruğu ve dayanıklı geçmiş.

## 2026-07-13 — PPTX→PDF sadakat sınırı

- **Karar:** Masaüstü Office/LibreOffice kullanmadan cihaz üstü OOXML render motoru hedeflenecek; desteklenmeyen PowerPoint özellikleri gizlenmeyecek, sonuçta uyarı olarak gösterilecek.
- **Neden:** Android'de COM/`soffice` yoktur ve genel PPTX standardının tüm efektlerini sıfırdan birebir render etmek geniş kapsamlıdır.
- **Risk:** İlk sürüm temel metin, görsel ve şekillerle başlayabilir; SmartArt, grafikler, gömülü medya, bazı font/tema/animasyon özellikleri kademeli destek gerektirir.

## 2026-07-13 — Ortam bulgusu

- PATH üzerinde `java`, `gradle`, `adb` yoktur; yaygın Android Studio ve SDK yolları da bulunamadı.
- Gradle 9.3.1 wrapper dağıtımı ve AGP 8.2.0/8.7.3/9.1.0 önbellekleri bulundu. Material 3 için yerelde yalnız 1.3.1 görülüyor; M3 Expressive için resmi güncel sürüm doğrulaması ve muhtemel bağımlılık indirme gerekecek.

## 2026-07-13 — M3 Expressive ve build sürüm kararı

- **Karar:** `material3:1.5.0-alpha23`, Compose BOM `2026.06.00`, AGP 9.2.1, Gradle 9.4.1, compile/target SDK 37 ve JDK 17 kullanılacak.
- **Neden:** Resmî 1.4 beta notları deneysel Expressive public API'lerinin 1.4 hattından çıkarıldığını ve 1.5 alpha hattına geçilmesini söylüyor. Kullanıcının M3E şartı stable 1.4.0 ile tam karşılanmıyor.
- **Risk azaltma:** Alpha API'ler design-system wrapper'larında merkezileştirilecek, exact version pin ve UI testleri kullanılacak. Stable 1.4.0 fallback yalnız geçici derleme kurtarma yolu; ürün kabulü değildir.
- **Doğrulanmış API:** `MaterialExpressiveTheme`, `MotionScheme.expressive()`, `ShortNavigationBar`, `WideNavigationRail`, flexible app bar/toolbars, button shape overloadları ve wavy progress göstergeleri. Uydurma `ExpressiveButton` API'si kullanılmayacak.

## 2026-07-13 — compileSdk 37 paket adı düzeltmesi

- **Belirti:** İlk SDK kurulumunda `platforms;android-37` bulunamadığı için geçici olarak SDK 36 seçildi; ilk Gradle turunda M3E/Compose AAR'ları compileSdk 37 zorunluluğuyla durdu.
- **Kök neden:** Android 17 deposundaki paket adı `platforms;android-37.0`; eski tam sayı paket adı varsayımı yanlıştı.
- **Düzeltme:** `platforms;android-37.0` ve `build-tools;37.0.0` kuruldu; `compileSdk`/`targetSdk` yeniden 37 yapıldı. Resmî DSL hâlâ `compileSdk = 37` kullanır.

## 2026-07-13 — PPTX paketleme ve genel PPTX render stratejisi

- **PDF→PPTX:** Android/AWT uyumluluk ve APK boyutu risklerinden kaçınmak için Apache POI yerine uygulamaya ait minimal, deterministik OOXML paketleyici hedeflendi.
- **PPTX→PDF v1:** Uygulamanın ürettiği ve tek tam-slayt görseli kullanan basit PPTX'ler cihaz üstünde `PdfDocument` ile desteklenir.
- **Genel PPTX:** LibreOfficeKit teknik olarak mümkün fakat büyük native çekirdek, ABI/NDK, bellek, güvenlik güncellemesi ve MPL 2.0 yükümlülükleri taşır. Ayrı fizibilite kapısından geçmeden genel sadakat iddiası yapılmaz.

## 2026-07-13 — Çekirdek veri sınırları ve seçim parser'ı

- **Değişiklik:** Android'den bağımsız job/item/document/options modelleri, yapılandırılmış ve 0-tabanlı seçim çözümleyici, Room geçmişi, Preferences DataStore ve SAF handle/gateway katmanı eklendi.
- **Neden:** UI, kalıcı veri ve dönüşüm motorlarının aynı kullanıcı girdisini farklı yorumlamasını önlemek; `content://` URI'leri ham dosya yolu varsaymadan güvenle işlemek; toplu iş geçmişini süreç yeniden başlasa da korumak.
- **Doğrulama notu:** Seçim davranışı 22 unit test ile tanımlandı fakat JDK bulunmadığından testler henüz çalıştırılamadı.
- **Bug önleme:** “Remove” bütün öğeleri kaldırdığında `EMPTY_SELECTION` döner; aksi halde motorun “boş indeks listesi = tümü” sentinel'ı isteği tersine çevirirdi.

## 2026-07-13 — Android iskeleti, M3E ve sekiz dil

- **Değişiklik:** `com.converty.app` Compose iskeleti, version catalog, Expressive tema/navigation/wavy-progress wrapper'ları, güvenli manifest/XML ayarları ve İngilizce fallback + yedi locale kaynağı eklendi. İlk aramada `platforms;android-37` bulunamayınca compile/target geçici olarak 36 seçildi; bunun paket adı varsayımı hatası olduğu sonraki `compileSdk 37 paket adı düzeltmesi` kaydında açıklanmıştır. Nihai değer 37'dir.

## 2026-07-13 — İlk tam build/test/lint/APK doğrulaması

- **Doğrulama:** SDK 37/JDK 17 ile Kotlin compile başarılı; `testDebugUnitTest` 29/29 geçti; `lintDebug` ve `assembleDebug` başarılı; debug APK 23,323,828 bayt.
- **Lint bug düzeltmeleri:** `windowLightNavigationBar` için API 27 hedef işareti, custom WorkManager factory için varsayılan initializer metadata kaldırma, launcher icon ve Fransızca/Portekizce `many` plural eklendi. Baseline ile hata gizlenmedi.
- **Kalan risk:** Bağlı cihaz/emülatör yoktu; Android platform motorları, SAF/foreground worker, RTL ve büyük belge davranışı gerçek cihazda henüz çalıştırılmadı. Genel PPTX render hâlâ kademeli genişletiliyor.

## 2026-07-13 — Kayıtlı dil açılışında recreation döngüsü düzeltmesi

- **Belirti/risk:** ViewModel önce varsayılan `System` state'i, sonra DataStore'daki kayıtlı dili yayınlıyordu; MainActivity ilk değeri AppCompat'e uygulayarak sistem→kayıtlı dil ardışık recreation üretebilirdi.
- **Düzeltme:** Activity dil tercihini doğrudan DataStore repository akışından toplar; locale controller mevcut ve istenen BCP-47 etiketleri aynıysa `setApplicationLocales` çağırmaz.

## 2026-07-13 — Yerelleştirilmiş aralık noktalama düzeltmesi

- **Belirti:** Sekiz dilde UI örneği `3–5` en dash kullanıyordu; parser yalnız ASCII `-` kabul ediyordu. Arapça örnekteki `،` ayırıcı da geçersizdi.
- **Düzeltme:** Parser `-`, `–`, `—`, ASCII virgül ve Arapça virgülü kabul eder; gösterilen İngilizce/Arapça örnekleri unit testle sabitlendi.

## 2026-07-13 — Persistable SAF izin yaşam döngüsü düzeltmesi

- **Belirti/risk:** İzin dosya türü doğrulanmadan alınıyor; setup'tan çıkarma veya history silme/temizleme sonrasında bırakılmıyordu. Uzun kullanımda provider izin kotası ve gizlilik riski oluşabilirdi.
- **Düzeltme:** İzin yalnız kabul edilen dosya için alınır; başka aktif setup/job referansı kalmayan kaynak izinleri kaldırma, setup iptali ve history silme/temizleme sonrasında bırakılır.

## 2026-07-13 — Geçmiş hata görünürlüğü ve retry düzeltmesi

- **Belirti:** Worker item hata/uyarılarını Room'a yazdığı halde geçmiş yalnız genel durum gösteriyordu; ayrıca başarılı job için görünen Retry tüm başarılı item'ları atlayıp no-op oluyordu.
- **Düzeltme:** Item hata kodları yerelleştirilmiş mesaja, warning listesi kullanıcı uyarısına çevrildi; Retry yalnız FAILED/CANCELLED job'larda gösterilir.

## 2026-07-13 — Çıktı ad çakışması politikası düzeltmesi

- **Belirti:** `CREATE_UNIQUE/REPLACE/FAIL` Room modelinde saklanıyor fakat worker hepsinde doğrudan yeni belge oluşturuyordu.
- **Düzeltme:** SAF gateway çıktı ağacında aynı adlı doğrudan child'ı sorgular. Unique yeni belge açar, Replace mevcut URI'yi temp dönüşüm başarıyla kapanınca yazar, Fail mevcut dosyaya dokunmadan hata verir; yalnız worker'ın yeni oluşturduğu yarım URI hata/iptalde silinir.

## 2026-07-13 — Cihaz üstü PDF/PPTX motorları

- **Değişiklik:** Ortak motor sözleşmesi, sayfa sayfa `PdfRenderer`→deterministik image-only OOXML PPTX motoru ve güvenli OOXML→`PdfDocument` motoru eklendi; üçüncü taraf dönüşüm bağımlılığı eklenmedi.
- **Neden:** Eski masaüstü kodunun toplu bellek kullanımı ile COM/LibreOffice bağımlılığını kaldırmak ve Android'de çevrimdışı, seçime duyarlı dönüşüm sağlamak.
- **Güvenlik/sadakat kararı:** ZIP/XML limitleri ve XXE/path-traversal savunmaları zorunlu; v1 yalnız tek raster görselli basit slaytları render eder. Desteklenmeyen içerik sessizce atılmaz, yapılandırılmış warning/unsupported sonucu olur.
- **Doğrulama:** Saf writer/parser için determinism, round-trip ve saldırgan paket unit testleri yazıldı. Android SDK kurulumu sürdüğünden testler/derleme henüz çalıştırılmadı; platform motorları instrumentation testi bekliyor.

## 2026-07-13 — Android XML güvenlik sabiti derleme düzeltmesi

- **Bug:** SDK 37, masaüstü JDK'da bulunan `XMLConstants.ACCESS_EXTERNAL_DTD/SCHEMA` alanlarını expose etmediği için PPTX parser derlenmedi.
- **Düzeltme:** Aynı resmi JAXP property URI'leri yerel string sabitlere taşındı; XXE/harici kaynak savunmaları azaltılmadı.
- **Doğrulama notu:** Yeni unresolved reference kaldırıldı; yerel tekrar derlemesi eşzamanlı Gradle dağıtım JAR kilidi yüzünden kaynak derlemesinden önce durdu ve root build sonucu bekleniyor.

## 2026-07-13 — Room kaynaklı unique WorkManager orkestrasyonu

- **Karar:** WorkRequest input yalnız `jobId` taşır; URI, item, seçenek ve çıktı durumu Room'un tek doğruluk kaynağıdır. Job başına unique work normalde KEEP, açık retry'da REPLACE kullanır.
- **Neden:** WorkManager Data boyut/snapshot eskimesi riskini kaldırmak, süreç ölümünden sonra aynı kalıcı iş modelinden devam etmek ve çift enqueue'yu önlemek.
- **Bug önleme:** Seçim totalUnits null iken fail edilmez; motor `inspect` ile gerçek sayfa/slayt sayısını bulup Room'a yazar, selection bundan sonra çözülür. Worker'ın yarattığı yarım SAF hedefi hata/iptalde temizlenir.
- **Doğrulama:** Foreground data-sync manifest merge `BUILD SUCCESSFUL`; Kotlin compile worker katmanını geçti ve o anda tamamlanmamış UI sembollerinde durdu.

## 2026-07-13 — M3E uygulama akışı ve SAF kullanıcı arayüzü

- **Değişiklik:** AppCompat/Compose ana Activity, dört hedefli Expressive navigation, UDF/StateFlow ViewModel, Home/setup/Queue/History/Settings ekranları ve tekli-çoklu SAF seçiciler uygulandı. Job önce Room'a yazılır, sonra scheduler'a yalnız job kimliği verilir; queue/history aynı Room flow'undan çizilir.
- **Neden:** Dosya seçimi, kalıcı URI izinleri, seçenekler ve süreç ölümüne dayanıklı worker durumu tek sözleşmede buluşmalı; UI worker iç ayrıntılarına bağımlı olmamalıdır.
- **Bug önleme:** Output tree seçilmeden Convert etkinleşmez; queued work iptali worker hiç başlamasa da Room job/item durumlarını `CANCELLED` yapar. Aç/paylaş intent'leri read grant ve `ClipData` taşır.
- **Yerelleştirme:** Yeni selection/quality/content-fit metinleri İngilizce, Türkçe, Almanca, Basitleştirilmiş Çince, Arapça, Portekizce, Fransızca ve Rusçada eş anahtarlarla eklendi. Sonraki AI kullanıcıya görünen metni yalnız resource'tan eklemeli ve bütün locale dosyalarını/HANDOFF'u güncellemelidir.

## 2026-07-13 — PPTX→PDF temel shape ve text render kapsamı

- **Değişiklik:** Güvenli OOXML parser, image-only slaytların yanında `p:pic` ile temel `p:sp` shape/text öğelerini presentation ve `spTree` sırasıyla modelleyecek şekilde genişletildi. Android PDF motoru bu modeli z-order, transform/flip, clip, fill/stroke ve sarmalanan styled text ile Canvas'a çizer.
- **Neden:** Dışarıda üretilen sıradan PowerPoint dosyaları yalnız tek raster görselden oluşmaz. Tüm karmaşık öğeler bitene kadar slaydı reddetmek kullanılabilir çıktıyı gereksiz yere engelliyordu.
- **Sadakat kararı:** Desteklenen kardeş öğeler varsa chart/table/SmartArt, group, gradient/effect ve tema/master/placeholder mirası warning ile kısmi sadakatte render edilir; warning artık başarılı sonuca da taşınır. Hiç render edilebilir içerik kalmayan seçili slayt structured `Unsupported` olmaya devam eder, böylece sessiz boş/yanlış sayfa üretilmez.
- **Güvenlik:** Mevcut ZIP entry/path/boyut/compression-ratio limitleri ve XXE/DOCTYPE/external entity engelleri azaltılmadı. Yeni parser yalnız aynı limitli XML/relationship okuma hattını kullanır.
- **Uyumluluk ve test:** App-generated single-image sözleşmesi korundu. Dış-PPTX benzeri saf JVM fixture presentation/spTree order, picture, rect/roundRect/ellipse/line, renk/line, rotation/flip, text run/paragraf ve warning modelini doğrular; complex-only slide testi structured unsupported kararının parser girdisini sabitler. Root turunda compile, 60/60 test ve lint başarılıdır.

## 2026-07-13 — Nihai yerel doğrulama ve inceleme düzeltmeleri

- **Doğrulama:** SDK 37/JDK 17 ile tam compile, 60/60 unit test, `lintDebug` ve `assembleDebug` başarılı. APK 23,282,084 bayt; SHA-256 `87AFB1EE06442A8C19197CC02A02B6819E04A1930C0028AC882FA3E00EB17EEE`; applicationId/min/target `com.converty.app`/23/37; v1/v2 debug imzası doğrulandı.
- **İnceleme düzeltmeleri:** DataStore locale cold-start race/no-op guard, Android 13+ bildirim izni isteme, yerelleştirilmiş aralık noktalaması, persistable URI izin bırakma, gerçek collision policy, geçmişte hata/uyarı görünürlüğü, başarılı işte sahte Retry kaldırma ve toplu geçmiş silme onayı uygulandı.
- **Kalan sınır:** Bağlı cihaz/emülatör yoktu. Android `PdfRenderer`/`PdfDocument`, gerçek DocumentsProvider, foreground iptal, RTL/büyük font ve büyük/bozuk gerçek belge smoke testleri yapılmadı. Chart/table/SmartArt/group/tema-master tam sadakati warning/unsupported kapsamındadır.

## 2026-07-13 — UI görünmeden başlangıç çökmesi düzeltmesi

- **Belirti:** Kullanıcı debug APK'da simgeye dokunduktan 1–2 saniye sonra uygulamanın arayüz göstermeden kapandığını bildirdi.
- **Kök neden:** `MainActivity`, `AppCompatActivity` olmasına rağmen `Theme.Converty` gündüz `android:style/Theme.Material.Light.NoActionBar`, gece `android:style/Theme.Material.NoActionBar` platform parent'larını kullanıyordu. AppCompat ilk content/sub-decor kurulumunda bu temayı reddeder; tipik sonuç `IllegalStateException: You need to use a Theme.AppCompat theme (or descendant)` ve Compose root oluşmadan process kapanmasıdır. Cihaz ADB'ye bağlı olmadığı için gerçek stacktrace alınamadı; kaynak ve paketlenmiş eski resource bu deterministik runtime sözleşme ihlalini doğruladı.
- **Düzeltme:** İki `Theme.Converty` varyantının parent'ı `Theme.AppCompat.DayNight.NoActionBar` yapıldı. M3 Expressive ürün teması Compose içindeki `MaterialExpressiveTheme` olarak korunur; bu değişiklik yalnız Activity'nin güvenli pencere/bootstrap temasını düzeltir.
- **Regresyon koruması:** `AppCompatThemeContractTest` iki XML kaynağını parse edip parent'ı doğrular. `MainActivityStartupTest` bağlı cihaz/emülatörde Activity'nin açılıp Compose semantics root'u üretmesini sınar.
- **Doğrulama:** 61/61 JVM testi, `lintDebug` ve `assembleDebug` başarılı. `aapt2 dump resources`, APK içindeki gündüz/gece `Theme.Converty` parent'ının `Theme.AppCompat.DayNight.NoActionBar` olduğunu doğruladı. Güncel APK 22,573,677 bayt; SHA-256 `C1E1A3AA56A563F929CDF0AE0168DBDCF3C6E22DAB484B2904E521A0697C4932`; v1/v2 imza geçerli.
- **Ortam sınırı:** `adb devices` boştu; instrumentation testi cihazda çalıştırılamadı. Testin Kotlin kaynağı derlendi, ancak test APK'sının son Java aşaması OneDrive altındaki oluşturulmuş `R.jar` için Windows `AccessDeniedException` verdi. Bu ortam kilidi uygulama APK'sının unit/lint/build başarısını etkilemedi. Tema düzeltmesinden sonra yalnız eski geliştirme kurulumu üzerinde farklı bir crash sürerse sıradaki kontrol Room v1 identity/migration stacktrace'idir.

## 2026-07-13 — M3E ürün cilası, sabit eylem ve kesintisiz dil değişimi

- **Belirti:** Ayarlar ve setup içindeki çok seçenekli chip'ler üst üste yığılmış görünüyordu; iki dev ana sayfa kartı yeni formatlarla ölçeklenemezdi; Convert düğmesine ulaşmak için her seferinde aşağı kaydırmak gerekiyordu. Dil seçimi AppCompat locale çağrısı yüzünden Activity'yi yeniden oluşturarak ekranı kısa süre kaybediyordu.
- **Kök neden:** Her seçenek aynı anda FlowRow içinde render ediliyor, ana sayfa yön başına büyük sabit kart çiziyor, sheet eylemi kaydırılan içerikle aynı sütunda yaşıyor ve locale Activity seviyesinde uygulanıyordu.
- **Düzeltme:** Tek seçili değeri gösteren M3E `ChoiceField` menüleri ve ikonlu bölüm kartları; iki sütunlu kompakt/geleceğe açık dönüşüm kataloğu; kaydırılan gövdeden ayrılmış sabit alt Convert yüzeyi; Compose localized context ile recreation'sız locale uygulanması eklendi.
- **Tema/varsayılan:** Ocean, Violet, Forest ve Sunset açık-koyu paletleri eklendi. Varsayılan kalite `MAXIMUM` yapıldı ve mevcut kurulumlar için tek seferlik Preferences migration yazıldı.
- **Yerelleştirme:** 15 yeni anahtar sekiz dilin tümüne eklendi; her locale 125 string + 2 plural ile eşleşir.
- **Doğrulama:** 64/64 JVM testi, `lintDebug` ve `assembleDebug` başarılı. Güncel APK 23,371,172 bayt; SHA-256 `1D93DFC7F2E5A72AB49D16A2E6A2BB09D4C3ABAD903D6F9631B480F5B5321849`; v1/v2 debug imzası geçerli. Bağlı cihaz olmadığından görsel/device smoke testi hâlâ kullanıcı/cihaz doğrulaması bekler.

## 2026-07-13 — UI cilası sonrası ikinci açılış regresyonu düzeltmesi

- **Belirti:** Kullanıcı, UI cilası APK'sının ilk sürümdeki gibi hiç arayüz göstermeden kapandığını bildirdi.
- **Teşhis sınırı:** `adb devices` yine boştu; cihaz stacktrace'i alınamadığı için tek bir exception satırı kök neden olarak kanıtlanamadı. Codebase Memory de `Transport closed` döndürdü ve doğrudan kaynak fallback'i kullanıldı.
- **Regresyon yüzeyi:** Son sürüm açılışına özel üç yeni davranış vardı: tüm ağacı Activity olmayan localized `Context` ile sarmalama, settings Flow başlamadan DataStore migration yazısı ve LazyColumn içindeki ağırlıklı Home FlowRow ölçümü. Özellikle özel locale context, Android'in önerdiği public per-app locale akışı yerine custom resource mantığı kullanıyordu.
- **Düzeltme:** `ProvideAppLocale/createConfigurationContext` kaldırıldı. `ApplyAppLocale`, `settingsReady` sonrasında ve yalnız etiket değiştiğinde AppCompat public API'sini çağırır; `MainActivity` `locale|layoutDirection` değişikliğini recreation olmadan karşılar. Kalite migration'ı açılış yazısı yerine salt-okunur çözümlemeye, Home kataloğu veri tabanlı ikili `Row` ölçümüne geçirildi.
- **Regresyon koruması:** Locale contract testi public API + manifest configChanges + settings-ready guard'ını, Preferences testi legacy kaliteyi açılış yazısı olmadan maksimum çözmeyi doğrular. Paketlenmiş manifestte configChanges bitmask'i `0x00002004` olarak doğrulandı.
- **Doğrulama:** Kotlin compile, 64/64 JVM testi, `lintDebug` ve `assembleDebug` başarılı. APK 23,371,172 bayt; SHA-256 `F93B8BF7EB7643E43638ECE2EADFBB300632A4C1180ABF71EE3C605DB35D0BAA`; debug imza doğrulaması exit 0. Gerçek cihaz smoke testi hâlâ kullanıcı doğrulaması gerektirir.

## 2026-07-13 — Tekrarlayan UI-öncesi kapanma için başlangıç izolasyonu

- **Belirti:** Kullanıcı, önceki tema ve locale düzeltmelerinden sonra da uygulamanın simgeye dokununca hiçbir UI göstermeden kapandığını bildirdi.
- **Teşhis sınırı:** `adb devices` boştu; Codebase Memory `Transport closed` döndürdü. Yeni stacktrace olmadan tek kök neden uydurulmadı; ilk kareden önce çalışan WorkManager, Room, DataStore, AppCompat locale ve notification channel yüzeyleri denetlendi.
- **Kök risk:** `ConversionScheduler` constructor'ı ViewModel kurulurken `WorkManager.getInstance` çağırıyordu. Room/DataStore Flow exception'ları main coroutine'e, locale/notification exception'ları başlangıç akışına kaçabilirdi.
- **Düzeltme:** WorkManager senkronize lazy başlatmaya geçirildi; ayar ve geçmiş Flow'ları güvenli fallback + `ConvertyStartup` log sınırı aldı; locale ve bildirim kanalı işleri `runCatching` içine alındı. Yerel geçmiş/ayar silinmedi.
- **Aynı turdaki build düzeltmeleri:** `OfficeToPdfEngine` için `PdfDocument` explicit `try/finally close` ve ODF Sequence→List dönüşümleri; output adında mevcut büyük harfli hedef uzantısını koruma; `MAXIMUM` kalite test beklentisi ve sekiz locale'in yeni 27 anahtarı tamamlandı.
- **Doğrulama:** 14 suite / 65 test / 0 failure / 0 error, `lintDebug`, `assembleDebug`, APK v1/v2 imza ve manifest `configChanges=0x00002004` başarılı. APK 23.345.524 bayt, SHA-256 `A97519F2AECDAA1B16B034FB66B9B5479C43C2F3D1CEE5CDBD309DF3444DC189`. Cihaz smoke testi olmadan gerçek cihaz kapanmasının giderildiği kesin iddia edilmez.

## 2026-07-13 — Gerçek cihaz açılışı, paket/imza teşhisi ve Android XML parser bug'ı

- **Paket kimliği:** Kullanıcının `app/build/outputs/apk/debug/app-debug.apk` kurulumundan sonra ADB, cihazdaki paketi `com.converty.app` ve launcher'ı `com.converty.app/.MainActivity` olarak doğruladı. Kısa süre değerlendirilen ayrı test applicationId ve `com.crownguard.converty` yaklaşımı kullanıcı isteği üzerine durduruldu; kaynak kalıcı olarak `com.converty.app` kullanır ve ayrı test uygulaması kurulmadı.
- **İmza engeli:** OneDrive kilidini aşmak için `C:\tmp` temiz kopyasında yükseltilmiş Gradle çalıştırıldı. Bu süreç kullanıcı keystore'unu seçip cihazdaki workspace APK sertifikasından farklı imza üretti; Android `INSTALL_FAILED_UPDATE_INCOMPATIBLE` verdi ve 0 test çalıştı. Workspace APK sertifikası `29787975…EC313`, yanlış temiz-build sertifikası `A981F888…FE569` olarak ölçüldü. Workspace sandbox keystore'u geçici `app/build/tmp` kopyası üzerinden Gradle signing override'a verilince imzalar eşleşti; paket adı değiştirilmeden kurulum yapıldı.
- **Cihazda yakalanan bug:** İlk gerçek testlerde Office/ODF ve PPTX parse işlemleri Android/Harmony JAXP'nin `isXIncludeAware` ve `FEATURE_SECURE_PROCESSING` desteği olmaması nedeniyle başarısız oldu. JVM testlerinin geçmesi bu platform farkını gizlemişti.
- **Düzeltme:** Ortak `SecurePackageXml.kt` eklendi. Desteklenen JAXP özellikleri best-effort uygulanır; bağımsız DOCTYPE/ENTITY byte taraması ve harici entity'yi reddeden resolver zorunludur. `PptxPackageSecurityTest` güvenlik reddini doğru `SECURITY_LIMIT` sınıfında bekler. XXE savunması gevşetilmedi.
- **Sonuç:** İlk kapsam Samsung SM-S721B / Android 15 üzerinde 6/6 geçti; ardından AOSP CTS HEIC ve AVIF fixture'ları yalnız androidTest assets'e eklenerek codec matrisi genişletildi ve son tur **7/7 instrumentation testi** geçti. Test altyapısı hedef APK'yı tur sonunda kaldırdığı için tam unit/lint/assemble'dan geçen workspace APK yeniden kuruldu; ADB soğuk açılış `Status: ok`, 1.669 ms, çalışan PID ve boş crash log verdi.
- **Nihai yerel doğrulama:** 79/79 JVM testi, `lintDebug`, `assembleDebug` başarılı. APK 23.345.524 bayt; SHA-256 `D90ED9AD01C0A74B9D8E64B1F21DCBF7AEF22D542EEBA4A5B9767D89952C2E90`.

## 2026-07-13 — Katmanlı M3E launcher ikonu ve ilk release APK

- **İkon kararı:** `ICON` içindeki tasarım doğrudan kullanıldı; altı katmanlı master SVG, ayrı Android background/foreground ve monochrome vektörleri nedeniyle katmanlıdır. Tonal renk, organik container ve morph seam yaklaşımı M3E ürün diliyle uyumludur; görsel yeniden üretilmedi.
- **Entegrasyon:** Manifest drawable foreground yerine mipmap launcher/round kaynaklarına geçirildi. Android 6–7 legacy layer-list, Android 8+ adaptive icon ve Android 13+ monochrome themed icon varyantları eklendi. `LauncherIconContractTest` regresyon korumasıdır.
- **Derleme sorunu:** İlk zaman aşımı Gradle alt sürecini arka planda bıraktı; takip eden paralel build `R.jar` kilitledi. Süreçler bittikten sonra sandbox içindeki Java/Javac sınırı hem OneDrive hem ayrı yazılabilir build dizininde generated `R.jar` için `AccessDeniedException` üretti. Kaynak silinmedi; Gradle `clean` yalnız üretilmiş build artefaktlarına uygulandı. Windows sandbox dışındaki tek, önbelleksiz Gradle koşusu kaynakları/testleri tamamladı ve release unsigned APK'yı üretti.
- **Release imzası:** `output/release.apk`, release varyantının yerel kurulabilir teslimidir; Android debug anahtarıyla v1/v2/v3 imzalıdır, production/Play Store anahtarı değildir. Sertifika SHA-256 `A981F8885FFA56B27C76DA93E83BE551BEB59DB07482E86A12764DA29A6FE569`dur. Önceki cihaz kurulumunun kaydedilmiş `29787975…EC313` sertifikası farklı olduğundan mevcut uygulamanın üstüne kurulamaz; bir kez kaldırma gerekir. Kalıcı mağaza sürümü için kullanıcıya ait korunmuş release keystore kurulmalıdır.
- **Doğrulama:** 80/80 JVM testi, paket `com.converty.app`, minSdk 23, targetSdk 37. `release.apk` 15.683.394 bayt; SHA-256 `3C8381DEEF9D54F829CB5B72D6282909AFF18DC7724522BDDD894B0E2BF713C0`.

## 2026-07-13 — Çok dilli README ve yayın hazırlığı denetimi

- **Değişiklik:** İngilizce canonical `README.md` ile uygulamanın diğer yedi dili için tam README çevirileri eklendi. Her dosya diğer yedi dile bağlanır; teknik kapsam, sadakat sınırları, gizlilik, build adımları ve yayın checklist'i korunur.
- **Doğrulama:** Sekiz dosya ve tüm karşılıklı göreli bağlantılar bulundu; her README'de yedi benzersiz dil bağlantısı ve sıfır emoji doğrulandı.
- **Yayın kararı:** GitHub'da kaynak görünür paylaşılabilir, fakat kökte lisans yokken proje açık kaynak sayılamaz. F-Droid başvurusu lisans, asset/dependency lisans denetimi, kalıcı production signing identity, public release tag, metadata ve temiz source build tamamlanana kadar hazır değildir.
- **Belgeleme:** Yeni `HANDOFF/07-PUBLISHING.md`, README canonical içerik kuralını ve GitHub/F-Droid/signing kapılarını kalıcılaştırır.
- **Repo hijyeni:** Kökte `.gitignore` olmadığı için yerel SDK yolu, cache/build klasörleri, loglar ve debug-imzalı APK'nın yanlışlıkla public source'a eklenme riski vardı. Bu yerel/generated içerikleri dışlayan; kaynak, `ICON` ve `HANDOFF` belgelerini koruyan `.gitignore` eklendi. Hiçbir Git komutu kullanılmadı.

## 2026-07-13 — Apache-2.0, production signing ve F-Droid denetimi

- **Yetki değişikliği:** Kullanıcı önceki Git yasağını yayın hazırlığı için açıkça kaldırdı; destructive Git komutları yasak kalır. Yerel `.git` klasörü boş ve remote URL bilinmiyor. GitHub'daki başlangıç geçmişiyle çakışmamak için `git init`/commit/push yapılmadı.
- **Lisans:** Köke canonical Apache License 2.0 metni `LICENSE` olarak eklendi. Sekiz README lisans ve signing gerçeğiyle eşlenir.
- **Signing:** `app/build.gradle.kts`, gitignored `key.properties` varsa production `signingConfig` kurar; yoksa unsigned release üretimine izin verir. Yerelde RSA-4096 `converty-release.keystore` üretildi. Parola yalnız gitignored `key.properties` içindedir; hiçbir çıktı, README, HANDOFF veya loga yazılmadı.
- **Release:** Temizleme/test tekrarı olmadan yalnız `assembleRelease` çalıştırıldı ve 69 saniyede geçti. `output/Converty-Android-v0.1.0-release.apk` ile `output/release.apk` aynı production-signed içeriktir: 15.691.403 bayt, APK SHA-256 `FE4CC780F48A83D1ACADF674BE5CC4DF9EDAE12212609CCBE8420CC8C09CF47A`; sertifika SHA-256 `A3C87F2AD05364D20AC49864D1BA33DCF1D5F6D359A3455C62D4EFF74B388F7A`, v1/v2 geçerli.
- **F-Droid sonucu:** Apache-2.0 ve runtime dependency tabanı uygundur; Firebase/GMS/reklam/analytics/telemetry/INTERNET izni yoktur. `com.converty.app` Google Play'deki ilgisiz bir uygulamayla çakıştığı için kritik blocker'dır. Public remote/tag, fastlane/fdroiddata metadata, fixture provenance veya `scandelete` ve temiz Linux/F-Droid scanner/build kanıtı da tamamlanmalıdır.

## 2026-07-13 — Yerel Git başlangıç commit'i

- **Kullanıcı talebi:** Önceki remote-belirsizliği nedeniyle ertelenen Git işlemi, kullanıcının açık “commit et” talebiyle yetkilendirildi.
- **İşlem:** Yerel depo `main` dalında başlatıldı; `.gitignore` sonrasında signing secret'larının ignored olduğu doğrulanarak mevcut kaynak, README, Apache-2.0 lisans ve HANDOFF seti tek başlangıç commit'ine alındı.
- **Sınır:** GitHub remote URL'si hâlâ yapılandırılmamıştır. Push ve `v0.1.0` etiketi, gerçek repo URL'si doğrulandıktan sonra ayrı ve non-destructive bir adım olmalıdır.
