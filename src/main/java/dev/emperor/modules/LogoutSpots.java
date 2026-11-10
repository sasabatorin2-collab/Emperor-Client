package dev.emperor.modules;

import dev.emperor.hud.Notifications;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.Projection;
import dev.emperor.util.RenderUtil;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Marks where nearby players logged out, with how long ago it happened. */
public class LogoutSpots extends Module {
    private record Seen(String name, Box box, float health) {
    }

    private record Spot(String name, Box box, float health, long time) {
    }

    private final BoolSetting notify = add(new BoolSetting("Notify", true));
    private final Map<UUID, Seen> seen = new HashMap<>();
    private final Map<UUID, Spot> spots = new HashMap<>();
    private ClientWorld world;

    public LogoutSpots() {
        super("LogoutSpots", "Ghost box where a nearby player logged out.", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return spots.isEmpty() ? null : String.valueOf(spots.size());
    }

    @Override
    protected void onDisable() {
        seen.clear();
        spots.clear();
    }

    @Override
    public void onTick() {
        if (mc.world != world) {
            world = mc.world;
            seen.clear();
            spots.clear();
        }
        Map<UUID, Seen> now = new HashMap<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            now.put(p.getUuid(), new Seen(p.getGameProfile().getName(), p.getBoundingBox(), p.getHealth() + p.getAbsorptionAmount()));
            spots.remove(p.getUuid());
        }
        for (Map.Entry<UUID, Seen> e : seen.entrySet()) {
            if (now.containsKey(e.getKey())) continue;
            // Left render distance vs. left the server: only the latter removes them from the tab list.
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(e.getKey()) == null) {
                Seen s = e.getValue();
                spots.put(e.getKey(), new Spot(s.name, s.box, s.health, System.currentTimeMillis()));
                if (notify.get()) Notifications.push(s.name, "logged out nearby", Notifications.Kind.DISABLED);
            }
        }
        seen.clear();
        seen.putAll(now);
    }

    @Override
    public void onRender3d(float delta) {
        for (Spot s : spots.values()) RenderUtil.box(s.box, Theme.secondary(), true, true);
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        TextRenderer tr = mc.textRenderer;
        for (Spot s : spots.values()) {
            Vec3d top = new Vec3d(s.box.getCenter().x, s.box.maxY + 0.4, s.box.getCenter().z);
            Vec3d sc = Projection.toScreen(top);
            if (sc == null) continue;
            long ago = (System.currentTimeMillis() - s.time) / 1000;
            String t = s.name + " · " + (ago < 60 ? ago + "s" : ago / 60 + "m") + " ago · " + Math.round(s.health) + "❤";
            int w = tr.getWidth(t) + 10;
            int x = (int) sc.x - w / 2, y = (int) sc.y - 6;
            Rounded.fill(ctx, x, y, x + w, y + 12, 4, 0xC00E0E14);
            Rounded.outline(ctx, x, y, x + w, y + 12, 4, Theme.alpha(Theme.secondary(), 0xC0));
            ctx.drawTextWithShadow(tr, t, x + 5, y + 2, 0xFFE6E6EE);
        }
    }
}
