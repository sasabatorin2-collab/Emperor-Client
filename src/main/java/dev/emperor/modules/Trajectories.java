package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/** Predicts where arrows, pearls, tridents and throwables will land. */
public class Trajectories extends Module {
    private final BoolSetting entities = add(new BoolSetting("Hit entities", true));

    public Trajectories() {
        super("Trajectories", "Shows the flight path and landing spot of projectiles.", Category.RENDER);
    }

    /** {speed, gravity, drag} or null if the item is not a projectile. */
    private double[] physics(ItemStack s) {
        Item i = s.getItem();
        if (i instanceof BowItem) {
            float pull = mc.player.isUsingItem() && mc.player.getActiveItem() == s
                    ? BowItem.getPullProgress(mc.player.getItemUseTime()) : 1f;
            return new double[]{Math.max(0.1, pull) * 3.0, 0.05, 0.99};
        }
        if (i instanceof CrossbowItem) return new double[]{3.15, 0.05, 0.99};
        if (i instanceof TridentItem) return new double[]{2.5, 0.05, 0.99};
        if (i == Items.ENDER_PEARL || i == Items.SNOWBALL || i == Items.EGG) return new double[]{1.5, 0.03, 0.99};
        if (i == Items.SPLASH_POTION || i == Items.LINGERING_POTION) return new double[]{0.5, 0.05, 0.99};
        if (i == Items.EXPERIENCE_BOTTLE) return new double[]{0.7, 0.07, 0.99};
        return null;
    }

    @Override
    public void onRender3d(float delta) {
        if (mc.player == null || mc.world == null) return;
        double[] ph = physics(mc.player.getMainHandStack());
        if (ph == null) ph = physics(mc.player.getOffHandStack());
        if (ph == null) return;

        float yaw = (float) Math.toRadians(mc.player.getYaw(delta));
        float pitch = (float) Math.toRadians(mc.player.getPitch(delta));
        Vec3d pos = mc.player.getLerpedPos(delta).add(0, mc.player.getStandingEyeHeight() - 0.1, 0);
        Vec3d vel = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).multiply(ph[0]);

        List<Entity> candidates = new ArrayList<>();
        if (entities.get()) {
            for (Entity e : mc.world.getEntities()) {
                if (e != mc.player && e instanceof LivingEntity && e.isAlive() && mc.player.squaredDistanceTo(e) < 128 * 128) candidates.add(e);
            }
        }

        Vec3d prev = pos;
        HitResult hit = null;
        Entity hitEntity = null;
        for (int step = 0; step < 300; step++) {
            Vec3d next = prev.add(vel);
            BlockHitResult bh = mc.world.raycast(new RaycastContext(prev, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (bh.getType() != HitResult.Type.MISS) next = bh.getPos();
            for (Entity e : candidates) {
                var r = e.getBoundingBox().expand(0.3).raycast(prev, next);
                if (r.isPresent()) {
                    next = r.get();
                    hitEntity = e;
                }
            }
            if (step > 1) RenderUtil.line(prev.x, prev.y, prev.z, next.x, next.y, next.z, Theme.accent(step / 2), 0.9f);
            if (hitEntity != null || bh.getType() != HitResult.Type.MISS) {
                hit = bh;
                prev = next;
                break;
            }
            prev = next;
            vel = vel.multiply(ph[2]).subtract(0, ph[1], 0);
            if (prev.y < mc.world.getBottomY() - 16) break;
        }

        if (hitEntity != null) {
            RenderUtil.box(hitEntity.getBoundingBox(), 0xFFFF4C5C, true, true);
        } else if (hit != null) {
            double r = 0.25;
            RenderUtil.box(new Box(prev.x - r, prev.y - r, prev.z - r, prev.x + r, prev.y + r, prev.z + r), Theme.primary(), true, true);
            RenderUtil.ring(prev.x, prev.y + 0.02, prev.z, 0.5, 24, 0.9f, i -> Theme.accent(i));
        }
    }
}
