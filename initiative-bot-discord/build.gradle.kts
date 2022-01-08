plugins {
    `common-kotlin-jvm-configuration`
    application
    `code-coverage`
    alias(libs.plugins.docker.java.application)
}

dependencies {
    implementation(projects.initiativeBotCore)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.discord4j.core)
    implementation(libs.microutils.logging)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("io.github.petertrr.initbot.discord.MainKt")
    applicationName = "${project.name}-${project.version}"
}

docker {
    javaApplication {
        mainClassName.set("io.github.petertrr.initbot.discord.MainKt")
        baseImage.set("eclipse-temurin:17-jre")
        maintainer.set("petertrr")
        ports.set(emptyList())
        jvmArgs.set(listOf("-Xmx256m"))
    }
}

tasks.register<Exec>("runDockerBuildx") {
    workingDir("$buildDir/docker")
    val imageVersion = rootProject.version.toString().replace('+', '-')
    commandLine(
        "docker",
        "buildx",
        "build",
        "--platform",
        "linux/amd64,linux/arm64,linux/arm/v7",
        "--tag",
        "ghcr.io/petertrr/initiative-bot-discord:$imageVersion",
        "."
    )
}
