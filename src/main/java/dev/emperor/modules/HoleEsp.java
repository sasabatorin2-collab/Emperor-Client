package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/** Highlights 1x1 holes surrounded by bedrock (green) or bedrock/obsidian (orange). */
public class HoleEsp extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 6, 2, 12, 1));
    private final BoolSetting obsidian = add(new BoolSetting("Obsidian holes", true));

    private final List<BlockPos> bedrockHoles = new ArrayList<>();
    private final List<BlockPos> obbyHoles = new ArrayList<>();
    private long lastScan = -100;

    public HoleEsp() {
        super("HoleESP", "Shows safe holes (for crystal PvP / hiding).", Category.RENDER);
    }

    private static boolean bedrock(Block b) {
        return b == Blocks.BEDROCK;
    }

    private static boolean solidSafe(Block b) {
        return b == Blocks.BEDROCK || b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        long t = mc.world.getTime();
        if (t - lastScan < 10 && t >= lastScan) return;
        lastScan = t;

        bedrockHoles.clear();
        obbyHoles.clear();
        BlockPos base = mc.player.getBlockPos();
        int r = range.i();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    p.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    if (!mc.world.getBlockState(p).isAir()) continue;
                    if (!mc.world.getBlockState(p.up()).isAir()) continue;
                    Block d = mc.world.getBlockState(p.down()).getBlock();
                    Block n = mc.world.getBlockState(p.north()).getBlock();
                    Block s = mc.world.getBlockState(p.south()).getBlock();
                    Block e = mc.world.getBlockState(p.east()).getBlock();
                    Block w = mc.world.getBlockState(p.west()).getBlock();
                    if (!(solidSafe(d) && solidSafe(n) && solidSafe(s) && solidSafe(e) && solidSafe(w))) continue;
                    if (bedrock(d) && bedrock(n) && bedrock(s) && bedrock(e) && bedrock(w)) bedrockHoles.add(p.toImmutable());
                    else if (obsidian.get()) obbyHoles.add(p.toImmutable());
                }
            }
        }
    }

    @Override
    public void onRender3d(float delta) {
        for (BlockPos p : bedrockHoles) {
            RenderUtil.box(new Box(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 0.1, p.getZ() + 1), 0xFF55FF55, true, true);
        }
        for (BlockPos p : obbyHoles) {
            RenderUtil.box(new Box(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 0.1, p.getZ() + 1), 0xFFFFAA00, true, true);
        }
    }

    @Override
    protected void onDisable() {
        bedrockHoles.clear();
        obbyHoles.clear();
    }
}
