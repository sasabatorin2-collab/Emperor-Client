package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile", false));
    private final BoolSetting passive = add(new BoolSetting("Passive", false));

    public Tracers() {
        super("Tracers", "Draws lines to entities.", Category.RENDER);
    }

    @Override
    public void onRender3d(float delta) {
        if (mc.world == null || mc.player == null) return;
        Vec3d start = RenderUtil.lookStart();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity)) continue;
            int col;
            if (e instanceof PlayerEntity && players.get()) col = dev.emperor.friend.Friends.is(e) ? dev.emperor.hud.Theme.primary() : 0xFF55FFFF;
            else if (e instanceof HostileEntity && hostile.get()) col = 0xFFFF5555;
            else if (e instanceof AnimalEntity && passive.get()) col = 0xFF55FF55;
            else continue;
            Vec3d p = e.getLerpedPos(delta).add(0, e.getHeight() / 2.0, 0);
            RenderUtil.line(start.x, start.y, start.z, p.x, p.y, p.z, col);
        }
    }
}
