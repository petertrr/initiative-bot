plugins {
    id("org.ajoberstar.reckon")
    id("com.github.ben-manes.versions") version "0.39.0"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

reckon {
    scopeFromProp()
    stageFromProp("alpha", "rc", "final")
}
