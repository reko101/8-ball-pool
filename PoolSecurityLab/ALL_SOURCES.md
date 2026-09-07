# Pool Security Lab - complete source listing
Every file below is complete from its first line to its last line.

## `settings.gradle`
```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PoolSecurityLab"
include ':app'
```

## `build.gradle`
```groovy
// Top-level build file. Plugin versions are declared here and applied in :app.
plugins {
    id 'com.android.application' version '8.7.3' apply false
    id 'org.jetbrains.kotlin.android' version '2.0.21' apply false
}

tasks.register('clean', Delete) {
    delete rootProject.layout.buildDirectory
}
```

## `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

android.useAndroidX=true
android.nonTransitiveRClass=true

kotlin.code.style=official
```

## `gradle/wrapper/gradle-wrapper.properties`
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## `.gitignore`
```text
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/app/build
/captures
.externalNativeBuild
.cxx
```

## `README.md`
```markdown
# Pool Security Lab

An offline Android laboratory application for a university cybersecurity course.

It contains its **own** eight-ball pool simulation, and a set of screens that use that
simulation to teach how client-held game state can be exposed, how tampering is
detected, and why client-side integrity checking is not a trust boundary.

## Scope and boundaries

This application:

* is completely offline and declares **no permissions at all** (no `INTERNET`,
  no `SYSTEM_ALERT_WINDOW`, no accessibility service, no `QUERY_ALL_PACKAGES`);
* never reads, writes, patches, hooks, injects into or overlays any other application;
* contains no injection, no hooking, no APK patching, no instrumentation scripts,
  no anti-cheat bypass, no authentication bypass and no payment bypass;
* uses **invented** addresses on the memory screen that correspond to nothing real;
* uses a simulated coin balance that buys nothing and has no value.

Every value the app analyses belongs to its own process.

## Screens

| Screen | Purpose |
| --- | --- |
| **Game** | Playable offline eight-ball simulation: table, cue ball, 15 numbered balls, six pockets, turns, direction, power, physics, collisions, pocket detection, win/loss, reset. |
| **State** | Live inspector over the app's own `GameState`: name, logical path, type, value, simulated address. |
| **Memory** | A drawing of a hypothetical struct layout (`0x1000 GameState`, `0x1040 PlayerState`, ...). Tap a field for a teaching card: type, example value, simulated address, description, risk, recommended defence. |
| **Tamper** | Seal a baseline, apply a simulated unauthorised modification to this app's own fields, and detect it. Shows original value, current value, detection time and integrity status. Includes a control experiment using the sanctioned mutation path. |
| **Report** | The written security report: manipulation, client vs server authority, virtual currency, conceptual memory tampering, anti-cheat detection, defensive programming. |

## Requirements

* Android Studio Ladybug (2024.2.1) or newer
* JDK 17 (bundled with Android Studio)
* Android Gradle Plugin 8.7.3, Gradle 8.9, Kotlin 2.0.21
* `compileSdk` / `targetSdk` 35 (Android 15), `minSdk` 26

## Build

```
File > Open   ->  select the PoolSecurityLab folder
Tools > SDK Manager -> install "Android 15.0 (API 35)" SDK Platform + Build-Tools 35
Sync Project with Gradle Files
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
Release APK (R8 enabled, signed with the local debug key for lab use only):
`app/build/outputs/apk/release/app-release.apk`

Command line:

```
./gradlew assembleDebug
./gradlew assembleRelease
```

If the Gradle wrapper JAR is missing, either let Android Studio regenerate it on
first sync, or run `gradle wrapper --gradle-version 8.9` once with a local Gradle.

## Install

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

There is no runtime permission prompt, because the app requests no permissions.

## Test on Android 15

1. Create an AVD with system image **API 35**, or use a physical Android 15 device.
2. Install and launch. Confirm the app opens on the Game screen with no crash.
3. Confirm the layout is not hidden behind the status bar or the gesture bar
   (edge-to-edge insets are applied in `MainActivity`).
4. Enable airplane mode and repeat every screen; behaviour must be identical.
5. Play a shot, then open **State** and confirm the values changed.
6. On **Tamper**: `SEAL BASELINE` -> `Simulate balance overwrite` -> `RUN INTEGRITY CHECK`.
   Expect `WARNING: GAME-STATE INTEGRITY VIOLATION DETECTED` with 1000 -> 999999.
7. Press `RESTORE TRUSTED STATE`, check again, expect `INTEGRITY: OK`.
8. Press `Sanctioned change: award 25 coins`, check again, expect `INTEGRITY: OK`
   even though the balance changed. This is the point of the exercise.
9. Rotate the device and confirm no crash.

## Architecture

```
core/     GameHolder (single shared state), LabApplication
game/     TableSpec, Ball, GameState, PhysicsEngine, PoolGame, PoolTableView, GameFragment
security/ SimulatedAddresses, StateVariable, StateInspector, IntegrityGuard,
          DefenseCatalog, ReportContent, and the four laboratory fragments
```

`GameHolder.game.state` is the single source of truth. The table renderer and every
security screen read the same object, so nothing on the lab screens is staged or faked.
```

## `app/build.gradle`
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.university.poolseclab'
    compileSdk 35

    defaultConfig {
        applicationId "com.university.poolseclab"
        minSdk 26
        targetSdk 35
        versionCode 1
        versionName "1.0"

        // No test runner dependencies are required for this laboratory build.
        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        debug {
            minifyEnabled false
        }
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            // Signed with the local debug key so that a release APK can be built
            // and installed for the laboratory session without creating a keystore.
            // Never ship a production application signed with the debug key.
            signingConfig signingConfigs.debug
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
    }

    packaging {
        resources {
            excludes += ['META-INF/DEPENDENCIES', 'META-INF/LICENSE*', 'META-INF/NOTICE*']
        }
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.15.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.2.0'
    implementation 'androidx.fragment:fragment-ktx:1.8.5'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.8.7'
}
```

## `app/proguard-rules.pro`
```text
# Pool Security Lab - R8 / ProGuard configuration
#
# The application has no reflection-based entry points other than the ones the
# Android framework itself resolves, so the default optimised rules are enough.
# The rules below only keep what the framework instantiates by name.

-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep the line numbers so that a stack trace collected during the laboratory
# session can still be mapped back to the source with the generated mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Educational note:
# Shrinking and obfuscation raise the cost of static reverse engineering, but
# they are NOT a security control on their own. Every value that the client
# owns can still be located at runtime. See the in-app Security Report screen.
```

## `app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!--
        SECURITY / SCOPE NOTE
        =====================
        This laboratory application declares NO permissions at all.

        In particular it does NOT declare:
          android.permission.INTERNET            - the app is fully offline
          android.permission.QUERY_ALL_PACKAGES  - it never inspects other apps
          android.permission.SYSTEM_ALERT_WINDOW - it never draws over other apps
          BIND_ACCESSIBILITY_SERVICE             - it never observes other apps
          MANAGE_EXTERNAL_STORAGE / READ_LOGS    - not needed

        Every value the app analyses belongs to its own process. It never reads,
        writes, patches, hooks or overlays any other application.
    -->

    <application
        android:name=".core.LabApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:usesCleartextTraffic="false"
        android:theme="@style/Theme.PoolSecurityLab">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|uiMode"
            android:label="@string/app_name"
            android:theme="@style/Theme.PoolSecurityLab">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

## `app/src/main/res/xml/backup_rules.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
    Nothing in this laboratory application is backed up. All state is
    generated at runtime and is intentionally not persisted off device.
-->
<full-backup-content>
    <exclude domain="root" />
    <exclude domain="file" />
    <exclude domain="database" />
    <exclude domain="sharedpref" />
</full-backup-content>
```

## `app/src/main/res/xml/data_extraction_rules.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
    </device-transfer>
</data-extraction-rules>
```

## `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

## `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

## `app/src/main/res/values/colors.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Table -->
    <color name="felt_green">#1B6B45</color>
    <color name="felt_green_dark">#125234</color>
    <color name="rail_brown">#4E342E</color>
    <color name="rail_brown_dark">#33211B</color>
    <color name="pocket_black">#0C0C0C</color>
    <color name="cue_white">#F7F7F2</color>

    <!-- Laboratory surfaces -->
    <color name="lab_bg">#0E1216</color>
    <color name="lab_surface">#181F26</color>
    <color name="lab_surface_alt">#212B34</color>
    <color name="lab_text">#E7ECF2</color>
    <color name="lab_text_dim">#93A1AE</color>
    <color name="lab_accent">#4FC3F7</color>

    <!-- Status -->
    <color name="status_ok">#2E7D32</color>
    <color name="status_warn">#F9A825</color>
    <color name="status_alert">#C62828</color>

    <!-- Launcher -->
    <color name="ic_launcher_background">#125234</color>
</resources>
```

## `app/src/main/res/values/dimens.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="pad_screen">14dp</dimen>
    <dimen name="pad_card">12dp</dimen>
    <dimen name="pad_small">6dp</dimen>
    <dimen name="card_radius">10dp</dimen>
    <dimen name="text_title">18sp</dimen>
    <dimen name="text_section">15sp</dimen>
    <dimen name="text_body">13sp</dimen>
    <dimen name="text_mono">12sp</dimen>
    <dimen name="text_small">11sp</dimen>
</resources>
```

## `app/src/main/res/values/strings.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Pool Security Lab</string>

    <!-- Navigation -->
    <string name="nav_game">Game</string>
    <string name="nav_state">State</string>
    <string name="nav_memory">Memory</string>
    <string name="nav_tamper">Tamper</string>
    <string name="nav_report">Report</string>

    <!-- Game screen -->
    <string name="btn_shoot">SHOOT</string>
    <string name="btn_new_game">NEW GAME</string>
    <string name="label_power">Power</string>
    <string name="game_hint">Drag anywhere on the table to aim. Set the power, then press SHOOT.</string>

    <!-- State inspector -->
    <string name="state_title">Live game state inspector</string>
    <string name="state_sub">Every value below is read from the GameState object of THIS application. No other process is inspected.</string>
    <string name="btn_refresh">REFRESH VALUES</string>

    <!-- Memory map -->
    <string name="memory_title">Simulated memory map</string>
    <string name="memory_sub">Tap any field to open its teaching card.</string>
    <string name="memory_disclaimer">The addresses on this screen are invented for teaching only. They are a drawing of how a game state COULD be laid out. They are not real addresses, not offsets, and they do not correspond to any commercial product.</string>

    <!-- Tamper lab -->
    <string name="tamper_title">Tamper detection demonstration</string>
    <string name="tamper_sub">These buttons write directly into the fields of this application, bypassing its own sanctioned setters. That models what an attacker with process memory access would do. Nothing outside this app is touched.</string>
    <string name="btn_seal">SEAL BASELINE</string>
    <string name="btn_verify">RUN INTEGRITY CHECK</string>
    <string name="btn_restore">RESTORE TRUSTED STATE</string>
    <string name="btn_t_balance">Simulate balance overwrite</string>
    <string name="btn_t_score">Simulate score overwrite</string>
    <string name="btn_t_teleport">Simulate cue ball teleport</string>
    <string name="btn_t_speed">Simulate impossible velocity</string>
    <string name="btn_t_replay">Simulate replay or rewind</string>
    <string name="btn_t_legit">Sanctioned change: award 25 coins</string>
    <string name="defenses_title">Integrity techniques in this build</string>

    <!-- Report -->
    <string name="report_title">Security report</string>

    <!-- Shared -->
    <string name="dlg_close">CLOSE</string>
    <string name="lbl_name">Variable</string>
    <string name="lbl_type">Data type</string>
    <string name="lbl_value">Example value</string>
    <string name="lbl_address">Simulated address</string>
    <string name="lbl_desc">Description</string>
    <string name="lbl_risk">Why exposing this is a risk</string>
    <string name="lbl_defense">Recommended protection</string>
</resources>
```

## `app/src/main/res/values/themes.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="Theme.PoolSecurityLab" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/felt_green</item>
        <item name="colorOnPrimary">#FFFFFFFF</item>
        <item name="colorSecondary">@color/lab_accent</item>
        <item name="colorSurface">@color/lab_surface</item>
        <item name="colorOnSurface">@color/lab_text</item>
        <item name="android:colorBackground">@color/lab_bg</item>
        <item name="android:windowBackground">@color/lab_bg</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>

    <style name="LabTitle">
        <item name="android:textSize">@dimen/text_title</item>
        <item name="android:textStyle">bold</item>
        <item name="android:textColor">@color/lab_text</item>
    </style>

    <style name="LabSection">
        <item name="android:textSize">@dimen/text_section</item>
        <item name="android:textStyle">bold</item>
        <item name="android:textColor">@color/lab_accent</item>
    </style>

    <style name="LabBody">
        <item name="android:textSize">@dimen/text_body</item>
        <item name="android:textColor">@color/lab_text</item>
        <item name="android:lineSpacingExtra">3dp</item>
    </style>

    <style name="LabDim">
        <item name="android:textSize">@dimen/text_small</item>
        <item name="android:textColor">@color/lab_text_dim</item>
    </style>

    <style name="LabMono">
        <item name="android:textSize">@dimen/text_mono</item>
        <item name="android:typeface">monospace</item>
        <item name="android:textColor">@color/lab_accent</item>
    </style>

