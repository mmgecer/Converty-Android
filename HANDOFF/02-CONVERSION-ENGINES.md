# Dönüşüm Motorları HANDOFF

> Zorunlu: Önce `HANDOFF-INDEX.md` okunur; motor davranışı, destek matrisi, hata veya doğrulama değiştiğinde bu belge güncellenir.

## Eski koddan alınan mantık

Kaynak: `OLD BAD CODE/pdf_to_pptx.py`.

### PDF → PPTX

`pdf_to_pptx` (satır 227–275) seçilen PDF'leri `pdf2image.convert_from_path` ile sayfa görsellerine çevirir. Her görsel için boş slayt açar, kaynak ve slayt en-boy oranlarını karşılaştırır, görseli kırpmadan sığdırır ve ortalar. Geçici PNG eklenir/silinir; çıktı tek tek veya ZIP olarak yazılır.

Android karşılığı: `PdfRenderer` ile yalnız seçilen sayfaları kontrollü çözünürlükte bitmap'e çiz; OOXML PPTX paketinde her sayfa için boş slayt + media PNG/JPEG + oranlı/ortalanmış transform üret. Her sayfayı aynı anda RAM'de tutma. Kaynak sayfa boyutuna göre slayt boyutu/uyum modu seçenekleri ayrıca tasarlanabilir.

### PPTX → PDF

`pptx_to_pdf` (satır 277–336) iki masaüstü motora bağlıdır:

- Microsoft PowerPoint COM: `ExportAsFixedFormat`.
- `soffice --headless`: LibreOffice alt süreci.

İkisi de Android'de kullanılamaz. Bu kod doğrudan taşınmayacaktır. Android motoru PPTX'i ZIP/OOXML olarak ayrıştırıp desteklenen slayt öğelerini Canvas/PDF sayfasına render etmelidir. Tam PowerPoint sadakati gerçekçi değildir; desteklenmeyen efekt, font, SmartArt, video ve benzeri öğeler uyarı üretmelidir.

## Eski koddaki teknik riskler

- PDF→PPTX tüm sayfaları topluca görsele çevirir; büyük belgelerde bellek baskısına yol açar.
- İlk sunum varsayılan boş slaytla oluşturulabildiğinden gereksiz ilk slayt riski vardır.
- Yalnız dosya uzantısını `replace` ile değiştirir; büyük harf/çakışan ad/yanlış uzantı güvenli değildir.
- Geçici dosya ve harici işlem temizliği tüm hata yollarında garanti edilmez.
- PPTX→PDF sonucunun gerçekten oluştuğu doğrulanmadan geçmiş/çıktı listesine eklenebilir.
- Genel `Exception` ve yalnız terminal çıktısı, yapılandırılmış hata/yeniden deneme sunmaz.
- COM uygulaması bazı istisnalarda kapanmadan kalabilir.
- CSV geçmişi altı serbest metin alanına dayanır; iş/öğe/progress/hata ilişkisini modellemez.

## Hedef motor sözleşmesi

Her motor en az şunları alır: okunabilir kaynak `Uri`/akış, yazılabilir hedef, normalize edilmiş öğe seçimi, kalite/uyum seçenekleri ve iptal/progress callback'i. Sonuç; başarı/başarısızlık/iptal, çıktı meta verisi, uyarılar, desteklenmeyen öğeler ve makinece işlenebilir hata kodu taşımalıdır.

Motorlar idempotent çalışmaya yakın olmalı; kısmi hedef önce geçici alana yazılıp başarılı kapanıştan sonra kullanıcı hedefine aktarılmalıdır.

## Planlanan doğrulama fikstürleri

- Tek sayfa/slayt, portre, yatay, kare ve karışık boyut.
- `1`, `1-3`, `1,3,7-9`, sondan seçim ve çakışan/tekrarlı aralık normalleştirmesi.
- Şifreli/bozuk PDF, bozuk PPTX, parola korumalı sunum, 0 öğe, çok büyük belge.
- Unicode/RTL metin, eksik font, saydam görsel, şekil ve grup öğeleri.
- İş iptali, düşük depolama, çıktı adı çakışması ve süreç ölümü sonrası yeniden başlatma.

## Uygulanan cihaz üstü çekirdek — 2026-07-13

`com.converty.app.core.conversion` altında UI/Room bağımlılığı olmayan ortak `ConversionEngine`, inspection, istek, capability, progress, iptal ve yapılandırılmış success/failure/unsupported sözleşmeleri eklendi. Motor isteği normalize edilmiş, farklı ve 0-tabanlı `selectedItemIndices` alır; boş liste “tümü” demektir. Domain `ResolvedSelection.indices()` çıktısı `work/ConversionWorkerAdapters.kt` üzerinden aktarılır; kalite/fit/hata eşlemeleri de aynı adapter'dadır.

