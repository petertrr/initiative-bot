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
    registryCredentials {
        url.set("https://ghcr.io")
        username.set("petertrr")
        password.set(System.getenv("GHCR_PWD"))
    }

    javaApplication {
        mainClassName.set("io.github.petertrr.initbot.discord.MainKt")
        baseImage.set("openjdk:17-slim")
        maintainer.set("petertrr")
        ports.set(emptyList())
        val imageVersion = rootProject.version.toString().replace('+', '-')
        images.set(setOf("ghcr.io/petertrr/initiative-bot-discord:$imageVersion"))
        jvmArgs.set(listOf("-Xmx256m"))
    }
}