</resources>
```

## `app/src/main/res/layout/activity_main.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/lab_bg">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/container"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/lab_surface"
        app:labelVisibilityMode="labeled"
        app:menu="@menu/bottom_nav_menu" />

</LinearLayout>
```

## `app/src/main/res/layout/dialog_variable.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="@dimen/pad_screen">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_name" />
        <TextView android:id="@+id/dName" style="@style/LabBody" android:layout_width="match_parent" android:layout_height="wrap_content" android:textStyle="bold" android:paddingBottom="@dimen/pad_small" android:text="-" />

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_type" />
        <TextView android:id="@+id/dType" style="@style/LabBody" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="@dimen/pad_small" android:text="-" />

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_value" />
        <TextView android:id="@+id/dValue" style="@style/LabMono" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="@dimen/pad_small" android:text="-" />

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_address" />
        <TextView android:id="@+id/dAddress" style="@style/LabMono" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="@dimen/pad_small" android:text="-" />

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_desc" />
        <TextView android:id="@+id/dDesc" style="@style/LabBody" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="@dimen/pad_small" android:text="-" />

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_risk" />
        <TextView android:id="@+id/dRisk" style="@style/LabBody" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingBottom="@dimen/pad_small" android:text="-" />

        <TextView style="@style/LabDim" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lbl_defense" />
        <TextView android:id="@+id/dDefense" style="@style/LabBody" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="-" />

    </LinearLayout>
</ScrollView>
```

## `app/src/main/res/layout/fragment_game.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/lab_bg"
    android:padding="@dimen/pad_small">

    <TextView
        android:id="@+id/turnText"
        style="@style/LabTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingStart="@dimen/pad_small"
        android:paddingEnd="@dimen/pad_small"
        android:text="Player 1" />

    <TextView
        android:id="@+id/statusText"
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingStart="@dimen/pad_small"
        android:paddingEnd="@dimen/pad_small"
        android:paddingBottom="@dimen/pad_small"
        android:text="Break shot." />

    <com.university.poolseclab.game.PoolTableView
        android:id="@+id/poolView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingTop="@dimen/pad_small">

        <TextView
            style="@style/LabBody"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:paddingStart="@dimen/pad_small"
            android:paddingEnd="@dimen/pad_small"
            android:text="@string/label_power" />

        <SeekBar
            android:id="@+id/powerBar"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:max="100"
            android:progress="55" />

        <TextView
            android:id="@+id/powerValue"
            style="@style/LabMono"
            android:layout_width="48dp"
            android:layout_height="wrap_content"
            android:gravity="end"
            android:paddingEnd="@dimen/pad_small"
            android:text="55%" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingTop="@dimen/pad_small">

        <Button
            android:id="@+id/shootBtn"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="@dimen/pad_small"
            android:text="@string/btn_shoot" />

        <Button
            android:id="@+id/resetBtn"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/btn_new_game" />
    </LinearLayout>

    <TextView
        android:id="@+id/hintText"
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/pad_small"
        android:text="@string/game_hint" />

</LinearLayout>
```

## `app/src/main/res/layout/fragment_memory.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/lab_bg"
    android:padding="@dimen/pad_screen">

    <TextView
        style="@style/LabTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/memory_title" />

    <TextView
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_header"
        android:padding="@dimen/pad_card"
        android:layout_marginTop="@dimen/pad_small"
        android:text="@string/memory_disclaimer" />

    <TextView
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingTop="@dimen/pad_small"
        android:paddingBottom="@dimen/pad_small"
        android:text="@string/memory_sub" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false" />

</LinearLayout>
```

## `app/src/main/res/layout/fragment_report.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/lab_bg"
    android:padding="@dimen/pad_screen">

    <TextView
        style="@style/LabTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingBottom="@dimen/pad_small"
        android:text="@string/report_title" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false" />

</LinearLayout>
```

## `app/src/main/res/layout/fragment_state.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/lab_bg"
    android:padding="@dimen/pad_screen">

    <TextView
        style="@style/LabTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/state_title" />

    <TextView
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingTop="@dimen/pad_small"
        android:paddingBottom="@dimen/pad_small"
        android:text="@string/state_sub" />

    <Button
        android:id="@+id/refreshBtn"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/btn_refresh" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:paddingTop="@dimen/pad_small"
        android:clipToPadding="false" />

</LinearLayout>
```

## `app/src/main/res/layout/fragment_tamper.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/lab_bg"
    android:padding="@dimen/pad_screen">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <TextView
            style="@style/LabTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/tamper_title" />

        <TextView
            style="@style/LabDim"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_header"
            android:padding="@dimen/pad_card"
            android:layout_marginTop="@dimen/pad_small"
            android:text="@string/tamper_sub" />

        <TextView
            android:id="@+id/statusBanner"
            style="@style/LabTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card_alt"
            android:padding="@dimen/pad_card"
            android:layout_marginTop="@dimen/pad_card"
            android:text="INTEGRITY: NOT CHECKED" />

        <TextView
            android:id="@+id/reportText"
            style="@style/LabMono"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card"
            android:padding="@dimen/pad_card"
            android:layout_marginTop="@dimen/pad_small"
            android:textColor="@color/lab_text"
            android:text="Press SEAL BASELINE, then apply a simulated modification, then press RUN INTEGRITY CHECK." />

        <TextView
            style="@style/LabSection"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingTop="@dimen/pad_card"
            android:paddingBottom="@dimen/pad_small"
            android:text="Controls" />

        <Button android:id="@+id/sealBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_seal" />
        <Button android:id="@+id/verifyBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_verify" />
        <Button android:id="@+id/restoreBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_restore" />

        <TextView
            style="@style/LabSection"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingTop="@dimen/pad_card"
            android:paddingBottom="@dimen/pad_small"
            android:text="Simulated unauthorised modifications (inside this app only)" />

        <Button android:id="@+id/tBalanceBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_t_balance" />
        <Button android:id="@+id/tScoreBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_t_score" />
        <Button android:id="@+id/tTeleportBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_t_teleport" />
        <Button android:id="@+id/tSpeedBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_t_speed" />
        <Button android:id="@+id/tReplayBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_t_replay" />

        <TextView
            style="@style/LabSection"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingTop="@dimen/pad_card"
            android:paddingBottom="@dimen/pad_small"
            android:text="Control experiment" />

        <Button android:id="@+id/tLegitBtn" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/btn_t_legit" />

        <TextView
            style="@style/LabSection"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingTop="@dimen/pad_card"
            android:paddingBottom="@dimen/pad_small"
            android:text="@string/defenses_title" />

        <TextView
            android:id="@+id/defensesText"
            style="@style/LabBody"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card"
            android:padding="@dimen/pad_card"
            android:layout_marginBottom="@dimen/pad_card"
            android:text="-" />

    </LinearLayout>
</ScrollView>
```

## `app/src/main/res/layout/item_memory_field.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_card"
    android:padding="@dimen/pad_card"
    android:layout_marginBottom="4dp"
    android:clickable="true"
    android:focusable="true">

    <TextView
        android:id="@+id/fieldLine"
        style="@style/LabMono"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="0x1048  score" />

    <TextView
        android:id="@+id/fieldMeta"
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Int = 0" />

</LinearLayout>
```

## `app/src/main/res/layout/item_memory_header.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_header"
    android:padding="@dimen/pad_card"
    android:layout_marginTop="@dimen/pad_card"
    android:layout_marginBottom="@dimen/pad_small">

    <TextView
        android:id="@+id/hdrLine"
        style="@style/LabMono"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:text="0x1000  GameState" />

    <TextView
        android:id="@+id/hdrNote"
        style="@style/LabDim"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="note" />

</LinearLayout>
```

## `app/src/main/res/layout/item_report.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_card"
    android:padding="@dimen/pad_card"
    android:layout_marginBottom="@dimen/pad_small">

    <TextView
        android:id="@+id/secTitle"
        style="@style/LabSection"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingBottom="@dimen/pad_small"
        android:text="Title" />

    <TextView
        android:id="@+id/secBody"
        style="@style/LabBody"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Body" />

</LinearLayout>
```

## `app/src/main/res/layout/item_state_var.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_card"
    android:padding="@dimen/pad_card"
    android:layout_marginBottom="@dimen/pad_small">

    <TextView
        android:id="@+id/varName"
        style="@style/LabBody"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:text="Name" />

    <TextView
        android:id="@+id/varPath"
        style="@style/LabMono"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="GameState" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingTop="@dimen/pad_small">

        <TextView
            android:id="@+id/varType"
            style="@style/LabDim"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Type" />

        <TextView
            android:id="@+id/varAddress"
            style="@style/LabDim"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:typeface="monospace"
            android:text="0x0000" />
    </LinearLayout>

    <TextView
        android:id="@+id/varValue"
        style="@style/LabBody"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_card_alt"
        android:padding="@dimen/pad_small"
        android:layout_marginTop="@dimen/pad_small"
        android:typeface="monospace"
        android:text="value" />

</LinearLayout>
```

## `app/src/main/res/menu/bottom_nav_menu.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_game"   android:icon="@drawable/ic_game"   android:title="@string/nav_game" />
    <item android:id="@+id/nav_state"  android:icon="@drawable/ic_state"  android:title="@string/nav_state" />
    <item android:id="@+id/nav_memory" android:icon="@drawable/ic_memory" android:title="@string/nav_memory" />
    <item android:id="@+id/nav_tamper" android:icon="@drawable/ic_shield" android:title="@string/nav_tamper" />
    <item android:id="@+id/nav_report" android:icon="@drawable/ic_report" android:title="@string/nav_report" />
</menu>
```

## `app/src/main/res/drawable/bg_card.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/lab_surface" />
    <corners android:radius="@dimen/card_radius" />
    <stroke android:width="1dp" android:color="#2A3540" />
</shape>
```

## `app/src/main/res/drawable/bg_card_alt.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/lab_surface_alt" />
    <corners android:radius="@dimen/card_radius" />
</shape>
```

## `app/src/main/res/drawable/bg_header.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#0A2A3A" />
    <corners android:radius="6dp" />
    <stroke android:width="1dp" android:color="@color/lab_accent" />
</shape>
```

## `app/src/main/res/drawable/ic_game.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path android:fillColor="@android:color/white"
        android:pathData="M12,3 C16.97,3 21,7.03 21,12 C21,16.97 16.97,21 12,21 C7.03,21 3,16.97 3,12 C3,7.03 7.03,3 12,3 Z M12,5 C8.13,5 5,8.13 5,12 C5,15.87 8.13,19 12,19 C15.87,19 19,15.87 19,12 C19,8.13 15.87,5 12,5 Z" />
    <path android:fillColor="@android:color/white"
        android:pathData="M15,12 C15,13.66 13.66,15 12,15 C10.34,15 9,13.66 9,12 C9,10.34 10.34,9 12,9 C13.66,9 15,10.34 15,12 Z" />
</vector>
```

## `app/src/main/res/drawable/ic_launcher_foreground.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#101010"
        android:pathData="M84,54 C84,70.57 70.57,84 54,84 C37.43,84 24,70.57 24,54 C24,37.43 37.43,24 54,24 C70.57,24 84,37.43 84,54 Z" />
    <path
        android:fillColor="#F7F7F2"
        android:pathData="M66,54 C66,60.63 60.63,66 54,66 C47.37,66 42,60.63 42,54 C42,47.37 47.37,42 54,42 C60.63,42 66,47.37 66,54 Z" />
    <path
        android:fillColor="#4FC3F7"
        android:pathData="M60,50 C60,53.31 57.31,56 54,56 C50.69,56 48,53.31 48,50 C48,46.69 50.69,44 54,44 C57.31,44 60,46.69 60,50 Z" />
</vector>
```

## `app/src/main/res/drawable/ic_memory.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path android:fillColor="@android:color/white"
        android:pathData="M5,5 H19 V19 H5 Z M7,7 V17 H17 V7 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M9,2 H11 V5 H9 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M13,2 H15 V5 H13 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M9,19 H11 V22 H9 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M13,19 H15 V22 H13 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M9,9 H15 V15 H9 Z" />
</vector>
```

## `app/src/main/res/drawable/ic_report.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path android:fillColor="@android:color/white"
        android:pathData="M6,2 H14 L19,7 V22 H6 Z M8,4 V20 H17 V8.5 H12.5 V4 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M9.5,11 H15.5 V12.5 H9.5 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M9.5,14.5 H15.5 V16 H9.5 Z" />
</vector>
```

## `app/src/main/res/drawable/ic_shield.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path android:fillColor="@android:color/white"
        android:pathData="M12,2 L20,5 L20,11.5 C20,16.4 16.6,20.4 12,22 C7.4,20.4 4,16.4 4,11.5 L4,5 Z M12,4.2 L6,6.4 L6,11.5 C6,15.2 8.4,18.4 12,19.8 C15.6,18.4 18,15.2 18,11.5 L18,6.4 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M11,8 H13 V13 H11 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M11,14.5 H13 V16.5 H11 Z" />
</vector>
```

## `app/src/main/res/drawable/ic_state.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path android:fillColor="@android:color/white" android:pathData="M3,5 H21 V7.5 H3 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M3,10.75 H21 V13.25 H3 Z" />
    <path android:fillColor="@android:color/white" android:pathData="M3,16.5 H15 V19 H3 Z" />
