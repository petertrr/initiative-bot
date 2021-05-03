plugins {
    kotlin("jvm") version "1.4.32"
    jacoco
}

dependencies {
//    implementation("com.discord4j:discord4j-core:3.1.5")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.mockito:mockito-core:3.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.9.0")
}

tasks.test {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.6"
}
