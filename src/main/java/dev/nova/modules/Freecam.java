package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.BoolSetting;
import dev.nova.setting.NumberSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * Smooth free camera. The camera position and rotation are stored here and applied to the
 * render Camera every frame (see CameraMixin), so movement is not tied to the 20 TPS game tick.
 * The real player gets no input at all, so the server only sees you standing still.
 */
public class Freecam extends Module {
    private static Freecam instance;

    private final NumberSetting speed = add(new NumberSetting("Speed (blocks/s)", 14, 2, 80, 1));
    private final NumberSetting boost = add(new NumberSetting("Ctrl boost", 3.0, 1.0, 8.0, 0.5));
    private final BoolSetting followLook = add(new BoolSetting("Fly toward look direction", true));
    private final BoolSetting smooth = add(new BoolSetting("Smooth acceleration", true));

    private double x, y, z;
    private double vx, vy, vz;
    private float yaw, pitch;
    private long lastNs;
    private boolean prevCulling;

    public Freecam() {
        super("Freecam", "Fly around with a free camera while your body stays put.", Category.RENDER);
        instance = this;
    }

    /** The active instance, or null if Freecam is off. */
    public static Freecam active() {
        return instance != null && instance.isEnabled() ? instance : null;
    }

    @Override
    public boolean saveEnabled() {
        return false;
    }

    @Override
    public String getInfo() {
        return String.format(java.util.Locale.ROOT, "%.0f", speed.d());
    }

    @Override
    protected void onEnable() {
        Camera c = mc.gameRenderer.getCamera();
        x = c.getPos().x;
        y = c.getPos().y;
        z = c.getPos().z;
        yaw = c.getYaw();
        pitch = c.getPitch();
        vx = vy = vz = 0;
        lastNs = System.nanoTime();
        // Otherwise chunks around the real player stay visible but far chunks get culled wrongly.
        prevCulling = mc.chunkCullingEnabled;
        mc.chunkCullingEnabled = false;
    }

    @Override
    protected void onDisable() {
        mc.chunkCullingEnabled = prevCulling;
        // Re-sync the real key states that we forced to "released" while the camera was active.
        KeyBinding.updatePressedStates();
    }

    /** Mouse look (called from EntityMixin). True = consumed, the real player does not turn. */
    public static boolean handleLook(Entity entity, double dx, double dy) {
        Freecam f = active();
        if (f == null || entity != mc.player) return false;
        f.yaw += (float) dx * 0.15f;
        f.pitch = MathHelper.clamp(f.pitch + (float) dy * 0.15f, -90f, 90f);
        return true;
    }

    private static boolean down(KeyBinding kb) {
        if (mc.currentScreen != null) return false;
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(kb);
        if (key.getCategory() != InputUtil.Type.KEYSYM) return false;
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getCode());
    }

    /** Called every rendered frame from CameraMixin: moves the camera and returns nothing. */
    public void frameUpdate() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastNs) / 1_000_000_000.0);
        lastNs = now;

        float f = (down(mc.options.forwardKey) ? 1 : 0) - (down(mc.options.backKey) ? 1 : 0);
        float s = (down(mc.options.leftKey) ? 1 : 0) - (down(mc.options.rightKey) ? 1 : 0);
        float up = (down(mc.options.jumpKey) ? 1 : 0) - (down(mc.options.sneakKey) ? 1 : 0);

        double yawR = Math.toRadians(yaw);
        double pitchR = followLook.get() ? Math.toRadians(pitch) : 0.0;
        double cp = Math.cos(pitchR);
        double dx = -Math.sin(yawR) * cp * f + Math.cos(yawR) * s;
        double dy = -Math.sin(pitchR) * f + up;
        double dz = Math.cos(yawR) * cp * f + Math.sin(yawR) * s;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1e-6) {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        double sp = speed.d();
        boolean ctrl = mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL);
        if (ctrl) sp *= boost.d();

        double tx = dx * sp, ty = dy * sp, tz = dz * sp;
        if (smooth.get()) {
            double k = 1.0 - Math.exp(-dt * 14.0); // frame-rate independent easing
            vx += (tx - vx) * k;
            vy += (ty - vy) * k;
            vz += (tz - vz) * k;
        } else {
            vx = tx;
            vy = ty;
            vz = tz;
        }

        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    /** Runs at the start of every client tick, before the player reads its input. */
    @Override
    public void onPreTick() {
        if (mc.player == null) return;
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
        while (mc.options.attackKey.wasPressed()) {
        }
        while (mc.options.useKey.wasPressed()) {
        }
        while (mc.options.pickItemKey.wasPressed()) {
        }
    }
}
