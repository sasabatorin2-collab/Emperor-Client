package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.InvUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Water-bucket clutch: places water just before you hit the ground, then picks it back up. */
public class AutoMLG extends Module {
    private final NumberSetting minFall = add(new NumberSetting("Min fall", 5, 3, 30, 1));
    private final BoolSetting pickUp = add(new BoolSetting("Pick water up", true));

    private enum State { IDLE, PLACED, PICKUP }

    private State state = State.IDLE;
    private int prevSlot, slot, timer;
    private float prevPitch;

    public AutoMLG() {
        super("AutoMLG", "Saves you from fall damage with a water bucket clutch.", Category.WORLD);
    }

    @Override
    protected void onDisable() {
        state = State.IDLE;
    }

    @Override
    public void onTick() {
        if (mc.interactionManager == null || mc.world.getDimension().ultrawarm()) return;
        switch (state) {
            case IDLE -> {
                if (mc.player.isOnGround() || mc.player.fallDistance < minFall.d() || mc.player.getVelocity().y > -0.4) return;
                if (mc.player.isTouchingWater() || mc.player.isGliding() || mc.player.getAbilities().flying) return;
                Vec3d from = mc.player.getPos();
                double reach = Math.max(2.5, -mc.player.getVelocity().y * 3);
                BlockHitResult hit = mc.world.raycast(new RaycastContext(from, from.add(0, -reach, 0),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player));
                if (hit.getType() != HitResult.Type.BLOCK) return;
                int s = InvUtil.findHotbar(st -> st.isOf(Items.WATER_BUCKET));
                if (s < 0) return;
                prevSlot = InvUtil.selected();
                prevPitch = mc.player.getPitch();
                slot = s;
                InvUtil.selectSlot(s);
                mc.player.setPitch(90f);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                state = State.PLACED;
                timer = 0;
            }
            case PLACED -> {
                timer++;
                if (mc.player.isOnGround() || mc.player.isTouchingWater() || timer > 40) {
                    if (pickUp.get() && timer <= 40) {
                        state = State.PICKUP;
                        timer = 0;
                    } else {
                        restore();
                    }
                }
            }
            case PICKUP -> {
                if (++timer < 3) return;
                InvUtil.selectSlot(slot);
                if (mc.player.getInventory().getStack(slot).isOf(Items.BUCKET)) {
                    mc.player.setPitch(90f);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                restore();
            }
        }
    }

    private void restore() {
        InvUtil.selectSlot(prevSlot);
        mc.player.setPitch(prevPitch);
        state = State.IDLE;
    }
}
