package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 0.30, 0.10, 1.00, 0.01));
    private final BoolSetting inAir = add(new BoolSetting("In air", true));

    public Speed() {
        super("Speed", "Move faster on the ground.", Category.MOVEMENT);
    }

    @Override
    public String getInfo() {
        return String.format(java.util.Locale.ROOT, "%.2f", speed.d());
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.isSneaking() || mc.player.isTouchingWater() || mc.player.isInLava()) return;
        if (mc.player.getAbilities().flying) return;
        if (!mc.player.isOnGround() && !inAir.get()) return;

        float f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        float s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && s == 0) return;

        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double dx = -sin * f + cos * s;
        double dz = cos * f + sin * s;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len == 0) return;
        dx /= len;
        dz /= len;

        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(dx * speed.d(), v.y, dz * speed.d());
    }
}