### `PdfToPptxEngine`

- SAF `Uri` kaynağını `PdfRenderer` ile açar ve yalnız seçilen sayfaları tek tek bitmap'e çizer; her bitmap JPEG olarak paketlendikten sonra hemen `recycle()` edilir.
- Compact/Balanced/High DPI ve JPEG kalitesi, 4096 px kenar/16 MP bellek tavanı, contain/cover/stretch yerleşimi ve ilk seçili sayfaya uyan/16:9/4:3 slayt boyutu uygulanır.
- `PptxPackageWriter` harici kütüphane kullanmadan deterministik ZIP/OOXML üretir. Paket bir raster görselli slaytlar yanında presentation, slide master, blank layout ve theme parçalarını içerir; gereksiz ilk slayt üretmez.
- Çıktı önce cache geçici dosyasına kapanır, sonra hedef `Uri`'ye kopyalanır. Başarı, hata ve iptal yollarında geçici dosya/bitmap temizlenir.

### `PptxToPdfEngine`

- `PptxPackageParser`, uygulamanın image-only PPTX'leri yanında dış sunumların temel görsel, metin kutusu ve basit auto-shape öğelerini okur; presentation/spTree z-order, ilişkiler ve `a:xfrm` konum/boyut/dönüş bilgilerini çözer.
- ZIP path traversal, ters slash/absolute path, yinelenen entry/relationship, 4096 entry, parça/toplam boyut ve 100:1 sıkıştırma oranı sınırları uygulanır. XML parser DOCTYPE/XXE, harici entity, harici DTD/schema ve XInclude'ı kapatır; kaynak en fazla 256 MiB geçici dosyaya alınır.
- Seçili slaytta metin/şekil, chart/table/SmartArt, grup, çoklu/eksik/harici görsel, crop/rotation/flip/tile, özel arka plan, border/effect veya desteklenmeyen görsel biçimi varsa sessiz bozulma yerine `ConversionResult.Unsupported` + slayt indeksli warning döner.
- Desteklenen slaytlar `PdfDocument` Canvas'ına beyaz zemin üstünde OOXML transform'una göre çizilir; görsel sayfa bazında decode/recycle edilir ve decode piksel sınırı uygulanır.

### Dosyalar ve doğrulama

- `ConversionContracts.kt`: ortak motor API'si ve hata/uyarı modelleri.
- `ConversionIo.kt`: sınırlandırılmış SAF/cache kopyalama ve tüm yollarda geçici dosya temizliği.
- `PptxPackageWriter.kt` / `PptxPackageParser.kt`: saf Kotlin/JDK OOXML katmanı.
- `PdfToPptxEngine.kt` / `PptxToPdfEngine.kt`: Android platform motorları.
- `PptxPackageWriterTest.kt`: deterministik byte çıktısı, writer→parser round-trip ve contain geometri testi.
- `PptxPackageSecurityTest.kt`: path traversal, DOCTYPE/XXE ve entry-count limit testleri.

Yeni üçüncü taraf bağımlılık eklenmedi; yalnız JDK/Kotlin ve Android platform API'leri kullanıldı. SDK 37/JDK 17 ile tüm kaynaklar derlendi; 13 conversion testi ve toplam 60 unit test geçti. `PdfRenderer` ve `PdfDocument` uçtan uca davranışı local JVM yerine instrumentation/gerçek cihaz fikstürü gerektirir. Sonraki her AI motor davranışı, destek matrisi, limit veya bug değiştiğinde bu bölümü ve `05-DECISIONS-CHANGELOG.md` kaydını güncellemelidir.

### Derleme bug düzeltmesi — Android `XMLConstants`

- **Belirti:** SDK 37 Kotlin derlemesi `PptxPackageParser.kt` içindeki `XMLConstants.ACCESS_EXTERNAL_DTD` ve `ACCESS_EXTERNAL_SCHEMA` alanlarını unresolved reference olarak bildirdi.
- **Kök neden:** Bu JAXP 1.5 sabitleri masaüstü JDK'da bulunmasına rağmen Android SDK'nın `javax.xml.XMLConstants` API stub'unda yayımlanmıyor.
- **Düzeltme:** Güvenlik ayarı kaldırılmadı; aynı standart property değerleri olan `http://javax.xml.XMLConstants/property/accessExternalDTD` ve `.../accessExternalSchema` yerel sabitleri kullanıldı. DOCTYPE, external entity/parameter entity, external DTD ve XInclude feature engelleri aynen korunuyor.
- **Doğrulama:** Kaynak düzeyindeki unresolved reference giderildi; SDK 37 tam Kotlin compile, unit test, lint ve debug APK derlemesi başarılı oldu.

### Belge inceleme sözleşmesi — seçim entegrasyonu

