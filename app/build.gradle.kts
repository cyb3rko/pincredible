import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.kotlin.dsl.libs

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinter) // lintKotlin, formatKotlin
    alias(libs.plugins.dexcount) // :app:countReleaseDexMethods
    alias(libs.plugins.bundletool)
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "de.cyb3rko.pincredible"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.cyb3rko.pincredible"
        minSdk = 23
        targetSdk = 35
        versionCode = 13
        versionName = "1.0.0"
        resValue("string", "app_name", "PINcredible Dev")
        resValue("string", "version_name", "Pre-Release v${versionName}")
        signingConfig = signingConfigs.getByName("debug")
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "PINcredible DEV")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            resValue("string", "app_name", "PINcredible")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    bundle {
        storeArchive {
            enable = false
        }
    }
    packaging {
        resources {
            excludes.add("META-INF/*.version")
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

// Automatic pipeline build
// build with '-Psign assembleRelease'
// output at 'app/build/outputs/apk/release/app-release.apk'
// build with '-Psign bundleRelease'
// output at 'app/build/outputs/bundle/release/app-release.aab'
if (project.hasProperty("sign")) {
    android {
        signingConfigs {
            create("release") {
                enableV3Signing = true
                enableV4Signing = true
                storeFile = file(System.getenv("KEYSTORE_FILE"))
                storePassword = System.getenv("KEYSTORE_PASSWD")
                keyAlias = System.getenv("KEYSTORE_KEY_ALIAS")
                keyPassword = System.getenv("KEYSTORE_KEY_PASSWD")
            }
        }
    }
    android.buildTypes.getByName("release").signingConfig =
        android.signingConfigs.getByName("release")
}

// Automatic pipeline build for Accrescent
// build with '-Pmanual_upload_oss buildApksRelease'
// output at 'app/build/outputs/apkset/release/app-release.apks'
if (project.hasProperty("manual_upload_oss")) {
    bundletool {
        signingConfig {
            storeFile = file(System.getenv("KEYSTORE_FILE"))
            storePassword = System.getenv("KEYSTORE_PASSWD")
            keyAlias = System.getenv("KEYSTORE_KEY_ALIAS")
            keyPassword = System.getenv("KEYSTORE_KEY_PASSWD")
        }
    }
}

// Manual Google Play Store build
// build with '-Pmanual_upload bundleRelease'
// output at 'app/build/outputs/bundle/release/app-release.aab'
if (project.hasProperty("manual_upload")) {
    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())
    android {
        signingConfigs {
            create("upload") {
                enableV3Signing = true
                enableV4Signing = true
                storeFile = file(properties.getProperty("uploadsigning.file"))
                storePassword = properties.getProperty("uploadsigning.password")
                keyAlias = properties.getProperty("uploadsigning.key.alias")
                keyPassword = properties.getProperty("uploadsigning.key.password")
            }
        }
    }
    android.buildTypes.getByName("release").signingConfig = android.signingConfigs.getByName("upload")
}

dependencies {
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.preference)
    implementation(project(":backpack"))
    debugImplementation(libs.leakcanary)

//    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
//    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
//    testImplementation 'junit:junit:4.13.2'
}
