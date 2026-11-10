package dev.emperor.modules;

import dev.emperor.friend.Friends;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class TriggerBot extends Module {
    private final NumberSetting cooldown = add(new NumberSetting("Charge", 0.95, 0.5, 1.0, 0.05));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile", true));
    private final BoolSetting passive = add(new BoolSetting("Passive", false));

    public TriggerBot() {
        super("TriggerBot", "Attacks whatever is under your crosshair.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity le) || !le.isAlive() || Friends.is(e)) return;
        boolean ok = (e instanceof PlayerEntity && players.get()) || (e instanceof HostileEntity && hostile.get())
                || (e instanceof AnimalEntity && passive.get());
        if (!ok || mc.player.getAttackCooldownProgress(0.5f) < cooldown.f()) return;
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
