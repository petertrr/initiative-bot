import io.github.petertrr.buildutils.configureJacoco

plugins {
    kotlin("jvm") version Versions.kotlin
    application
}

dependencies {
    implementation(project(":initiative-bot-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.coroutines}")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.3")
    implementation("com.discord4j:discord4j-core:${Versions.discord4j}")
    implementation("io.github.microutils:kotlin-logging:2.0.8")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
    runtimeOnly("ch.qos.logback:logback-classic:${Versions.logback}")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
}

tasks.test {
    useJUnitPlatform()
}

configureJacoco()

application {
    mainClass.set("io.github.petertrr.initbot.discord.MainKt")
    applicationName = "${project.name}-${project.version}"
}
