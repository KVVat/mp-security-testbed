import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
    //need to configure wire plugin for compile protocol bufffer.
    //google's plugin would not work fine for
    id("com.google.protobuf") version "0.9.3"
    id("java")
}
dependencies {
    implementation(compose.desktop.currentOs)
    protobuf(project(":proto"))

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
    implementation ("jaxen:jaxen:1.1.6")
    implementation ("org.dom4j:dom4j:2.1.1")
    //Settings Library
    implementation("com.russhwolf:multiplatform-settings:1.1.1")
    //implementation("com.malinskiy.adam:android-junit4:0.5.0")
    //implementation("com.malinskiy.adam:android-junit4-test-annotation-producer:0.5.0")
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.components.resources)
    //implementation(project(":shared"))

    //("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-stub:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-protobuf:${rootProject.ext["grpcVersion"]}")
    implementation("io.grpc:grpc-kotlin-stub:${rootProject.ext["grpcKotlinVersion"]}")
    implementation("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")
    implementation("com.google.protobuf:protobuf-java:${rootProject.ext["protobufVersion"]}")
    implementation("com.google.protobuf:protobuf-kotlin:${rootProject.ext["protobufVersion"]}")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.grpc:grpc-okhttp:${rootProject.ext["grpcVersion"]}")

}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Workaround for MPP plugin bug that doesn't put resources in classpath
// https://youtrack.jetbrains.com/issue/KT-24463
tasks.register<Copy>("copyResources"){
    dependsOn("createDistributable")
    dependsOn("packageMsi")
    dependsOn("packageDeb")
    //println("copyResourcs run")
    from("build/processedResources/jvm/main"){
    }
    into("build/classes/kotlin/jvm/main")
    into("build/compose/tmp/prepareAppResources")
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("classes"){
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
            //flag=on if release the package
            includeAllModules=true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            outputBaseDir.set(project.buildDir.resolve("output"))
        }
    }
}
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${rootProject.ext["protobufVersion"]}"
    }
    plugins {
        create("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
        }
        create("grpckt") {
            artifact =
                "io.grpc:protoc-gen-grpc-kotlin:${rootProject.ext["grpcKotlinVersion"]}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc") {
                }
                create("grpckt") {
                }
            }
            it.builtins {
                create("kotlin") {
                }
            }
        }
    }
}