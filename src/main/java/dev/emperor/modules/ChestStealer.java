package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** Moves items out of an opened chest/barrel/shulker automatically. */
public class ChestStealer extends Module {
    private final ModeSetting mode = add(new ModeSetting("Items", "Valuables", "Valuables", "All"));
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 6, 1));
    private final BoolSetting autoClose = add(new BoolSetting("Auto close", true));

    private int timer;

    public ChestStealer() {
        super("ChestStealer", "Takes items from containers. Valuables = diamond, netherite, totems, elytra, shulkers, enchanted gear.", Category.WORLD);
    }

    static boolean valuable(ItemStack s) {
        if (s.isEmpty()) return false;
        Item item = s.getItem();
        if (item instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) return true;
        if (item == Items.TOTEM_OF_UNDYING || item == Items.ELYTRA || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.END_CRYSTAL || item == Items.ANCIENT_DEBRIS || item == Items.ENCHANTED_BOOK
                || item == Items.EXPERIENCE_BOTTLE || item == Items.TRIDENT || item == Items.NETHER_STAR
                || item == Items.BEACON || item == Items.DRAGON_EGG || item == Items.OBSIDIAN) return true;
        String path = Registries.ITEM.getId(item).getPath();
        if (path.contains("diamond") || path.contains("netherite") || path.contains("emerald")) return true;
        return s.hasEnchantments();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        ScreenHandler h = mc.player.currentScreenHandler;
        int size;
        if (h instanceof GenericContainerScreenHandler g) size = g.getRows() * 9;
        else if (h instanceof ShulkerBoxScreenHandler) size = 27;
        else return;

        if (timer > 0) {
            timer--;
            return;
        }

        boolean full = mc.player.getInventory().getEmptySlot() == -1;
        if (!full) {
            for (int i = 0; i < size; i++) {
                ItemStack st = h.getSlot(i).getStack();
                if (st.isEmpty()) continue;
                if (mode.is("Valuables") && !valuable(st)) continue;
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                timer = delay.i();
                return;
            }
        }
        if (autoClose.get()) mc.player.closeHandledScreen();
    }
}
