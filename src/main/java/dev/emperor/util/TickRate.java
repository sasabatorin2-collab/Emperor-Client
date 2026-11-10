package dev.emperor.util;

import java.util.ArrayDeque;

/**
 * Estimates server TPS from world-time packets, which the server sends once every 20 ticks.
 */
public final class TickRate {
    private static final ArrayDeque<Float> SAMPLES = new ArrayDeque<>();
    private static long lastPacket = -1;

    private TickRate() {
    }

    public static void onTimeUpdate() {
        long now = System.currentTimeMillis();
        if (lastPacket > 0) {
            float secs = (now - lastPacket) / 1000f;
            if (secs > 0.05f) {
                SAMPLES.addLast(Math.max(0f, Math.min(20f, 20f / secs)));
                while (SAMPLES.size() > 8) SAMPLES.removeFirst();
            }
        }
        lastPacket = now;
    }

    public static void reset() {
        SAMPLES.clear();
        lastPacket = -1;
    }

    public static float tps() {
        if (SAMPLES.isEmpty()) return 20f;
        float s = 0;
        for (float f : SAMPLES) s += f;
        return s / SAMPLES.size();
    }

    /** Seconds since the server last sent a time update (> ~1.5s means the server is lagging). */
    public static float sinceLastPacket() {
        return lastPacket < 0 ? 0f : (System.currentTimeMillis() - lastPacket) / 1000f;
    }
}
