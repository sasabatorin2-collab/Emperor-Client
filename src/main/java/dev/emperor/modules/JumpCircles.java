package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Fx;
import dev.emperor.util.RenderUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Glowing gradient shockwave rings where players jump and land. */
public class JumpCircles extends Module {
    private record Circle(Vec3d pos, long start) {
    }

    private final ModeSetting trigger = add(new ModeSetting("Trigger", "Both", "Jump", "Land", "Both"));
    private final NumberSetting radius = add(new NumberSetting("Radius", 1.4, 0.5, 3.0, 0.1));
    private final NumberSetting duration = add(new NumberSetting("Duration (s)", 1.2, 0.4, 3.0, 0.1));
    private final BoolSetting others = add(new BoolSetting("Other players", false));

    private final List<Circle> circles = new ArrayList<>();
    private final Map<UUID, Boolean> ground = new HashMap<>();

    public JumpCircles() {
        super("JumpCircles", "Animated shockwave rings when you jump and land.", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        circles.clear();
        ground.clear();
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !others.get()) continue;
            boolean on = p.isOnGround();
            Boolean was = ground.put(p.getUuid(), on);
            if (was == null || was == on) continue;
            boolean jumped = was && p.getVelocity().y > 0.1;
            boolean landed = !was;
            if ((jumped && !trigger.is("Land")) || (landed && !trigger.is("Jump"))) {
                circles.add(new Circle(p.getPos().add(0, 0.02, 0), System.currentTimeMillis()));
            }
        }
        if (circles.size() > 40) circles.remove(0);
    }

    @Override
    public void onRender3d(float delta) {
        long now = System.currentTimeMillis();
        double dur = duration.d() * 1000;
        Iterator<Circle> it = circles.iterator();
        while (it.hasNext()) {
            Circle c = it.next();
            float t = (float) ((now - c.start) / dur);
            if (t >= 1f) {
                it.remove();
                continue;
            }
            double r = radius.d() * Fx.easeOutCubic(t);
            float a = 1f - t;
            RenderUtil.ring(c.pos.x, c.pos.y, c.pos.z, r, 48, a, i -> Theme.accent(i % 24));
            RenderUtil.ring(c.pos.x, c.pos.y, c.pos.z, r * 0.7, 40, a * 0.6f, i -> Theme.accent(i % 20 + 6));
            RenderUtil.ring(c.pos.x, c.pos.y + 0.01, c.pos.z, r * 1.04, 48, a * 0.35f, i -> 0xFFFFFF);
        }
    }
}
