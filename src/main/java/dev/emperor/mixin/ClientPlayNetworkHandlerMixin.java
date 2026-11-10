package dev.emperor.mixin;

import dev.emperor.command.Commands;
import dev.emperor.modules.Velocity;
import dev.emperor.util.TickRate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void emperor$commands(String content, CallbackInfo ci) {
        if (Commands.handle(content)) ci.cancel();
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void emperor$velocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (Velocity.handle(packet)) ci.cancel();
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void emperor$tps(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        // Handlers run once on the netty thread, then again on the main thread; count only one.
        if (MinecraftClient.getInstance().isOnThread()) TickRate.onTimeUpdate();
    }
}
