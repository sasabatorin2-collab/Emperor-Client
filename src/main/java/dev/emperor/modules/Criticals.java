package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {
    public Criticals() {
        super("Criticals", "Turns every melee hit into a critical hit.", Category.COMBAT);
    }

    /** Called from the attack mixin just before the attack packet is sent. */
    public static void onAttack(Entity target) {
        Criticals c = ModuleManager.get(Criticals.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.isEnabled() || mc.player == null || mc.getNetworkHandler() == null) return;
        if (!(target instanceof LivingEntity) || !mc.player.isOnGround() || mc.player.isTouchingWater()
                || mc.player.isInLava() || mc.player.isClimbing() || mc.player.hasVehicle()) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        boolean hc = mc.player.horizontalCollision;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, hc));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, hc));
    }
}
