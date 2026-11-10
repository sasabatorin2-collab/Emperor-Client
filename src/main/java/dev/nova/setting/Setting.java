package dev.nova.setting;

import com.google.gson.JsonElement;

public abstract class Setting<T> {
    public final String name;
    protected T value;

    protected Setting(String name, T def) {
        this.name = name;
        this.value = def;
    }

    public T get() {
        return value;
    }

    public void set(T v) {
        this.value = v;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);
}
