package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.InvUtil;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Scaffold extends Module {
    private final BoolSetting switchBack = add(new BoolSetting("Switch back", true));

    public Scaffold() {
        super("Scaffold", "Places blocks under your feet while you walk.", Category.WORLD);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        BlockPos target = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        if (!mc.world.getBlockState(target).isReplaceable()) return;

        int slot = InvUtil.findHotbar(s -> s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof FallingBlock));
        if (slot < 0) return;

        int prev = InvUtil.selected();
        InvUtil.selectSlot(slot);

        for (Direction d : Direction.values()) {
            BlockPos neighbor = target.offset(d);
            if (mc.world.getBlockState(neighbor).isReplaceable()) continue;
            Direction face = d.getOpposite();
            Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(face.getVector()).multiply(0.5));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, face, neighbor, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
        }

        if (switchBack.get()) InvUtil.selectSlot(prev);
    }
}