</vector>
```

## `app/src/main/java/com/university/poolseclab/MainActivity.kt`
```kotlin
package com.university.poolseclab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.ActivityMainBinding
import com.university.poolseclab.game.GameFragment
import com.university.poolseclab.security.MemoryMapFragment
import com.university.poolseclab.security.ReportFragment
import com.university.poolseclab.security.SecurityLabFragment
import com.university.poolseclab.security.TamperLabFragment

/**
 * Hosts the five laboratory screens.
 *
 * Android 15 note: applications targeting SDK 35 are laid out edge to edge by
 * default, so the window insets are applied here as padding. Without this the
 * content would sit under the status bar and the gesture bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GameHolder.ensureInitialised()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_game -> GameFragment()
                R.id.nav_state -> SecurityLabFragment()
                R.id.nav_memory -> MemoryMapFragment()
                R.id.nav_tamper -> TamperLabFragment()
                R.id.nav_report -> ReportFragment()
                else -> GameFragment()
            }
            show(fragment)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_game
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
```

## `app/src/main/java/com/university/poolseclab/game/Ball.kt`
```kotlin
package com.university.poolseclab.game

import kotlin.math.hypot

/** Which half of the object balls a ball belongs to. */
enum class BallGroup { UNASSIGNED, SOLIDS, STRIPES, EIGHT }

/**
 * One ball on the simulated table.
 *
 * Number 0 is the cue ball, 1 to 7 are solids, 8 is the black ball and
 * 9 to 15 are stripes.
 *
 * Note for the laboratory: the fields below are ordinary mutable properties.
 * That is deliberate. The Security Lab screens show what happens when a value
 * like this is the only copy of the truth and anybody who can reach the process
 * can change it.
 */
class Ball(val number: Int, var x: Float, var y: Float) {

    var vx: Float = 0f
    var vy: Float = 0f
    var potted: Boolean = false

    val isCue: Boolean get() = number == 0

    val group: BallGroup
        get() = when (number) {
            0 -> BallGroup.UNASSIGNED
            in 1..7 -> BallGroup.SOLIDS
            8 -> BallGroup.EIGHT
            else -> BallGroup.STRIPES
        }

    fun speed(): Float = hypot(vx, vy)

    fun stop() {
        vx = 0f
        vy = 0f
    }

    fun isMoving(): Boolean = !potted && (vx != 0f || vy != 0f)
}
```

## `app/src/main/java/com/university/poolseclab/game/GameFragment.kt`
```kotlin
package com.university.poolseclab.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.FragmentGameBinding
import java.util.Locale

/** The playable table. */
class GameFragment : Fragment() {

    private var binding: FragmentGameBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentGameBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        val game = GameHolder.game

        b.poolView.attach(game)

        b.poolView.onAimChanged = { _ ->
            binding?.let { bb -> updateHeader(bb) }
        }

        b.poolView.onShotFinished = {
            val message = game.onBallsStopped()
            // Every legitimate change to the state re-seals the integrity baseline.
            GameHolder.guard.seal()
            binding?.let {
                it.statusText.text = message
                updateHeader(it)
                it.poolView.invalidate()
            }
        }

        b.powerBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                game.state.shotPower = progress / 100f
                binding?.powerValue?.text = String.format(Locale.US, "%d%%", progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        b.powerBar.progress = (game.state.shotPower * 100f).toInt().coerceIn(5, 100)

        b.shootBtn.setOnClickListener {
            if (!game.canShoot()) return@setOnClickListener
            game.shoot(b.powerBar.progress / 100f, game.state.shotAngleDeg)
            b.poolView.kick()
            updateHeader(b)
        }

        b.resetBtn.setOnClickListener {
            game.newGame()
            GameHolder.guard.seal()
            b.statusText.text = game.state.statusMessage
            b.poolView.invalidate()
            updateHeader(b)
        }

        b.statusText.text = game.state.statusMessage
        updateHeader(b)
    }

    override fun onResume() {
        super.onResume()
        binding?.let {
            updateHeader(it)
            it.poolView.invalidate()
        }
    }

    private fun updateHeader(b: FragmentGameBinding) {
        val s = GameHolder.game.state
        val groupText = when (s.groupOf(s.currentTurn)) {
            BallGroup.SOLIDS -> "solids"
            BallGroup.STRIPES -> "stripes"
            else -> "open table"
        }
        val header = when (s.matchPhase) {
            MatchPhase.PLAYER_ONE_WINS -> "Player 1 wins"
            MatchPhase.PLAYER_TWO_WINS -> "Player 2 wins"
            else -> String.format(
                Locale.US,
                "Player %d  (%s)   %d - %d   angle %.0f deg   coins %d",
                s.currentTurn, groupText, s.playerOneScore, s.playerTwoScore,
                s.shotAngleDeg, s.virtualBalance
            )
        }
        b.turnText.text = header
    }

    override fun onDestroyView() {
        binding?.poolView?.onShotFinished = null
        binding?.poolView?.onAimChanged = null
        binding = null
        super.onDestroyView()
    }
}
```

## `app/src/main/java/com/university/poolseclab/game/GameState.kt`
```kotlin
package com.university.poolseclab.game

/** Phase of the current match. */
enum class MatchPhase { READY, BALLS_MOVING, PLAYER_ONE_WINS, PLAYER_TWO_WINS }

/**
 * The complete client side state of a match.
 *
 * This single object is the subject of the whole laboratory. Everything the
 * Security Lab, the memory map and the tamper demonstration show is read from
 * here, and nowhere else. No other process is ever inspected.
 */
class GameState {

    // ---- Identity -------------------------------------------------------
    var playerId: String = "STUDENT-LAB-0001"
    var playerName: String = "Lab Player 1"
    var matchId: String = "MATCH-000000"

    // ---- Turn and groups ------------------------------------------------
    var currentTurn: Int = 1                       // 1 or 2
    var playerOneGroup: BallGroup = BallGroup.UNASSIGNED
    var playerTwoGroup: BallGroup = BallGroup.UNASSIGNED

    // ---- Scores and virtual economy -------------------------------------
    var playerOneScore: Int = 0
    var playerTwoScore: Int = 0
    var virtualBalance: Long = 1000L               // simulated in-app coins

    // ---- Last shot ------------------------------------------------------
    var shotPower: Float = 0.55f                   // 0.0 .. 1.0
    var shotAngleDeg: Float = 0f
    var shotCount: Int = 0

    // ---- Match bookkeeping ----------------------------------------------
    var matchPhase: MatchPhase = MatchPhase.READY
    var statusMessage: String = "Break shot."

    /**
     * Monotonically increasing counter. Every sanctioned mutation increments it.
     * A value that goes backwards is the signal used by the replay detector.
     */
    var stateSequence: Long = 0L

    var lastUpdateMillis: Long = System.currentTimeMillis()

    /** Index 0 is always the cue ball; indices 1..15 are the object balls. */
    val balls: MutableList<Ball> = ArrayList(16)

    val cueBall: Ball get() = balls[0]

    fun groupOf(player: Int): BallGroup =
        if (player == 1) playerOneGroup else playerTwoGroup

    fun scoreOf(player: Int): Int =
        if (player == 1) playerOneScore else playerTwoScore

    fun remainingIn(group: BallGroup): Int {
        if (group == BallGroup.UNASSIGNED) return 7
        var n = 0
        for (b in balls) {
            if (!b.potted && b.group == group) n++
        }
        return n
    }

    fun ballByNumber(number: Int): Ball? = balls.firstOrNull { it.number == number }

    fun maxBallSpeed(): Float {
        var m = 0f
        for (b in balls) {
            if (!b.potted && b.speed() > m) m = b.speed()
        }
        return m
    }
}
```

## `app/src/main/java/com/university/poolseclab/game/PhysicsEngine.kt`
```kotlin
package com.university.poolseclab.game

import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A small fixed step physics solver: straight line motion, rolling friction,
 * equal mass ball to ball impacts, cushion bounces and pocket capture.
 *
 * The solver is deterministic. Given the same starting state and the same shot
 * it always produces the same result, which is what makes it usable as a
 * teaching bench for state integrity.
 */
class PhysicsEngine(private val state: GameState) {

    /** Numbers of the balls pocketed during the shot currently being simulated. */
    val pottedThisShot: MutableList<Int> = ArrayList()

    var cueBallPotted: Boolean = false
        private set

    /** Number of the first object ball the cue ball touched, or -1. */
    var firstContact: Int = -1
        private set

    fun resetShotRecord() {
        pottedThisShot.clear()
        cueBallPotted = false
        firstContact = -1
    }

    fun anyMoving(): Boolean {
        for (b in state.balls) {
            if (b.isMoving()) return true
        }
        return false
    }

    /** Advances the simulation by [dtSeconds] of wall clock time. */
    fun step(dtSeconds: Float) {
        var remaining = dtSeconds.coerceIn(0f, 0.05f)
        val h = 1f / 480f
        while (remaining > 0f) {
            val slice = min(remaining, h)
            substep(slice)
            remaining -= slice
        }
        state.lastUpdateMillis = System.currentTimeMillis()
    }

    private fun substep(h: Float) {
        integrate(h)
        resolveBallCollisions()
        resolvePocketsAndCushions()
    }

    private fun integrate(h: Float) {
        for (b in state.balls) {
            if (b.potted) continue
            b.x += b.vx * h
            b.y += b.vy * h

            val sp = hypot(b.vx, b.vy)
            if (sp > 0f) {
                val newSp = sp - TableSpec.FRICTION * h
                if (newSp <= TableSpec.MIN_SPEED) {
                    b.stop()
                } else {
                    val k = newSp / sp
                    b.vx *= k
                    b.vy *= k
                }
            }
        }
    }

    private fun resolveBallCollisions() {
        val list = state.balls
        val diameter = 2f * TableSpec.BALL_R
        for (i in list.indices) {
            val a = list[i]
            if (a.potted) continue
            for (j in i + 1 until list.size) {
                val b = list[j]
                if (b.potted) continue

                var dx = b.x - a.x
                var dy = b.y - a.y
                var dist = hypot(dx, dy)
                if (dist >= diameter) continue

                if (dist == 0f) {
                    // Perfectly coincident centres: nudge them apart deterministically.
                    dx = 0.001f
                    dy = 0f
                    dist = 0.001f
                }

                val nx = dx / dist
                val ny = dy / dist

                // Positional correction so the balls stop overlapping.
                val overlap = (diameter - dist) * 0.5f
                a.x -= nx * overlap
                a.y -= ny * overlap
                b.x += nx * overlap
                b.y += ny * overlap

                // Relative velocity along the contact normal.
                val rvx = b.vx - a.vx
                val rvy = b.vy - a.vy
                val sep = rvx * nx + rvy * ny
                if (sep >= 0f) continue

                if (firstContact == -1 && (a.isCue || b.isCue)) {
                    firstContact = if (a.isCue) b.number else a.number
                }

                // Equal masses, so the impulse splits evenly.
                val impulse = -(1f + TableSpec.BALL_RESTITUTION) * sep / 2f
                a.vx -= nx * impulse
                a.vy -= ny * impulse
                b.vx += nx * impulse
                b.vy += ny * impulse
            }
        }
    }

    private fun resolvePocketsAndCushions() {
        for (b in state.balls) {
            if (b.potted) continue

            // Pocket capture is tested before the cushion so that a ball rolling
            // along a rail can still drop into the mouth of a pocket.
            var captured = false
            for (p in TableSpec.POCKETS) {
                val dx = b.x - p[0]
                val dy = b.y - p[1]
                if (hypot(dx, dy) < TableSpec.POCKET_R) {
                    captured = true
                    break
                }
            }
            if (captured) {
                b.potted = true
                b.stop()
                if (b.isCue) cueBallPotted = true else pottedThisShot.add(b.number)
                continue
            }

            val r = TableSpec.BALL_R
            if (b.x < r) {
                b.x = r
                if (b.vx < 0f) b.vx = -b.vx * TableSpec.CUSHION_RESTITUTION
            } else if (b.x > TableSpec.WIDTH - r) {
                b.x = TableSpec.WIDTH - r
                if (b.vx > 0f) b.vx = -b.vx * TableSpec.CUSHION_RESTITUTION
            }
            if (b.y < r) {
                b.y = r
                if (b.vy < 0f) b.vy = -b.vy * TableSpec.CUSHION_RESTITUTION
            } else if (b.y > TableSpec.HEIGHT - r) {
                b.y = TableSpec.HEIGHT - r
                if (b.vy > 0f) b.vy = -b.vy * TableSpec.CUSHION_RESTITUTION
            }
        }
    }

    /**
     * Distance from the cue ball to the first thing a straight shot would touch.
     * Used only to draw the aiming guide, exactly like the guide line that every
     * pool game draws for its own table.
     */
    fun firstObstacleDistance(dirX: Float, dirY: Float): Float {
        val cue = state.cueBall
        var best = Float.MAX_VALUE
        val contactDist = 2f * TableSpec.BALL_R

        for (b in state.balls) {
            if (b === cue || b.potted) continue
            val ex = b.x - cue.x
            val ey = b.y - cue.y
            val t = ex * dirX + ey * dirY
            if (t <= 0f) continue
            val perp2 = ex * ex + ey * ey - t * t
            val c2 = contactDist * contactDist
            if (perp2 > c2) continue
            val back = sqrt(c2 - perp2)
            val hit = t - back
            if (hit in 0f..best) best = hit
        }

        val r = TableSpec.BALL_R
        if (dirX > 0.0001f) best = min(best, (TableSpec.WIDTH - r - cue.x) / dirX)
        if (dirX < -0.0001f) best = min(best, (r - cue.x) / dirX)
        if (dirY > 0.0001f) best = min(best, (TableSpec.HEIGHT - r - cue.y) / dirY)
        if (dirY < -0.0001f) best = min(best, (r - cue.y) / dirY)

        return if (best == Float.MAX_VALUE) 40f else best.coerceIn(0f, 400f)
    }
}
```

## `app/src/main/java/com/university/poolseclab/game/PoolGame.kt`
```kotlin
package com.university.poolseclab.game

