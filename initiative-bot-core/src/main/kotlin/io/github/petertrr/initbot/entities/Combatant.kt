package io.github.petertrr.initbot.entities

data class Combatant(
    val name: String,
    val baseModifier: Int,
    var currentInitiative: Int? = null,
) {
    internal fun getCurrentInitiativeSafe() =
        requireNotNull(currentInitiative) { "Combatant $name has unset initiative, but new round has been requested" }
}
