package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;
import net.minecraft.util.Hand;

public class AntiAfk extends Module {
    private final NumberSetting interval = add(new NumberSetting("Interval (sec)", 20, 5, 60, 5));
    private int ticks;

    public AntiAfk() {
        super("AntiAFK", "Jumps and turns a bit so you are not kicked for idling (queues).", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        if (++ticks < interval.i() * 20) return;
        ticks = 0;
        mc.player.setYaw(mc.player.getYaw() + (float) ((Math.random() - 0.5) * 30.0));
        if (mc.player.isOnGround()) mc.player.jump();
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