import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * The rules layer. It owns a [GameState] and a [PhysicsEngine] and turns the
 * outcome of a shot into turns, scores and a win or loss.
 *
 * The rule set is a simplified eight ball game, which is enough to produce a
 * realistic looking client state for the security exercises.
 */
class PoolGame {

    val state = GameState()
    val engine = PhysicsEngine(state)

    /** Coins granted for a win. Also the ceiling used by the impossible value check. */
    val winReward: Long = 100L

    init {
        newGame()
    }

    // -----------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------

    fun newGame() {
        state.matchId = String.format(Locale.US, "MATCH-%06d", Random.nextInt(1, 999_999))
        state.currentTurn = 1
        state.playerOneGroup = BallGroup.UNASSIGNED
        state.playerTwoGroup = BallGroup.UNASSIGNED
        state.playerOneScore = 0
        state.playerTwoScore = 0
        state.shotCount = 0
        state.shotPower = 0.55f
        state.shotAngleDeg = 0f
        state.matchPhase = MatchPhase.READY
        state.statusMessage = "Break shot. Player 1 to play."
        state.stateSequence++
        rack()
        engine.resetShotRecord()
    }

    private fun rack() {
        state.balls.clear()
        state.balls.add(Ball(0, TableSpec.CUE_SPOT_X, TableSpec.CUE_SPOT_Y))

        // Standard triangle: apex towards the cue ball, black ball in the middle row.
        val rows = arrayOf(
            intArrayOf(1),
            intArrayOf(9, 2),
            intArrayOf(10, 8, 3),
            intArrayOf(11, 4, 12, 5),
            intArrayOf(13, 6, 14, 7, 15)
        )

        val gap = 2f * TableSpec.BALL_R * 1.02f
        val rowStepX = gap * 0.866f      // cos(30 degrees)
        val apexX = 138f
        val centreY = TableSpec.HEIGHT / 2f

        for (r in rows.indices) {
            val row = rows[r]
            val x = apexX + r * rowStepX
            val startY = centreY - (row.size - 1) * gap / 2f
            for (c in row.indices) {
                state.balls.add(Ball(row[c], x, startY + c * gap))
            }
        }
    }

    // -----------------------------------------------------------------
    // Shooting
    // -----------------------------------------------------------------

    fun isFinished(): Boolean =
        state.matchPhase == MatchPhase.PLAYER_ONE_WINS || state.matchPhase == MatchPhase.PLAYER_TWO_WINS

    fun canShoot(): Boolean = !isFinished() && !engine.anyMoving()

    fun shoot(powerFraction: Float, angleDeg: Float) {
        if (!canShoot()) return

        val p = powerFraction.coerceIn(0.05f, 1f)
        state.shotPower = p
        state.shotAngleDeg = angleDeg

        val rad = Math.toRadians(angleDeg.toDouble())
        val cue = state.cueBall
        cue.vx = (cos(rad) * p * TableSpec.MAX_SPEED).toFloat()
        cue.vy = (sin(rad) * p * TableSpec.MAX_SPEED).toFloat()

        state.shotCount++
        state.stateSequence++
        state.matchPhase = MatchPhase.BALLS_MOVING
        engine.resetShotRecord()
    }

    /**
     * Called once, after every ball has come to rest. Applies the rules and
     * returns a human readable summary of what happened.
     */
    fun onBallsStopped(): String {
        val potted = engine.pottedThisShot.toList()
        val cuePotted = engine.cueBallPotted
        val touched = engine.firstContact
        engine.resetShotRecord()

        val shooter = state.currentTurn
        val objectBalls = potted.filter { it != 8 }
        state.stateSequence++

        // First legal pot decides which half belongs to which player.
        if (state.playerOneGroup == BallGroup.UNASSIGNED && objectBalls.isNotEmpty() && !cuePotted) {
            val g = if (objectBalls.first() <= 7) BallGroup.SOLIDS else BallGroup.STRIPES
            if (shooter == 1) {
                state.playerOneGroup = g
                state.playerTwoGroup = opposite(g)
            } else {
                state.playerTwoGroup = g
                state.playerOneGroup = opposite(g)
            }
        }

        recountScores()

        // The black ball ends the match either way.
        if (potted.contains(8)) {
            val myGroup = state.groupOf(shooter)
            val cleared = myGroup != BallGroup.UNASSIGNED && state.remainingIn(myGroup) == 0
            val winner = if (cleared && !cuePotted) shooter else 3 - shooter
            state.matchPhase =
                if (winner == 1) MatchPhase.PLAYER_ONE_WINS else MatchPhase.PLAYER_TWO_WINS
            if (winner == 1) awardCoinsSanctioned(winReward)
            val why = if (cleared && !cuePotted) "cleared the group and potted the black" else "potted the black too early"
            val msg = "Player " + winner + " wins. Player " + shooter + " " + why + "."
            state.statusMessage = msg
            return msg
        }

        val msg: String
        if (cuePotted) {
            respotCueBall()
            switchTurn()
            msg = "Foul: cue ball pocketed. Player " + state.currentTurn + " to play."
        } else if (objectBalls.isEmpty() && touched == -1) {
            switchTurn()
            msg = "Foul: no ball was hit. Player " + state.currentTurn + " to play."
        } else {
            val myGroup = state.groupOf(shooter)
            val pottedOwn = objectBalls.any { numberGroup(it) == myGroup } ||
                    (myGroup == BallGroup.UNASSIGNED && objectBalls.isNotEmpty())
            if (pottedOwn) {
                msg = "Potted. Player " + shooter + " continues."
            } else {
                switchTurn()
                msg = "Player " + state.currentTurn + " to play."
            }
        }

        state.matchPhase = MatchPhase.READY
        state.statusMessage = msg
        return msg
    }

    // -----------------------------------------------------------------
    // Sanctioned mutators
    // -----------------------------------------------------------------

    /**
     * The only legitimate way for the balance to change. Every mutation goes
     * through here so that the integrity guard can distinguish a change made by
     * the rules from a change made by somebody poking at the field directly.
     */
    fun awardCoinsSanctioned(amount: Long) {
        if (amount <= 0L) return
        state.virtualBalance += amount
        state.stateSequence++
        state.lastUpdateMillis = System.currentTimeMillis()
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private fun opposite(g: BallGroup): BallGroup =
        if (g == BallGroup.SOLIDS) BallGroup.STRIPES else BallGroup.SOLIDS

    private fun numberGroup(n: Int): BallGroup = when (n) {
        in 1..7 -> BallGroup.SOLIDS
        8 -> BallGroup.EIGHT
        in 9..15 -> BallGroup.STRIPES
        else -> BallGroup.UNASSIGNED
    }

    private fun switchTurn() {
        state.currentTurn = 3 - state.currentTurn
    }

    private fun recountScores() {
        state.playerOneScore = countPotted(state.playerOneGroup)
        state.playerTwoScore = countPotted(state.playerTwoGroup)
    }

    private fun countPotted(g: BallGroup): Int {
        if (g == BallGroup.UNASSIGNED) return 0
        var n = 0
        for (b in state.balls) {
            if (b.potted && b.group == g) n++
        }
        return n
    }

    private fun respotCueBall() {
        val cue = state.cueBall
        cue.potted = false
        cue.stop()
        var x = TableSpec.CUE_SPOT_X
        val y = TableSpec.CUE_SPOT_Y
        var attempts = 0
        while (attempts < 40 && occupied(x, y)) {
            x -= 3f * TableSpec.BALL_R
            if (x < TableSpec.BALL_R * 2f) x = TableSpec.CUE_SPOT_X + 3f * TableSpec.BALL_R * attempts
            attempts++
        }
        cue.x = x.coerceIn(TableSpec.BALL_R, TableSpec.WIDTH - TableSpec.BALL_R)
        cue.y = y
    }

    private fun occupied(x: Float, y: Float): Boolean {
        for (b in state.balls) {
            if (b.isCue || b.potted) continue
            if (hypot(b.x - x, b.y - y) < 2.5f * TableSpec.BALL_R) return true
        }
        return false
    }
}
```

## `app/src/main/java/com/university/poolseclab/game/PoolTableView.kt`
```kotlin
package com.university.poolseclab.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the table and drives the simulation clock.
 *
 * The view keeps no game data of its own. It renders whatever is inside the
 * [PoolGame] it was attached to, which is the same object the Security Lab
 * screens read from.
 */
class PoolTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var game: PoolGame? = null

    /** Invoked on the UI thread once every ball has come to rest. */
    var onShotFinished: (() -> Unit)? = null

    /** Invoked while the player drags to aim. Argument is the angle in degrees. */
    var onAimChanged: ((Float) -> Unit)? = null

    private var lastFrameNs = 0L
    private var wasMoving = false

    private var scale = 1f
    private var originX = 0f
    private var originY = 0f

