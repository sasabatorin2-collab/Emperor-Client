package dev.nova.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc"),
    CLIENT("Client");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
