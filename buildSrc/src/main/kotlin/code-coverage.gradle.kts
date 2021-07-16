import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.named<Test>("test") {
    finalizedBy("jacocoTestReport")
}
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named<Test>("test"))
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}
