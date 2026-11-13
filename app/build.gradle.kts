plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.pure.chageumtong"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pure.chageumtong"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 서명 키·비밀번호는 레포 밖(~/keys, ~/.gradle/gradle.properties)에 둔다. 없으면 release는 미서명으로 빌드됨
    val storeFilePath = providers.gradleProperty("CHAGEUMTONG_STORE_FILE").orNull
    signingConfigs {
        if (storeFilePath != null) create("release") {
            val pw = providers.gradleProperty("CHAGEUMTONG_STORE_PASSWORD").get()
            storeFile = file(storeFilePath)
            storePassword = pw
            keyAlias = "chageumtong"
            keyPassword = pw
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            // R8을 켜면 kotlin 클래스 이름이 바뀌어 Pleos 기어 수신에서 IllegalAccessError로 죽음 (step 7 확인) → 끔
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // 차속(PERF_VEHICLE_SPEED)은 Vehicle SDK 2.3.2에 API가 없어 AAOS 표준 Car API로 읽는다
    useLibrary("android.car")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.tts)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}