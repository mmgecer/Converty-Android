# Material 3 Expressive, UX ve Yerelleştirme HANDOFF

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur; ekran, tema, erişilebilirlik veya metin kaynağı değiştiğinde bu belge güncellenir.

## Deneyim hedefi

Ana akış tek bakışta anlaşılmalıdır: dönüşüm yönünü seç → dosyaları ekle → öğe/aralık ve çıktı seçeneklerini ayarla → dönüştür → aç/paylaş. Toplu işlemler dosya başına durum gösterecek; başarısız bir dosya kalan kuyruğu zorunlu olarak durdurmayacaktır.

## Planlanan ekranlar

- `Home`: iki belirgin PDF↔PPTX eylemi, son işler ve hızlı yeniden çalıştırma.
- `Job setup`: seçilen dosya kartları, çoklu ekleme/çıkarma/sıralama, sayfa veya slayt seçimi, çıktı/kalite seçenekleri.
- `Queue/progress`: toplam ve dosya bazlı ilerleme, iptal, hata ayrıntısı ve yeniden deneme.
- `History`: filtre/arama, başarı durumu, çıktı kayıp işareti, yeniden çalıştır, aç/paylaş/sil.
- `Settings`: sistem/açık/koyu tema, dinamik renk, uygulama dili, varsayılan çıktı/kalite ve gizlilik/açık kaynak bilgileri.

## M3 Expressive ilkeleri

- Compose Material 3 ve doğrulanmış Expressive API'leri kullanılacak; deneysel API gerektiğinde `@OptIn` ve stabil fallback belgelenecek.
- Dinamik renk (Android 12+), belirgin fakat tutarlı şekiller, durum odaklı motion, erişilebilir renk kontrastı ve en az 48dp dokunma hedefi.
- Wavy/expressive ilerleme yalnız gerçek işlem durumunu temsil etmeli; hareket azaltma/animasyon ölçeğine saygı duyulmalı.
- Telefon için bottom navigation veya uygun tek sütun; geniş ekranda navigation rail/list-detail gibi adaptive düzen.
- Edge-to-edge ve sistem inset'leri; TalkBack açıklamaları, klavye/focus sırası ve RTL yerleşim zorunludur.

## Doğrulanmış Expressive API seti — 2026-07-13

- Sürüm: `androidx.compose.material3:material3:1.5.0-alpha23`; ana tema `MaterialExpressiveTheme`, hareket `MotionScheme.expressive()`.
- Telefon navigasyonu: `ShortNavigationBar` / `ShortNavigationBarItem`; geniş ekran: `WideNavigationRail` ve gerekirse adaptive navigation suite.
- App bar/toolbar: `MediumFlexibleTopAppBar`, `LargeFlexibleTopAppBar`, `FlexibleBottomAppBar`, `HorizontalFloatingToolbar`.
- Düğmeler: standart `Button` ailesinin `ButtonShapes` overloadları, `ButtonDefaults.shapesFor(...)`, `ButtonGroup`, `SplitButton` ve FAB menüsü. `ExpressiveButton` adında bir API kullanılmamalıdır.
- İlerleme: `LinearWavyProgressIndicator` ve `CircularWavyProgressIndicator`. Deneysel `LoadingIndicator` merkezi wrapper dışına sızdırılmaz.

Stable fallback gerekirse `material3:1.4.0` + `MaterialTheme` + klasik navigation/progress bileşenleridir; fakat bu yalnız build kurtarma seçeneğidir ve ürünün tam M3E kabul ölçütünü karşılamaz.

## Dil altyapısı

Android `res/values/strings.xml` kaynak anahtarları temel alınır; her dil ayrı `values-xx` klasöründedir. UI metni Kotlin içine gömülmez. Sayı, tarih, yüzde, çoğul ve dosya boyutu locale uyumlu biçimlendirilir.