    private val feltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B6B45") }
    private val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4E342E") }
    private val railEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2A1712")
    }
    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0C0C0C") }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#22000000")
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val stripeClip = Path()
    private val aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#CCFFFFFF")
    }
    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#88FFFFFF")
    }
    private val rect = RectF()

    /** Colour of each numbered ball. Index is the ball number. */
    private val ballColors = intArrayOf(
        Color.parseColor("#F7F7F2"), // 0 cue
        Color.parseColor("#E8C41F"), // 1
        Color.parseColor("#1E5FB4"), // 2
        Color.parseColor("#C62828"), // 3
        Color.parseColor("#6A3AB2"), // 4
        Color.parseColor("#E77817"), // 5
        Color.parseColor("#1F8A4C"), // 6
        Color.parseColor("#8C2F1E"), // 7
        Color.parseColor("#101010"), // 8
        Color.parseColor("#E8C41F"), // 9
        Color.parseColor("#1E5FB4"), // 10
        Color.parseColor("#C62828"), // 11
        Color.parseColor("#6A3AB2"), // 12
        Color.parseColor("#E77817"), // 13
        Color.parseColor("#1F8A4C"), // 14
        Color.parseColor("#8C2F1E")  // 15
    )

    fun attach(g: PoolGame) {
        game = g
        invalidate()
    }

    fun isMoving(): Boolean = game?.engine?.anyMoving() == true

    /** Starts the animation loop after a shot has been played. */
    fun kick() {
        lastFrameNs = 0L
        wasMoving = true
        postInvalidateOnAnimation()
    }

    // -----------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val g = game ?: return

        computeTransform()

        val now = System.nanoTime()
        if (lastFrameNs == 0L) lastFrameNs = now
        val dt = ((now - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
        lastFrameNs = now

        if (g.engine.anyMoving()) {
            g.engine.step(dt)
        }

        drawTable(canvas)
        drawBalls(canvas, g)
        if (!g.engine.anyMoving() && !g.isFinished()) {
            drawAim(canvas, g)
        }

        if (g.engine.anyMoving()) {
            postInvalidateOnAnimation()
        } else if (wasMoving) {
            wasMoving = false
            lastFrameNs = 0L
            post { onShotFinished?.invoke() }
        }
    }

    private fun computeTransform() {
        val totalW = TableSpec.WIDTH + 2f * TableSpec.RAIL
        val totalH = TableSpec.HEIGHT + 2f * TableSpec.RAIL
        val pad = 8f
        val availW = (width - 2f * pad).coerceAtLeast(1f)
        val availH = (height - 2f * pad).coerceAtLeast(1f)
        scale = min(availW / totalW, availH / totalH)
        originX = (width - totalW * scale) / 2f + TableSpec.RAIL * scale
        originY = (height - totalH * scale) / 2f + TableSpec.RAIL * scale
    }

    private fun sx(x: Float) = originX + x * scale
    private fun sy(y: Float) = originY + y * scale

    private fun drawTable(canvas: Canvas) {
        rect.set(
            sx(-TableSpec.RAIL), sy(-TableSpec.RAIL),
            sx(TableSpec.WIDTH + TableSpec.RAIL), sy(TableSpec.HEIGHT + TableSpec.RAIL)
        )
        val r = 6f * scale
        canvas.drawRoundRect(rect, r, r, railPaint)
        railEdgePaint.strokeWidth = 1.5f * scale
        canvas.drawRoundRect(rect, r, r, railEdgePaint)

        rect.set(sx(0f), sy(0f), sx(TableSpec.WIDTH), sy(TableSpec.HEIGHT))
        canvas.drawRect(rect, feltPaint)

        for (p in TableSpec.POCKETS) {
            canvas.drawCircle(sx(p[0]), sy(p[1]), TableSpec.POCKET_R * scale, pocketPaint)
        }
    }

    private fun drawBalls(canvas: Canvas, g: PoolGame) {
        val r = TableSpec.BALL_R * scale
        numberPaint.textSize = r * 0.95f
        ballEdgePaint.strokeWidth = 1f.coerceAtLeast(r * 0.08f)

        for (b in g.state.balls) {
            if (b.potted) continue
            val cx = sx(b.x)
            val cy = sy(b.y)
            val colour = ballColors[b.number.coerceIn(0, 15)]

            if (b.number in 9..15) {
                // Striped ball: white body with a coloured band across the middle.
                ballPaint.color = Color.parseColor("#F7F7F2")
                canvas.drawCircle(cx, cy, r, ballPaint)
                canvas.save()
                stripeClip.reset()
                stripeClip.addCircle(cx, cy, r, Path.Direction.CW)
                canvas.clipPath(stripeClip)
                ballPaint.color = colour
                canvas.drawRect(cx - r, cy - r * 0.52f, cx + r, cy + r * 0.52f, ballPaint)
                canvas.restore()
            } else {
                ballPaint.color = colour
                canvas.drawCircle(cx, cy, r, ballPaint)
            }

            canvas.drawCircle(cx, cy, r, ballEdgePaint)

            if (!b.isCue) {
                ballPaint.color = Color.parseColor("#F7F7F2")
                canvas.drawCircle(cx, cy, r * 0.52f, ballPaint)
                numberPaint.color = Color.parseColor("#101010")
                canvas.drawText(
                    b.number.toString(),
                    cx,
                    cy + numberPaint.textSize * 0.35f,
                    numberPaint
                )
            }
        }
    }

    private fun drawAim(canvas: Canvas, g: PoolGame) {
        val cue = g.state.cueBall
        if (cue.potted) return

        val rad = Math.toRadians(g.state.shotAngleDeg.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val dist = g.engine.firstObstacleDistance(dx, dy)

        aimPaint.strokeWidth = 1.4f.coerceAtLeast(scale * 0.5f)
        aimPaint.pathEffect = DashPathEffect(floatArrayOf(6f * scale * 0.4f, 4f * scale * 0.4f), 0f)
        canvas.drawLine(
            sx(cue.x), sy(cue.y),
            sx(cue.x + dx * dist), sy(cue.y + dy * dist),
            aimPaint
        )

        ghostPaint.strokeWidth = 1.2f.coerceAtLeast(scale * 0.4f)
        canvas.drawCircle(
            sx(cue.x + dx * dist),
            sy(cue.y + dy * dist),
            TableSpec.BALL_R * scale,
            ghostPaint
        )
    }

    // -----------------------------------------------------------------
    // Aiming input
    // -----------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val g = game ?: return false
        if (g.engine.anyMoving() || g.isFinished()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateAim(g, event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                updateAim(g, event.x, event.y)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateAim(g: PoolGame, px: Float, py: Float) {
        if (scale <= 0f) return
        val wx = (px - originX) / scale
        val wy = (py - originY) / scale
        val cue = g.state.cueBall
        val ddx = wx - cue.x
        val ddy = wy - cue.y
        if (ddx * ddx + ddy * ddy < 0.5f) return
        val angle = Math.toDegrees(atan2(ddy.toDouble(), ddx.toDouble())).toFloat()
        g.state.shotAngleDeg = angle
        onAimChanged?.invoke(angle)
        invalidate()
    }
}
```

## `app/src/main/java/com/university/poolseclab/game/TableSpec.kt`
```kotlin
package com.university.poolseclab.game

/**
 * Geometry and physical constants of the simulated table.
 *
 * All units are "table units". The playing surface is 200 x 100 units, which is
 * the 2:1 ratio of a real pool table. The renderer scales these units to pixels,
 * so the simulation itself is completely resolution independent.
 */
object TableSpec {

    const val WIDTH = 200f
    const val HEIGHT = 100f

    /** Radius of every ball. */
    const val BALL_R = 2.35f

    /** Capture radius of a pocket, measured from the pocket centre. */
    const val POCKET_R = 4.6f

    /** Width of the wooden rail drawn around the playing surface. */
    const val RAIL = 7f

    /** Rolling deceleration in units per second squared. */
    const val FRICTION = 26f

    /** Energy kept after bouncing off a cushion. */
    const val CUSHION_RESTITUTION = 0.86f

    /** Energy kept in a ball to ball impact. */
    const val BALL_RESTITUTION = 0.95f

    /** Below this speed a ball is considered stopped. */
    const val MIN_SPEED = 1.5f

    /** Speed produced by a shot at 100 percent power. */
    const val MAX_SPEED = 260f

    /** Head spot where the cue ball is re-spotted after a foul. */
    const val CUE_SPOT_X = 50f
    const val CUE_SPOT_Y = 50f

    /** Six pockets: four corners and two in the middle of the long cushions. */
    val POCKETS: Array<FloatArray> = arrayOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(WIDTH / 2f, 0f),
        floatArrayOf(WIDTH, 0f),
        floatArrayOf(0f, HEIGHT),
        floatArrayOf(WIDTH / 2f, HEIGHT),
        floatArrayOf(WIDTH, HEIGHT)
    )
}
```

## `app/src/main/java/com/university/poolseclab/core/GameHolder.kt`
```kotlin
package com.university.poolseclab.core

import com.university.poolseclab.game.PoolGame
import com.university.poolseclab.security.IntegrityGuard

/**
 * Single shared instance of the match and of the guard that watches it.
 *
 * Every screen reads from this one object, so the values in the Security Lab
 * are the same values the table is being drawn from. There is no copy and no
 * second source of truth.
 */
object GameHolder {

    val game: PoolGame = PoolGame()
    val guard: IntegrityGuard = IntegrityGuard(game.state)

    private var initialised = false

    @Synchronized
    fun ensureInitialised() {
        if (initialised) return
        guard.seal()
        initialised = true
    }
}
```

## `app/src/main/java/com/university/poolseclab/core/LabApplication.kt`
```kotlin
package com.university.poolseclab.core

import android.app.Application

/**
 * Application entry point. It only seeds the shared match state, so there is
 * nothing here that can fail at start up.
 */
class LabApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GameHolder.ensureInitialised()
    }
}
```

## `app/src/main/java/com/university/poolseclab/security/DefenseCatalog.kt`
```kotlin
package com.university.poolseclab.security

/**
 * The defensive techniques this build demonstrates, and an honest statement of
 * how far each one goes when it runs on hardware the player controls.
 */
object DefenseCatalog {

    data class Technique(
        val name: String,
        val whatItDoes: String,
        val scope: String
    )

    val techniques: List<Technique> = listOf(
        Technique(
            "Canonical serialisation",
            "Turns the protected part of the state into one deterministic string so that the same state always produces the same bytes.",
            "Fully implemented in this build"
        ),
        Technique(
            "Hashing and HMAC-SHA256",
            "Computes a keyed tag over the canonical state and compares it against the tag recorded when the state was last sealed.",
            "Computed for real here, but only meaningful when the key lives on a server"
        ),
        Technique(
            "State validation",
            "Checks that the turn is a valid seat, that the phase is a legal enum value and that derived counters agree with the arrays they summarise.",
            "Fully implemented in this build"
        ),
        Technique(
            "Range checking",
            "Rejects a balance outside 0 to 100000 and a score outside 0 to 7.",
            "Fully implemented in this build"
        ),
        Technique(
            "Impossible value detection",
            "Rejects a balance increase larger than any single match can pay, a ball outside the playing surface, and a speed above the maximum the simulation can produce.",
            "Fully implemented in this build"
        ),
        Technique(
            "Replay and rewind detection",
            "Requires a monotonic sequence counter, so that an older but validly signed state cannot be presented again.",
            "Fully implemented in this build"
        ),
        Technique(
            "Server authoritative design",
            "The server simulates the shot, owns the balance and the score, and sends the client a result to display rather than accepting one.",
            "Explained only. It cannot be demonstrated offline, and that limitation is itself the lesson"
        ),
        Technique(
            "Secure storage principles",
            "Secrets belong in the Android Keystore where the key material is not extractable, sensitive state is not written to shared storage, and backup is disabled. This build persists nothing at all.",
            "Principles explained. This build has nothing to store"
        ),
        Technique(
            "Shrinking and obfuscation",
            "R8 is enabled for the release build. It raises the cost of reading the code, which slows an analyst down.",
            "Enabled, but it is a speed bump and not a security control"
        )
    )

    fun asText(): String {
        val sb = StringBuilder()
        for ((i, t) in techniques.withIndex()) {
            sb.append(i + 1).append(". ").append(t.name).append('\n')
            sb.append("   ").append(t.whatItDoes).append('\n')
            sb.append("   Scope: ").append(t.scope).append('\n')
            if (i != techniques.lastIndex) sb.append('\n')
        }
        return sb.toString()
    }
}
```

## `app/src/main/java/com/university/poolseclab/security/IntegrityGuard.kt`
```kotlin
package com.university.poolseclab.security

import com.university.poolseclab.game.GameState
import com.university.poolseclab.game.TableSpec
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** One thing that failed a check. */
data class IntegrityFinding(
    val field: String,
    val originalValue: String,
    val currentValue: String,
    val rule: String
)

/** Result of a full verification pass. */
data class IntegrityReport(
    val ok: Boolean,
    val findings: List<IntegrityFinding>,
    val detectedAt: String,
    val expectedTag: String,
    val actualTag: String,
    val sealedSequence: Long,
    val currentSequence: Long,
    val neverSealed: Boolean
)

/**
 * Demonstrates client side integrity checking over the application own state.
 *
 * WHAT IS REAL HERE
 *   The hashing, the HMAC, the range checks, the impossible value checks and
 *   the monotonic counter check are all genuinely computed.
 *
 * WHAT IS ONLY A DEMONSTRATION
 *   The key lives in this process. Anybody who can modify the state can also
 *   read the key and forge a fresh tag. That is not a flaw in the code, it is
 *   the actual lesson: a client cannot verify itself against its own owner.
 *   The same construction becomes meaningful only when the key and the check
 *   live on a machine the player does not control. See the Security Report.
 */
class IntegrityGuard(private val state: GameState) {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /** Session key, generated at start up and never persisted or transmitted. */
    private val key: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    private var sealedTag: String = ""
    private var sealedValues: LinkedHashMap<String, String> = LinkedHashMap()
    private var snapshot: Snapshot? = null

    val isSealed: Boolean get() = snapshot != null
    var sealedAtText: String = "never"
        private set

    /** Upper bound used by the range check on the simulated currency. */
    private val maxReasonableBalance = 100_000L

    /** Largest balance increase that one match can legitimately produce. */
    private val maxBalanceGainPerMatch = 100L

    private class Snapshot(
        val balance: Long,
        val scoreOne: Int,
        val scoreTwo: Int,
        val turn: Int,
        val sequence: Long,
        val phase: String,
        val ballX: FloatArray,
        val ballY: FloatArray,
        val potted: BooleanArray
    )

    // -----------------------------------------------------------------
    // Canonical serialisation and tagging
    // -----------------------------------------------------------------

    /**
     * Deterministic text form of the protected part of the state. Canonical
     * means: same state, same bytes, every time, on every device. Without that
     * property a signature over the data is meaningless.
     */
    fun canonicalState(): String {
        val sb = StringBuilder(256)
        sb.append("v1|")
        sb.append("player=").append(state.playerId).append('|')
        sb.append("match=").append(state.matchId).append('|')
        sb.append("turn=").append(state.currentTurn).append('|')
        sb.append("s1=").append(state.playerOneScore).append('|')
        sb.append("s2=").append(state.playerTwoScore).append('|')
        sb.append("balance=").append(state.virtualBalance).append('|')
        sb.append("phase=").append(state.matchPhase.name).append('|')
        sb.append("seq=").append(state.stateSequence).append('|')
        for (b in state.balls) {
            sb.append('b').append(b.number).append('=')
                .append(String.format(Locale.US, "%.3f,%.3f,%b", b.x, b.y, b.potted))
                .append(';')
        }
        return sb.toString()
    }

