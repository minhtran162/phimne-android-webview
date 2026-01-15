import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val staticServer : String = localProperties.getProperty("STATIC_SERVER")
    ?: project.findProperty("STATIC_SERVER") as? String
    ?: ""

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.minhtran.phimne"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minhtran.phimne"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "STATIC_SERVER",
            "\"$staticServer\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    buildToolsVersion = "35.0.0"
    ndkVersion = "25.1.8937393"
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.webkit)
    implementation(libs.play.services.base)
    implementation (libs.androidx.core)
    implementation (libs.androidx.appcompat)
    implementation (libs.androidx.constraintlayout)

    // Optional: Only add if you want advanced WebView features
    implementation (libs.androidx.webkit)
}