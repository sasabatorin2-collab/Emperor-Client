package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;

public class AirJump extends Module {
    private boolean wasDown;

    public AirJump() {
        super("AirJump", "Jump again while in the air.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        boolean down = mc.options.jumpKey.isPressed();
        if (down && !wasDown && !mc.player.isOnGround()) mc.player.jump();
        wasDown = down;
    }
}
