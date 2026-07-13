# Converty

[English](README.md) | [Türkçe](README.tr.md) | Deutsch | [简体中文](README.zh-CN.md) | [العربية](README.ar.md) | [Português](README.pt.md) | [Français](README.fr.md) | [Русский](README.ru.md)

Converty ist eine Android-Anwendung zur lokalen Dateikonvertierung. Sie verwendet das Storage Access Framework von Android, WorkManager und eine Oberfläche auf Basis von Material 3 Expressive. Unterstützt werden einzelne Dateien und Stapel, die Auswahl bestimmter Seiten oder Folien, ein dauerhafter Verlauf sowie das direkte Öffnen und Teilen erzeugter Dateien.

Das Projekt befindet sich in aktiver Entwicklung. PDF nach PPTX erhält das Erscheinungsbild, indem gerenderte Seiten als Bilder in Folien eingefügt werden; der Text bleibt dabei nicht editierbar. PPTX- und Office-Konvertierungen unterstützen einen praktischen Teil der jeweiligen Formate, beanspruchen aber keine vollständige Wiedergabetreue von Microsoft Office.

## Aktueller Stand

- Anwendungs-ID: `com.converty.app`
- Mindestversion: Android 6.0, API 23
- Ziel- und Compile-SDK: API 37
- Oberfläche: Jetpack Compose und Material 3 Expressive
- Verarbeitung: lokal auf dem Gerät
- Verifikation: 80 JVM-Tests und 7 Instrumentierungstests auf einem Android-Gerät
- Öffentliche Veröffentlichung: Der Quellcode ist unter Apache-2.0 zur Veröffentlichung vorbereitet und ein lokal signierter Release wurde erzeugt; die F-Droid-Vorbereitung ist noch nicht abgeschlossen.

## Funktionen

- Auswahl einer oder mehrerer Dateien in einem Arbeitsablauf.
- Hintergrundverarbeitung mit WorkManager sowie Fortschritt pro Datei und Auftrag.
- Lokaler Verlauf für erfolgreiche, fehlgeschlagene und abgebrochene Aufträge.
- Öffnen und Teilen einzelner Ausgabedateien.
- Wahl des Ausgabeordners über den Android-Systemdialog.
- Verarbeitung aller, der ersten oder letzten N Seiten beziehungsweise Folien.
- Eigene und getrennte Bereiche wie `1,3-5,9`; Bereiche können beibehalten oder ausgeschlossen werden.
- Qualitäts-, DPI-, WebP-Lossless- und Bildanpassungsoptionen, sofern das Zielformat sie unterstützt.
- System-, Hell- und Dunkelmodus, dynamische Farben und mehrere Markenpaletten.
- Englisch, Türkisch, Deutsch, vereinfachtes Chinesisch, Arabisch, Portugiesisch, Französisch und Russisch.

## Unterstützte Konvertierungen

### Dokumente und Präsentationen

| Quelle | Ziel | Hinweise |
|---|---|---|
| PDF | PPTX | Jede gewählte Seite wird gerendert und als Bild in eine Folie eingefügt. Text ist nicht editierbar. |
| PPTX | PDF | Unterstützt grundlegende Texte, Bilder, Füllungen, Linien und Formen; nicht unterstützte Funktionen können Warnungen erzeugen. |
| DOCX | PDF | Grundlegende Dokumentinhalte; komplexes Word-Layout kann abweichen. |
| XLSX | PDF | Grundlegende Tabelleninhalte; komplexe Layout- und Berechnungsfunktionen sind eingeschränkt. |
| ODT, ODS, ODP | PDF | Unterstützte OpenDocument-Inhalte werden lokal gerendert. |

### PDF- und Bildausgaben

- PDF nach PNG, JPG, TIFF und JPEG-Vorschaubild.
- PNG nach JPG und WebP.
- JPG nach PNG und WebP.
- WebP nach PNG und JPG.
- TIFF und BMP nach PNG oder JPG.
- HEIC beziehungsweise HEIF nach PNG oder JPG.
- SVG nach PNG.
- AVIF nach PNG, JPG oder WebP.

Die Codec-Unterstützung für HEIC, HEIF, AVIF, TIFF und WebP kann vom Android-Gerät abhängen.

## Datenschutz und Dateizugriff

Die aktuelle Anwendung fordert keine Android-Berechtigung `INTERNET` an und enthält weder Werbung noch Analyse-SDKs. Konvertierungen erfolgen auf dem Gerät. Zugriff besteht nur auf Dateien und Ausgabeordner, die über den Android-Dokumentdialog ausgewählt wurden.

Verlauf und Einstellungen werden lokal mit Room und DataStore gespeichert. Diese Daten sind von Cloud-Backup und Geräteübertragung ausgeschlossen. Unverschlüsselter Netzwerkverkehr ist in der Anwendungskonfiguration deaktiviert. Diese Aussagen müssen bei neuen Berechtigungen, Netzwerkfunktionen oder Telemetrie erneut geprüft werden.

## Bekannte Einschränkungen

