package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.ModeSetting;
import dev.nova.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Breaks blocks around you. Great for clearing walls and tunnelling through bases. */
public class Nuker extends Module {
    private final NumberSetting radius = add(new NumberSetting("Radius", 4, 1, 6, 1));
    private final ModeSetting mode = add(new ModeSetting("Mode", "All", "All", "Above", "Tunnel"));
    private final NumberSetting perTick = add(new NumberSetting("Blocks/tick (creative)", 4, 1, 12, 1));
    private final BoolSetting autoTool = add(new BoolSetting("Auto tool", true));

    private BlockPos current;

    public Nuker() {
        super("Nuker", "Mines blocks around you. All = sphere, Above = feet level and up, Tunnel = 2-high corridor.", Category.WORLD);
    }

    @Override
    public String getInfo() {
        return mode.get();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;

        BlockPos feet = mc.player.getBlockPos();
        Vec3d eye = mc.player.getEyePos();
        int r = radius.i();
        double r2 = r * r;

        List<BlockPos> list = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = feet.add(x, y, z);
                    if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > r2) continue;
                    if (mode.is("Above") && pos.getY() < feet.getY()) continue;
                    if (mode.is("Tunnel") && (pos.getY() < feet.getY() || pos.getY() > feet.getY() + 1)) continue;
                    BlockState st = mc.world.getBlockState(pos);
                    if (st.isAir() || st.getBlock() instanceof FluidBlock) continue;
                    if (st.getHardness(mc.world, pos) < 0) continue;
                    list.add(pos);
                }
            }
        }
        if (list.isEmpty()) {
            current = null;
            return;
        }
        list.sort((a, b) -> Double.compare(eye.squaredDistanceTo(Vec3d.ofCenter(a)), eye.squaredDistanceTo(Vec3d.ofCenter(b))));

        boolean creative = mc.player.isCreative();
        int n = creative ? Math.min(perTick.i(), list.size()) : 1;

        if (!creative) {
            if (current == null || !list.contains(current)) current = list.get(0);
            breakBlock(current, eye, false);
        } else {
            for (int i = 0; i < n; i++) breakBlock(list.get(i), eye, true);
        }
    }

    private void breakBlock(BlockPos pos, Vec3d eye, boolean creative) {
        Vec3d c = Vec3d.ofCenter(pos);
        Direction d = Direction.getFacing(eye.x - c.x, eye.y - c.y, eye.z - c.z);
        if (autoTool.get() && !creative) {
            mc.player.getInventory().selectedSlot = AutoTool.bestSlot(mc.world.getBlockState(pos));
        }
        if (creative) mc.interactionManager.attackBlock(pos, d);
        else mc.interactionManager.updateBlockBreakingProgress(pos, d);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    protected void onDisable() {
        current = null;
        if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
    }
}
