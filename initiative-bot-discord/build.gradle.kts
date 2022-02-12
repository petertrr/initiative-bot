import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.Paths

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

application {
    mainClass.set("io.github.petertrr.initbot.discord.MainKt")
    applicationName = "${project.name}-${project.version}"
}

tasks.register<Copy>("prepareDockerWorkspace") {
    val applicationName = "${project.name}-${project.version}"
    val installDistPath = Paths.get("$buildDir/install/$applicationName")
    val dockerfile = Paths.get("$buildDir/docker/Dockerfile")

    dependsOn("installDist")
    from(installDistPath)
    into("$buildDir/docker/workspace")
    doLast {
        if (!Files.exists(dockerfile)) Files.createFile(dockerfile)
        dockerfile.toFile().writeText(
            """
                FROM eclipse-temurin:17-jre
                WORKDIR /workspace
                COPY workspace/bin bin/
                COPY workspace/lib lib/
                ENTRYPOINT ["/workspace/bin/$applicationName"]
            """.trimIndent()
        )
    }
}

tasks.register<Exec>("runDockerBuildx") {
    val buildDirPath = "${buildDir.absolutePath}"

    dependsOn("prepareDockerWorkspace")
    workingDir("$buildDirPath/docker")
    val imageVersion = rootProject.version.toString().replace('+', '-')
    commandLine(
        "docker",
        "buildx",
        "build",
        "--platform",
        "linux/amd64,linux/arm64,linux/arm/v7",
        "--tag",
        "ghcr.io/petertrr/initiative-bot-discord:$imageVersion",
        "--tag",
        "ghcr.io/petertrr/initiative-bot-discord:latest",
        "--push",
        "."
    )
}
