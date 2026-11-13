package nz.ninjagoldfinch.minecraft;

/** The per-player displays a player can subscribe to. */
enum Display {
    TABLIST("tablist", "Tab list"),
    SCOREBOARD("scoreboard", "Scoreboard");

    /** Used as the command argument, the config section and the stored preference key. */
    final String id;
    final String label;

    Display(String id, String label) {
        this.id = id;
        this.label = label;
    }
}
