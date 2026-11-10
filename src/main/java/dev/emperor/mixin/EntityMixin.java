package dev.emperor.mixin;

import dev.emperor.modules.Freecam;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void emperor$freecamLook(double dx, double dy, CallbackInfo ci) {
        if (Freecam.handleLook((Entity) (Object) this, dx, dy)) ci.cancel();
    }
}
