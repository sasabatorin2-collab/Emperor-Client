package dev.emperor.command;

import dev.emperor.EmperorClient;
import dev.emperor.friend.Friends;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.util.Chat;
import dev.emperor.util.Session;
import dev.emperor.waypoint.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chat commands starting with {@link #PREFIX}; they are never sent to the server. */
public final class Commands {
    public static final String PREFIX = ".";

    private Commands() {
    }

    /** Returns true if the message was a client command and must not be sent. */
    public static boolean handle(String message) {
        if (!message.startsWith(PREFIX) || message.startsWith(PREFIX + PREFIX)) return false;
        String[] a = message.substring(PREFIX.length()).trim().split("\\s+");
        String cmd = a[0].toLowerCase(Locale.ROOT);
        try {
            switch (cmd) {
                case "", "help", "h", "?" -> help();
                case "toggle", "t" -> toggle(a);
                case "bind", "b" -> bind(a);
                case "friend", "friends", "f" -> friend(a);
                case "modules", "mods", "list" -> modules();
                case "vclip" -> vclip(a);
                case "coords", "pos" -> coords();
                case "panic" -> panic();
                case "waypoint", "wp" -> waypoint(a);
                case "config", "profile", "cfg" -> config(a);
                case "stats" -> stats();
                case "save" -> {
                    ModuleManager.save();
                    Chat.info("Config saved.");
                }
                default -> Chat.info("§cUnknown command §f" + cmd + "§c. Type §f" + PREFIX + "help");
            }
        } catch (Exception e) {
            Chat.info("§cError: " + e.getMessage());
        }
        return true;
    }

    private static void help() {
        Chat.info("§6§l♛ " + EmperorClient.NAME + " Client §7v" + EmperorClient.VERSION);
        for (String[] u : USAGE) line(u[0], u[1]);
    }

    /** {usage, description}; also used for Spotlight suggestions. */
    public static final String[][] USAGE = {
            {"toggle <module>", "turn a module on/off"},
            {"bind <module> <key|none>", "set a module keybind"},
            {"friend add|del|list <name>", "manage friends"},
            {"wp add <name> [x y z]", "create a waypoint (default: here)"},
            {"wp del <name>", "delete a waypoint"},
            {"wp list", "list waypoints"},
            {"config save|load|del <name>", "save or load a profile"},
            {"config list", "list saved profiles"},
            {"stats", "show session statistics"},
            {"modules", "list all modules"},
            {"vclip <blocks>", "teleport up/down"},
            {"coords", "copy your coordinates"},
            {"panic", "disable every module"},
            {"save", "save the config now"},
    };

    private static String dim() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world == null ? "minecraft:overworld" : mc.world.getRegistryKey().getValue().toString();
    }

    private static void waypoint(String[] a) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "add", "set" -> {
                if (a.length < 3) throw new IllegalArgumentException("usage: " + PREFIX + "wp add <name> [x y z]");
                int x, y, z;
                if (a.length >= 6) {
                    x = Integer.parseInt(a[3]);
                    y = Integer.parseInt(a[4]);
                    z = Integer.parseInt(a[5]);
                } else {
                    x = mc.player.getBlockX();
                    y = mc.player.getBlockY();
                    z = mc.player.getBlockZ();
                }
                Waypoints.set(a[2], x, y, z, dim());
                Chat.info("Waypoint §e" + a[2] + "§f set at §e" + x + " " + y + " " + z);
                ModuleManager.save();
            }
            case "del", "remove", "rm" -> {
                if (a.length < 3) throw new IllegalArgumentException("missing name");
                Chat.info(Waypoints.remove(a[2]) ? "Deleted waypoint §c" + a[2] : "No waypoint named " + a[2]);
                ModuleManager.save();
            }
            case "clear" -> {
                Waypoints.clear();
                Chat.info("All waypoints deleted.");
            }
            default -> {
                if (Waypoints.all().isEmpty()) {
                    Chat.info("No waypoints yet. §7" + PREFIX + "wp add <name>");
                    return;
                }
                for (Waypoints.Waypoint w : Waypoints.all()) {
                    String d = w.dim().substring(w.dim().indexOf(':') + 1);
                    Chat.info("§e" + w.name() + " §f" + w.x() + " " + w.y() + " " + w.z() + " §8(" + d + ")");
                }
            }
        }
    }

    private static void config(String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
        if (!sub.equals("list") && a.length < 3) throw new IllegalArgumentException("usage: " + PREFIX + "config " + sub + " <name>");
        switch (sub) {
            case "save" -> Chat.info(ModuleManager.saveProfile(a[2]) ? "Saved profile §e" + a[2] : "§cCould not save profile");
            case "load" -> Chat.info(ModuleManager.loadProfile(a[2]) ? "Loaded profile §e" + a[2] : "§cNo profile named " + a[2]);
            case "del", "delete", "rm" -> Chat.info(ModuleManager.deleteProfile(a[2]) ? "Deleted profile §c" + a[2] : "No profile named " + a[2]);
            default -> {
                List<String> p = ModuleManager.profiles();
                Chat.info(p.isEmpty() ? "No profiles yet. §7" + PREFIX + "config save <name>" : "Profiles: §e" + String.join("§7, §e", p));
            }
        }
    }

    private static void stats() {
        Chat.info("§6Session §8- §fplaytime §e" + Session.playtime() + "§f, kills §e" + Session.kills + "§f, deaths §e"
                + Session.deaths + "§f, K/D §e" + Session.kd() + "§f, blocks §e" + Session.blocks
                + "§f, distance §e" + Math.round(Session.distance) + "m");
    }

    private static void line(String usage, String desc) {
        Chat.info("§e" + PREFIX + usage + " §8- §7" + desc);
    }

    private static Module module(String[] a, int idx) {
        if (a.length <= idx) throw new IllegalArgumentException("missing module name");
        Module m = ModuleManager.byName(a[idx]);
        if (m == null) throw new IllegalArgumentException("no module named " + a[idx]);
        return m;
    }

    private static void toggle(String[] a) {
        Module m = module(a, 1);
        m.toggle();
        Chat.info(m.name + " " + (m.isEnabled() ? "§aenabled" : "§cdisabled"));
    }

    private static void bind(String[] a) {
        Module m = module(a, 1);
        if (a.length < 3) throw new IllegalArgumentException("usage: " + PREFIX + "bind <module> <key|none>");
        String k = a[2].toLowerCase(Locale.ROOT);
        if (k.equals("none") || k.equals("clear") || k.equals("-")) {
            m.setKey(-1);
            Chat.info("Unbound §e" + m.name);
            return;
        }
        InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + k);
        if (key.getCode() < 0) throw new IllegalArgumentException("unknown key " + a[2]);
        m.setKey(key.getCode());
        Chat.info("Bound §e" + m.name + "§f to §e" + key.getLocalizedText().getString().toUpperCase(Locale.ROOT));
    }

    private static void friend(String[] a) {
        String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "add" -> {
                if (a.length < 3) throw new IllegalArgumentException("missing name");
                Chat.info(Friends.add(a[2]) ? "Added §a" + a[2] + "§f to friends." : a[2] + " is already a friend.");
            }
            case "del", "remove", "rm" -> {
                if (a.length < 3) throw new IllegalArgumentException("missing name");
                Chat.info(Friends.remove(a[2]) ? "Removed §c" + a[2] + "§f from friends." : a[2] + " is not a friend.");
            }
            case "clear" -> {
                Friends.clear();
                Chat.info("Friend list cleared.");
            }
            default -> {
                if (Friends.all().isEmpty()) Chat.info("You have no friends yet. §7" + PREFIX + "friend add <name>");
                else Chat.info("Friends (" + Friends.all().size() + "): §a" + String.join("§7, §a", Friends.all()));
            }
        }
    }

    private static void modules() {
        for (Category c : Category.values()) {
            List<String> names = new ArrayList<>();
            for (Module m : ModuleManager.byCategory(c)) names.add((m.isEnabled() ? "§a" : "§7") + m.name);
            Chat.info("§6" + c.label + "§8: " + String.join("§8, ", names));
        }
    }

    private static void vclip(String[] a) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (a.length < 2) throw new IllegalArgumentException("usage: " + PREFIX + "vclip <blocks>");
        double d = Double.parseDouble(a[1]);
        var p = mc.player;
        if (p == null) return;
        if (p.hasVehicle()) p.getVehicle().setPosition(p.getVehicle().getX(), p.getVehicle().getY() + d, p.getVehicle().getZ());
        p.setPosition(p.getX(), p.getY() + d, p.getZ());
        Chat.info("Teleported " + (d >= 0 ? "up " : "down ") + Math.abs(d) + " blocks.");
    }

    private static void coords() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        String s = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        mc.keyboard.setClipboard(s);
        Chat.info("Copied §e" + s + "§f to clipboard.");
    }

    private static void panic() {
        int n = 0;
        for (Module m : ModuleManager.all()) {
            if (m.isEnabled() && m.category != Category.CLIENT) {
                m.setEnabled(false);
                n++;
            }
        }
        Chat.info("Disabled §c" + n + "§f modules.");
    }
}
