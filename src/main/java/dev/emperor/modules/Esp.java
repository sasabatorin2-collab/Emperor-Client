package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Esp extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Box", "Box", "Glow"));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile", true));
    private final BoolSetting passive = add(new BoolSetting("Passive", false));

    public Esp() {
        super("ESP", "Outlines entities through walls.", Category.RENDER);
    }

    private boolean matches(Entity e) {
        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof HostileEntity) return hostile.get();
        if (e instanceof AnimalEntity) return passive.get();
        return false;
    }

    private static int colorOf(Entity e) {
        if (e instanceof PlayerEntity) return 0xFF55FFFF;
        if (e instanceof HostileEntity) return 0xFFFF5555;
        return 0xFF55FF55;
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        boolean glow = mode.is("Glow");
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le)) continue;
            if (glow && matches(e)) {
                e.setGlowing(true);
            } else if (!le.hasStatusEffect(StatusEffects.GLOWING)) {
                e.setGlowing(false);
            }
        }
    }

    @Override
    public void onRender3d(float delta) {
        if (!mode.is("Box") || mc.world == null) return;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity) || !matches(e)) continue;
            Vec3d lp = e.getLerpedPos(delta);
            Box box = e.getBoundingBox().offset(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ());
            RenderUtil.box(box, colorOf(e), true, true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.world == null) return;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity le && !le.hasStatusEffect(StatusEffects.GLOWING)) {
                e.setGlowing(false);
            }
        }
    }
}
