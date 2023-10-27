import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.compose") version "1.5.3"
}
dependencies {

    implementation(compose.desktop.currentOs)

    implementation("junit:junit:4.13.2")
    implementation("com.malinskiy.adam:adam:0.5.1")

    implementation("com.jcraft:jsch:0.1.55")
    implementation("io.netty:netty-all:4.1.68.Final")
    //implementation("com.malinskiy.adam:android-junit4:0.5.0")
    //implementation("com.malinskiy.adam:android-junit4-test-annotation-producer:0.5.0")
}
compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopApplication"
            packageVersion = "1.0.0"
            macOS {

            }
            linux {

            }
        }
    }
}