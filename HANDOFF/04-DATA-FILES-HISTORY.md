# Veri, Dosya, Kuyruk ve Geçmiş HANDOFF

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur; SAF, veri modeli, geçmiş veya paylaşım davranışı değiştiğinde bu belge güncellenir.

## Dosya erişimi

- Tek/çoklu giriş için Activity Result API ile `OpenDocument` / `OpenMultipleDocuments`; MIME ile birlikte uzantı/magic doğrulaması.
- Kullanıcının seçtiği çıktı klasörü için `OpenDocumentTree`; kalıcı erişim gerekiyorsa persistable URI permission.
- `content://` URI bir dosya yolu değildir. Motorlar `ContentResolver` akışlarıyla veya uygulama cache'indeki kontrollü geçici kopyayla çalışır.
- Açma/paylaşma `ACTION_VIEW` / `ACTION_SEND` ve URI grant flag'leriyle yapılır; `file://` paylaşılmaz.
- Kaynak kullanıcı dosyası asla yerinde değiştirilmez. Aynı adlı çıktı için ad çakışması politikası uygulanır.
- Çakışma politikası worker'da gerçektir: `CREATE_UNIQUE` provider'ın yeni belge/benzersiz ad davranışını kullanır; `REPLACE` aynı adlı doğrudan child URI'sine yalnız motorun temp çıktısı tamamlandıktan sonra yazar; `FAIL` mevcut hedefi değiştirmeden yapılandırılmış çıktı-oluşturma hatası verir.

## Öğe seçimi / kırpma modeli

UI; hazır seçimler (tümü, ilk N, son N, tek öğe) ve gelişmiş ayrık aralık girişi sunar. İç model 0-tabanlı, sıralı, tekrarsız öğe indeksleri veya sıkıştırılmış aralıklar tutar; kullanıcıya daima 1-tabanlı gösterilir.

Örnek giriş: `1, 3-5, 9, 12-14`. Doğrulama boşlukları, ASCII tire/en dash/em dash (`-`, `–`, `—`) ve Arapça virgülü kabul eder; ters, negatif, sıfır veya belge sınırı dışındaki aralıklar yerelleştirilmiş hata verir. Aynı seçim dosya başına farklı toplam sayıya uygulanırken sınırlar yeniden doğrulanır. "Sondan N" toplam öğe sayısı bilindikten sonra çözülür.

## Kuyruk ve arka plan

Her kullanıcı başlatması bir `ConversionJob`, her kaynak bir `ConversionItem` olarak düşünülür. WorkManager uzun iş ve yeniden başlatma dayanıklılığı sağlar; uzun dönüşümde kullanıcıya görünür foreground bildirim gerekir. İlerleme en az dosya ve sayfa/slayt düzeyinde raporlanır. İptal kooperatif olmalı ve geçici çıktıyı temizlemelidir.

## Room geçmişi hedefi

- Job: id, yön, oluşturulma/başlama/bitiş zamanı, genel durum, seçenek özeti.
- Item: job id, kaynak görünen ad/URI, hedef URI/ad, toplam/seçilen öğeler, progress, durum, hata kodu/özet, çıktı boyutu.
- Kaynak URI izni kalmamışsa geçmiş kaydı durur ama hızlı aç/yeniden çalıştır devre dışı ve açıklamalı olur.
- Geçmişten silme varsayılan olarak yalnız kaydı siler; fiziksel çıktıyı silmek ayrı ve açık onaylı eylemdir.

## Gizlilik ve dayanıklılık

Belge içeriği analitik/log içine konmaz. Kullanıcı izin vermedikçe bulut yüklemesi yoktur. Geçici dosyalar uygulama cache'inde, tahmin edilemez isimle tutulur ve iş sonu/başlangıç bakımıyla temizlenir.

## 2026-07-13 — Uygulanan çekirdek ve veri katmanı

Sonraki AI bu dosya haritasını okumalı; model, seçim, Room, ayar veya SAF davranışını değiştirirse bu belgeyi ve anlamlı karar/bug düzeltmesi için `05-DECISIONS-CHANGELOG.md` dosyasını güncellemelidir.

### Çekirdek model dosyaları

- `core/model/`: `ConversionDirection`, ortak `ConversionStatus`, yapılandırılmış `ConversionError`, Android'den bağımsız URI-string kullanan `ConversionDocument`, `ConversionOutput`, `ConversionJob`, `ConversionItem`, `ConversionOptions`, kalite ve yerleşim modelleri. Hata kodları UI tarafından yerelleştirilir; diagnostic alanına belge içeriği yazılmaz.
- `core/model/selection/SelectionModels.kt`: `ALL`, `FIRST`, `LAST`, `KEEP`, `REMOVE` istekleri; motorlara verilen 0-tabanlı `SelectionRange` ve `ResolvedSelection`.
- `core/model/selection/SelectionParser.kt`: 1-tabanlı `1,3-5,9` girdisini doğrular; boşlukları kabul eder, ayrık aralıkları sıralar, duplicate/overlap/bitişik aralıkları birleştirir. Remove modunda seçimin tümleyeni hesaplanır; her şeyi kaldıran istek motorun “boş liste = tümü” sentinel'ına dönüşmemesi için `EMPTY_SELECTION` hatasıdır. Sıfır, negatif, ters, bozuk, taşan ve belge sınırı dışı değerler yerelleştirilebilir `SelectionErrorCode` ile döner. Toplam öğe sayısı dosya başına verilerek yeniden çözülmelidir.
- `core/repository/`: UI ve saklama uygulaması arasındaki `ConversionHistoryRepository` ile `SettingsRepository` sözleşmeleri.