İlk dil seti: `en`, `tr`, `de`, `zh-Hans`, `ar`, `pt`, `fr`, `ru`. Basitleştirilmiş Çince kaynak klasörü modern BCP-47 biçimiyle `values-b+zh+Hans` kullanır. Portekizce ilk aşamada genel `pt` olacaktır; gerekirse daha sonra `pt-BR`/`pt-PT` ayrımı eklenebilir. Android 13+ uygulama başına dil ve eski sürümler için AppCompat locale uyumu etkinleştirilmiştir. `locale_config.xml` desteklenen dilleri açıkça listeler.

Yeni dil ekleme kontrolü: yeni `values-<locale>/strings.xml`, `locale_config.xml` girişi, eksik anahtar testi, taşma ekran görüntüleri ve RTL ise yön testi.

## Uygulanan design-system ve kaynak haritası — 2026-07-13

- `ui/theme/Theme.kt`: `ThemeMode` sistem/açık/koyu seçimini, Android 12+ dinamik rengi ve `MaterialExpressiveTheme(MotionScheme.expressive())` sarmalamasını merkezileştirir.
- `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`: markalı açık/koyu renk şemaları, okunabilir sistem fontu tipografisi ve yeni `largeIncreased`/`extraExtraLarge` dahil Expressive şekil ölçeğini tanımlar.
- `ui/theme/ExpressiveComponents.kt`: ekranların alpha API'lere doğrudan bağlanmaması için gerçek `ShortNavigationBar`/`ShortNavigationBarItem` ile belirli-belirsiz `LinearWavyProgressIndicator` ve `CircularWavyProgressIndicator` wrapper'larını sunar. İlerleme 0..1 aralığına sıkıştırılır; `motionEnabled=false` dalga genliğini sıfırlar.
- `ui/theme/SupportedLocales.kt`: dil etiketi ile çevrilmeyen endonim kaynaklarını eşler. `AppLocaleController.kt`, kalıcı `LanguagePreference` değerini `AppCompatDelegate` ile uygular; çağrı AppCompat tabanlı Activity yaşam döngüsünden yapılmalıdır. Yeni dil ekleyen AI bu listeyi, `locale_config.xml` dosyasını ve ilgili `values-*` kaynağını aynı değişiklikte güncellemelidir.
- `res/values/strings.xml` İngilizce fallback'tir; `values-tr`, `values-de`, `values-b+zh+Hans`, `values-ar`, `values-pt`, `values-fr`, `values-ru` aynı 110 string anahtarını ve iki çoğul kaynağını içerir. Metinler ana ekran, iş kurulumu, ayrık aralık seçimi, kuyruk, geçmiş, ayarlar, hata ve erişilebilirlik durumlarını kapsar.
- `AndroidManifest.xml`: `supportsRtl`, `localeConfig` ve AppCompat otomatik locale saklama servisinin metadata'sını taşır. `res/xml` altında locale, cleartext'i kapatan ağ güvenliği ve kullanıcı dosyası/geçmişini yedek dışı bırakan veri çıkarma kuralları bulunur.
- XML başlangıç teması yalnız Compose açılana kadar güvenli açık/koyu pencere renklerini sağlar; ürün teması Compose içindeki `ConvertyTheme`'dir. Activity `AppCompatActivity` olduğundan XML parent'ı gündüz ve gecede `Theme.AppCompat.DayNight.NoActionBar` olmalıdır. Önceki platform Material parent'ı AppCompat'in sub-decor kurulumunu reddedip UI görünmeden çökme oluşturduğu için bu sözleşme unit ve instrumentation testleriyle korunur.

## Uygulanan ekran ve UI dosya haritası — 2026-07-13

