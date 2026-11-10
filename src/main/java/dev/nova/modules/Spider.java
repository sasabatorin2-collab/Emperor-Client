package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.20, 0.05, 0.50, 0.01));

    public Spider() {
        super("Spider", "Climb walls by pushing into them.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        boolean moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        if (mc.player.horizontalCollision && moving) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, speed.d(), v.z);
        }
    }
}
