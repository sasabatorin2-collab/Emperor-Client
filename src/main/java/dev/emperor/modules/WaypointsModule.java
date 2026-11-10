package dev.emperor.modules;

import dev.emperor.hud.Notifications;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Projection;
import dev.emperor.util.RenderUtil;
import dev.emperor.util.Rounded;
import dev.emperor.waypoint.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public class WaypointsModule extends Module {
    private final BoolSetting beams = add(new BoolSetting("Beams", true));
    private final BoolSetting death = add(new BoolSetting("Death waypoint", true));
    private final NumberSetting maxDist = add(new NumberSetting("Max distance", 10000, 100, 30000, 100));

    public WaypointsModule() {
        super("Waypoints", "In-world waypoint labels and beams (.wp add <name>).", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return String.valueOf(Waypoints.all().size());
    }

    private static String dim() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world.getRegistryKey().getValue().toString();
    }

    public static void onDeath(BlockPos pos) {
        WaypointsModule w = ModuleManager.get(WaypointsModule.class);
        if (w == null || !w.death.get() || MinecraftClient.getInstance().world == null) return;
        Waypoints.set("Death", pos.getX(), pos.getY(), pos.getZ(), dim());
        Notifications.push("Death waypoint", pos.getX() + " " + pos.getY() + " " + pos.getZ(), Notifications.Kind.INFO);
    }

    @Override
    public void onRender3d(float delta) {
        if (!beams.get() || mc.world == null) return;
        long t = System.currentTimeMillis();
        for (Waypoints.Waypoint w : Waypoints.in(dim())) {
            double cx = w.x() + 0.5, cz = w.z() + 0.5;
            if (mc.player.squaredDistanceTo(cx, w.y(), cz) > maxDist.d() * maxDist.d()) continue;
            RenderUtil.line(cx, w.y(), cz, cx, w.y() + 256, cz, w.color(), 0.7f);
            RenderUtil.box(new Box(w.x(), w.y(), w.z(), w.x() + 1, w.y() + 1, w.z() + 1), w.color(), true, true);
            double pulse = (t % 1500) / 1500.0;
            RenderUtil.ring(cx, w.y() + 0.02, cz, 0.6 + pulse * 1.2, 32, (float) (1 - pulse), i -> w.color());
        }
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        if (mc.world == null) return;
        TextRenderer tr = mc.textRenderer;
        Vec3d eye = mc.player.getCameraPosVec(delta);
        for (Waypoints.Waypoint w : Waypoints.in(dim())) {
            Vec3d pos = new Vec3d(w.x() + 0.5, w.y() + 1.5, w.z() + 0.5);
            double dist = eye.distanceTo(pos);
            if (dist > maxDist.d()) continue;
            // Far waypoints are pulled closer so they still project correctly.
            Vec3d target = dist > 200 ? eye.add(pos.subtract(eye).normalize().multiply(200)) : pos;
            Vec3d s = Projection.toScreen(target);
            if (s == null) continue;

            String name = w.name();
            String d = dist >= 1000 ? String.format(Locale.ROOT, "%.1fkm", dist / 1000) : String.format(Locale.ROOT, "%.0fm", dist);
            int nw = tr.getWidth(Text.literal(name).formatted(Formatting.BOLD));
            int tw = 14 + nw + 4 + tr.getWidth(d) + 6;
            int x = (int) s.x - tw / 2, y = (int) s.y - 7;
            Rounded.fill(ctx, x, y, x + tw, y + 14, 5, 0xC00E0E14);
            Rounded.outline(ctx, x, y, x + tw, y + 14, 5, Theme.alpha(w.color(), 0xA0));
            int dx = x + 7, dy = y + 7;
            for (int i = 0; i < 4; i++) ctx.fill(dx - i, dy - 3 + i, dx + i + 1, dy - 2 + i, w.color());
            for (int i = 0; i < 3; i++) ctx.fill(dx - 2 + i, dy + 1 + i, dx + 3 - i, dy + 2 + i, w.color());
            ctx.drawTextWithShadow(tr, Text.literal(name).formatted(Formatting.BOLD), x + 14, y + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, d, x + 14 + nw + 4, y + 3, 0xFF9A9AA8);
        }
    }
}
