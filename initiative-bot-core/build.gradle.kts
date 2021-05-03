plugins {
    kotlin("jvm") version Versions.kotlin
    jacoco
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.mockito}")
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.6"
}
