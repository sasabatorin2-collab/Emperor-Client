package dev.emperor.modules;

import dev.emperor.friend.Friends;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Smoothly pulls your aim towards the closest target inside a field of view (per frame). */
public class AimAssist extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 4.5, 1.0, 8.0, 0.1));
    private final NumberSetting fov = add(new NumberSetting("FOV", 70, 10, 180, 5));
    private final NumberSetting speed = add(new NumberSetting("Speed (deg/s)", 180, 20, 720, 10));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostile = add(new BoolSetting("Hostile", false));
    private final BoolSetting onClick = add(new BoolSetting("Only while attacking", false));

    private long lastNs = System.nanoTime();

    public AimAssist() {
        super("AimAssist", "Smoothly aims at the nearest target in your view.", Category.COMBAT);
    }

    @Override
    public void onRender3d(float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (onClick.get() && !mc.options.attackKey.isPressed()) return;

        Entity best = null;
        float bestAngle = fov.f() / 2f;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || Friends.is(e)) continue;
            if (!((e instanceof PlayerEntity && players.get()) || (e instanceof HostileEntity && hostile.get()))) continue;
            if (mc.player.distanceTo(e) > range.d() || !mc.player.canSee(e)) continue;
            float a = Rotations.angleTo(e);
            if (a < bestAngle) {
                bestAngle = a;
                best = e;
            }
        }
        if (best != null) Rotations.face(best, speed.f() * dt);
    }
}
