package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;

public class Step extends Module {
    private final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.6, 2.5, 0.1));

    public Step() {
        super("Step", "Walk up blocks without jumping.", Category.MOVEMENT);
    }

    @Override
    public String getInfo() {
        return String.format(java.util.Locale.ROOT, "%.1f", height.d());
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        EntityAttributeInstance a = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (a != null) a.setBaseValue(height.d());
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;
        EntityAttributeInstance a = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (a != null) a.setBaseValue(0.6);
    }
}
