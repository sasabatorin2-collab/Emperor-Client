package dev.emperor.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Batched through-the-walls drawing. Modules call box()/line() from onRender3d,
 * the manager calls begin() before and flush() after all modules.
 */
public final class RenderUtil {
    private static final List<float[]> QUADS = new ArrayList<>();
    private static final List<float[]> LINES = new ArrayList<>();
    private static final int MAX_VERTS = 120_000;

    private static Matrix4f matrix = new Matrix4f();
    private static Vec3d cam = Vec3d.ZERO;

    private RenderUtil() {
    }

    public static void begin(MatrixStack stack, Vec3d camera) {
        matrix = new Matrix4f(stack.peek().getPositionMatrix());
        cam = camera;
        QUADS.clear();
        LINES.clear();
    }

    public static Vec3d camera() {
        return cam;
    }

    /** A point just in front of the camera, used as the start of tracer lines (works with Freecam). */
    public static Vec3d lookStart() {
        net.minecraft.client.render.Camera c = net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
        double yaw = Math.toRadians(c.getYaw());
        double pitch = Math.toRadians(c.getPitch());
        double cp = Math.cos(pitch);
        return cam.add(-Math.sin(yaw) * cp, -Math.sin(pitch), Math.cos(yaw) * cp);
    }

    private static void v(List<float[]> list, double x, double y, double z, float r, float g, float b, float a) {
        list.add(new float[]{(float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), r, g, b, a});
    }

    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, int rgb) {
        line(x1, y1, z1, x2, y2, z2, rgb, 0.95f);
    }

    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, int rgb, float alpha) {
        if (LINES.size() > MAX_VERTS || alpha <= 0.01f) return;
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        v(LINES, x1, y1, z1, r, g, b, alpha);
        v(LINES, x2, y2, z2, r, g, b, alpha);
    }

    /** Horizontal ring (y fixed) with per-segment theme colours. */
    public static void ring(double cx, double y, double cz, double radius, int segments, float alpha,
                            java.util.function.IntUnaryOperator color) {
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * 2 * i / segments, a2 = Math.PI * 2 * (i + 1) / segments;
            line(cx + Math.cos(a1) * radius, y, cz + Math.sin(a1) * radius,
                    cx + Math.cos(a2) * radius, y, cz + Math.sin(a2) * radius, color.applyAsInt(i), alpha);
        }
    }

    public static void box(Box bx, int rgb, boolean fill, boolean outline) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;

        if (fill && QUADS.size() < MAX_VERTS) {
            float a = 0.22f;
            // bottom / top
            quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
            quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
            // north / south
            quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
            quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
            // west / east
            quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a);
            quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
        }
        if (outline && LINES.size() < MAX_VERTS) {
            float a = 0.95f;
            // bottom ring
            e(x1, y1, z1, x2, y1, z1, r, g, b, a);
            e(x2, y1, z1, x2, y1, z2, r, g, b, a);
            e(x2, y1, z2, x1, y1, z2, r, g, b, a);
            e(x1, y1, z2, x1, y1, z1, r, g, b, a);
            // top ring
            e(x1, y2, z1, x2, y2, z1, r, g, b, a);
            e(x2, y2, z1, x2, y2, z2, r, g, b, a);
            e(x2, y2, z2, x1, y2, z2, r, g, b, a);
            e(x1, y2, z2, x1, y2, z1, r, g, b, a);
            // pillars
            e(x1, y1, z1, x1, y2, z1, r, g, b, a);
            e(x2, y1, z1, x2, y2, z1, r, g, b, a);
            e(x2, y1, z2, x2, y2, z2, r, g, b, a);
            e(x1, y1, z2, x1, y2, z2, r, g, b, a);
        }
    }

    private static void quad(double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz,
                             float r, float g, float b, float a) {
        v(QUADS, ax, ay, az, r, g, b, a);
        v(QUADS, bx, by, bz, r, g, b, a);
        v(QUADS, cx, cy, cz, r, g, b, a);
        v(QUADS, dx, dy, dz, r, g, b, a);
    }

    private static void e(double x1, double y1, double z1, double x2, double y2, double z2,
                          float r, float g, float b, float a) {
        v(LINES, x1, y1, z1, r, g, b, a);
        v(LINES, x2, y2, z2, r, g, b, a);
    }

    public static void flush() {
        if (QUADS.isEmpty() && LINES.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (!QUADS.isEmpty()) {
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (float[] p : QUADS) buf.vertex(matrix, p[0], p[1], p[2]).color(p[3], p[4], p[5], p[6]);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
        if (!LINES.isEmpty()) {
            RenderSystem.lineWidth(2.0f);
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (float[] p : LINES) buf.vertex(matrix, p[0], p[1], p[2]).color(p[3], p[4], p[5], p[6]);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        QUADS.clear();
        LINES.clear();
    }
}
