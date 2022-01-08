plugins {
    `common-kotlin-jvm-configuration`
    application
    `code-coverage`
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

val applicationName = "${project.name}-${project.version}"
application {
    mainClass.set("io.github.petertrr.initbot.discord.MainKt")
    this.applicationName = applicationName
}

tasks.register<Copy>("generateDockerfile") {
    dependsOn("installDist")
    doFirst {
        mkdir("$buildDir/docker")
    }
    from("$buildDir/install/$applicationName")
    into("$buildDir/docker")
    doLast {
        file("$buildDir/install/$applicationName/Dockerfile").writeText(
            """
                FROM eclipse-temurin:17-jre
                WORKDIR /app
                COPY bin bin/
                COPY lib lib/
                ENTRYPOINT ["/workspace/bin/"]
            """.trimIndent()
        )
    }
}

tasks.register<Exec>("runDockerBuildx") {
    dependsOn("generateDockerfile")
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
        "--push",
        "."
    )
}
