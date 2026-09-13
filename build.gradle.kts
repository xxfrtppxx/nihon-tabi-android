plugins {
    // AGP 9's built-in Kotlin support means `org.jetbrains.kotlin.android` is
    // never applied in this project (see app/build.gradle.kts).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
}