### Room geçmişi

- `data/history/local/HistoryEntities.kt`: `conversion_jobs`, job'a cascade bağlı `conversion_items`, item'a cascade bağlı `conversion_outputs`. Fiziksel SAF çıktısı Room cascade ile silinmez; yalnızca metadata kaydı silinir.
- `ConversionHistoryDao.kt`, `HistoryRelations.kt`, `HistoryTypeConverters.kt`, `ConvertyDatabase.kt`: gözlemlenebilir job/item/output ağacı, atomik snapshot desteği, ilerleme/durum güncelleme ve singleton DB kurulumu.
- `data/history/HistoryMappers.kt` domain ↔ entity dönüşümünü; `RoomConversionHistoryRepository.kt` transaction ve Flow tabanlı repository uygulamasını taşır.
- DB sürümü `1`; şema değişikliklerinde sürüm artırılmalı ve yıkıcı fallback yerine migration eklenmelidir.

### Preferences DataStore

- `core/settings/AppSettings.kt`: sistem/açık/koyu tema, dinamik renk, dil, varsayılan kalite/fit/seçim, çıktı ağacı, URI izin tercihi ve geçmiş saklama süresi.
- Dil tercihi açık BCP-47 değeridir; yeni dil eklemek persistence enum migration gerektirmez. Dahili başlangıç listesi `en`, `tr`, `de`, `zh-Hans`, `ar`, `pt`, `fr`, `ru` içerir.
- `data/settings/SettingsDataStore.kt` Context delegate'ini, `PreferencesSettingsRepository.kt` güvenli decode/default ve atomik update/reset işlemlerini içerir.

### SAF temel sınıfları

- `data/files/SafDocumentMetadata.kt`: provider metadata'sını ve persist edilmiş okuma iznini okur, Android'siz çekirdek belge modeline çevirir.
- `SafDocumentHandles.kt`: input/output stream ve file-descriptor ömürlerini çağırana açıkça bırakır; motor gerektiğinde kontrollü cache kopyası oluşturabilir.
- `SafDocumentGateway.kt`: tree içinde çıktı oluşturma, metadata/handle üretme ve persistable URI izinlerini alma/bırakma işlemlerini merkezileştirir.
- Persistable okuma izni yalnız MIME/uzantı kabulünden sonra alınır. Setup'tan kaldırma/iptal veya history silme/temizleme sonrasında başka job/setup tarafından kullanılmayan kaynak URI izinleri bırakılır; bu, provider grant kotasının ve gereksiz uzun süreli erişimin birikmesini önler.

### Test durumu

- `SelectionParserTest.kt` 23 test ile all/first/last, keep/remove, ayrık seçim, 1→0 taban dönüşümü, merge/complement, yerelleştirilmiş tire/virgül ve sınır/biçim/overflow hatalarını kapsar.
- İzole JDK 17/SDK 37 ile `testDebugUnitTest` başarıyla çalıştı: seçim, conversion, worker adapter, model invariant, Room mapper ve 8-locale parity dahil 60/60 geçti. Tam Kotlin compile, manifest merge, lint ve debug APK paketleme de başarılıdır. Room migration/DAO ve gerçek WorkManager/SAF akışı cihaz instrumentation testi bekler.

## 2026-07-13 — WorkManager kuyruk ve worker veri akışı

