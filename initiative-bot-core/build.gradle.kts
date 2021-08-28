plugins {
    `common-kotlin-jvm-configuration`
    `code-coverage`
}

dependencies {
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
