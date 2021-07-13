import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    java

    `maven-publish`
    checkstyle
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val buildExtras = System.getenv("BUILD_EXTRAS")?.toBoolean() ?: false

repositories {
    mavenLocal()

    maven("https://repo.glowstone.net/repository/maven-public/")
    maven("https://repo.glowstone.net/repository/snapshots/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation(libs.bundles.linkstone)
    implementation(libs.bundles.glowstone)
    implementation(libs.bundles.kotlin)

    implementation(libs.jansi)
    implementation(libs.jline)
    implementation(libs.fastutil)
    implementation(libs.flow)
    implementation(libs.fastuuid)
    implementation(libs.brigadier)
    implementation(libs.gluegen)
    implementation(libs.jocl)
    implementation(libs.naether)
    implementation(libs.maven.artifact)

    runtimeOnly(libs.slf4j.jdk14)

    testImplementation(libs.bundles.junit)

    testImplementation(libs.hamcrest)
    testImplementation(libs.bundles.powermock)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    compileOnly(libs.jetbrains.annotations)
}

group = "net.glowstone"
version = "2021.7.1-SNAPSHOT"
description = "A fast, customizable and compatible open source Minecraft server."


publishing {
    publications {
        create<MavenPublication>("maven") {
            project.shadow.component(this)
        }
    }
}

checkstyle {
    configFile = File(project.projectDir, "/etc/checkstyle.xml")
    configProperties["checkstyle.header.file"] = File(project.projectDir, "LICENSE")
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = true
}

fun getGitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8

    if (buildExtras) {
        withSourcesJar()
        withJavadocJar()
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
    manifest {
        attributes(
                "Launcher-Agent-Class" to "net.glowstone.util.ClassPathAgent",
                "Main-Class" to "net.glowstone.GlowServer",

                "Implementation-Title" to project.name,
                "Implementation-Version" to "${project.version}.git-${project.name}-${getGitHash()}",
                "Implementation-Vendor" to Date(),

                "Specification-Title" to "Bukkit",
                "Specification-Version" to libs.versions.api.get()
        )
    }
}

tasks.shadowJar {
    relocate("jline", "org.bukkit.craftbukkit.libs.jline")
    relocate("it.unimi", "org.bukkit.craftbukkit.libs.it.unimi")

    exclude("mojang-translations/*")
}

tasks.assemble { dependsOn(tasks.shadowJar) }
