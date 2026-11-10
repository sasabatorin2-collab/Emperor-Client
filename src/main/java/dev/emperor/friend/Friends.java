package dev.emperor.friend;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Friend list: friends are ignored by combat modules and highlighted by render modules. */
public final class Friends {
    private static final Set<String> NAMES = new TreeSet<>();

    private Friends() {
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean is(String name) {
        return NAMES.contains(key(name));
    }

    public static boolean is(Entity e) {
        return e instanceof PlayerEntity p && is(p.getGameProfile().getName());
    }

    public static boolean add(String name) {
        return NAMES.add(key(name));
    }

    public static boolean remove(String name) {
        return NAMES.remove(key(name));
    }

    /** Returns true if the player is a friend after the call. */
    public static boolean toggle(String name) {
        if (remove(name)) return false;
        add(name);
        return true;
    }

    public static Collection<String> all() {
        return Collections.unmodifiableSet(NAMES);
    }

    public static void clear() {
        NAMES.clear();
    }
}
