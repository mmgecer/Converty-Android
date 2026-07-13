# Proje Kuralları ve Ürün Kapsamı

> Zorunlu: Bu projede çalışan her yapay zekâ önce `HANDOFF-INDEX.md` dosyasını okumalı ve yaptığı değişikliklerden sonra ilgili HANDOFF belgelerini güncellemelidir.

## Ürün hedefi

Converty; PDF ve PPTX dosyalarını Android cihaz üzerinde birbirine dönüştüren, Material Design 3 Expressive görsel dilini kullanan, acemi kullanıcı için anlaşılır fakat toplu işlerde güçlü bir uygulama olacaktır.

Zorunlu ana yetenekler:

- PDF → PPTX ve PPTX → PDF.
- Tek dosya, birden fazla dosya ve uygun olduğunda klasör/ağaç seçimi.
- Baş, son, orta veya aynı dosyanın birden çok ayrık bölümünden sayfa/slayt seçimi; tek öğe ve aralıkların birlikte kullanılabilmesi.
- İşlem kuyruğu ve geçmişi; durum, ilerleme, hata, yeniden deneme ve çıktı bilgisi.
- Çıktıyı hızlı açma ve Android Sharesheet ile paylaşma.
- Sistem/açık/koyu tema; dinamik renk desteği ve M3 Expressive bileşen/hareket yaklaşımı.
- İngilizce (`en`), Türkçe (`tr`), Almanca (`de`), Basitleştirilmiş Çince (`zh-rCN`), Arapça (`ar`), Portekizce (`pt`), Fransızca (`fr`) ve Rusça (`ru`). Kaynak tabanlı yapı yeni dil eklemeyi kolaylaştırmalı; Arapça RTL doğrulanmalı.

## Kapsam sınırları

- Masaüstü PowerPoint COM, kurulu Microsoft Office veya masaüstü LibreOffice çalıştırılmasına güvenilemez.
- Android Storage Access Framework dışında ham, kalıcı dosya yolu varsayılmaz.
- İlk motorlarda PDF→PPTX, kaynak PDF sayfasını görsel olarak slayda yerleştirir; metni düzenlenebilir öğelere OCR ile ayırmak ayrı/gelecek özelliktir.
- PPTX→PDF için amaç mümkün olan en iyi cihaz üstü görünüm sadakatidir; PowerPoint'in tüm efektlerini birebir uygulama iddiası yoktur. Desteklenmeyen öğeler açıkça raporlanmalıdır.

## Çalışma protokolü

- Kullanıcı 2026-07-13 yayın hazırlığı turunda önceki Git yasağını kaldırdı ve ardından yerel commit istedi. Depo `main` dalında başlatıldı; destructive komutlar (`reset --hard`, zorla checkout/clean vb.) çalıştırılmaz ve remote doğrulanmadan push/force yapılmaz.
- Planlı ilerlenir; aktif faz ve test durumu `06-VERIFICATION-ROADMAP.md` içinde tutulur.
- Yeni karar veya önemli trade-off `05-DECISIONS-CHANGELOG.md` içine tarihli eklenir.
- Kod görevi ve dosya haritası `01-ARCHITECTURE.md`; motor ayrıntıları `02-CONVERSION-ENGINES.md`; ekran/dil değişiklikleri `03-UX-M3E-I18N.md`; veri/dosya akışı `04-DATA-FILES-HISTORY.md` içinde güncellenir.
- Hassas veya geçici kullanıcı içeriği loglara yazılmaz; geçici dosyalar başarı, hata ve iptalde temizlenir.
