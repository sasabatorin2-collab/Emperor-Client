package dev.nova.mixin;

import dev.nova.modules.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void nova$freecam(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                              float tickDelta, CallbackInfo ci) {
        Freecam f = Freecam.active();
        if (f == null) return;
        f.frameUpdate();
        setRotation(f.getYaw(), f.getPitch());
        setPos(f.getX(), f.getY(), f.getZ());
        // Makes the game draw your own body (and hide the first-person hand).
        this.thirdPerson = true;
    }
}
