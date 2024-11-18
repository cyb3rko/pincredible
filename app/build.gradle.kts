import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.kotlin.dsl.libs

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinter) // lintKotlin, formatKotlin
    alias(libs.plugins.dexcount) // :app:countReleaseDexMethods
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "com.cyb3rko.pincredible"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cyb3rko.pincredible"
        minSdk = 23
        targetSdk = 35
        versionCode = 12
        versionName = "1.0.0b"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

if (project.hasProperty("sign")) {
    android {
        signingConfigs {
            create("release") {
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
