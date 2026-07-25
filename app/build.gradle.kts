plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tarjaman.offline"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tarjaman.offline"
        // Android 8.0 (Oreo) فما فوق - يغطي كل أجهزة Redmi/الأجهزة الضعيفة تقريباً
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // توقيع تصحيح موحّد حتى تُثبَّت نسخة debug بثبات بجانب أي نسخة أخرى
        // في الإنتاج الحقيقي يجب استبدال هذا بمفتاح توقيع خاص بك (release keystore)
        getByName("debug") {
            // يستخدم مفتاح debug الافتراضي من Android Studio تلقائياً
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // تقسيم الـ APK حسب معمارية المعالج يقلل حجم كل APK بشكل كبير
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // لا حاجة لـ core library desugaring لأن minSdk 26 يدعم معظم واجهات java.time
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xsuppress-version-warnings")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    // تفعيل App Bundle مع تقسيم الموارد حسب اللغة والكثافة لتقليل حجم التنزيل من المتجر
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {
    // --- Core / Kotlin ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // --- Jetpack Compose (Material 3) عبر BOM لضمان توافق الإصدارات ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- التنقل بين الشاشات ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- تخزين تفضيلات المستخدم (خفيف جداً مقارنة بقواعد بيانات كاملة) ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- ML Kit Translate: الترجمة الكاملة على الجهاز (Offline) ---
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")

    // --- ML Kit Text Recognition: قراءة النص من لقطة الشاشة (OCR) على الجهاز بالكامل، لازمة للمترجم العائم ---
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // --- Splash Screen حديث متوافق مع كل الإصدارات ---
    implementation("androidx.core:core-splashscreen:1.0.1")

    // --- اختبارات أساسية ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