    /** SHA-256 based HMAC over the canonical state. */
    fun hmac(payload: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            val raw = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder(raw.size * 2)
            for (byte in raw) {
                sb.append(String.format(Locale.US, "%02x", byte))
            }
            sb.toString()
        } catch (e: Exception) {
            "unavailable:" + e.javaClass.simpleName
        }
    }

    fun currentTag(): String = hmac(canonicalState())

    private fun protectedValues(): LinkedHashMap<String, String> {
        val m = LinkedHashMap<String, String>()
        m["GameState -> Player -> Balance"] = state.virtualBalance.toString()
        m["GameState -> Player -> Score"] = state.playerOneScore.toString()
        m["GameState -> Opponent -> Score"] = state.playerTwoScore.toString()
        m["GameState -> Match -> CurrentTurn"] = state.currentTurn.toString()
        m["GameState -> Match -> Phase"] = state.matchPhase.name
        m["GameState -> Sequence"] = state.stateSequence.toString()
        m["GameState -> CueBall -> Position"] =
            String.format(Locale.US, "(%.2f, %.2f)", state.cueBall.x, state.cueBall.y)
        m["GameState -> MaxBallSpeed"] =
            String.format(Locale.US, "%.2f", state.maxBallSpeed())
        return m
    }

    // -----------------------------------------------------------------
    // Sealing and verification
    // -----------------------------------------------------------------

    /** Records the current state as the trusted baseline. */
    fun seal() {
        sealedTag = currentTag()
        sealedValues = protectedValues()
        snapshot = Snapshot(
            balance = state.virtualBalance,
            scoreOne = state.playerOneScore,
            scoreTwo = state.playerTwoScore,
            turn = state.currentTurn,
            sequence = state.stateSequence,
            phase = state.matchPhase.name,
            ballX = FloatArray(state.balls.size) { state.balls[it].x },
            ballY = FloatArray(state.balls.size) { state.balls[it].y },
            potted = BooleanArray(state.balls.size) { state.balls[it].potted }
        )
        sealedAtText = timeFormat.format(Date())
    }

    /** Runs every check and reports what, if anything, changed without permission. */
    fun verify(): IntegrityReport {
        val now = timeFormat.format(Date())
        val snap = snapshot
            ?: return IntegrityReport(
                ok = false,
                findings = emptyList(),
                detectedAt = now,
                expectedTag = "-",
                actualTag = "-",
                sealedSequence = 0L,
                currentSequence = state.stateSequence,
                neverSealed = true
            )

        val findings = ArrayList<IntegrityFinding>()
        val actual = currentTag()
        val current = protectedValues()

        // 1. Cryptographic check over the whole protected payload.
        if (actual != sealedTag) {
            for ((field, sealedValue) in sealedValues) {
                val nowValue = current[field] ?: "?"
                if (nowValue != sealedValue) {
                    findings.add(
                        IntegrityFinding(field, sealedValue, nowValue, "HMAC-SHA256 payload mismatch")
                    )
                }
            }
            if (findings.isEmpty()) {
                findings.add(
                    IntegrityFinding(
                        "GameState (whole payload)", sealedTag.take(16), actual.take(16),
                        "HMAC-SHA256 tag mismatch"
                    )
                )
            }
        }

        // 2. Range check on the simulated currency.
        if (state.virtualBalance < 0L || state.virtualBalance > maxReasonableBalance) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Player -> Balance",
                    snap.balance.toString(),
                    state.virtualBalance.toString(),
                    "Range check: allowed 0 .. " + maxReasonableBalance
                )
            )
        }

        // 3. Impossible value check: no single match can pay more than the reward.
        val gain = state.virtualBalance - snap.balance
        if (gain > maxBalanceGainPerMatch) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Player -> Balance",
                    snap.balance.toString(),
                    state.virtualBalance.toString(),
                    "Impossible gain: +" + gain + " exceeds the maximum of " + maxBalanceGainPerMatch + " per match"
                )
            )
        }

        // 4. Range check on the scores.
        if (state.playerOneScore !in 0..7 || state.playerTwoScore !in 0..7) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Player -> Score",
                    snap.scoreOne.toString() + " / " + snap.scoreTwo,
                    state.playerOneScore.toString() + " / " + state.playerTwoScore,
                    "Range check: a group holds at most 7 balls"
                )
            )
        }

        // 5. Turn must be a valid seat.
        if (state.currentTurn != 1 && state.currentTurn != 2) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Match -> CurrentTurn",
                    snap.turn.toString(),
                    state.currentTurn.toString(),
                    "Domain check: turn must be 1 or 2"
                )
            )
        }

        // 6. Geometry check: every ball must be on the table.
        for (b in state.balls) {
            if (b.potted) continue
            val outside = b.x < -1f || b.x > TableSpec.WIDTH + 1f ||
                    b.y < -1f || b.y > TableSpec.HEIGHT + 1f
            if (outside) {
                findings.add(
                    IntegrityFinding(
                        "GameState -> Balls[" + b.number + "] -> Position",
                        "inside the 200 x 100 playing surface",
                        String.format(Locale.US, "(%.2f, %.2f)", b.x, b.y),
                        "Geometry check: position is off the table"
                    )
                )
            }
        }

        // 7. Physics check: no ball can exceed the maximum speed of the simulation.
        val limit = TableSpec.MAX_SPEED * 1.05f
        for (b in state.balls) {
            if (b.potted) continue
            if (b.speed() > limit) {
                findings.add(
                    IntegrityFinding(
                        "GameState -> Balls[" + b.number + "] -> Velocity",
                        String.format(Locale.US, "at most %.0f", limit),
                        String.format(Locale.US, "%.0f", b.speed()),
                        "Physics check: speed above the simulation maximum"
                    )
                )
            }
        }

        // 8. Replay check: the sequence counter must never move backwards.
        if (state.stateSequence < snap.sequence) {
            findings.add(
                IntegrityFinding(
                    "GameState -> Sequence",
                    snap.sequence.toString(),
                    state.stateSequence.toString(),
                    "Replay check: monotonic counter moved backwards"
                )
            )
        }

        return IntegrityReport(
            ok = findings.isEmpty(),
            findings = findings,
            detectedAt = now,
            expectedTag = sealedTag,
            actualTag = actual,
            sealedSequence = snap.sequence,
            currentSequence = state.stateSequence,
            neverSealed = false
        )
    }

    /** Puts the sealed baseline back, the way a server would resend authoritative state. */
    fun restore() {
        val snap = snapshot ?: return
        state.virtualBalance = snap.balance
        state.playerOneScore = snap.scoreOne
        state.playerTwoScore = snap.scoreTwo
        state.currentTurn = snap.turn
        state.stateSequence = snap.sequence
        for (i in state.balls.indices) {
            if (i >= snap.ballX.size) break
            val b = state.balls[i]
            b.x = snap.ballX[i]
            b.y = snap.ballY[i]
            b.potted = snap.potted[i]
            b.stop()
        }
        state.lastUpdateMillis = System.currentTimeMillis()
    }
}
```

## `app/src/main/java/com/university/poolseclab/security/MemoryMapFragment.kt`
```kotlin
package com.university.poolseclab.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.poolseclab.R
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.DialogVariableBinding
import com.university.poolseclab.databinding.FragmentMemoryBinding
import com.university.poolseclab.databinding.ItemMemoryFieldBinding
import com.university.poolseclab.databinding.ItemMemoryHeaderBinding

/** One row of the simulated memory picture. */
sealed class MemoryRow {
    data class Header(val line: String, val note: String) : MemoryRow()
    data class Field(val variable: StateVariable) : MemoryRow()
}

/**
 * Draws an invented struct layout and lets the student open a teaching card for
 * any field. Nothing here reads real memory.
 */
class MemoryMapFragment : Fragment() {

