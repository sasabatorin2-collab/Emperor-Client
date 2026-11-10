package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.NumberSetting;

public class ElytraFly extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.8, 0.2, 5.0, 0.1));
    private final NumberSetting vertical = add(new NumberSetting("Vertical speed", 1.0, 0.1, 3.0, 0.1));

    public ElytraFly() {
        super("ElytraFly", "Full control while gliding: hover, fly in any direction.", Category.MOVEMENT);
    }

    @Override
    public String getInfo() {
        return String.format(java.util.Locale.ROOT, "%.1f", speed.d());
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.isGliding()) return;
        float f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        float s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        double yaw = Math.toRadians(mc.player.getYaw());
        double dx = -Math.sin(yaw) * f + Math.cos(yaw) * s;
        double dz = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.hypot(dx, dz);
        if (len > 1e-6) {
            dx /= len;
            dz /= len;
        }
        double vy = mc.options.jumpKey.isPressed() ? vertical.d() : mc.options.sneakKey.isPressed() ? -vertical.d() : 0;
        mc.player.setVelocity(dx * speed.d(), vy, dz * speed.d());
    }
}
