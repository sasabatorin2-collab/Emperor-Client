package dev.emperor.mixin;

import dev.emperor.command.Commands;
import dev.emperor.modules.Velocity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
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
}
