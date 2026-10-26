plugins { id("com.android.application") }

android {
    namespace = "com.piaochu.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.piaochu.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions { animationsDisabled = true }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // 默认连接 Android Studio 模拟器宿主机；真机可在 App 的联调设置中修改。
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8090/\"")
        }
        release {
            isMinifyEnabled = false
            // 发布地址由构建参数提供，避免把本地 HTTP 地址意外装进正式包。
            val endpoint = providers.gradleProperty("releaseApiUrl").getOrElse("https://api.example.invalid/")
            require(endpoint.startsWith("https://") && !endpoint.contains('"') && !endpoint.contains('\\'))
            buildConfigField("String", "API_BASE_URL", "\"$endpoint\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