- PDF nach PPTX erzeugt bildbasierte Folien und keine editierbaren PowerPoint-Objekte.
- PPTX nach PDF ersetzt keine vollständige PowerPoint- oder LibreOffice-Engine. SmartArt, Diagramme, Tabellen, Gruppen, Medien, Animationen, Masterlayouts, komplexe Themen, Schriftartenersetzung und Effekte können fehlen.
- DOCX, XLSX und OpenDocument verwenden ein begrenztes lokales Render-Modell; exakte Seitengestaltung ist nicht garantiert.
- Verschlüsselte, beschädigte, sehr große oder feindlich präparierte Dokumente können abgewiesen werden.
- Große Dateien benötigen möglicherweise viel Speicher und Zeit.
- Auf Android 13 und neuer ist für Vordergrundaufträge die Benachrichtigungsberechtigung erforderlich; die interne Warteschlange funktioniert auch ohne sie.

## Aus dem Quellcode bauen

Erforderlich sind JDK 17, Android SDK Platform 37, Build Tools 37.0.0 und der enthaltene Gradle Wrapper. Hauptversionen: AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.10, Compose BOM 2026.06.00 und Material 3 1.5.0-alpha23.

Linux oder macOS:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Release-Variante:

```sh
./gradlew assembleRelease
```

Die Produktionssignierung ist lokal über `key.properties` und einen eigenen Release-Keystore konfiguriert. Beide Dateien werden durch `.gitignore` ausgeschlossen und dürfen weder committed noch in Dokumentation, Logs oder Issues veröffentlicht werden. Sind die lokalen Dateien vorhanden, erzeugt `assembleRelease` eine signierte APK. Ein sauberer öffentlicher Checkout enthält kein privates Signaturmaterial und kann einen unsignierten Release bauen; dies ist der richtige Quellbuild-Pfad für F-Droid, da F-Droid die verteilten Binärdateien selbst signiert.

## Projektstruktur

- `core`: Konvertierungsverträge, Auswahlregeln und Engines.
- `data`: Storage Access Framework, Room-Verlauf und DataStore-Einstellungen.
- `work`: WorkManager, Fortschritt, Abbruch und Worker-Adapter.
- `feature` und `ui`: Compose-Bildschirme sowie Material-3-Expressive-Designsystem.
- `res`: Übersetzungen, Themes, Launcher-Symbole und Android-Konfiguration.
- `src/test` und `src/androidTest`: JVM- und Gerätetests.
- `HANDOFF`: Architektur-, Entscheidungs- und Verifikationsnotizen.

## Veröffentlichungsstatus

Der Quellbaum ist für die Veröffentlichung auf GitHub unter der Apache License 2.0 vorbereitet. Ein eigener Produktionsschlüssel ist ausschließlich lokal konfiguriert und eine signierte Release-APK wurde erzeugt. Private Signaturdateien fehlen absichtlich im öffentlichen Quellbaum.

Die aktuelle signierte APK ist als lokaler Release-Kandidat zu betrachten, nicht als F-Droid-fertiges Paket. Vor einer F-Droid-Einreichung oder einer dauerhaften öffentlichen Paketveröffentlichung bleiben folgende Punkte offen:

1. `com.converty.app` muss durch eine dauerhafte, weltweit eindeutige Anwendungs-ID ersetzt werden. Diese ID wird bereits von einer nicht verbundenen Anwendung verwendet und würde deshalb eine Identitätskollision verursachen.
2. Der öffentliche `main`-Branch ist auf [GitHub](https://github.com/mmgecer/Converty-Android) veröffentlicht. Der Tag `v0.1.0`, der exakt dem eingereichten Quellstand entspricht, muss noch erstellt und gepusht werden.
3. F-Droid-Build-Metadaten mit Versionszuordnung und einer vollständig quellbasierten Build-Anleitung müssen hinzugefügt werden.
4. Der Build muss aus einem sauberen Linux-Checkout in einer F-Droid-ähnlichen Umgebung wiederholt werden und die F-Droid-Scanner- und Reproduzierbarkeitsprüfungen bestehen.
5. Die abschließende Prüfung der Assets und Abhängigkeitslizenzen muss abgeschlossen und dabei ermittelte Hinweise oder Namensnennungen müssen ergänzt werden.
6. Für jede Veröffentlichung muss `versionCode` erhöht werden; Release Notes, Prüfsummen, Screenshots, Datenschutzhinweise und ein Support- oder Issue-Kanal müssen veröffentlicht werden.

Die Apache-2.0-Lizenz, die lokale Produktionssignierung und die Veröffentlichung von `main` sind geklärt. Die Kollision der Anwendungs-ID, der Release-Tag `v0.1.0`, die F-Droid-Metadaten sowie der saubere Linux/F-Droid-Build mit Scannerprüfung bleiben blockierend.

## Lizenz

Converty steht unter der Apache License 2.0. Die vollständigen Bedingungen enthält die Datei [`LICENSE`](LICENSE) im Stammverzeichnis.