- `MainActivity.kt`: `AppCompatActivity`, uygulama container'ından ViewModel factory kurulumu, edge-to-edge Compose başlangıcı ve `AppLocaleController`/`AppCompatDelegate` için yaşam döngüsüne bağlı dil akışı.
- Dil collector'ı geçici ViewModel başlangıç state'ini değil doğrudan DataStore akışını izler; `AppLocaleController` yalnız BCP-47 etiketleri gerçekten değiştiğinde AppCompat'e yazar. Bu, kayıtlı dil açılışında sistem→kayıtlı dil çift recreation döngüsünü önler.
- `feature/app/ConvertyApp.kt`: `ConvertyTheme` içindeki dört ana hedefi (`Home`, `Queue`, `History`, `Settings`) gerçek `ExpressiveNavigationBar` ile birleştirir. `OpenDocument`, `OpenMultipleDocuments` ve `OpenDocumentTree` launcher'sı burada yaşar; çıktı aç/paylaş intent'leri geçici URI okuma izni taşır.
- Android 13+ için ilk conversion öncesi `POST_NOTIFICATIONS` Activity Result izni istenir; kullanıcı reddederse açık isteği engellenmez ve in-app Queue iptal/progress kontrolü kullanılabilir.
- `feature/app/ConvertyViewModel.kt`: UI'nin tek yönlü `ConvertyAction` → `StateFlow<ConvertyUiState>` sözleşmesi. Room job akışı ile DataStore ayarlarını birleştirir; SAF metadata/kalıcı izin, job snapshot oluşturma, WorkManager enqueue/retry/cancel ve geçmiş sil/temizle eylemlerini yürütür. Output tree zorunludur; böylece worker'ın hedef klasör eksikliğiyle sonradan fail etmesi önlenir.
- `feature/app/HomeQueueHistoryScreens.kt`: belirgin PDF→PPTX/PPTX→PDF kartları, son işler, kuyrukta job/dosya bazlı wavy ilerleme ve iptal; geçmişte arama/filtre ile aç/paylaş/yeniden dene/sil/temizle eylemleri.
- Geçmiş öğeleri yapılandırılmış hata kodunu yerelleştirilmiş dosya/çıktı/seçim/format mesajına eşler ve warning durumunu gösterir. Retry yalnız başarısız/iptal job'da görünür; başarılı job'ı QUEUED yapıp worker'ın tüm item'ları atladığı yanıltıcı no-op kaldırılmıştır.
- Tüm geçmişi temizleme, sekiz dilde açık onay diyaloğu gösterir; tek dokunuşla geri alınamaz toplu silme engellenir.
- `feature/app/SetupSettingsScreens.kt`: tekli/çoklu dosya ekleme ve kaldırma; `ALL/FIRST/LAST/KEEP/REMOVE`, count veya `1,3-5,9` ifadesi; dört kalite, contain/cover/stretch, SAF çıktı ağacı ve convert. Ayarlarda sistem/açık/koyu, dinamik renk, sekiz uygulama dili, varsayılan kalite ve çıktı klasörü bulunur.
- Yeni UI metinleri Kotlin'e gömülmedi. `exclude_selection`, kalite ve content-fit anahtarları sekiz locale'in tamamına eklendi; XML parse ve 109/109 anahtar eşliği kontrol edildi.

Sonraki AI ekran, eylem, navigasyon veya kullanıcıya görünen metin eklediğinde bu haritayı ve sekiz locale kaynağını birlikte güncellemeli; yeni UI işi ViewModel UDF sınırını ve theme Expressive wrapper'larını atlamamalıdır.

## M3E ürün cilası ve ölçeklenebilir katalog — 2026-07-13

