rootProject.name = "initiative-bot"
enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("initiative-bot-core")
include("initiative-bot-discord")

if (System.getenv("GITHUB_ACTIONS") != null) {
    buildCache {
        remote<HttpBuildCache> {
            url = uri("http://localhost:3696/")
            isAllowInsecureProtocol = true
        }
    }
}
