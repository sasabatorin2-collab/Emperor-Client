package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

/** Replaces the thin black block outline with an animated, theme-coloured one. */
public class BlockOutline extends Module {
    private final BoolSetting fill = add(new BoolSetting("Fill", true));
    private final BoolSetting pulse = add(new BoolSetting("Pulse", true));

    public BlockOutline() {
        super("BlockOutline", "Animated gradient outline around the block you look at.", Category.RENDER);
    }

    /** Called from the outline event: false hides the vanilla outline. */
    public static boolean vanillaAllowed() {
        BlockOutline b = ModuleManager.get(BlockOutline.class);
        return b == null || !b.isEnabled();
    }

    @Override
    public void onRender3d(float delta) {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return;
        Box box = shape.getBoundingBox().offset(pos);
        double t = System.currentTimeMillis() / 1000.0;
        if (pulse.get()) box = box.expand(0.004 + 0.012 * (0.5 + 0.5 * Math.sin(t * 4)));
        else box = box.expand(0.004);
        RenderUtil.box(box, Theme.main(), fill.get(), true);
    }
}
