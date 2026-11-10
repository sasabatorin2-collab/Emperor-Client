package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Iterator;

public class Breadcrumbs extends Module {
    private final NumberSetting length = add(new NumberSetting("Length", 400, 50, 2000, 50));
    private final ArrayDeque<Vec3d> points = new ArrayDeque<>();
    private ClientWorld world;

    public Breadcrumbs() {
        super("Breadcrumbs", "Leaves a glowing trail where you walked.", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        points.clear();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.world != world) {
            points.clear();
            world = mc.world;
        }
        Vec3d p = mc.player.getPos().add(0, 0.1, 0);
        if (points.isEmpty() || points.peekLast().squaredDistanceTo(p) > 0.04) points.addLast(p);
        while (points.size() > length.i()) points.removeFirst();
    }

    @Override
    public void onRender3d(float delta) {
        Iterator<Vec3d> it = points.iterator();
        if (!it.hasNext()) return;
        Vec3d prev = it.next();
        int i = 0;
        while (it.hasNext()) {
            Vec3d p = it.next();
            if (prev.squaredDistanceTo(p) < 25) RenderUtil.line(prev.x, prev.y, prev.z, p.x, p.y, p.z, Theme.accent(i / 4));
            prev = p;
            i++;
        }
    }
}
