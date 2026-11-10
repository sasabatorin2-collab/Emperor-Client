package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class Velocity extends Module {
    private final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 0, 100, 5));
    private final NumberSetting vertical = add(new NumberSetting("Vertical %", 0, 0, 100, 5));

    public Velocity() {
        super("Velocity", "Reduces the knockback you take.", Category.COMBAT);
    }

    @Override
    public String getInfo() {
        return horizontal.i() + "% " + vertical.i() + "%";
    }

    /** Returns true if the packet was handled here and vanilla handling must be skipped. */
    public static boolean handle(EntityVelocityUpdateS2CPacket p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Velocity v = ModuleManager.get(Velocity.class);
        // The handler first runs on the network thread and is re-queued onto the main thread.
        if (v == null || !v.isEnabled() || !mc.isOnThread() || mc.player == null) return false;
        if (p.getEntityId() != mc.player.getId()) return false;
        double h = v.horizontal.d() / 100.0, y = v.vertical.d() / 100.0;
        mc.player.setVelocityClient(p.getVelocityX() * h, p.getVelocityY() * y, p.getVelocityZ() * h);
        return true;
    }
}