- `HomeQueueHistoryScreens.kt` ana sayfası gradient hero + iki sütunlu kompakt dönüşüm kataloğuna dönüştürüldü. PDF→PPTX ve PPTX→PDF aktiftir; görsel ve belge formatı karoları açıkça `Yakında`/disabled gösterilir. Yeni formatlar dev kartlar eklemek yerine aynı katalog modeline küçük karo olarak eklenmelidir.
- `SetupSettingsScreens.kt` içindeki kalabalık chip/FlowRow seçenekleri, seçili değeri tek satırda gösteren ve radio işaretli menü açan `ChoiceField` bileşenine taşındı. Seçim, kalite, yerleşim, tema, palet ve dil ayarları ikonlu bölüm kartlarıyla gruplanır.
- Dönüşüm sheet'i yüksekliğin %92'sini kullanır; orta içerik `LazyColumn` ile kayar, alttaki yüksek `Convert` yüzeyi sabittir. Kullanıcı dosya ve seçenekleri gezerken eylem ekranın dışına itilmez.
- `Color.kt`/`Theme.kt` dört açık-koyu marka paleti sunar: Ocean, Violet, Forest, Sunset. Android 12+ dinamik renk ayrı bir seçenektir; özel palet seçmek dinamik rengi kapatır.
- Dil seçimi Activity recreation yapmaz: `ApplyAppLocale`, AppCompat public locale API'sini kullanır; Activity manifestte `locale|layoutDirection` değişikliklerini kendisi karşılar ve Compose aynı ekranda yeniden çizilir. `settingsReady` beklemesi kayıtlı locale'i geçici başlangıç state'iyle silmeyi önler. Özel localized `Context`/`CompositionLocalProvider` yaklaşımı açılış regresyonu sonrasında kaldırılmıştır.
- İngilizce, Türkçe, Almanca, Basitleştirilmiş Çince, Arapça, Portekizce, Fransızca ve Rusça kaynakların her birinde 125 string ve 2 plural vardır. Yeni katalog/palet/ayar metinleri sekiz locale'e eş anahtarlarla eklenmiştir; yeni dil eklerken `SupportedLocales`, `locale_config.xml`, yeni `values-*` ve parity testi birlikte güncellenmelidir.
- Home kataloğu `ConverterTileSpec` listesini ikili satırlara böler. İlk frame'de LazyColumn içindeki ağırlıklı FlowRow ölçümü kullanılmaz; yeni format eklemek listeye yeni spec eklemektir.

## Build ve doğrulama notu

- Build pinleri: AGP `9.2.1`, Gradle `9.4.1`, AGP built-in Kotlin ve Compose plugin `2.3.10`, Compose BOM `2026.06.00`, Material 3 `1.5.0-alpha23`, minSdk 23, compile/target SDK 37. SDK Manager paket adı `platforms;android-37.0`dır; Gradle DSL `compileSdk = 37` kullanır.
- Room codegen built-in Kotlin ile uyumlu KSP `2.3.9` üzerinden kuruldu; kapt/kotlin-android kullanılmaz. Activity, lifecycle, navigation, Room, WorkManager, DataStore, DocumentFile ve Compose test bağımlılıkları version catalog içinde sabittir.
- Statik kontrolde İngilizce fallback + 7 locale dosyasının 109 ortak string anahtarı ve iki çoğul kaynağı XML olarak parse edildi; biçim placeholder'ları ayrıca korunmalıdır.
- Gradle wrapper üretildi; SDK 37 ile Kotlin compile, 61 unit test, `lintDebug` ve `assembleDebug` başarılıdır. Locale parity testi 8 dilde 110 key, placeholder ve plural sözleşmesini doğrular; yeni tema sözleşmesi testi AppCompat parent'ını iki kaynak varyantında sabitler. Lint için WorkManager initializer, API 27 navigation-bar tema işareti, launcher icon ve Fransızca/Portekizce plural eksikleri düzeltildi. Arapça RTL, büyük font, uzun Almanca/Fransızca metin taşmaları ve gerçek Activity açılış testi bağlı cihaz/emülatörde hâlâ çalıştırılmalıdır.

## İki taraflı M3E Home ve format seçenekleri — 2026-07-13

