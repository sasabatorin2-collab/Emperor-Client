package dev.emperor.mixin;

import dev.emperor.module.ModuleManager;
import dev.emperor.modules.Zoom;
import dev.emperor.util.Keys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void emperor$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Zoom.onScroll(vertical)) ci.cancel();
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void emperor$mouseBinds(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window == MinecraftClient.getInstance().getWindow().getHandle()) ModuleManager.onBindEvent(Keys.mouse(button), action);
    }
}
