package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.util.Rounded;
import dev.emperor.waypoint.Waypoints;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** Skyrim-style heading strip with waypoint markers. */
public class CompassElement extends HudElement {
    private static final int W = 200, H = 22;
    private static final float SPAN = 180f; // degrees visible across the strip

    public CompassElement() {
        super("Compass", true, 1, 0, 0, 4);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return W;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return H;
    }

    private int px(float diff) {
        return (int) (x + W / 2f + diff / SPAN * W);
    }

    private static int edgeFade(int color, float diff) {
        float a = 1f - Math.max(0f, (Math.abs(diff) - SPAN * 0.3f) / (SPAN * 0.2f));
        return Theme.fade(color, Math.max(0f, a));
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw(mc.getRenderTickCounter().getTickDelta(true)));
        Rounded.fill(ctx, x, y + 4, x + W, y + H - 4, 7, 0x900E0E14);

        for (int deg = 0; deg < 360; deg += 15) {
            float diff = MathHelper.wrapDegrees(deg - yaw);
            if (Math.abs(diff) > SPAN / 2) continue;
            int tx = px(diff);
            boolean major = deg % 45 == 0;
            ctx.fill(tx, y + H - 8, tx + 1, y + H - (major ? 4 : 6), edgeFade(0x90FFFFFF, diff));
            if (major) {
                String label = switch (deg) {
                    case 0 -> "S";
                    case 45 -> "SW";
                    case 90 -> "W";
                    case 135 -> "NW";
                    case 180 -> "N";
                    case 225 -> "NE";
                    case 270 -> "E";
                    default -> "SE";
                };
                int col = deg == 180 ? Theme.primary() : deg % 90 == 0 ? 0xFFFFFFFF : 0xFF9A9AA8;
                if (Math.abs(diff) < SPAN * 0.48f) ctx.drawCenteredTextWithShadow(tr, label, tx, y + 7, edgeFade(col, diff));
            }
        }

        if (mc.world != null) {
            String dim = mc.world.getRegistryKey().getValue().toString();
            for (Waypoints.Waypoint wp : Waypoints.in(dim)) {
                double dx = wp.x() + 0.5 - mc.player.getX(), dz = wp.z() + 0.5 - mc.player.getZ();
                float bearing = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
                float diff = MathHelper.wrapDegrees(bearing - yaw);
                if (Math.abs(diff) > SPAN / 2) continue;
                int tx = px(diff);
                int c = edgeFade(wp.color(), diff);
                for (int i = 0; i < 4; i++) ctx.fill(tx - i, y + 1 + i, tx + i + 1, y + 2 + i, c);
                for (int i = 0; i < 3; i++) ctx.fill(tx - 2 + i, y + 5 + i, tx + 3 - i, y + 6 + i, c);
            }
        }

        // centre marker
        int c = x + W / 2;
        for (int i = 0; i < 4; i++) ctx.fill(c - i, y + H - 4 + i - 3, c + i + 1, y + H - 3 + i - 3, Theme.primary());
        String heading = String.valueOf(Math.round(MathHelper.wrapDegrees(yaw + 180) + 180) % 360) + "°";
        ctx.drawCenteredTextWithShadow(tr, heading, c, y + H + 1, 0xFFB0B0C0);
    }
}
