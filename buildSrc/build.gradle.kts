plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("gradle-plugin", "1.5.30"))
    implementation("com.github.ajoberstar.reckon:reckon-gradle:PR160-SNAPSHOT")
}