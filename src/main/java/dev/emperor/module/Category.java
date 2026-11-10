package dev.emperor.module;

public enum Category {
    COMBAT("Combat", "⚔"),
    MOVEMENT("Movement", "➶"),
    RENDER("Render", "◉"),
    WORLD("World", "⛏"),
    MISC("Misc", "✦"),
    CLIENT("Client", "♛");

    public final String label;
    public final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }
}
