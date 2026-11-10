package dev.emperor.mixin;

import dev.emperor.modules.NoRender;
import dev.emperor.modules.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void emperor$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        // changingFov is false for the hand/item pass, which should not zoom.
        if (changingFov) cir.setReturnValue(Zoom.apply(cir.getReturnValueF()));
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void emperor$noHurtCam(CallbackInfo ci) {
        if (NoRender.hurtCam()) ci.cancel();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void emperor$noTotem(ItemStack stack, CallbackInfo ci) {
        if (stack.isOf(Items.TOTEM_OF_UNDYING) && NoRender.totem()) ci.cancel();
    }
}
