package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import net.minecraft.client.option.KeyBinding;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", "Holds the forward key for you (long trips, AFK travel).", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        mc.options.forwardKey.setPressed(true);
    }

    @Override
    protected void onDisable() {
        mc.options.forwardKey.setPressed(false);
        KeyBinding.updatePressedStates();
    }
}
