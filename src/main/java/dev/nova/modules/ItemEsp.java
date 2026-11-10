package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import dev.nova.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Dropped items through walls. Valuable items (diamond, netherite, totems...) get a different colour. */
public class ItemEsp extends Module {
    private final BoolSetting onlyValuable = add(new BoolSetting("Only valuable", false));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    private final NumberSetting range = add(new NumberSetting("Range", 64, 8, 200, 8));

    public ItemEsp() {
        super("ItemESP", "Shows dropped items through walls (player death loot!).", Category.RENDER);
    }

    @Override
    public void onRender3d(float delta) {
        if (mc.world == null || mc.player == null) return;
        Vec3d start = RenderUtil.lookStart();
        Vec3d cam = RenderUtil.camera();
        double max = range.d() * range.d();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity ie)) continue;
            if (e.squaredDistanceTo(cam) > max) continue;
            boolean val = ChestStealer.valuable(ie.getStack());
            if (onlyValuable.get() && !val) continue;
            int col = val ? 0xFF55FFFF : 0xFFFFFF55;
            Vec3d lp = e.getLerpedPos(delta);
            Box box = e.getBoundingBox().offset(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ()).expand(0.1);
            RenderUtil.box(box, col, true, true);
            if (tracers.get()) RenderUtil.line(start.x, start.y, start.z, lp.x, lp.y + 0.2, lp.z, col);
        }
    }
}
