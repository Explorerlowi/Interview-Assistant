import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val desktopAppVersion = Properties().run {
    project.file("src/desktopMain/resources/app.properties").inputStream().use(::load)
    requireNotNull(getProperty("version")) { "Missing desktop app version" }
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":core:design"))
                implementation(compose.desktop.currentOs)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.koin.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.example.interviewassistant.desktop.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "Interview Assistant"
            packageVersion = desktopAppVersion
            // SQLDelight JDBC driver needs java.sql; jlink omits it unless listed explicitly.
            modules("java.sql")
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
        }
    }
}
