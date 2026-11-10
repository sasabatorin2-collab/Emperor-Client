package dev.emperor.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Waypoints {
    public record Waypoint(String name, int x, int y, int z, String dim, int color) {
    }

    private static final int[] PALETTE = {0xFFFFD36E, 0xFFB45CFF, 0xFF4FE3FF, 0xFF4CFFB0, 0xFFFF6F8E, 0xFFFFA14C, 0xFF9FB4FF};
    private static final List<Waypoint> LIST = new ArrayList<>();

    private Waypoints() {
    }

    public static int colorFor(String name) {
        return PALETTE[Math.floorMod(name.toLowerCase(Locale.ROOT).hashCode(), PALETTE.length)];
    }

    /** Adds or replaces a waypoint with the same name. */
    public static Waypoint set(String name, int x, int y, int z, String dim) {
        remove(name);
        Waypoint w = new Waypoint(name, x, y, z, dim, colorFor(name));
        LIST.add(w);
        return w;
    }

    public static boolean remove(String name) {
        return LIST.removeIf(w -> w.name().equalsIgnoreCase(name));
    }

    public static void clear() {
        LIST.clear();
    }

    public static List<Waypoint> all() {
        return Collections.unmodifiableList(LIST);
    }

    public static List<Waypoint> in(String dim) {
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : LIST) if (w.dim().equals(dim)) out.add(w);
        return out;
    }

    public static JsonArray save() {
        JsonArray a = new JsonArray();
        for (Waypoint w : LIST) {
            JsonObject o = new JsonObject();
            o.addProperty("name", w.name());
            o.addProperty("x", w.x());
            o.addProperty("y", w.y());
            o.addProperty("z", w.z());
            o.addProperty("dim", w.dim());
            a.add(o);
        }
        return a;
    }

    public static void load(JsonArray a) {
        LIST.clear();
        for (JsonElement e : a) {
            JsonObject o = e.getAsJsonObject();
            set(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt(),
                    o.get("dim").getAsString());
        }
    }
}
