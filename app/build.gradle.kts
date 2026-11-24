import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    // Aboutページで表示する依存ライブラリとライセンス情報をビルド時に収集する
    id("com.mikepenz.aboutlibraries.plugin.android")
}

// リリース署名の設定。
// リポジトリ直下の keystore.properties（.gitignore 済み）が存在する場合のみ署名を行う。
// ファイルが無い場合は未署名の Release APK が出力される。
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword").forEach { key ->
            requireNotNull(getProperty(key)) {
                "keystore.properties の '$key' が未設定です。keystore.properties.example を参照してください。"
            }
        }
    }
}

android {
    namespace = "com.amesingularity.misskeysharer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.amesingularity.misskeysharer"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("keystore.properties が見つからないため、リリース APK は未署名になります。")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Compose Preview は各ビルドバリアントでユニットテストが有効であることを要求する
// （リソース APK に Android XML リソースやテーマをパッケージするため）。
// AGP 9.x では release バリアントのユニットテストが既定で無効のため、明示的に有効化する。
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        (variantBuilder as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true
    }
}

dependencies {
    // Jetpack Compose (BOMでバージョン管理)
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Material Icons (BOMでバージョン管理)
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Misskey API (HTTP) 通信用
    implementation("com.squareup.okhttp3:okhttp:5.5.1")

    // Aboutページでのライセンス一覧表示
    implementation("com.mikepenz:aboutlibraries-compose-m3:15.3.0")

    // 単体テスト
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
}
