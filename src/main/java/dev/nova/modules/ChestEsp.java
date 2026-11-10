package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import dev.nova.util.RenderUtil;
import dev.nova.util.WorldScan;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ChestEsp extends Module {
    private final BoolSetting chests = add(new BoolSetting("Chests", true));
    private final BoolSetting ender = add(new BoolSetting("Ender chests", true));
    private final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));
    private final BoolSetting barrels = add(new BoolSetting("Barrels", true));
    private final BoolSetting other = add(new BoolSetting("Furnace/Hopper/Dispenser", false));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    private final NumberSetting range = add(new NumberSetting("Range", 128, 16, 256, 8));

    private int count;

    public ChestEsp() {
        super("ChestESP", "Shows storage blocks through walls.", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return String.valueOf(count);
    }

    private int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return chests.get() ? 0xFFFF5555 : -1;
        if (be instanceof ChestBlockEntity) return chests.get() ? 0xFFFFAA00 : -1;
        if (be instanceof EnderChestBlockEntity) return ender.get() ? 0xFFB06CFF : -1;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.get() ? 0xFFFF55FF : -1;
        if (be instanceof BarrelBlockEntity) return barrels.get() ? 0xFFC08A4A : -1;
        if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity) {
            return other.get() ? 0xFF9AA0A6 : -1;
        }
        return -1;
    }

    @Override
    public void onRender3d(float delta) {
        if (mc.player == null) return;
        Vec3d cam = RenderUtil.camera();
        double max = range.d() * range.d();
        Vec3d start = RenderUtil.lookStart();
        int n = 0;
        for (BlockEntity be : WorldScan.blockEntities()) {
            int col = colorOf(be);
            if (col == -1) continue;
            if (be.getPos().getSquaredDistance(cam.x, cam.y, cam.z) > max) continue;
            n++;
            RenderUtil.box(new Box(be.getPos()).contract(0.05), col, true, true);
            if (tracers.get()) {
                Vec3d c = Vec3d.ofCenter(be.getPos());
                RenderUtil.line(start.x, start.y, start.z, c.x, c.y, c.z, col);
            }
        }
        count = n;
    }
}
