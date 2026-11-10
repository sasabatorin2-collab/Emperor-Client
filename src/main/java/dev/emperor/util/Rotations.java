package dev.emperor.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Rotations {
    private Rotations() {
    }

    /** {yaw, pitch} from the player's eyes to the point. */
    public static float[] to(Vec3d target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    public static Vec3d aimPoint(Entity e) {
        return e.getBoundingBox().getCenter().add(0, e.getHeight() * 0.15, 0);
    }

    /** Absolute angle between the current look direction and the entity. */
    public static float angleTo(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float[] r = to(aimPoint(e));
        float dy = MathHelper.wrapDegrees(r[0] - mc.player.getYaw());
        float dp = r[1] - mc.player.getPitch();
        return (float) Math.sqrt(dy * dy + dp * dp);
    }

    /** Turns the player towards the entity by at most {@code maxStep} degrees (Infinity = snap). */
    public static void face(Entity e, float maxStep) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float[] r = to(aimPoint(e));
        float dy = MathHelper.wrapDegrees(r[0] - mc.player.getYaw());
        float dp = r[1] - mc.player.getPitch();
        mc.player.setYaw(mc.player.getYaw() + MathHelper.clamp(dy, -maxStep, maxStep));
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + MathHelper.clamp(dp, -maxStep, maxStep), -90f, 90f));
    }
}