- `ConversionScheduler` her job için `conversion-job-{jobId}` unique work adı kullanır. Normal enqueue KEEP ile çift tıklamayı önler; açık retry REPLACE ile yeni attempt başlatır; exponential backoff ve storage-not-low constraint vardır.
- WorkManager input data yalnız `jobId` içerir. Worker job/items/options/source/output bilgisini `ConversionHistoryRepository` üzerinden Room'dan okur; büyük URI listeleri veya seçenek snapshot'ları WorkManager Data limitine konmaz.
- Her item önce yönüne göre motorun `inspect(source)` API'siyle incelenir. Sayfa/slayt sayısı `totalUnits` alanına, inspection uyarıları `warningCodes` alanına yazılır; ardından FIRST/LAST/KEEP/REMOVE dosyanın gerçek toplamına karşı çözülür. ALL de toplam metadata'yı kaydeder fakat engine'e verimli boş-list=`all` sentinel'ı verir.
- Çıktı URI önceden item üzerinde varsa kullanılır. Yoksa job'ın persist edilmiş output tree URI'sinde SAF ile hedef belge yaratılır. Motorlar kendi güvenli temp çıktısını tamamladıktan sonra hedef URI'ye kopyalar; worker'ın yarattığı yarım belge hata/iptalde silinir, UI'nın önceden verdiği mevcut çıktı URI'si izinsiz silinmez.
- PDF→PPTX/PPTX→PDF motoru `ConversionDirection` ile seçilir; kalite/fit domain değerleri adapter'da engine değerlerine çevrilir. Senkron inspect/convert `Dispatchers.IO` üzerinde çalışır.
- Engine progress hem WorkManager progress data'ya hem Room item progress/status alanına yazılır. Success'te output metadata/size ve warnings; failure/unsupported/cancel'de yapılandırılmış domain error/status kaydedilir. Retryable tüm hatalar için en fazla iki otomatik backoff denemesi, ayrıca scheduler üzerinden açık retry vardır; başarılı item'lar retry'da atlanır.
- Foreground notification generic içerik kullanır, dosya içeriğini göstermez; sayfa/slayt ilerlemesi ve WorkManager'ın worker-id cancel pending intent'i vardır. İptal kooperatiftir ve oluşturulan yarım SAF çıktısını temizler.

## Ayar şeması güncellemesi — 2026-07-13

- Preferences DataStore artık `theme_palette` ile dört marka paletini ve `quality_default_version` ile varsayılan kalite migration sürümünü saklar. Enum decode işlemleri bilinmeyen/gelecek değerlerde güvenli varsayılana düşer.
- Ürün varsayılanı `MAXIMUM` kalitedir. Repository, migration sürümü henüz yoksa legacy değeri bellekte `MAXIMUM` olarak çözer; settings Flow başlamadan `DataStore.edit` yapmaz. Her normal ayar yazımı sürüm 1'i kaydeder; bundan sonra kullanıcının `COMPACT`, `BALANCED`, `HIGH` veya `MAXIMUM` seçimi korunur.
- Bu anahtarların anlamını değiştiren sonraki AI, migration'ı saf preferences testiyle doğrulamalı ve bu belgeyi güncellemelidir; Room DB sürümü bu Preferences değişikliğinden etkilenmez.

## Room v3, DPI/lossless ve çoklu çıktı — 2026-07-13

- `ConversionJobEntity` `dpi` ve `lossless` saklar. DB v1→v2 migration'ı bunları `300` ve `true` varsayılanıyla ekler.
- Domain `ConversionItem.outputs` liste taşır; `output` ilk öğeyi veren uyumluluk erişimcisidir. Repository `saveOutputs`, DAO toplu insert/delete ve worker çoklu hedef desteği içerir.
- DB v2→v3 migration'ı `conversion_outputs` tablosunu `(item_id, position)` birleşik anahtarıyla yeniden kurar; eski tek çıktıları `position=0` olarak korur ve foreign-key/index sözleşmesini sürdürür. Yıkıcı migration kullanılmaz.
- Room başlangıç akışı hata verirse ViewModel hatayı loglayıp boş geçmiş yayar; veritabanını silmez. Bu Home'un kapanmasını önler, gerçek şema sorunu yine logcat ile teşhis edilmelidir.
- Dosya seçimi her yönün MIME kümesiyle tek `OpenMultipleDocuments` launcher kullanır; tekli ve toplu seçim aynı callback'e gelir.

## Çoklu çıktı geçmişi ve migration cihaz testi — 2026-07-13

- History artık yalnız uyumluluk erişimcisi `item.output` ile sınırlı değildir. `outputsForHistory()` tüm `item.outputs` listesini sıralı döndürür; her çıktı adı/URI/biçimi ayrı görünür ve ayrı aç/paylaş eylemi alır. Çıktı yoksa kaynak satırı fallback olarak gösterilir; hata ve warning item başına bir kez çizilir.
- `matchesHistoryQuery()` aramayı iş/yön/kaynak yanında bütün çıktı adlarına uygular. `HistoryOutputModelTest` çoklu çıktının görünürlük ve arama sözleşmesini doğrular.
- `DatabaseMigrationSmokeTest.kt` gerçek Android SQLite üzerinde minimal v1 fixture oluşturur; `MIGRATION_1_2` ve `MIGRATION_2_3` sırasını doğrudan uygular. `dpi=300`, `lossless=true`, eski çıktının `position=0` korunması ve aynı item için ikinci output position eklenebilmesi doğrulandı.
- Samsung cihazındaki bağlı test turunda migration testi geçti. Yıkıcı migration yoktur. Sonraki şema değişikliğinde bu fixture zinciri yeni sürüme uzatılmalı; yalnız JVM mapper testi yeterli sayılmamalıdır.
