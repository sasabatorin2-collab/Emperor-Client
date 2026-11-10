package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.InvUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoEat extends Module {
    private final NumberSetting hunger = add(new NumberSetting("Eat at hunger", 14, 1, 19, 1));

    private int prevSlot = -1;
    private boolean eating;

    public AutoEat() {
        super("AutoEat", "Eats food from your hotbar when hungry (skips golden apples).", Category.MISC);
    }

    private static boolean isFood(ItemStack s) {
        if (s.isEmpty() || !s.contains(DataComponentTypes.FOOD)) return false;
        return !(s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE) || s.isOf(Items.ROTTEN_FLESH)
                || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.PUFFERFISH) || s.isOf(Items.POISONOUS_POTATO)
                || s.isOf(Items.CHORUS_FRUIT));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;
        int food = mc.player.getHungerManager().getFoodLevel();

        if (food <= hunger.i()) {
            int slot = InvUtil.findHotbar(AutoEat::isFood);
            if (slot >= 0) {
                if (!eating) {
                    prevSlot = InvUtil.selected();
                    eating = true;
                }
                InvUtil.selectSlot(slot);
                mc.options.useKey.setPressed(true);
                return;
            }
        }
        stopEating();
    }

    private void stopEating() {
        if (!eating) return;
        mc.options.useKey.setPressed(false);
        if (prevSlot >= 0) InvUtil.selectSlot(prevSlot);
        prevSlot = -1;
        eating = false;
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) stopEating();
    }
}
