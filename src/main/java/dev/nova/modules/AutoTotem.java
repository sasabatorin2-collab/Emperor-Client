package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 2, 0, 10, 1));
    private int timer;

    public AutoTotem() {
        super("AutoTotem", "Keeps a totem of undying in your offhand.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (timer > 0) {
            timer--;
            return;
        }
        for (int i = 0; i < 36; i++) {
            if (!mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) continue;
            int slotId = i < 9 ? 36 + i : i;
            // button 40 = swap with the offhand slot
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotId, 40, SlotActionType.SWAP, mc.player);
            timer = delay.i();
            return;
        }
    }
}