- Aktif Home `ConversionHomeScreen.kt` içindedir: `Converty` markası, kaynak/hedef M3E açılır seçicileri, geçerli ters yön varsa etkin swap ve tek devam eylemi. “Dosyaları güvenle dönüştürün”, “Yakında” kataloğu ve kalabalık açıklamalar aktif Home'da gösterilmez.
- Kaynak değişiminde geçerli hedef korunur; değilse ilk izinli hedef seçilir. Hedef yalnız izinli çift oluşturuyorsa kabul edilir. PDF→PPTX “Görünümü koru” notu sayfaların tam slayt görseli, metnin düzenlenemez olduğunu belirtir.
- Setup'ta ayrı tekli/çoklu düğmeler kaldırıldı. Tek `OpenMultipleDocuments` seçici bir veya birden çok dosya alır. Sayfa/slayt seçimi, kalite, DPI, WebP lossless ve PDF→PPTX fit kartları yöne göre koşullu gösterilir.
- Yeni anahtarlar tüm sekiz locale'e eklendi. Her locale 152 string + 2 plural taşır; parity testi anahtar/placeholder eşliğini doğrular.

## Home/geçmiş son temizliği ve cihaz açılışı — 2026-07-13

- Eski `LegacyHomeScreen`, `ConverterTileSpec`, hero/katalog/tile ve “yakında” aktif kodu `HomeQueueHistoryScreens.kt` içinden fiziksel olarak kaldırıldı. Home için tek kaynak `ConversionHomeScreen.kt`; kaynak/hedef format ikonları ve geçerli swap davranışı `ConversionHomeContractTest` ile korunur.
- Format ikonu ortaklaştırıldı; geçmiş başlığı artık çoğu işte PDF ikonu göstermek yerine gerçek hedef format ikonunu kullanır. History her `ConversionItem.outputs` çıktısını ayrı satırda, ayrı aç/paylaş eylemiyle gösterir.
- Setup yalnız `OpenMultipleDocuments` kullanır. Thumbnail JPEG kalitesi gösterir fakat DPI göstermez; DPI yalnız PDF→PPTX/PNG/JPG/TIFF yönlerindedir. WebP lossless ve yön-bazlı seçim/fit kartları koşullu kalır.
- Sekiz locale hâlâ 152 string + 2 plural ile eşleşir. `testDebugUnitTest` içindeki parity testi anahtar, placeholder ve çoğul sözleşmesini geçti.
- `MainActivityStartupTest` gerçek Samsung cihazda Compose root'u üretti. Son APK ayrıca ADB ile soğuk başlatıldı: `Status: ok`, yaklaşık 1,669 saniye, çalışan süreç ve `FATAL EXCEPTION` olmadan Home açılışı. Başlangıç korumaları (AppCompat tema, WorkManager lazy, Room/DataStore fallback, locale/channel guard) korunmalıdır.

## Katmanlı launcher ikonu — 2026-07-13

- Kullanıcının `ICON` paketi M3 Expressive marka diliyle uyumludur: tonal mor/turkuaz alanlar, yuvarlatılmış belge yüzeyi ve metinden renkli karo yapısına geçen belirgin morph çizgisi kullanır. `converter_master_layered.svg` altı düzenlenebilir tasarım katmanı; Android paketi ise ayrı background, foreground ve monochrome kaynakları taşır. Bu nedenle ikon düz bir bitmap değildir ve yeniden çizilmedi.
- `AndroidManifest.xml` artık `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round` kullanır. `res/drawable/ic_launcher_{background,foreground,monochrome}.xml` görsel katmanları; `mipmap-anydpi-v26` adaptive icon; `mipmap-anydpi-v33` Android 13+ temalı ikon sözleşmesini sağlar. `mipmap-anydpi` layer-list kaynakları minSdk 23–25 için geri dönüştür.
- Statik ikon renkleri `res/values/colors.xml` içindeki `converter_icon_*` kaynaklarıdır; launcher runtime Compose temasını okuyamadığı için bunlar kasıtlı sabittir. `LauncherIconContractTest`, manifest referanslarını, tüm legacy/adaptive/round dosyalarını ve v33 monochrome katmanını korur.
