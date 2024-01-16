import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    //kotlin("multiplatform")
    //kotlin("jvm") version "1.9.10"
    kotlin("multiplatform")
    //kotlin("jvm")
    id("org.jetbrains.compose") version "1.5.10"
}

kotlin {
    jvm {}

    sourceSets {
        val commonMain by getting {
            resources.srcDirs("src/commonMain/resources")
            resources.srcDirs.forEach{
                println(">"+it.absolutePath.toString())
            }
            dependencies {
                implementation(compose.desktop.currentOs)

                implementation("junit:junit:4.13.2")
                implementation("com.malinskiy.adam:adam:0.5.1")

                implementation("com.jcraft:jsch:0.1.55")
                implementation("io.netty:netty-all:4.1.68.Final")

                implementation ("org.apache.commons:commons-compress:1.20")
                implementation ("com.flipkart.zjsonpatch:zjsonpatch:0.4.14")
                implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
                implementation ("com.github.jsurfer:jsurfer-jackson:1.6.3")

                implementation("com.darkrockstudios:mpfilepicker:3.1.0")
                implementation("com.russhwolf:multiplatform-settings:1.1.1")

                //Settings Library
                implementation("com.russhwolf:multiplatform-settings:1.1.1")
                //implementation("com.malinskiy.adam:android-junit4:0.5.0")
                //implementation("com.malinskiy.adam:android-junit4-test-annotation-producer:0.5.0")
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                //implementation(project(":shared"))
            }
        }
    }
}

// Workaround for MPP plugin bug that doesn't put resources in classpath
// https://youtrack.jetbrains.com/issue/KT-24463
tasks.register<Copy>("copyResources"){
    dependsOn("createDistributable")
    dependsOn("packageMsi")
    //println("copyResourcs run")
    from("build/processedResources/jvm/main"){
    }
    into("build/classes/kotlin/jvm/main")
    into("build/compose/tmp/prepareAppResources")
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
tasks.named("jvmMainClasses"){
    //shouldRunAfter("compileKotlinJvm")
    finalizedBy("copyResources")
}

//./gradlew package
compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopApplication"
            packageVersion = "1.0.0"
            includeAllModules=true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            outputBaseDir.set(project.buildDir.resolve("output"))

        }
    }
}

