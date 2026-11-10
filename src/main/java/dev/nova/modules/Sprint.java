package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;

public class Sprint extends Module {
    private final BoolSetting omni = add(new BoolSetting("Omni-directional", false));

    public Sprint() {
        super("Sprint", "Automatically sprints for you.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        boolean forward = mc.options.forwardKey.isPressed();
        boolean moving = forward;
        if (omni.get()) {
            moving = forward
                    || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed()
                    || mc.options.rightKey.isPressed();
        }
        if (moving
                && !mc.player.isSneaking()
                && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }
}
