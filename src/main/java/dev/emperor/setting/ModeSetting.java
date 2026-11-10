package dev.emperor.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = Arrays.asList(modes);
    }

    public void next() {
        int i = modes.indexOf(value);
        value = modes.get((i + 1) % modes.size());
    }

    public void prev() {
        int i = modes.indexOf(value);
        value = modes.get((i - 1 + modes.size()) % modes.size());
    }

    public boolean is(String mode) {
        return value.equalsIgnoreCase(mode);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        String s = e.getAsString();
        if (modes.contains(s)) value = s;
    }
}
