package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class AutoArmor extends Module {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 3, 0, 20, 1));
    private int timer;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the best armor you have.", Category.COMBAT);
    }

    private static double value(ItemStack s) {
        if (s.isEmpty()) return 0;
        double v = 0;
        AttributeModifiersComponent mods = s.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        for (AttributeModifiersComponent.Entry e : mods.modifiers()) {
            if (e.attribute().equals(EntityAttributes.ARMOR)) v += e.modifier().value();
            else if (e.attribute().equals(EntityAttributes.ARMOR_TOUGHNESS)) v += e.modifier().value() * 0.4;
        }
        return v + 0.001 * (s.getMaxDamage() - s.getDamage()) / Math.max(1, s.getMaxDamage());
    }

    private static EquipmentSlot slotOf(ItemStack s) {
        EquippableComponent eq = s.get(DataComponentTypes.EQUIPPABLE);
        return eq == null ? null : eq.slot();
    }

    /** Armor slot index inside the player's inventory screen handler (5 = head .. 8 = feet). */
    private static int armorSlotId(EquipmentSlot slot) {
        return 8 - slot.getEntitySlotId();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;
        if (timer > 0) {
            timer--;
            return;
        }
        int sync = mc.player.playerScreenHandler.syncId;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack worn = mc.player.getEquippedStack(slot);
            if (worn.isOf(net.minecraft.item.Items.ELYTRA)) continue; // never strip a worn elytra mid-flight
            double best = value(worn);
            int bestIdx = -1;
            for (int i = 0; i < 36; i++) {
                ItemStack s = mc.player.getInventory().getStack(i);
                if (slotOf(s) != slot || s.isOf(net.minecraft.item.Items.ELYTRA)) continue;
                double v = value(s);
                if (v > best + 1e-6) {
                    best = v;
                    bestIdx = i;
                }
            }
            if (bestIdx < 0) continue;
            if (!worn.isEmpty()) {
                // take the old piece off first; the new one goes on next time
                mc.interactionManager.clickSlot(sync, armorSlotId(slot), 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
                int id = bestIdx < 9 ? 36 + bestIdx : bestIdx;
                mc.interactionManager.clickSlot(sync, id, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
            timer = delay.i();
            return;
        }
    }
}
