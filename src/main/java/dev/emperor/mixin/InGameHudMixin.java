package dev.emperor.mixin;

import dev.emperor.hud.Hud;
import dev.emperor.modules.Crosshair;
import dev.emperor.modules.NoRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void emperor$noVignette(CallbackInfo ci) {
        if (NoRender.vignette()) ci.cancel();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void emperor$noPumpkin(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (texture.getPath().contains("pumpkinblur") && NoRender.pumpkin()) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void emperor$crosshair(CallbackInfo ci) {
        if (Crosshair.active()) ci.cancel();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void emperor$effects(CallbackInfo ci) {
        if (Hud.hideVanillaEffects()) ci.cancel();
    }
}
