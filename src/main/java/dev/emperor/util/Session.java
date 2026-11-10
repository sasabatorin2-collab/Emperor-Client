package dev.emperor.util;

import dev.emperor.modules.KillEffects;
import dev.emperor.modules.WaypointsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Per-session statistics (reset every time you join a world) and kill/death detection. */
public final class Session {
    public static long start = System.currentTimeMillis();
    public static int kills, deaths, blocks;
    public static double distance;

    private static final Map<Integer, Long> ATTACKED = new HashMap<>();
    private static ClientWorld world;
    private static Vec3d lastPos;
    private static boolean wasDead;

    private Session() {
    }

    public static void reset() {
        start = System.currentTimeMillis();
        kills = deaths = blocks = 0;
        distance = 0;
        ATTACKED.clear();
        lastPos = null;
        TickRate.reset();
    }

    public static void onAttack(Entity e) {
        if (e instanceof LivingEntity) ATTACKED.put(e.getId(), System.currentTimeMillis());
    }

    public static void onBlockBroken() {
        blocks++;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            world = null;
            return;
        }
        if (world == null) reset();
        world = mc.world;

        Vec3d p = mc.player.getPos();
        if (lastPos != null) {
            double d = Math.hypot(p.x - lastPos.x, p.z - lastPos.z);
            if (d < 10) distance += d; // ignore teleports
        }
        lastPos = p;

        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) {
            deaths++;
            WaypointsModule.onDeath(mc.player.getBlockPos());
        }
        wasDead = dead;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> it = ATTACKED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> en = it.next();
            Entity e = mc.world.getEntityById(en.getKey());
            if (e instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0)) {
                kills++;
                KillEffects.onKill(le);
                it.remove();
            } else if (e == null || now - en.getValue() > 5000) {
                it.remove();
            }
        }
    }

    public static String playtime() {
        long s = (System.currentTimeMillis() - start) / 1000;
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
                : String.format("%d:%02d", s / 60, s % 60);
    }

    public static String kd() {
        return String.format(java.util.Locale.ROOT, "%.2f", deaths == 0 ? kills : kills / (double) deaths);
    }

}
