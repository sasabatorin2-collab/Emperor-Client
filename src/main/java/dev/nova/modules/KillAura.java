package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class KillAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 3.5, 1.0, 6.0, 0.1));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile", true));

    public KillAura() {
        super("KillAura", "Attacks nearby entities when your weapon is charged.", Category.COMBAT);
    }

    @Override
    public String getInfo() {
        return String.format(java.util.Locale.ROOT, "%.1f", range.d());
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.95f) return;

        Entity best = null;
        double bestDist = range.d() * range.d();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive()) continue;
            boolean ok = (e instanceof PlayerEntity && players.get()) || (e instanceof HostileEntity && hostile.get());
            if (!ok) continue;
            double d = mc.player.squaredDistanceTo(e);
            if (d <= bestDist) {
                bestDist = d;
                best = e;
            }
        }
        if (best == null) return;

        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
