# Converty

[English](README.md) | [Türkçe](README.tr.md) | [Deutsch](README.de.md) | [简体中文](README.zh-CN.md) | [العربية](README.ar.md) | [Português](README.pt.md) | Français | [Русский](README.ru.md)

Converty est une application Android de conversion locale de fichiers. Elle s'appuie sur le Storage Access Framework d'Android, WorkManager et une interface Material 3 Expressive. Elle prend en charge les fichiers uniques et les lots, la sélection de pages ou de diapositives, un historique persistant ainsi que l'ouverture et le partage directs des résultats.

Le projet est en développement actif. La conversion PDF vers PPTX préserve l'apparence en plaçant chaque page rendue sous forme d'image dans une diapositive ; le texte n'est donc pas modifiable. Les conversions PPTX et Office vers PDF couvrent une partie pratique des formats sans prétendre reproduire intégralement le rendu de Microsoft Office.

## État actuel

- Identifiant Android : `com.converty.app`
- Version minimale : Android 6.0, API 23
- SDK cible et de compilation : API 37
- Interface : Jetpack Compose avec Material 3 Expressive
- Traitement : local sur l'appareil
- Vérification : 80 tests JVM et 7 tests instrumentation sur appareil Android
- Publication publique : le code source est prêt à être publié sous Apache-2.0 et un release signé localement a été produit ; la préparation F-Droid n'est pas encore terminée.

## Fonctionnalités

- Sélection d'un ou plusieurs fichiers dans le même flux.
- Conversions en arrière-plan avec WorkManager et progression par fichier et par tâche.
- Historique local des tâches terminées, échouées et annulées.
- Ouverture et partage de chaque fichier généré.
- Choix du dossier de sortie par le sélecteur de documents Android.
- Traitement de tous les éléments, des N premiers ou derniers, ou de plages telles que `1,3-5,9`.
- Conservation ou exclusion de plusieurs plages sans modifier la source.
- Réglages de qualité, DPI, WebP sans perte et adaptation d'image selon le format.
- Thèmes système, clair et sombre, couleurs dynamiques et plusieurs palettes.
- Interface en anglais, turc, allemand, chinois simplifié, arabe, portugais, français et russe.
- Icônes adaptive, rondes et monochromes pour Android 13.

## Conversions prises en charge

### Documents et présentations

| Source | Cible | Comportement |
|---|---|---|
| PDF | PPTX | Rend chaque page sélectionnée comme image dans une diapositive ; le texte n'est pas modifiable. |
| PPTX | PDF | Prend en charge les textes, images, remplissages, lignes et formes simples ; les fonctions partielles peuvent produire des avertissements. |
| DOCX | PDF | Prend en charge le contenu de base ; les mises en page Word complexes peuvent différer. |
| XLSX | PDF | Prend en charge le contenu de base des feuilles ; la mise en page avancée et les calculs sont limités. |
| ODT, ODS, ODP | PDF | Rend localement le contenu OpenDocument pris en charge. |

### PDF et images

- PDF vers PNG, JPG, TIFF et miniature JPEG.
- PNG vers JPG ou WebP.
- JPG vers PNG ou WebP.
- WebP vers PNG ou JPG.
- TIFF et BMP vers PNG ou JPG.
- HEIC et HEIF vers PNG ou JPG.
- SVG vers PNG.
- AVIF vers PNG, JPG ou WebP.

La disponibilité des codecs HEIC, HEIF, AVIF, TIFF et WebP peut varier selon la version d'Android et l'appareil.

## Sélection et sortie

Les entrées PDF et PPTX peuvent traiter tous les éléments, les N premiers, les N derniers, des plages conservées, des plages exclues ou plusieurs plages séparées en une seule opération. PDF vers PPTX propose les modes contain, cover et stretch. Les directions PDF concernées acceptent un DPI de 72 à 600. Les noms de sortie sont sécurisés tout en conservant Unicode.

## Confidentialité et stockage

L'application actuelle ne déclare pas la permission Android `INTERNET` et n'inclut ni publicité ni SDK d'analyse. Les conversions sont exécutées sur l'appareil. L'application accède uniquement aux fichiers et dossiers choisis par l'utilisateur dans le sélecteur Android.

