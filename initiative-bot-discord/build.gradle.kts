plugins {
    kotlin("jvm") version Versions.kotlin
}

dependencies {
    implementation(project(":initiative-bot-core"))
    implementation("com.discord4j:discord4j-core:${Versions.discord4j}")
}
