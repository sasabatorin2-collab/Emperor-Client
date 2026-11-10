package dev.emperor.mixin;

import dev.emperor.modules.SafeWalk;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void emperor$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalk.active((PlayerEntity) (Object) this)) cir.setReturnValue(true);
    }
}
