package dev.emperor.modules;

import dev.emperor.mixin.FishingBobberEntityAccessor;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFish extends Module {
    private final NumberSetting recast = add(new NumberSetting("Recast delay", 15, 5, 60, 1));
    private int recastTimer;
    private int reelCooldown;
    private int caught;

    public AutoFish() {
        super("AutoFish", "Reels in and recasts your fishing rod automatically.", Category.WORLD);
    }

    @Override
    public String getInfo() {
        return String.valueOf(caught);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!mc.player.getMainHandStack().isOf(Items.FISHING_ROD)) return;
        if (reelCooldown > 0) reelCooldown--;

        if (mc.player.fishHook == null) {
            if (recastTimer > 0 && --recastTimer == 0) use();
            return;
        }
        if (reelCooldown == 0 && ((FishingBobberEntityAccessor) mc.player.fishHook).emperor$isCaughtFish()) {
            use();
            caught++;
            reelCooldown = 10;
            recastTimer = recast.i();
        }
    }

    private void use() {
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
