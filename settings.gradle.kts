rootProject.name = "initiative-bot"

include("initiative-bot-core")
include("initiative-bot-discord")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}