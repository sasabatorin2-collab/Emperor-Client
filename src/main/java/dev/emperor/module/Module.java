package dev.emperor.module;

import dev.emperor.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = -1;
    boolean keyWasDown;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable();
        else onDisable();
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    /** Extra text shown next to the module name in the HUD list. */
    public String getInfo() {
        return null;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }

    /** Called at the START of every client tick (before the player reads input) while enabled. */
    public void onPreTick() {
    }

    /** Whether the enabled/disabled state is restored from the config on startup. */
    public boolean saveEnabled() {
        return true;
    }

    /** Called every frame while enabled; draw through-walls geometry with RenderUtil. */
    public void onRender3d(float delta) {
    }
}
