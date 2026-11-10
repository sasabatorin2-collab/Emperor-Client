package dev.emperor.modules;

import dev.emperor.friend.Friends;
import dev.emperor.hud.Hud;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.RenderUtil;
import dev.emperor.util.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class KillAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 3.5, 1.0, 6.0, 0.1));
    private final ModeSetting priority = add(new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile", true));
    private final BoolSetting passive = add(new BoolSetting("Passive", false));
    private final BoolSetting walls = add(new BoolSetting("Through walls", false));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", true));
    private final BoolSetting targetEsp = add(new BoolSetting("Target circle", true));

    private LivingEntity target;

    public KillAura() {
        super("KillAura", "Attacks nearby entities when your weapon is charged.", Category.COMBAT);
    }

    @Override
    public String getInfo() {
        return priority.get();
    }

    private boolean valid(Entity e) {
        if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || Friends.is(e)) return false;
        boolean ok = (e instanceof PlayerEntity && players.get()) || (e instanceof HostileEntity && hostile.get())
                || (e instanceof AnimalEntity && passive.get());
        if (!ok || mc.player.squaredDistanceTo(e) > range.d() * range.d()) return false;
        return walls.get() || mc.player.canSee(e);
    }

    private double score(LivingEntity e) {
        return switch (priority.get()) {
            case "Health" -> e.getHealth() + e.getAbsorptionAmount();
            case "Angle" -> Rotations.angleTo(e);
            default -> mc.player.squaredDistanceTo(e);
        };
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        target = null;
        if (mc.currentScreen != null) return;

        double best = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!valid(e)) continue;
            double s = score((LivingEntity) e);
            if (s < best) {
                best = s;
                target = (LivingEntity) e;
            }
        }
        if (target == null) return;
        Hud.setTarget(target);
        if (rotate.get()) Rotations.face(target, Float.POSITIVE_INFINITY);
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.95f) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void onRender3d(float delta) {
        if (!targetEsp.get() || target == null || !target.isAlive()) return;
        Vec3d p = target.getLerpedPos(delta);
        double t = System.currentTimeMillis() / 1000.0;
        double y = p.y + (Math.sin(t * 2.5) * 0.5 + 0.5) * target.getHeight();
        double r = target.getWidth() * 0.8 + 0.1;
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double a1 = Math.PI * 2 * i / seg, a2 = Math.PI * 2 * (i + 1) / seg;
            RenderUtil.line(p.x + Math.cos(a1) * r, y, p.z + Math.sin(a1) * r,
                    p.x + Math.cos(a2) * r, y, p.z + Math.sin(a2) * r, Theme.accent(i % (seg / 2)));
        }
    }

    @Override
    protected void onDisable() {
        target = null;
    }
}
