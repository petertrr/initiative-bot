plugins {
    id("com.gradle.enterprise") version("3.11.4")
}

rootProject.name = "initiative-bot"

include("initiative-bot-core")
include("initiative-bot-discord")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

if (System.getenv("CI") != null) {
    gradleEnterprise {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