- **Belirti:** Yeni Room item'larında `totalUnits` başlangıçta `null` olduğu için worker, `FIRST/LAST/KEEP/REMOVE` seçimini çözmeden işi hemen `FAILED` yapıyordu.
- **Kök neden:** `ConversionEngine` yalnız `convert` sunuyor, belgeyi dönüştürmeden sayfa/slayt sayısını güvenli biçimde öğrenme yolu vermiyordu.
- **Düzeltme:** `inspect(source, cancellation)` ile `DocumentInspectionResult` eklendi. Başarı sonucu format, `itemCount` ve PPTX için seçime geçmeden gösterilebilecek destek uyarılarını taşır; failure ve cancellation ayrı yapılandırılmış sonuçlardır.
- **Motor davranışı:** PDF inspection yalnız `PdfRenderer.pageCount` okur. PPTX inspection kaynağı aynı 256 MiB sınırlı cache staging hattından geçirir, mevcut güvenli ZIP/XML parser ile `slides.size` okur ve geçici dosyayı tüm sonuç yollarında siler. Böylece inspection, conversion güvenlik limitlerini atlamaz.
- **Kullanım sırası:** Worker önce `inspect`; success durumunda `totalUnits` ve inspection uyarılarını Room'a yazar, `SelectionParser.resolve(request, inspection.itemCount)` çalıştırır ve sonra normalize indekslerle `convert` çağırır. Bu akış uygulanmıştır.
- **Test:** Saf writer/parser round-trip testine iki slaytlı paket için kesin `slides.size == 2` kontrolü eklendi. `PdfRenderer` inspection davranışı instrumentation testi gerektirir.

## PPTX→PDF temel PowerPoint içeriği — 2026-07-13

`PptxPackageParser.kt` image-only sınırından, dışarıda üretilmiş basit sunumları da temsil eden sıralı bir render modeline genişletildi. Presentation `sldId` sırası ve her slaydın `p:spTree` doğrudan çocuk sırası korunur; bu liste Canvas z-order'ıdır. Model şu öğeleri taşır:

- `p:pic`: PNG/JPEG paket parçası, EMU konum/boyut, 1/60000 derece rotation ve yatay/dikey flip.
- `p:sp`: rect, roundRect, ellipse ve line preset geometry; `solidFill`/`noFill`; solid line rengi ve kalınlığı.
- `p:txBody`: paragraf ve run sırası, açık satır sonu, temel font boyutu/rengi, bold/italic/font ailesi ve paragraf hizası. Metin kutusu inset'leri de korunur.

Tema/master/placeholder mirası tam PowerPoint stil motoru gibi çözülmez. Bunun yerine güvenli sabit renk/font fallback'i uygulanır ve `theme-*`, `placeholder-*` veya `master-*` açıklamalı warning üretilir. Bilinmeyen preset geometry rect fallback'i alır. Chart/table/SmartArt, group, gradient/pattern, effect/3D, crop/tile ve benzeri karmaşık özellikler desteklenen kardeş öğelerin render edilmesini engellemez; `ConversionResult.Success.warnings` içinde slayt indeksiyle raporlanır. Bir slaytta hiç render edilebilir öğe kalmazsa `no-renderable-content` eklenir ve seçili slayt için çıktı üretmeden structured `ConversionResult.Unsupported` döner.

`PptxToPdfEngine.kt`, sıralı model öğelerini beyaz `PdfDocument` sayfasına çizer. Görseller mevcut decode pixel limiti altında öğe başına decode/recycle edilir. Canvas transform + clip ile rotation/flip uygulanır; temel shape fill/stroke çizilir. Metin `StaticLayout` ile kutuya sarılır, paragraf hizası ve run span'leri korunur; Android typeface seçimi eksik font/glyph için sistem fallback zincirini kullanır. Uygulamanın ürettiği tek görselli slayt davranışı, eski `imagePart`/`placement` uyumluluk erişimcileri ve writer→parser testiyle korunur.

`PptxPackageParserTest.kt`, dış-PPTX benzeri elle paketlenmiş OOXML fikstüründe presentation sırasını, picture→shape z-order'ını, dört temel geometriyi, transform/flip'i, fill/line renklerini, text run/paragraf özelliklerini, tema/placeholder ve complex-content warning'lerini doğrular. Yalnız `graphicFrame` içeren slaydın render edilemez işaretlenmesi de test edilir. Mevcut path traversal, XXE/DOCTYPE ve ZIP entry limit testleri değiştirilmedi; parserın ZIP sayı/boyut/oran/path kontrolleri ve güvenli XML factory ayarları aynen korundu. Root doğrulamasında compile, 60/60 test ve lint başarılıdır.

## Genişletilmiş format motorları — 2026-07-13