    private var binding: FragmentMemoryBinding? = null
    private lateinit var adapter: MemoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentMemoryBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        adapter = MemoryAdapter { showCard(it) }
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (this::adapter.isInitialized) refresh()
    }

    private fun refresh() {
        adapter.submit(buildRows())
    }

    private fun buildRows(): List<MemoryRow> {
        val vars = StateInspector.variables(GameHolder.game.state)
        val rows = ArrayList<MemoryRow>()

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.GAME_STATE) + "  GameState        (root, 0x40 bytes)",
                "Simulated. The root record that owns every other record below."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> Sequence") || it.path.startsWith("GameState -> LastUpdate") }.map { MemoryRow.Field(it) })

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.PLAYER_STATE) + "  PlayerState      (0x40 bytes)",
                "Simulated. Identity, score and the simulated currency."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> Player") }.map { MemoryRow.Field(it) })

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.MATCH_STATE) + "  MatchState       (0x40 bytes)",
                "Simulated. Turn, phase and the parameters of the last shot."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> Match") || it.path.startsWith("GameState -> Shot") }.map { MemoryRow.Field(it) })

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.CUE_BALL_STATE) + "  CueBallState     (0x40 bytes)",
                "Simulated. Position and velocity of the cue ball."
            )
        )
        rows.addAll(vars.filter { it.path.startsWith("GameState -> CueBall") }.map { MemoryRow.Field(it) })

        val ballVars = vars.filter { it.path.startsWith("GameState -> Balls[") }
        for (i in 0 until 4) {
            val prefix = "GameState -> Balls[" + i + "]"
            val group = ballVars.filter { it.path.startsWith(prefix) }
            if (group.isEmpty()) continue
            rows.add(
                MemoryRow.Header(
                    SimulatedAddresses.hex(SimulatedAddresses.ballBase(i)) + "  BallState[" + i + "]     (stride 0x40)",
                    "Simulated. A fixed stride array is what makes element " + i + " trivially reachable once element 0 is known."
                )
            )
            rows.addAll(group.map { MemoryRow.Field(it) })
        }

        rows.add(
            MemoryRow.Header(
                SimulatedAddresses.hex(SimulatedAddresses.ballBase(4)) + " .. " +
                        SimulatedAddresses.hex(SimulatedAddresses.ballBase(14)) + "  BallState[4..14]",
                "Simulated. The remaining object balls follow the same 0x40 stride."
            )
        )

        return rows
    }

    private fun showCard(v: StateVariable) {
        val ctx = context ?: return
        val db = DialogVariableBinding.inflate(LayoutInflater.from(ctx))
        db.dName.text = v.name + "\n" + v.path
        db.dType.text = v.type
        db.dValue.text = v.value
        db.dAddress.text = v.address + "\n(simulated, not a real address)"
        db.dDesc.text = v.description
        db.dRisk.text = v.risk
        db.dDefense.text = v.defense

        AlertDialog.Builder(ctx)
            .setTitle(v.name)
            .setView(db.root)
            .setPositiveButton(R.string.dlg_close, null)
            .show()
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

/** Two view type adapter: struct headers and clickable fields. */
class MemoryAdapter(
    private val onFieldClick: (StateVariable) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = ArrayList<MemoryRow>()

    fun submit(newRows: List<MemoryRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    class HeaderHolder(val binding: ItemMemoryHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class FieldHolder(val binding: ItemMemoryFieldBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is MemoryRow.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            HeaderHolder(ItemMemoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            FieldHolder(ItemMemoryFieldBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is MemoryRow.Header -> {
                val h = holder as HeaderHolder
                h.binding.hdrLine.text = row.line
                h.binding.hdrNote.text = row.note
            }
            is MemoryRow.Field -> {
                val f = holder as FieldHolder
                val v = row.variable
                f.binding.fieldLine.text = v.address.substringBefore("  ") + "   " + v.name
                f.binding.fieldMeta.text = v.type + " = " + v.value
                f.binding.root.setOnClickListener { onFieldClick(v) }
            }
        }
    }

    override fun getItemCount(): Int = rows.size
}
```

## `app/src/main/java/com/university/poolseclab/security/ReportContent.kt`
```kotlin
package com.university.poolseclab.security

/** One titled section of the in-app security report. */
data class ReportSection(val title: String, val body: String)

/**
 * The written material for the Security Report screen. It is kept in Kotlin
 * rather than in strings.xml because it is long form prose rather than a set of
 * user interface labels.
 */
object ReportContent {

    val sections: List<ReportSection> = listOf(

        ReportSection(
            "0. Scope of this laboratory",
            "This application is a self contained teaching bench. It contains its own pool " +
                    "simulation, its own state and its own defences.\n\n" +
                    "It does not read, write, patch, hook, overlay or communicate with any other " +
                    "application. It declares no permissions at all, including no INTERNET " +
                    "permission. Every address shown on the memory screen was invented for the " +
                    "diagram and corresponds to nothing outside this app.\n\n" +
                    "The exercise is about understanding a class of weakness and the controls " +
                    "that answer it. It is not about attacking a product."
        ),

        ReportSection(
            "1. What game-state manipulation means",
            "A game holds its state in the memory of the process that runs it: where the balls " +
                    "are, whose turn it is, what the score is, how much virtual currency the player " +
                    "has. Game-state manipulation is changing one of those values outside the rules " +
                    "that were supposed to govern it.\n\n" +
                    "The important part is the word outside. A player who pots a ball changes the " +
                    "score, and that is fine, because the change travelled through the code path " +
                    "that is allowed to make it. Manipulation is a change that arrives at the value " +
                    "by another route, so none of the conditions the rules would have enforced were " +
                    "ever tested.\n\n" +
                    "This is why an integrity check that only asks did this value change is close to " +
                    "useless. The question worth asking is: did this change arrive through a path " +
                    "that was permitted to make it."
        ),

        ReportSection(
            "2. Why client-side-only values are vulnerable",
            "A value that exists only on the player device sits on hardware the player owns, " +
                    "administers and can inspect at leisure. There is no time limit, no observer and " +
                    "no cost to failure.\n\n" +
                    "Three properties make client held state easy to find, and this app deliberately " +
                    "has all three:\n\n" +
                    "  Structure. State is grouped into records, so finding one field tends to " +
                    "reveal its neighbours.\n\n" +
                    "  Regularity. Arrays use a fixed stride, so element zero gives you every other " +
                    "element by arithmetic.\n\n" +
                    "  Observability. A value the interface displays can be located by watching what " +
                    "changes when the display changes.\n\n" +
                    "None of these is a bug. They are ordinary, sensible engineering. The mistake is " +
                    "not that the state is structured, it is trusting a structured value that lives " +
                    "somewhere you do not control."
        ),

        ReportSection(
            "3. Client-authoritative versus server-authoritative",
            "In a client-authoritative design the device decides what happened and tells the " +
                    "server the result: I won, my score is seven, my balance is now nine thousand. " +
                    "The server records it. Every value in that report is exactly as trustworthy as " +
                    "the device that produced it, which is to say not at all.\n\n" +
                    "In a server-authoritative design the device sends inputs, not outcomes: I shot " +
                    "at 41 degrees with 62 percent power. The server runs the same simulation, " +
                    "produces the outcome itself, and sends back the state to display. The client " +
                    "becomes a renderer and an input device.\n\n" +
                    "The shift matters because of where the attacker sits. Tampering with a " +
                    "client-authoritative game changes the answer. Tampering with a " +
                    "server-authoritative game changes only what one player sees on their own " +
                    "screen, while the match itself proceeds from the server copy.\n\n" +
                    "The honest engineering caveat is cost. Server simulation costs computation, " +
                    "bandwidth and latency, and single player or casual modes are often left " +
                    "client-authoritative on purpose. That is a defensible decision as long as it is " +
                    "a decision, and as long as nothing of value crosses from that mode into a " +
                    "shared economy."
        ),

        ReportSection(
            "4. Why virtual currency must never be trusted from the client",
            "Currency is the highest value target because it is fungible, it usually persists " +
                    "across sessions, and it often has a real money purchase path beside it. A " +
                    "balance that the client can set is a mint.\n\n" +
                    "The rule is that the authoritative balance lives on the server and changes only " +
                    "through server side transactions that are themselves validated: this player " +
                    "won this match, this match paid this reward, this reward has not already been " +
                    "claimed. The number the client holds is a display cache. If it disagrees with " +
                    "the server, the server is right by definition.\n\n" +
                    "Note what this app does in its rules layer. Every change to the balance goes " +
                    "through one method, awardCoinsSanctioned. Funnelling mutation through a single " +
                    "checkable path does not stop an attacker who can write the field directly, but " +
                    "it is what makes a server side version of the same design enforceable, and it " +
                    "is what lets the tamper screen tell a legitimate change from an injected one."
        ),

        ReportSection(
            "5. How memory tampering works, conceptually",
            "Conceptually the attack has three phases, and it is worth understanding them at " +
                    "this level because the defences map onto them.\n\n" +
                    "Locate. The attacker finds the value, usually by changing something visible and " +
                    "narrowing down what changed with it.\n\n" +
                    "Understand. The attacker works out the layout around it: the type, the record " +
                    "it belongs to, the stride of the array it sits in.\n\n" +
                    "Modify or replay. The attacker writes a new value, or captures a valid state " +
                    "and presents it again later.\n\n" +
                    "The corresponding defences are: make the value not worth locating by keeping " +
                    "the authoritative copy elsewhere; make an incorrect value detectable by binding " +
                    "the state together cryptographically and validating it against the rules of the " +
                    "simulation; and make replay useless by binding a monotonic counter into the " +
                    "signed payload.\n\n" +
                    "Deliberately absent from that list: any technique for performing the attack. " +
                    "The defensive design does not require it, and this laboratory does not teach it."
        ),

        ReportSection(
            "6. How anti-cheat systems detect abnormal behaviour",
            "Detection is mostly statistics rather than cryptography, because a cheat has to " +
                    "produce results, and results leave a distribution.\n\n" +
                    "Impossible values. A speed above the maximum the simulation can generate, a " +
                    "position off the table, a balance that grew faster than any sequence of matches " +
                    "could pay. This app implements all three; they are cheap and they catch the " +
                    "unsubtle case.\n\n" +
                    "Impossible transitions. A ball that moved further than the elapsed time allows, " +
                    "or a phase that jumped from break to victory without the states in between.\n\n" +
                    "Behavioural outliers. Aiming error that is too small too consistently, reaction " +
                    "times below human capability, or a win rate that no honest player sustains. " +
                    "Humans are noisy; automation usually is not.\n\n" +
                    "Cross-account correlation. The same anomaly appearing across accounts that " +
                    "share a device, a payment method or a session pattern.\n\n" +
                    "Two engineering cautions. First, every detector has a false positive rate, and " +
                    "a skilled player looks statistically strange, so detection should feed a review " +
                    "process rather than an instant ban. Second, detection is a control on the " +
                    "server. A detector that runs on the attacker device is subject to the same " +
                    "problem as the value it is watching."
        ),

        ReportSection(
            "7. Defensive programming techniques",
            "Validate on the trust boundary, not before it. Clamping in the sender is user " +
                    "interface work. The check that counts is the one on the side that does not " +
                    "trust the sender.\n\n" +
                    "Make state transitions explicit. An enum plus a table of legal transitions " +
                    "turns whole classes of manipulation into an unreachable state rather than a " +
                    "condition somebody has to remember to test.\n\n" +
                    "Funnel mutation. One sanctioned method per protected value gives you a single " +
                    "place to check, audit and log.\n\n" +
                    "Recompute derived values. Anything derivable from the base data should be " +
                    "recomputed and not received, so no disagreement can be manufactured.\n\n" +
                    "Bind data together. Signing fields individually lets an attacker mix and match " +
                    "valid pieces. Sign the whole state as one canonical payload, with the identity, " +
                    "the match and the sequence counter inside it.\n\n" +
                    "Bind time and order. A monotonic counter inside the signed payload turns replay " +
                    "into a signature failure.\n\n" +
                    "Fail closed and quietly. Reject the state and resend the authoritative copy. Do " +
                    "not print which check failed; a precise error message is free tuning feedback " +
                    "for the attacker.\n\n" +
                    "Keep secrets out of the client. A key in the application binary is not a " +
                    "secret. On Android, keys that must exist on the device belong in the Keystore, " +
                    "where the material is not extractable, and they should authorise a request " +
                    "rather than certify a claim.\n\n" +
                    "Be honest about obfuscation. R8 is enabled in the release build of this project " +
                    "and it makes static reading harder. It raises cost. It does not create a trust " +
                    "boundary, and a design that depends on it has no boundary at all."
        ),

        ReportSection(
            "8. What this build demonstrates and what it cannot",
            "Genuinely implemented and running: canonical serialisation, HMAC-SHA256 over the " +
                    "protected state, range checks, geometry and physics plausibility checks, " +
                    "monotonic sequence checking, sanctioned mutation paths, and restoration of a " +
                    "trusted baseline.\n\n" +
                    "Explained but not demonstrable offline: server-authoritative simulation, server " +
                    "side transaction validation, server issued match identifiers, server time, and " +
                    "cross-account correlation. All of these need a party the player does not " +
                    "control, and this application deliberately has no network access.\n\n" +
                    "The most important conclusion of the exercise is the one the tamper screen " +
                    "states about itself: the verifier runs in the same process as the value it " +
                    "verifies, and holds its key there too. A client cannot be made to enforce rules " +
                    "against the person who owns it. Client side integrity checking raises cost and " +
                    "produces useful telemetry. It is not a trust boundary, and it should never be " +
                    "the only thing standing between a player and the economy of a game."
        )
    )
}
```

## `app/src/main/java/com/university/poolseclab/security/ReportFragment.kt`
```kotlin
package com.university.poolseclab.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.poolseclab.databinding.FragmentReportBinding
import com.university.poolseclab.databinding.ItemReportBinding

/** The written security report. */
class ReportFragment : Fragment() {

    private var binding: FragmentReportBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentReportBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = ReportAdapter(ReportContent.sections)
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

class ReportAdapter(
    private val sections: List<ReportSection>
) : RecyclerView.Adapter<ReportAdapter.Holder>() {

    class Holder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemReportBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val s = sections[position]
        holder.binding.secTitle.text = s.title
        holder.binding.secBody.text = s.body
    }

    override fun getItemCount(): Int = sections.size
}
```

## `app/src/main/java/com/university/poolseclab/security/SecurityLabFragment.kt`
```kotlin
package com.university.poolseclab.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.FragmentStateBinding
import com.university.poolseclab.databinding.ItemStateVarBinding

/** Lists every inspectable value of the application own state. */
class SecurityLabFragment : Fragment() {

    private var binding: FragmentStateBinding? = null
    private val adapter = StateVarAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentStateBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        b.refreshBtn.setOnClickListener { refresh() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        adapter.submit(StateInspector.variables(GameHolder.game.state))
    }

    override fun onDestroyView() {
        binding?.list?.adapter = null
        binding = null
        super.onDestroyView()
    }
}

/** Simple adapter over [StateVariable]. */
class StateVarAdapter : RecyclerView.Adapter<StateVarAdapter.Holder>() {

    private val items = ArrayList<StateVariable>()

    fun submit(newItems: List<StateVariable>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemStateVarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemStateVarBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val v = items[position]
        holder.binding.varName.text = v.name
        holder.binding.varPath.text = v.path
        holder.binding.varType.text = v.type
        holder.binding.varAddress.text = v.address
        holder.binding.varValue.text = v.value
    }

    override fun getItemCount(): Int = items.size
}
```

## `app/src/main/java/com/university/poolseclab/security/SimulatedAddresses.kt`
```kotlin
package com.university.poolseclab.security

import java.util.Locale

/**
 * Invented addresses used only for drawing a picture of a memory layout.
 *
 * IMPORTANT
 * =========
 * Every number in this file was chosen by the author of this teaching app.
 * They are not real addresses, not offsets, not pointers and not signatures.
 * They do not correspond to any commercial application, and nothing in this
 * app ever reads or writes memory outside its own objects.
 *
 * The point of the exercise is the SHAPE of a state layout: a struct per
 * concept, a fixed stride per array element, and predictable field offsets.
 * That shape is what makes client held state easy to locate, and that is the
 * lesson, not the specific numbers.
 */
object SimulatedAddresses {

    const val GAME_STATE = 0x1000L
    const val PLAYER_STATE = 0x1040L
    const val MATCH_STATE = 0x1080L
    const val CUE_BALL_STATE = 0x10C0L
    const val BALL_STATE_BASE = 0x1100L
    const val BALL_STRIDE = 0x40L

    fun hex(address: Long): String =
        String.format(Locale.US, "0x%04X", address)

    fun field(base: Long, offset: Long): String =
        String.format(Locale.US, "0x%04X  (base 0x%04X + 0x%02X)", base + offset, base, offset)

    fun ballBase(index: Int): Long = BALL_STATE_BASE + index * BALL_STRIDE
}
```

## `app/src/main/java/com/university/poolseclab/security/StateInspector.kt`
```kotlin
package com.university.poolseclab.security

import com.university.poolseclab.game.GameState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the list of inspectable variables by reading the application own
 * [GameState]. Nothing outside this process is touched.
 */
object StateInspector {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun variables(state: GameState): List<StateVariable> {
        val list = ArrayList<StateVariable>(32)

        list.add(
            StateVariable(
                name = "playerId",
                path = "GameState -> Player -> Id",
                type = "String",
                value = state.playerId,
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x00),
                description = "Identifier of the local player for this laboratory session.",
                risk = "An identifier that the client can rewrite lets one account impersonate another when the server trusts whatever identifier arrives with a request.",
                defense = "Derive the identity on the server from an authenticated session token. Never accept an identity that the client simply asserts."
            )
        )

        list.add(
            StateVariable(
                name = "matchId",
                path = "GameState -> Match -> Id",
                type = "String",
                value = state.matchId,
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x00),
                description = "Identifier of the match currently being played.",
                risk = "If a result can be submitted for any match identifier, an attacker can report outcomes for matches they never played, or replay one winning result many times.",
                defense = "Server issued, single use match identifiers bound to the authenticated session, with a server side record of which matches are still open."
            )
        )

        list.add(
            StateVariable(
                name = "currentTurn",
                path = "GameState -> Match -> CurrentTurn",
                type = "Int (1 or 2)",
                value = state.currentTurn.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x08),
                description = "Which player is allowed to take the next shot.",
                risk = "A client that decides whose turn it is can simply keep the turn forever and take unlimited consecutive shots.",
                defense = "The authoritative turn lives on the server. The client renders a turn, it does not grant one."
            )
        )

        list.add(
            StateVariable(
                name = "playerPosition",
                path = "GameState -> Player -> Position",
                type = "Int (seat index)",
                value = "seat " + state.currentTurn + " of 2",
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x04),
                description = "Seat that the local player occupies in the match.",
                risk = "Seat confusion can be used to act on behalf of the opponent if the server maps actions to seats supplied by the client.",
                defense = "Map every incoming action to a seat using the server side session, not a field in the request body."
            )
        )

        val cue = state.cueBall
        list.add(
            StateVariable(
                name = "cueBall.position",
                path = "GameState -> CueBall -> Position",
                type = "Vector2f (x, y)",
                value = String.format(Locale.US, "(%.2f, %.2f)", cue.x, cue.y),
                address = SimulatedAddresses.field(SimulatedAddresses.CUE_BALL_STATE, 0x00),
                description = "Position of the cue ball on the 200 by 100 unit table.",
                risk = "Writing this value directly is a teleport. The ball reaches a position that no legal shot could have produced.",
                defense = "Recompute positions from the shot on the authoritative side, and validate that every reported position is reachable from the previous one within the elapsed time."
            )
        )

        list.add(
            StateVariable(
                name = "cueBall.velocity",
                path = "GameState -> CueBall -> Velocity",
                type = "Vector2f (vx, vy)",
                value = String.format(Locale.US, "(%.2f, %.2f)", cue.vx, cue.vy),
                address = SimulatedAddresses.field(SimulatedAddresses.CUE_BALL_STATE, 0x08),
                description = "Current velocity of the cue ball in table units per second.",
                risk = "A velocity above the physical maximum of the simulation is an impossible value. It is both a cheat and, usefully, an easy signal to detect.",
                defense = "Range check every physical quantity against the limits the simulation itself can produce, and reject anything outside them."
            )
        )

        for (i in 0 until 4) {
            val b = state.balls.getOrNull(i + 1) ?: continue
            list.add(
                StateVariable(
                    name = "balls[" + i + "].position",
                    path = "GameState -> Balls[" + i + "] -> Position",
                    type = "Vector2f (x, y)",
                    value = String.format(Locale.US, "(%.2f, %.2f)", b.x, b.y),
                    address = SimulatedAddresses.field(SimulatedAddresses.ballBase(i), 0x00),
                    description = "Position of object ball number " + b.number + ".",
                    risk = "Object balls arranged into a winning layout give an instant, unearned victory.",
                    defense = "Treat the whole table layout as one signed unit. Verify the signature before the layout is used to decide anything."
                )
            )
            list.add(
                StateVariable(
                    name = "balls[" + i + "].velocity",
                    path = "GameState -> Balls[" + i + "] -> Velocity",
                    type = "Vector2f (vx, vy)",
                    value = String.format(Locale.US, "(%.2f, %.2f)", b.vx, b.vy),
                    address = SimulatedAddresses.field(SimulatedAddresses.ballBase(i), 0x08),
                    description = "Velocity of object ball number " + b.number + ".",
                    risk = "Injected velocity moves balls without a shot, which breaks the link between input and outcome.",
                    defense = "Derive motion only from an accepted shot. Never accept motion as an input in its own right."
                )
            )
        }

        list.add(
            StateVariable(
                name = "shotPower",
                path = "GameState -> Shot -> Power",
                type = "Float (0.0 .. 1.0)",
                value = String.format(Locale.US, "%.2f", state.shotPower),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x10),
                description = "Normalised power of the last shot.",
                risk = "A power outside the normalised range produces motion the game was never designed to allow.",
                defense = "Clamp on receipt as well as on send. An input validated only by the sender is not validated."
            )
        )

        list.add(
            StateVariable(
                name = "shotAngle",
                path = "GameState -> Shot -> AngleDeg",
                type = "Float (degrees)",
                value = String.format(Locale.US, "%.1f", state.shotAngleDeg),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x14),
                description = "Direction of the last shot in degrees.",
                risk = "Angle by itself is harmless, but with power it is the whole input to the simulation, so it is the value an automated aiming tool would drive.",
                defense = "Send the shot input and let the authoritative simulation produce the outcome, rather than sending the outcome."
            )
        )

        list.add(
            StateVariable(
                name = "remainingBalls",
                path = "GameState -> Match -> RemainingBalls",
                type = "Int",
                value = state.balls.count { !it.potted && !it.isCue }.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x18),
                description = "How many object balls are still on the table.",
                risk = "A derived counter that is stored rather than recomputed can drift out of agreement with the array it summarises, and disagreement is exactly what an attacker exploits.",
                defense = "Recompute derived values from the underlying data on the authoritative side instead of trusting a stored counter."
            )
        )

        list.add(
            StateVariable(
                name = "playerScore",
                path = "GameState -> Player -> Score",
                type = "Int (0 .. 7)",
                value = state.playerOneScore.toString() + " / opponent " + state.playerTwoScore,
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x08),
                description = "Number of balls of the player own group that have been potted.",
                risk = "Score written straight to 7 is an instant win in any design where the client reports the score.",
                defense = "Score is a consequence of accepted shots. Recompute it, do not receive it."
            )
        )

        list.add(
            StateVariable(
                name = "virtualBalance",
                path = "GameState -> Player -> Balance",
                type = "Long (coins)",
                value = state.virtualBalance.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.PLAYER_STATE, 0x10),
                description = "Simulated in-app currency held for this laboratory session only. It buys nothing and has no value.",
                risk = "This is the classic target. If the client is the only place the balance lives, then whoever controls the device controls the economy of the game.",
                defense = "Keep the authoritative balance on the server, change it only through server side transactions, and treat the client copy as a display cache."
            )
        )

        list.add(
            StateVariable(
                name = "matchState",
                path = "GameState -> Match -> Phase",
                type = "Enum",
                value = state.matchPhase.name,
                address = SimulatedAddresses.field(SimulatedAddresses.MATCH_STATE, 0x1C),
                description = "Whether the match is ready, in motion, or finished.",
                risk = "Forcing the phase straight to a win state skips every rule between the break and the result.",
                defense = "Allow only legal transitions, and enforce that state machine where the attacker cannot reach it."
            )
        )

        list.add(
            StateVariable(
                name = "gameState.sequence",
                path = "GameState -> Sequence",
                type = "Long (monotonic)",
                value = state.stateSequence.toString(),
                address = SimulatedAddresses.field(SimulatedAddresses.GAME_STATE, 0x08),
                description = "Counter incremented by every sanctioned change to the state.",
                risk = "If a counter can move backwards, an old but validly signed state can be replayed, and a signature alone will not notice.",
                defense = "Require the counter to increase strictly, and bind it into the signed payload so a rewind invalidates the signature."
            )
        )

        list.add(
            StateVariable(
                name = "timestamp",
                path = "GameState -> LastUpdate",
                type = "Long (epoch millis)",
                value = state.lastUpdateMillis.toString() + "  (" + timeFormat.format(Date(state.lastUpdateMillis)) + ")",
                address = SimulatedAddresses.field(SimulatedAddresses.GAME_STATE, 0x10),
                description = "When the state was last modified, taken from the device clock.",
                risk = "The device clock belongs to the device owner. Any expiry, cooldown or rate limit that depends on it can be moved at will.",
                defense = "Use server time for anything security relevant. Treat client timestamps as a hint for the user interface only."
            )
        )

        return list
    }
}
```

## `app/src/main/java/com/university/poolseclab/security/StateVariable.kt`
```kotlin
package com.university.poolseclab.security

