package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {
    public AutoTool() {
        super("AutoTool", "Picks the fastest hotbar tool for the block you mine.", Category.WORLD);
    }

    /** Index of the best hotbar slot for this block (current slot if nothing is better). */
    public static int bestSlot(BlockState state) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return 0;
        int best = p.getInventory().selectedSlot;
        float bestSpeed = p.getInventory().getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getStack(i);
            float sp = s.getMiningSpeedMultiplier(state);
            if (sp > bestSpeed) {
                bestSpeed = sp;
                best = i;
            }
        }
        return best;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        mc.player.getInventory().selectedSlot = bestSlot(state);
    }
}
