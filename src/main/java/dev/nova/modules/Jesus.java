package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import net.minecraft.util.math.Vec3d;

/** Keeps you floating at the water surface instead of sinking. */
public class Jesus extends Module {
    public Jesus() {
        super("Jesus", "Float on top of water.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.isSneaking() || mc.options.jumpKey.isPressed()) return;
        if (!mc.player.isTouchingWater()) return;
        Vec3d v = mc.player.getVelocity();
        double y = mc.player.isSubmergedInWater() ? 0.12 : Math.max(v.y, 0.0);
        mc.player.setVelocity(v.x, y, v.z);
    }
}
