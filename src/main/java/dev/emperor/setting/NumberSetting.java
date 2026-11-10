package dev.emperor.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    public final double min;
    public final double max;
    public final double step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) {
            c = Math.round(c / step) * step;
            c = Math.max(min, Math.min(max, c));
        }
        value = c;
    }

    public double d() {
        return value;
    }

    public float f() {
        return value.floatValue();
    }

    public int i() {
        return (int) Math.round(value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        set(e.getAsDouble());
    }
}
