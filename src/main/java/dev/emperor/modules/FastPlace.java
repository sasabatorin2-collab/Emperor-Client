package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.mixin.MinecraftClientAccessor;
import dev.emperor.setting.NumberSetting;

public class FastPlace extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 0, 0, 3, 1));

    public FastPlace() {
        super("FastPlace", "Removes the delay between placing blocks / using items.", Category.WORLD);
    }

    @Override
    public void onTick() {
        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        if (acc.emperor$getItemUseCooldown() > delay.i()) acc.emperor$setItemUseCooldown(delay.i());
    }
}
