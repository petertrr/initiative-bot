plugins {
    `common-kotlin-jvm-configuration`
    `code-coverage`
}

dependencies {
    testImplementation("org.mockito:mockito-core:${Versions.mockito}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.mockito}")
}
