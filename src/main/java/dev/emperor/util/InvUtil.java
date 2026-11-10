package dev.emperor.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public final class InvUtil {
    private InvUtil() {
    }

    public static int findHotbar(Predicate<ItemStack> filter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (filter.test(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    public static void selectSlot(int slot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.getInventory().selectedSlot = slot;
    }

    public static int selected() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player == null ? 0 : mc.player.getInventory().selectedSlot;
    }
}
