package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;

/** Client-side cosmetics on your own character: halo, orbiting aura and a coloured trail. */
public class Cosmetics extends Module {
    private final BoolSetting halo = add(new BoolSetting("Halo", true));
    private final BoolSetting aura = add(new BoolSetting("Aura", true));
    private final ModeSetting trail = add(new ModeSetting("Trail", "Dust", "Off", "Dust", "Sparkle"));
    private Vec3d lastPos;

    public Cosmetics() {
        super("Cosmetics", "Halo, aura and particle trail for your character (only you see them).", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (trail.is("Off")) return;
        Vec3d p = mc.player.getPos();
        if (lastPos != null && p.squaredDistanceTo(lastPos) > 0.004) {
            for (int i = 0; i < 2; i++) {
                double ox = (Math.random() - 0.5) * 0.4, oz = (Math.random() - 0.5) * 0.4;
                if (trail.is("Dust")) {
                    mc.world.addParticle(new DustParticleEffect(Theme.accent(i * 8) & 0xFFFFFF, 1.1f),
                            p.x + ox, p.y + 0.15 + Math.random() * 0.3, p.z + oz, 0, 0, 0);
                } else {
                    mc.world.addParticle(net.minecraft.particle.ParticleTypes.END_ROD,
                            p.x + ox, p.y + 0.2, p.z + oz, 0, 0.01, 0);
                }
            }
        }
        lastPos = p;
    }

    @Override
    public void onRender3d(float delta) {
        boolean firstPerson = mc.options.getPerspective() == Perspective.FIRST_PERSON && Freecam.active() == null;
        Vec3d p = mc.player.getLerpedPos(delta);
        double t = System.currentTimeMillis() / 1000.0;
        if (halo.get() && !firstPerson) {
            double y = p.y + mc.player.getHeight() + 0.25 + Math.sin(t * 2) * 0.04;
            RenderUtil.ring(p.x, y, p.z, 0.3, 40, 0.95f, i -> Theme.accent(i));
            RenderUtil.ring(p.x, y + 0.01, p.z, 0.27, 40, 0.5f, i -> 0xFFFFFF);
        }
        if (aura.get()) {
            int n = 3;
            for (int k = 0; k < n; k++) {
                double a = t * 2.2 + k * Math.PI * 2 / n;
                double y = p.y + 0.2 + (Math.sin(t * 1.5 + k) * 0.5 + 0.5) * mc.player.getHeight() * 0.8;
                double cx = p.x + Math.cos(a) * 0.75, cz = p.z + Math.sin(a) * 0.75;
                int orb = Theme.accent(k * 6);
                RenderUtil.ring(cx, y, cz, 0.08, 12, 1f, i -> orb);
                // short fading tail behind each orb
                for (int s = 1; s <= 8; s++) {
                    double a1 = a - s * 0.07, a2 = a - (s - 1) * 0.07;
                    RenderUtil.line(p.x + Math.cos(a1) * 0.75, y, p.z + Math.sin(a1) * 0.75,
                            p.x + Math.cos(a2) * 0.75, y, p.z + Math.sin(a2) * 0.75, Theme.accent(k * 6), 1f - s / 9f);
                }
            }
        }
    }
}
