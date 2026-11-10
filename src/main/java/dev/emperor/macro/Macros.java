package dev.emperor.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emperor.command.Commands;
import dev.emperor.hud.Notifications;
import dev.emperor.util.Keys;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Key -> chat message, server command ("/...") or client command ("...") shortcuts. */
public final class Macros {
    public static final class Macro {
        public int key;
        public String text;

        Macro(int key, String text) {
            this.key = key;
            this.text = text;
        }
    }

    private static final List<Macro> LIST = new ArrayList<>();

    private Macros() {
    }

    public static List<Macro> all() {
        return Collections.unmodifiableList(LIST);
    }

    /** Adds or replaces the macro on that key. */
    public static void set(int key, String text) {
        remove(key);
        LIST.add(new Macro(key, text));
    }

    public static boolean remove(int key) {
        return LIST.removeIf(m -> m.key == key);
    }

    public static void remove(Macro m) {
        LIST.remove(m);
    }

    public static void run(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        if (Commands.handle(text)) return;
        if (text.startsWith("/")) mc.getNetworkHandler().sendChatCommand(text.substring(1));
        else mc.getNetworkHandler().sendChatMessage(text);
    }

    public static void onPress(int code) {
        for (Macro m : new ArrayList<>(LIST)) {
            if (m.key != code) continue;
            run(m.text);
            Notifications.push("Macro " + Keys.name(m.key), m.text, Notifications.Kind.INFO);
        }
    }

    public static JsonArray save() {
        JsonArray a = new JsonArray();
        for (Macro m : LIST) {
            JsonObject o = new JsonObject();
            o.addProperty("key", m.key);
            o.addProperty("text", m.text);
            a.add(o);
        }
        return a;
    }

    public static void load(JsonArray a) {
        LIST.clear();
        for (JsonElement e : a) {
            JsonObject o = e.getAsJsonObject();
            LIST.add(new Macro(o.get("key").getAsInt(), o.get("text").getAsString()));
        }
    }
}
