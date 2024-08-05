plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.20-dev1646"
}

group = "dev.lunasa"
version = "1.0-SNAPSHOT"

val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
val targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val skVersion = "0.8.9" // or any more recent version
val target = "${targetOs}-${targetArch}"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:3.3.4"))

    implementation("org.jetbrains.skiko:skiko-awt-runtime-$target:$skVersion")
    implementation(project(":compose:desktop:desktop"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    runtimeOnly("org.lwjgl:lwjgl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl::natives-windows")
}

kotlin {
    jvmToolchain(17)
}