/**
 * One inspectable value, together with the teaching material that belongs to it.
 *
 * [path] uses the logical notation asked for in the assignment, for example
 * "GameState -> Player -> Balance". [address] is a simulated address only.
 */
data class StateVariable(
    val name: String,
    val path: String,
    val type: String,
    val value: String,
    val address: String,
    val description: String,
    val risk: String,
    val defense: String
)
```

## `app/src/main/java/com/university/poolseclab/security/TamperLabFragment.kt`
```kotlin
package com.university.poolseclab.security

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.university.poolseclab.core.GameHolder
import com.university.poolseclab.databinding.FragmentTamperBinding
import com.university.poolseclab.game.TableSpec

/**
 * The tamper demonstration.
 *
 * Every button below writes to a field of THIS application, bypassing the
 * sanctioned setters in PoolGame. That models what an attacker with access to
 * the process would do, without touching anything outside the app sandbox.
 */
class TamperLabFragment : Fragment() {

    private var binding: FragmentTamperBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentTamperBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return
        val state = GameHolder.game.state
        val guard = GameHolder.guard

        b.defensesText.text = DefenseCatalog.asText()

        b.sealBtn.setOnClickListener {
            guard.seal()
            b.statusBanner.setTextColor(Color.parseColor("#4FC3F7"))
            b.statusBanner.text = "BASELINE SEALED"
            b.reportText.text = buildString {
                append("Trusted baseline recorded at ").append(guard.sealedAtText).append('\n')
                append("Sequence   : ").append(state.stateSequence).append('\n')
                append("Balance    : ").append(state.virtualBalance).append('\n')
                append("Score      : ").append(state.playerOneScore).append(" - ").append(state.playerTwoScore).append('\n')
                append("HMAC tag   : ").append(guard.currentTag().take(32)).append("...\n\n")
                append("Now apply a simulated modification, then run the integrity check.")
            }
        }

        b.verifyBtn.setOnClickListener { renderReport(guard.verify()) }

        b.restoreBtn.setOnClickListener {
            guard.restore()
            b.statusBanner.setTextColor(Color.parseColor("#4FC3F7"))
            b.statusBanner.text = "TRUSTED STATE RESTORED"
            b.reportText.text =
                "The sealed baseline was written back over the modified fields.\n\n" +
                        "This is what a server authoritative game does after it rejects a client " +
                        "report: it does not argue with the client, it simply resends the state " +
                        "that it, and not the client, considers true.\n\n" +
                        "Balance  : " + state.virtualBalance + "\n" +
                        "Score    : " + state.playerOneScore + " - " + state.playerTwoScore + "\n" +
                        "Sequence : " + state.stateSequence
        }

        // ---- Simulated unauthorised modifications --------------------
        b.tBalanceBtn.setOnClickListener {
            val before = state.virtualBalance
            state.virtualBalance = 999_999L      // direct write, no sanctioned setter
            announce("Balance overwritten directly: " + before + " -> " + state.virtualBalance)
        }

        b.tScoreBtn.setOnClickListener {
            val before = state.playerOneScore
            state.playerOneScore = 99
            announce("Score overwritten directly: " + before + " -> " + state.playerOneScore)
        }

        b.tTeleportBtn.setOnClickListener {
            val cue = state.cueBall
            val bx = cue.x
            val by = cue.y
            cue.x = -40f
            cue.y = -25f
            announce(
                "Cue ball teleported off the table: (" +
                        String.format("%.1f", bx) + ", " + String.format("%.1f", by) +
                        ") -> (-40.0, -25.0)"
            )
        }

        b.tSpeedBtn.setOnClickListener {
            val cue = state.cueBall
            cue.vx = TableSpec.MAX_SPEED * 12f
            cue.vy = 0f
            announce(
                "Impossible velocity injected: vx = " +
                        String.format("%.0f", cue.vx) +
                        ", simulation maximum is " + String.format("%.0f", TableSpec.MAX_SPEED)
            )
        }

        b.tReplayBtn.setOnClickListener {
            val before = state.stateSequence
            state.stateSequence = if (before >= 5L) before - 5L else 0L
            announce("Sequence counter rewound: " + before + " -> " + state.stateSequence)
        }

        // ---- Control experiment -------------------------------------
        b.tLegitBtn.setOnClickListener {
            GameHolder.game.awardCoinsSanctioned(25L)
            GameHolder.guard.seal()
            b.statusBanner.setTextColor(Color.parseColor("#2E7D32"))
            b.statusBanner.text = "SANCTIONED CHANGE APPLIED"
            b.reportText.text =
                "25 coins were added through awardCoinsSanctioned(), the only method the " +
                        "rules layer allows to change the balance. The sequence counter advanced and " +
                        "the baseline was re-sealed.\n\n" +
                        "Run the integrity check now: it reports no violation.\n\n" +
                        "That is the distinction the whole exercise turns on. Integrity checking " +
                        "does not ask whether a value changed. It asks whether the change came " +
                        "through a path that was allowed to make it.\n\n" +
                        "Balance  : " + state.virtualBalance + "\n" +
                        "Sequence : " + state.stateSequence
        }
    }

    private fun announce(what: String) {
        val b = binding ?: return
        b.statusBanner.setTextColor(Color.parseColor("#F9A825"))
        b.statusBanner.text = "MODIFICATION APPLIED - NOT YET CHECKED"
        b.reportText.text = what + "\n\nNow press RUN INTEGRITY CHECK."
    }

    private fun renderReport(report: IntegrityReport) {
        val b = binding ?: return

        if (report.neverSealed) {
            b.statusBanner.setTextColor(Color.parseColor("#F9A825"))
            b.statusBanner.text = "NO BASELINE"
            b.reportText.text = "Press SEAL BASELINE first. There is nothing to compare against yet."
            return
        }

        val sb = StringBuilder()
        sb.append("Detection time : ").append(report.detectedAt).append('\n')
        sb.append("Sealed at      : ").append(GameHolder.guard.sealedAtText).append('\n')
        sb.append("Expected tag   : ").append(report.expectedTag.take(32)).append("...\n")
        sb.append("Actual tag     : ").append(report.actualTag.take(32)).append("...\n")
        sb.append("Sequence       : sealed ").append(report.sealedSequence)
            .append(", current ").append(report.currentSequence).append('\n')
        sb.append('\n')

        if (report.ok) {
            b.statusBanner.setTextColor(Color.parseColor("#2E7D32"))
            b.statusBanner.text = "INTEGRITY: OK"
            sb.append("Integrity status : OK\n")
            sb.append("No unexpected change was found. Every protected field matches the sealed baseline and every range, geometry, physics and replay check passed.")
        } else {
            b.statusBanner.setTextColor(Color.parseColor("#C62828"))
            b.statusBanner.text = "WARNING: GAME-STATE INTEGRITY VIOLATION DETECTED"
            sb.append("Integrity status : VIOLATION\n")
            sb.append("Findings         : ").append(report.findings.size).append("\n\n")
            for ((i, f) in report.findings.withIndex()) {
                sb.append(i + 1).append(") ").append(f.field).append('\n')
                sb.append("    original value : ").append(f.originalValue).append('\n')
                sb.append("    current value  : ").append(f.currentValue).append('\n')
                sb.append("    triggered rule : ").append(f.rule).append('\n')
                if (i != report.findings.lastIndex) sb.append('\n')
            }
            sb.append("\n\nHonest note: this check runs in the same process as the value it is ")
            sb.append("checking, so an attacker who can change the balance can also change this ")
            sb.append("verifier or read its key. The check is a speed bump on the client and a ")
            sb.append("real control only when it runs on a server.")
        }

        b.reportText.text = sb.toString()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
```
