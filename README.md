# Xiaomi and Asus GoStrix Headphone

Application Android 15 (Jetpack Compose) de test pour tenter un traitement audio **global sans root** (Equalizer / BassBoost / Limiter) et afficher un **analyseur de spectre**.

## Objectif

- Appliquer des effets via `AudioEffect` sur la session audio globale (`audioSessionId = 0`) :
  - `Equalizer` (bandes)
  - `BassBoost`
  - `DynamicsProcessing` (limiter simple)
- Afficher un spectre via `Visualizer` (FFT)
- Fournir une UI Compose pour régler les paramètres et diagnostiquer la sortie active

## Limites importantes (sans root)

- Android ne permet pas de retraiter de façon fiable **tout l’audio de toutes les applications**.
- Certaines applis utilisent des chemins audio qui **bypass** le mixeur/effets système.
  - Exemple typique : sorties "Hi-Res", "Direct USB", certains moteurs audio.
- Avec un **dongle USB (casque gaming ASUS)**, il est possible que les effets `AudioEffect` n’aient **aucun effet audible** selon la ROM et l’app.

En clair : c’est un **prototype “best effort”**. S’il faut une solution 100% globale et stable, c’est généralement côté système (souvent root / modules audio).

## Prérequis

- Android Studio récent
- JDK 17 (obligatoire pour AGP/Kotlin)

### Fix JDK (important)

Ce projet échoue si Gradle démarre avec Java 25+.

Le projet force Gradle à utiliser le JDK d’Android Studio via `gradle.properties` :

- `org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr`

Si votre chemin est différent, adaptez-le.

Vérification :

```powershell
.\gradlew.bat -version
```

Vous devez voir une JVM 17.

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

## Utilisation

- Lancer l’app
- En haut, lire la section **Sortie audio détectée (diagnostic)**
  - Vérifier que le dongle apparaît en `USB_HEADSET` ou `USB_DEVICE`
- Activer/désactiver **Global DSP**
- Ajuster l’Equalizer / BassBoost
- Pour l’analyse spectre, accorder la permission `RECORD_AUDIO` quand demandé

## Test avec YouTube Music / SoundCloud / Poweramp

- YouTube Music / SoundCloud : tester si bouger fortement une bande EQ change le son.
- Poweramp : certaines options peuvent contourner les effets Android.
  - Désactiver les modes de sortie “direct/hi-res” si présents.
  - Dans votre cas, vous avez déjà désactivé `OpenSL ES` : c’est bien pour tester l’application.

## Code

- `app/src/main/java/com/example/k60gostrix/audio/GlobalAudioEffectsController.kt`
  - Initialise et pilote `Equalizer`, `BassBoost`, `DynamicsProcessing`
- `app/src/main/java/com/example/k60gostrix/audio/SpectrumAnalyzer.kt`
  - `Visualizer` + conversion FFT -> magnitudes
- `app/src/main/java/com/example/k60gostrix/AudioTuningScreen.kt`
  - UI Compose (sliders + graphe + diagnostic sortie)

## Notes

- L’analyse spectre nécessite `RECORD_AUDIO` car `Visualizer` est souvent protégé et le système demande cette permission.
- Le nombre de bandes EQ dépend de l’implémentation du device.
