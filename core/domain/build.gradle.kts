plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "jp.co.testreason.core.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
