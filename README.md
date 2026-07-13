# Converty

English | [Türkçe](README.tr.md) | [Deutsch](README.de.md) | [简体中文](README.zh-CN.md) | [العربية](README.ar.md) | [Português](README.pt.md) | [Français](README.fr.md) | [Русский](README.ru.md)

Converty is an Android file conversion application designed around on-device processing, Android's Storage Access Framework, and a Material 3 Expressive interface. It supports individual files and batches, selective page or slide processing, persistent conversion history, background work, and direct open or share actions for generated files.

The project is under active development. Its conversion engines are intentionally explicit about format limitations. PDF-to-PPTX preserves page appearance by placing rendered pages into slides; the resulting text is not editable. PPTX-to-PDF and Office-to-PDF support a practical subset of document content and do not claim complete Microsoft Office rendering fidelity.

## Current status

- Android application ID: `com.converty.app`
- Minimum Android version: Android 6.0, API 23
- Target and compile SDK: API 37
- Interface: Jetpack Compose with Material 3 Expressive APIs
- Processing model: local, on-device conversion
- Current verification: 80 JVM tests and 7 Android device instrumentation tests
- Current public-distribution status: source publication is prepared under Apache-2.0 and a signed local release has been produced; F-Droid preparation is still incomplete. See [Release and distribution status](#release-and-distribution-status).

## Main capabilities

- Select one file or multiple files in a single workflow.
- Run conversions through WorkManager with file-level and job-level progress.
- Keep a local history of completed, failed, and cancelled jobs.
- Open or share each generated output from the history screen.
- Choose an output directory through Android's Storage Access Framework.
- Process all pages or slides, the first or last N items, custom ranges, or disjoint selections such as `1,3-5,9`.
- Keep or remove selected ranges without modifying the source document.
- Configure image quality, DPI where applicable, WebP lossless output, and PDF-to-PPTX image fitting.
- Use system, light, or dark themes; dynamic color is available on supported Android versions.
- Use the application in English, Turkish, German, Simplified Chinese, Arabic, Portuguese, French, or Russian.
- Use Android 13 themed icons and adaptive launcher icons on supported launchers.

## Supported conversions

### Documents and presentations

| Source | Target | Behavior |
|---|---|---|
| PDF | PPTX | Renders each selected PDF page and places it on a slide. Appearance is preserved as an image; text is not editable. |
| PPTX | PDF | Renders supported slide text, images, fills, lines, and basic shapes. Unsupported or partially supported presentation features may produce warnings. |
| DOCX | PDF | Converts supported document content on-device. Complex Word layout may differ from Microsoft Word. |
| XLSX | PDF | Converts supported worksheet content on-device. Advanced spreadsheet layout and calculation behavior are limited. |
| ODT | PDF | Converts supported OpenDocument text content. |
| ODS | PDF | Converts supported OpenDocument spreadsheet content. |
| ODP | PDF | Converts supported OpenDocument presentation content. |

### PDF image outputs

| Source | Target | Notes |
|---|---|---|
| PDF | PNG | Produces one lossless PNG for each selected page. |
| PDF | JPG | Produces one JPEG for each selected page with configurable quality and DPI. |
| PDF | TIFF | Produces TIFF output for selected pages. |
| PDF | Thumbnail | Produces a compact JPEG preview. |

### Image conversion

- PNG to JPG and WebP
- JPG to PNG and WebP
- WebP to PNG and JPG
- TIFF to PNG and JPG
- BMP to PNG and JPG
- HEIC or HEIF to PNG and JPG
- SVG to PNG
- AVIF to PNG, JPG, and WebP

Codec availability and behavior can depend on the Android version and device implementation, particularly for HEIC, HEIF, AVIF, and TIFF.

## Selection and output controls

For PDF and PPTX inputs, Converty can process:

- all pages or slides;
- the first N items;
- the last N items;
- explicitly kept ranges;
- explicitly removed ranges;
- multiple separate ranges in one operation.

PDF-to-PPTX supports contain, cover, and stretch fitting. PDF raster conversion supports a configurable DPI range from 72 to 600 where the selected direction exposes DPI controls. Output names are sanitized, Unicode names are preserved, and file collisions are handled through the configured output policy.

## Privacy and storage

Converty currently declares no Android `INTERNET` permission and includes no advertising or analytics SDK. Conversion is performed on the device. The application accesses only the documents and output directories selected through Android's system document picker.

Conversion history and settings are stored locally using Room and DataStore. Application data, history, and selected file metadata are excluded from Android cloud backup and device-transfer backup. Cleartext network traffic is disabled in the application configuration.

These statements describe the current source tree and should be reviewed whenever permissions, dependencies, telemetry, crash reporting, or network features are added.

## Material 3 Expressive interface

The interface uses Material 3 Expressive components, motion, tonal color, and adaptive navigation. It includes:

- a two-sided source and target format selector;
- expressive progress indicators for active conversions;
- phone-oriented bottom navigation and adaptive layout foundations;
- system, light, dark, dynamic-color, and branded palette options;
- right-to-left layout support for Arabic;
- localized resources rather than hard-coded interface strings;
- adaptive, round, and Android 13 monochrome launcher icon variants.

## Known limitations

- PDF-to-PPTX creates image-based slides. Text and page objects are not reconstructed as editable PowerPoint elements.
- PPTX-to-PDF is not a replacement for Microsoft PowerPoint, LibreOffice, or a full OOXML rendering engine. SmartArt, charts, tables, grouped objects, media, animations, complex themes, master-layout behavior, font substitution, and advanced effects may be incomplete or unsupported.
- DOCX, XLSX, ODT, ODS, and ODP conversion supports a limited on-device rendering model. Complex documents may not preserve exact pagination or visual layout.
- Password-protected, encrypted, malformed, exceptionally large, or hostile documents may be rejected.
- Large documents can require significant memory and processing time.
- Device codec differences can affect HEIC, HEIF, AVIF, TIFF, and WebP conversion.
- Foreground work notifications require notification permission on Android 13 and later. Declining the permission does not disable the in-application queue view.

## Building from source

### Requirements

- JDK 17
- Android SDK Platform 37
- Android Build Tools 37.0.0
- The Gradle wrapper included in this project

The main toolchain versions are AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.10, Compose BOM 2026.06.00, and Material 3 1.5.0-alpha23.

### Verification and debug build

On Linux or macOS:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

### Release build

```sh
./gradlew assembleRelease
```

Production signing is configured locally through `key.properties` and a dedicated release keystore. Both files are excluded by `.gitignore` and must never be committed, copied into documentation, or exposed in logs or issue reports. When those local files are present, `assembleRelease` produces a signed APK. A clean public clone contains no private signing material and can build an unsigned release, which is the appropriate source-build path for F-Droid because F-Droid signs distributed binaries itself.

## Project structure

- `app/src/main/java/com/converty/app/core`: conversion contracts, selection logic, and file conversion engines.
- `app/src/main/java/com/converty/app/data`: Storage Access Framework integration, Room history, and DataStore settings.
- `app/src/main/java/com/converty/app/work`: WorkManager scheduling, foreground progress, cancellation, and worker adapters.
- `app/src/main/java/com/converty/app/feature`: Compose screens, application state, and user actions.
- `app/src/main/java/com/converty/app/ui`: Material 3 Expressive theme and reusable interface components.
- `app/src/main/res`: localized strings, themes, launcher icons, and Android configuration resources.
- `app/src/test`: JVM contract, parser, selection, history, localization, and resource tests.
- `app/src/androidTest`: device startup, database migration, and conversion engine smoke tests.
- `HANDOFF`: architecture, decision, verification, and maintenance notes for future development sessions.

## Release and distribution status

The source tree is prepared for publication on GitHub under the Apache License 2.0. A dedicated production key is configured locally and a signed release APK has been generated. Private signing files are intentionally absent from the public source tree.

The current signed APK should be treated as a local release candidate, not as an F-Droid-ready package. The following work remains before an F-Droid submission or a permanent public package release:

1. Replace `com.converty.app` with a permanent, globally unique application ID. That ID is already used by an unrelated application, so publishing with it creates an identity collision.
2. The public `main` branch is published at [GitHub](https://github.com/mmgecer/Converty-Android). Create and push the `v0.1.0` tag that exactly matches the submitted source.
3. Add F-Droid build metadata, including the version mapping and an entirely source-based build recipe.
4. Rebuild from a clean Linux checkout in an F-Droid-like environment and pass the F-Droid scanner and reproducibility checks.
5. Complete the final asset and dependency-license review and add any notices or attributions that the review identifies.
6. Increase `versionCode` for every published update and publish release notes, checksums, screenshots, a privacy statement, and a support or issue-reporting channel.

The Apache-2.0 license, local production signing, and publication of `main` are resolved. The application-ID collision, the `v0.1.0` release tag, F-Droid metadata, and clean Linux/F-Droid build and scanner verification remain blocking items.

## Contributing

A contribution policy and code of conduct have not yet been adopted. Before accepting external contributions, the project should add `CONTRIBUTING.md`, a code of conduct, an issue template, and a process for certifying that contributed code and assets may be redistributed under the project license.

## License

Converty is licensed under the Apache License 2.0. See the root [`LICENSE`](LICENSE) file for the complete terms.
