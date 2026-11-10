package dev.emperor.mixin;

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
}
