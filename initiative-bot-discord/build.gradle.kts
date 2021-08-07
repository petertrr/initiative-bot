plugins {
    `common-kotlin-jvm-configuration`
    application
    `code-coverage`
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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
}

application {
    mainClass.set("io.github.petertrr.initbot.discord.MainKt")
    applicationName = "${project.name}-${project.version}"
}
