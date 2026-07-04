// Pure JVM module: no Android SDK, no android.* imports allowed here.
// This is what keeps TipEngine unit-testable on the plain JVM (QR1) and keeps
// business logic decoupled from the platform (constraint #6).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Flow is a plain Kotlin/coroutines primitive (no Android dependency), needed here so the
    // repository interfaces (SettingsRepository, TipHistoryRepository) can be defined in the
    // domain layer per the Dependency Inversion Principle, with DataStore-backed
    // implementations living in :app.
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
