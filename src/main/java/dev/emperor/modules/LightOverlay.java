package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

/** Marks blocks where hostile mobs can spawn: red = any time, yellow = only at night. */
public class LightOverlay extends Module {
    private record Spot(BlockPos pos, boolean always) {
    }

    private final NumberSetting radius = add(new NumberSetting("Radius", 16, 4, 32, 1));
    private final BoolSetting nightOnly = add(new BoolSetting("Show night-only spots", true));
    private final List<Spot> spots = new ArrayList<>();
    private int timer;

    public LightOverlay() {
        super("LightOverlay", "Shows where mobs can spawn (dark blocks).", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return String.valueOf(spots.size());
    }

    @Override
    protected void onDisable() {
        spots.clear();
    }

    @Override
    public void onTick() {
        if (--timer > 0) return;
        timer = 10;
        spots.clear();
        int r = radius.i();
        BlockPos c = mc.player.getBlockPos();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -8; dy <= 4; dy++) {
                    p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState here = mc.world.getBlockState(p);
                    if (!here.getCollisionShape(mc.world, p).isEmpty() || !here.getFluidState().isEmpty()) continue;
                    BlockPos below = p.down();
                    BlockState floor = mc.world.getBlockState(below);
                    if (!floor.isSideSolidFullSquare(mc.world, below, Direction.UP)
                            || !floor.allowsSpawning(mc.world, below, EntityType.ZOMBIE)) continue;
                    if (mc.world.getLightLevel(LightType.BLOCK, p) > 0) continue;
                    boolean always = mc.world.getLightLevel(LightType.SKY, p) == 0;
                    if (!always && !nightOnly.get()) continue;
                    spots.add(new Spot(p.toImmutable(), always));
                    if (spots.size() > 4000) return;
                }
            }
        }
    }

    @Override
    public void onRender3d(float delta) {
        for (Spot s : spots) {
            double x = s.pos.getX(), y = s.pos.getY() + 0.01, z = s.pos.getZ();
            int col = s.always ? 0xFFFF4C5C : 0xFFFFD84C;
            RenderUtil.line(x + 0.2, y, z + 0.2, x + 0.8, y, z + 0.8, col, 0.85f);
            RenderUtil.line(x + 0.8, y, z + 0.2, x + 0.2, y, z + 0.8, col, 0.85f);
            if (s.always) RenderUtil.box(new Box(x, y - 0.005, z, x + 1, y, z + 1), col, true, false);
        }
    }
}
