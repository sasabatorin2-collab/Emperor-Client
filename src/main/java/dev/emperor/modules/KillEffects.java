package dev.emperor.modules;

import dev.emperor.hud.Notifications;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Client-side effects when something you hit dies. */
public class KillEffects extends Module {
    private final ModeSetting mode = add(new ModeSetting("Effect", "Lightning", "Lightning", "Blood", "Fireworks", "Souls"));
    private final BoolSetting notify = add(new BoolSetting("Kill notification", true));
    private final Random rnd = new Random();

    public KillEffects() {
        super("KillEffects", "Lightning, blood, fireworks or souls on every kill.", Category.RENDER);
    }

    public static void onKill(LivingEntity e) {
        KillEffects k = ModuleManager.get(KillEffects.class);
        if (k == null || !k.isEnabled()) return;
        k.play(e);
        if (k.notify.get()) Notifications.push("Eliminated", e.getName().getString(), Notifications.Kind.INFO);
    }

    private void play(LivingEntity e) {
        if (mc.world == null) return;
        Vec3d p = e.getPos();
        switch (mode.get()) {
            case "Lightning" -> {
                LightningEntity l = EntityType.LIGHTNING_BOLT.create(mc.world, SpawnReason.TRIGGERED);
                if (l == null) return;
                l.refreshPositionAfterTeleport(p);
                l.setCosmetic(true);
                mc.world.addEntity(l);
            }
            case "Blood" -> {
                var fx = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
                for (int i = 0; i < 60; i++) {
                    mc.world.addParticle(fx, p.x + g(0.4), p.y + e.getHeight() * rnd.nextDouble(), p.z + g(0.4), g(0.3), rnd.nextDouble() * 0.3, g(0.3));
                }
            }
            case "Fireworks" -> {
                for (int i = 0; i < 80; i++) {
                    double th = rnd.nextDouble() * Math.PI * 2, ph = Math.acos(2 * rnd.nextDouble() - 1), s = 0.25;
                    mc.world.addParticle(ParticleTypes.FIREWORK, p.x, p.y + e.getHeight() / 2, p.z,
                            Math.sin(ph) * Math.cos(th) * s, Math.cos(ph) * s, Math.sin(ph) * Math.sin(th) * s);
                }
            }
            default -> {
                for (int i = 0; i < 25; i++) {
                    mc.world.addParticle(i % 2 == 0 ? ParticleTypes.SOUL : ParticleTypes.SCULK_SOUL,
                            p.x + g(0.4), p.y + rnd.nextDouble() * e.getHeight(), p.z + g(0.4), 0, 0.05 + rnd.nextDouble() * 0.08, 0);
                }
            }
        }
    }

    private double g(double s) {
        return (rnd.nextDouble() - 0.5) * 2 * s;
    }
}
