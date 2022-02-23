plugins {
    id("org.ajoberstar.reckon")
    id("com.github.ben-manes.versions") version "0.42.0"
}

reckon {
    scopeFromProp()
    stageFromProp("alpha", "rc", "final")
}
