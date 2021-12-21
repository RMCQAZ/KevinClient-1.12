package kevin.module

enum class ModuleCategory(val readableName: String) {
    COMBAT("Combat"),
    EXPLOIT("Exploit"),
    MISC("Misc"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World")
    ;

    companion object {
        fun fromReadableName(name: String) = values().find { name.equals(it.name, true) }
    }
}