İzinli matris: PDF↔PPTX; PNG/JPG/WebP karşılıklı; TIFF ve BMP→PNG/JPG; HEIC/HEIF→JPG/PNG; SVG→PNG; AVIF→PNG/JPG/WebP; DOCX/XLSX/ODT/ODS/ODP→PDF; PDF→PNG/JPG/TIFF ve ilk sayfa thumbnail. Aynı-format yönü yoktur.

- `ImageTranscodeEngine.kt`: platform codec'leriyle PNG/JPG/WebP; cihaz codec'i bulunduğunda HEIC/HEIF ve AVIF; güvenlik limitli özel decoder ile BMP/TIFF; `SvgRasterDecoder.kt` ile temel SVG şekil/path dönüşümü. JPG alpha'yı beyaza düzleştirir, WebP lossless/lossy seçimini uygular; desteklenmeyen SVG/TIFF kapsamı warning üretir.
- `PdfToRasterPagesEngine.kt`: seçilen sayfaları DPI'ya göre ayrı PNG/JPG çıktılara render eden çoklu motor. `PdfToTiffEngine` çok sayfalı sıkıştırmasız RGB TIFF, `PdfThumbnailEngine` yalnız ilk sayfadan en fazla 512 px kenarlı JPG üretir.
- `OfficeToPdfEngine.kt`: DOCX/XLSX/ODT/ODS/ODP ZIP/XML metnini entry/path/boyut/oran/XXE sınırlarıyla çıkarıp temel A4 PDF düzenine yazar. Tam Office sadakati iddia etmez; `basic-office-layout` warning'i üretir.
- `ConversionOptions` ve motor isteği `dpi` (72..600) ile `lossless` taşır. `ImageQuality.MAXIMUM` 300 DPI/100 kalite eşlemesine sahiptir. PDF→PPTX non-editable raster slayt özelliği UI'de “Görünümü koru” olarak açıklanır.

Bu bölümün ilk yerel turunda motorlar 65 JVM testi ve lint ile derlenmişti; aşağıdaki gerçek cihaz turu Android `Bitmap`/`PdfRenderer`/`PdfDocument`, HEIC/AVIF codec'i ve Office/ODF çıktıları için eksik uçtan uca kanıtı tamamladı.

## Gerçek cihaz motor doğrulaması ve XML uyumluluk düzeltmesi — 2026-07-13

- `ConversionEngineSmokeTest.kt` Samsung SM-S721B / Android 15 üzerinde beş test çalıştırır: PDF raster/TIFF/thumbnail, raster/SVG dönüşümleri, HEIC/HEIF ve AVIF platform codec'leri, beş Office/ODF kaynağı ve PDF→PPTX→PDF round-trip. Çıktılar yalnız success durumuyla değil PNG/JPEG/WebP/BMP/TIFF/PDF/ZIP imzaları ve yeniden açılabilir paket davranışıyla kontrol edilir; son tur **5/5 geçti**.
- İlk cihaz turunda Office/ODF ve PPTX yolları `DocumentBuilderFactory.isXIncludeAware` için `UnsupportedOperationException`, sonraki turda `XMLConstants.FEATURE_SECURE_PROCESSING` için `ParserConfigurationException` verdi. Masaüstü JVM testleri bu Android/Harmony farkını göstermiyordu.
- `SecurePackageXml.kt` ortak güvenli DOM girişidir. Parser destekliyorsa secure-processing, disallow-doctype, external entity/DTD ve JAXP external-access ayarlarını uygular. Desteklemeyen Android parser'larda güvenlik düşmez: UTF-8/UTF-16LE/UTF-16BE byte taraması `DOCTYPE`/`ENTITY` bildirilerini parse öncesi reddeder ve `EntityResolver` tüm harici çözümlemeleri hata yapar. Office reddi `OfficeSecurityException`, PPTX reddi `SECURITY_LIMIT` olarak sınıflanır.
- `RasterDecoders.kt` TIFF Deflate strip açılımını bildirilen strip boyutuyla sınırlar; samples/bits/strip coverage tutarlılığı long-safe doğrulanır. `SvgRasterDecoder.kt` artık `defs`, `clipPath`, `mask` ve `symbol` alt ağaçlarını görünür içerik gibi çizmez. `RasterDecodersSecurityTest` bounded inflate ve taşma reddini korur.
- HEIC/HEIF (API 28+) ve AVIF (API 31+) yolları platform codec'ine bağlıdır. `app/src/androidTest/assets` altındaki küçük AOSP CTS fixture'ları ve yanındaki provenance README ile HEIC/HEIF→JPG/PNG ve AVIF→PNG/JPG/WebP yollarının tamamı cihazda doğrulandı. Fixture'lar normal APK'ya paketlenmez; güncellenirlerse kaynak/provenance birlikte korunmalıdır.