L'historique et les paramètres sont stockés localement avec Room et DataStore et sont exclus des sauvegardes cloud et du transfert entre appareils. Le trafic réseau en clair est désactivé. Ces déclarations doivent être réexaminées si des permissions, fonctions réseau, télémétries ou rapports de plantage sont ajoutés.

## Limites connues

- PDF vers PPTX produit des diapositives basées sur des images, pas des objets PowerPoint modifiables.
- PPTX vers PDF ne remplace pas Microsoft PowerPoint, LibreOffice ni un moteur OOXML complet. SmartArt, graphiques, tableaux, groupes, médias, animations, masques, thèmes, polices et effets avancés peuvent être incomplets.
- DOCX, XLSX et OpenDocument utilisent un modèle de rendu local limité et ne garantissent pas une pagination ou une mise en page identique.
- Les fichiers chiffrés, endommagés, excessivement volumineux ou malveillants peuvent être refusés.
- Les documents volumineux peuvent demander beaucoup de mémoire et de temps.
- Sous Android 13 ou ultérieur, les tâches foreground demandent la permission de notification ; son refus ne désactive pas la file interne.

## Compilation depuis les sources

Prérequis : JDK 17, Android SDK Platform 37, Build Tools 37.0.0 et le Gradle Wrapper inclus. Versions principales : AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.10, Compose BOM 2026.06.00 et Material 3 1.5.0-alpha23.

Linux ou macOS :

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows :

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Variante release :

```sh
./gradlew assembleRelease
```

La signature production est configurée localement au moyen de `key.properties` et d'un release keystore dédié. Ces deux fichiers sont exclus par `.gitignore` et ne doivent jamais être commités, copiés dans la documentation ni exposés dans des logs ou des issues. Lorsqu'ils sont présents localement, `assembleRelease` produit un APK signé. Une copie publique propre ne contient aucun élément de signature privé et peut produire un release non signé ; il s'agit du chemin de build source adapté à F-Droid, puisque F-Droid signe lui-même les binaires qu'il distribue.

## Structure du projet

- `core` : contrats, sélection et moteurs de conversion.
- `data` : Storage Access Framework, historique Room et paramètres DataStore.
- `work` : WorkManager, progression, annulation et adaptateurs.
- `feature` et `ui` : écrans Compose et système Material 3 Expressive.
- `res` : traductions, thèmes, icônes et configuration Android.
- `src/test` et `src/androidTest` : tests JVM et sur appareil.
- `HANDOFF` : notes d'architecture, de décision et de vérification.

## État de publication

L'arborescence source est prête à être publiée sur GitHub sous Apache License 2.0. Une clé production dédiée est configurée uniquement en local et un release APK signé a été généré. Les fichiers de signature privés sont volontairement absents de l'arborescence publique.

L'APK signé actuel doit être considéré comme un candidat release local, et non comme un paquet prêt pour F-Droid. Les éléments suivants restent à terminer avant une soumission F-Droid ou une publication publique permanente du paquet :

1. Remplacer `com.converty.app` par un identifiant d'application permanent et unique au niveau mondial. Cet identifiant est déjà utilisé par une application sans rapport avec Converty ; le publier en l'état provoquerait donc une collision d'identité.
2. La branche publique `main` est publiée sur [GitHub](https://github.com/mmgecer/Converty-Android). Il reste à définir et pousser le tag `v0.1.0` correspondant exactement au code source soumis.
3. Ajouter les métadonnées de build F-Droid, notamment la correspondance des versions et une recette de build entièrement fondée sur le code source.
4. Recompiler depuis un checkout Linux propre dans un environnement proche de F-Droid et réussir les vérifications du scanner F-Droid et de reproductibilité.
5. Achever la revue finale des ressources et des licences de dépendances, puis ajouter les mentions ou attributions qu'elle identifie.
6. Augmenter `versionCode` à chaque publication et fournir notes de version, checksums, captures d'écran, déclaration de confidentialité et canal de support ou d'issues.

La licence Apache-2.0, la signature production locale et la publication de `main` sont en place. La collision de l'identifiant d'application, le release tag `v0.1.0`, les métadonnées F-Droid ainsi que la vérification d'un build Linux/F-Droid propre par le scanner restent bloquants.

## Licence

Converty est distribué sous Apache License 2.0. Consultez le fichier [`LICENSE`](LICENSE) à la racine pour les conditions complètes.
