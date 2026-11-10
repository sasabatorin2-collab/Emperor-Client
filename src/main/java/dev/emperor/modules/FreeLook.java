package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Look around your character in third person without turning your body. */
public class FreeLook extends Module {
    private final BoolSetting hold = add(new BoolSetting("Hold key", true));
    private float yaw, pitch;
    private Perspective prev;

    public FreeLook() {
        super("FreeLook", "Orbit the camera around yourself while you keep walking straight.", Category.RENDER);
    }

    @Override
    public boolean saveEnabled() {
        return false;
    }

    @Override
    public boolean showNotifications() {
        return false;
    }

    public static FreeLook active() {
        FreeLook f = ModuleManager.get(FreeLook.class);
        return f != null && f.isEnabled() && Freecam.active() == null ? f : null;
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) return;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        prev = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    @Override
    protected void onDisable() {
        if (prev != null) mc.options.setPerspective(prev);
    }

    @Override
    public void onTick() {
        if (hold.get() && getKey() >= 0 && !holdMode && !dev.emperor.util.Keys.isDown(getKey())) setEnabled(false);
    }

    public static boolean handleLook(Entity entity, double dx, double dy) {
        FreeLook f = active();
        if (f == null || entity != MinecraftClient.getInstance().player) return false;
        f.yaw += (float) dx * 0.15f;
        f.pitch = MathHelper.clamp(f.pitch + (float) dy * 0.15f, -90f, 90f);
        return true;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }
}
