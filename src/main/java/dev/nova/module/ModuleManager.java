package dev.nova.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nova.modules.*;
import dev.nova.setting.Setting;
import dev.nova.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final Logger LOG = LoggerFactory.getLogger("Nova");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    public static void init() {
        MODULES.clear();
        // Combat
        register(new KillAura());
        register(new AutoTotem());
        // Movement
        register(new Sprint());
        register(new Speed());
        register(new Flight());
        register(new Spider());
        register(new NoFall());
        register(new Step());
        register(new Jesus());
        register(new AutoWalk());
        // Render
        register(new Esp());
        register(new ChestEsp());
        register(new BaseFinder());
        register(new OreEsp());
        register(new Tracers());
        register(new Fullbright());
        register(new Freecam());
        register(new HoleEsp());
        register(new ItemEsp());
        // World
        register(new Nuker());
        register(new Scaffold());
        register(new AutoTool());
        register(new ChestStealer());
        // Misc
        register(new AutoEat());
        register(new AutoLog());
        register(new Notifier());
        register(new AntiAfk());
        register(new AutoRespawn());
        register(new DeathCoords());
        // Client
        register(new HudModule());
        register(new ClickGuiModule());

        get(HudModule.class).setEnabled(true);
        load();
    }

    private static void register(Module m) {
        MODULES.add(m);
    }

    public static List<Module> all() {
        return MODULES;
    }

    public static List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return (T) m;
        return null;
    }

    public static void preTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        for (Module m : MODULES) {
            if (!m.isEnabled()) continue;
            try {
                m.onPreTick();
            } catch (Exception e) {
                LOG.error("Module {} crashed in pre-tick, disabling", m.name, e);
                m.setEnabled(false);
            }
        }
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen == null) {
            long window = mc.getWindow().getHandle();
            for (Module m : MODULES) {
                if (m.getKey() < 0) continue;
                boolean down = InputUtil.isKeyPressed(window, m.getKey());
                if (down && !m.keyWasDown) m.toggle();
                m.keyWasDown = down;
            }
        }

        for (Module m : MODULES) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick();
            } catch (Exception e) {
                LOG.error("Module {} crashed in tick, disabling", m.name, e);
                m.setEnabled(false);
            }
        }
    }

    public static void render3d(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || ctx.matrixStack() == null) return;

        RenderUtil.begin(ctx.matrixStack(), ctx.camera().getPos());
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        for (Module m : MODULES) {
            if (!m.isEnabled()) continue;
            try {
                m.onRender3d(delta);
            } catch (Exception e) {
                LOG.error("Module {} crashed while rendering, disabling", m.name, e);
                m.setEnabled(false);
            }
        }
        RenderUtil.flush();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nova.json");
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : MODULES) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            JsonObject s = new JsonObject();
            for (Setting<?> set : m.settings) s.add(set.name, set.save());
            o.add("settings", s);
            root.add(m.name, o);
        }
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException e) {
            LOG.error("Failed to save config", e);
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (Module m : MODULES) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("key")) m.setKey(o.get("key").getAsInt());
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> set : m.settings) {
                        JsonElement e = s.get(set.name);
                        if (e != null) {
                            try {
                                set.load(e);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                if (m.saveEnabled() && o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
        } catch (Exception e) {
            LOG.error("Failed to load config", e);
        }
    }
}
