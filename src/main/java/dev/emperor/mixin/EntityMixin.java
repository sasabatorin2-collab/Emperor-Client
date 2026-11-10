package dev.emperor.mixin;

import dev.emperor.modules.Freecam;
import dev.emperor.modules.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void emperor$freecamLook(double dx, double dy, CallbackInfo ci) {
        if (Freecam.handleLook((Entity) (Object) this, dx, dy)) ci.cancel();
    }

    // Lower mouse sensitivity while zoomed so aiming stays precise.
    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double emperor$zoomSensX(double dx) {
        return (Object) this == MinecraftClient.getInstance().player ? dx * Zoom.sensitivity() : dx;
    }

    @ModifyVariable(method = "changeLookDirection", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double emperor$zoomSensY(double dy) {
        return (Object) this == MinecraftClient.getInstance().player ? dy * Zoom.sensitivity() : dy;
    }
}
