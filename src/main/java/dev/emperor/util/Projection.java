package dev.emperor.util;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Projects world positions to scaled GUI coordinates using the last rendered frame's matrices. */
public final class Projection {
    private static final Matrix4f MATRIX = new Matrix4f();
    private static Vec3d cam = Vec3d.ZERO;

    private Projection() {
    }

    public static void capture(WorldRenderContext ctx) {
        MATRIX.set(ctx.projectionMatrix()).mul(ctx.positionMatrix());
        cam = ctx.camera().getPos();
    }

    /** Screen x, y and clip-space w (distance-like), or null when behind the camera. */
    public static Vec3d toScreen(Vec3d pos) {
        Vector4f v = new Vector4f((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z), 1f);
        MATRIX.transform(v);
        if (v.w <= 0.05f) return null;
        Window win = MinecraftClient.getInstance().getWindow();
        double sx = (v.x / v.w * 0.5 + 0.5) * win.getScaledWidth();
        double sy = (0.5 - v.y / v.w * 0.5) * win.getScaledHeight();
        return new Vec3d(sx, sy, v.w);
    }
}
