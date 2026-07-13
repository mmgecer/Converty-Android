# Material 3 Expressive — Dosya Dönüştürücü İkonu

## Tasarım fikri

Tek bir dosya, soldaki metin satırlarından sağdaki yuvarlatılmış veri/format
bloklarına dönüşüyor. Ortadaki kalın, akışkan dikiş “morf” anını gösteriyor.
İki ayrı dosya, çift yönlü ok veya portal kullanılmadı.

## Dosyalar

- `svg/converter_master_layered.svg`
  - Inkscape katman adları ve düzenlenebilir gruplar içerir.
  - `data-color-role` alanları Material renk rolü eşlemesini gösterir.
- `svg/converter_background.svg`
  - Adaptive icon arka plan katmanı.
- `svg/converter_foreground.svg`
  - Adaptive icon ön plan katmanı; arka planı şeffaftır.
- `svg/converter_monochrome.svg`
  - Android temalı ikon için tek renkli kaynak.
- `android/res/...`
  - Doğrudan projeye kopyalanabilir adaptive icon ve VectorDrawable dosyaları.
- `compose/ConverterMark.kt`
  - Uygulama içinde kullanılacak, `MaterialTheme.colorScheme` rollerine bağlı
    gerçek dinamik renkli Compose çizimi.

## Android'e ekleme

`android/res` klasörünün içeriğini uygulamanızdaki `app/src/main/res` içine
kopyalayın. Manifest'teki launcher icon alanları `@mipmap/ic_launcher` ve
`@mipmap/ic_launcher_round` olarak kalabilir.

Android 13 ve üzerindeki temalı launcher ikonunda sistem, `monochrome`
katmanını kullanarak kullanıcı temasına göre tint uygular.

## Dinamik renk ayrımı

Launcher ikonunun tam renkli foreground/background katmanları çalışma anındaki
Compose `MaterialTheme` renklerini okuyamaz. Launcher tarafındaki sistem rengi
uyumu `monochrome` katmanı üzerinden sağlanır.

Uygulama içindeki logo/ikon ise `ConverterMark.kt` ile `primary`,
`tertiary`, `surfaceContainerHighest`, `secondaryContainer` gibi semantik
rolleri okur. Uygulama temanız `dynamicLightColorScheme()` veya
`dynamicDarkColorScheme()` kuruyorsa ikon otomatik olarak sistem paletine geçer.

## Güvenli alan

Ana belge ve anlam taşıyan detaylar 108×108 adaptive icon koordinat sisteminin
merkezinde tutuldu. Dıştaki tonal organik şekiller kırpılabilir dekoratif
öğelerdir; kırpılsalar bile ikonun anlamı kaybolmaz.
