package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import net.minecraft.util.math.Box;

public class Parkour extends Module {
    public Parkour() {
        super("Parkour", "Jumps automatically at the very edge of a block.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isOnGround() || mc.player.isSneaking() || mc.options.jumpKey.isPressed()) return;
        Box below = mc.player.getBoundingBox().offset(0, -0.5, 0).expand(-0.001, 0, -0.001);
        if (!mc.world.getBlockCollisions(mc.player, below).iterator().hasNext()) mc.player.jump();
    }
}
