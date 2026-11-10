package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "See clearly in the dark.", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        // Long duration, no particles, no icon.
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;
        mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
