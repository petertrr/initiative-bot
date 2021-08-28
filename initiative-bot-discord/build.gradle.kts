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
