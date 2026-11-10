package dev.emperor.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.emperor.friend.Friends;
import dev.emperor.hud.Hud;
import dev.emperor.modules.*;
import dev.emperor.setting.Setting;
import dev.emperor.util.Projection;
import dev.emperor.util.RenderUtil;
import dev.emperor.waypoint.Waypoints;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModuleManager {
    private static final String FRIENDS_KEY = "$friends";
    private static final String WAYPOINTS_KEY = "$waypoints";
    private static final String HUD_KEY = "$hud";
    private static final Logger LOG = LoggerFactory.getLogger("Emperor");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {
    }

    public static void init() {
        MODULES.clear();
        // Combat
        register(new KillAura());
        register(new TriggerBot());
        register(new AimAssist());
        register(new Criticals());
        register(new Velocity());
        register(new AutoArmor());
        register(new AutoTotem());
        // Movement
        register(new Sprint());
        register(new Speed());
        register(new Flight());
        register(new ElytraFly());
        register(new AirJump());
        register(new Parkour());
        register(new SafeWalk());
        register(new Spider());
        register(new NoFall());
        register(new Step());
        register(new Jesus());
        register(new AutoWalk());
        // Render
        register(new Esp());
        register(new Nametags());
        register(new ChestEsp());
        register(new BaseFinder());
        register(new OreEsp());
        register(new Tracers());
        register(new Breadcrumbs());
        register(new Zoom());
        register(new NoRender());
        register(new Fullbright());
        register(new Freecam());
        register(new HoleEsp());
        register(new ItemEsp());
        register(new WaypointsModule());
        register(new DamageNumbers());
        register(new JumpCircles());
        register(new Crosshair());
        register(new Trajectories());
        register(new FreeLook());
        register(new Ambience());
        register(new KillEffects());
        // World
        register(new Nuker());
        register(new Scaffold());
        register(new FastPlace());
        register(new AutoTool());
        register(new AutoFish());
        register(new ChestStealer());
        // Misc
        register(new AutoEat());
        register(new AutoLog());
        register(new MiddleClickFriend());
        register(new Guardian());
        register(new AutoReconnect());
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

    /** Case-insensitive lookup that ignores spaces, e.g. "killaura" or "Kill Aura". */
    public static Module byName(String name) {
        String q = name.replace(" ", "").toLowerCase(Locale.ROOT);
        for (Module m : MODULES) {
            if (m.name.replace(" ", "").toLowerCase(Locale.ROOT).equals(q)) return m;
        }
        return null;
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

        Projection.capture(ctx);
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

    public static void render2d(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        for (Module m : MODULES) {
            if (!m.isEnabled()) continue;
            try {
                m.onRender2d(ctx, delta);
            } catch (Exception e) {
                LOG.error("Module {} crashed while rendering 2D, disabling", m.name, e);
                m.setEnabled(false);
            }
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("emperor.json");
    }

    private static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("emperor").resolve("profiles");
    }

    private static Path profile(String name) {
        return profilesDir().resolve(name.replaceAll("[^A-Za-z0-9_\\-]", "_") + ".json");
    }

    /** Serialises modules + HUD layout; global data (friends, waypoints) only when {@code global}. */
    private static JsonObject toJson(boolean global) {
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
        root.add(HUD_KEY, Hud.save());
        if (global) {
            JsonArray friends = new JsonArray();
            for (String f : Friends.all()) friends.add(f);
            root.add(FRIENDS_KEY, friends);
            root.add(WAYPOINTS_KEY, Waypoints.save());
        }
        return root;
    }

    private static void fromJson(JsonObject root) {
        if (root.has(FRIENDS_KEY) && root.get(FRIENDS_KEY).isJsonArray()) {
            Friends.clear();
            for (JsonElement e : root.getAsJsonArray(FRIENDS_KEY)) Friends.add(e.getAsString());
        }
        if (root.has(WAYPOINTS_KEY) && root.get(WAYPOINTS_KEY).isJsonArray()) {
            try {
                Waypoints.load(root.getAsJsonArray(WAYPOINTS_KEY));
            } catch (Exception e) {
                LOG.error("Failed to load waypoints", e);
            }
        }
        if (root.has(HUD_KEY) && root.get(HUD_KEY).isJsonObject()) Hud.load(root.getAsJsonObject(HUD_KEY));
        for (Module m : MODULES) {
            if (!root.has(m.name) || !root.get(m.name).isJsonObject()) continue;
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
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(toJson(true)));
        } catch (IOException e) {
            LOG.error("Failed to save config", e);
        }
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            fromJson(JsonParser.parseString(Files.readString(f)).getAsJsonObject());
        } catch (Exception e) {
            LOG.error("Failed to load config", e);
        }
    }

    public static boolean saveProfile(String name) {
        try {
            Files.createDirectories(profilesDir());
            Files.writeString(profile(name), GSON.toJson(toJson(false)));
            return true;
        } catch (IOException e) {
            LOG.error("Failed to save profile {}", name, e);
            return false;
        }
    }

    public static boolean loadProfile(String name) {
        Path p = profile(name);
        if (!Files.exists(p)) return false;
        try {
            fromJson(JsonParser.parseString(Files.readString(p)).getAsJsonObject());
            save();
            return true;
        } catch (Exception e) {
            LOG.error("Failed to load profile {}", name, e);
            return false;
        }
    }

    public static boolean deleteProfile(String name) {
        try {
            return Files.deleteIfExists(profile(name));
        } catch (IOException e) {
            return false;
        }
    }

    public static List<String> profiles() {
        List<String> out = new ArrayList<>();
        Path dir = profilesDir();
        if (!Files.isDirectory(dir)) return out;
        try (var s = Files.list(dir)) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted().forEach(out::add);
        } catch (IOException ignored) {
        }
        return out;
    }
}
