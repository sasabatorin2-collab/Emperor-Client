package dev.emperor.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.awt.Color;

/** ARGB colour edited with hue / saturation / brightness sliders. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int def) {
        super(name, def | 0xFF000000);
    }

    public float[] hsb() {
        int v = value;
        return Color.RGBtoHSB((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF, null);
    }

    public void setHsb(float h, float s, float b) {
        value = Color.HSBtoRGB(h, s, b) | 0xFF000000;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(String.format("#%06X", value & 0xFFFFFF));
    }

    @Override
    public void load(JsonElement e) {
        String s = e.getAsString().replace("#", "");
        value = Integer.parseUnsignedInt(s, 16) | 0xFF000000;
    }
}
