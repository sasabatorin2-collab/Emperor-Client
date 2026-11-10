package dev.emperor.mixin;

import dev.emperor.hud.Hud;
import dev.emperor.modules.Criticals;
import dev.emperor.modules.Crosshair;
import dev.emperor.modules.HitSounds;
import dev.emperor.util.Session;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void emperor$attack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof LivingEntity le) Hud.setTarget(le);
        Session.onAttack(target);
        Crosshair.onHit();
        HitSounds.onHit();
        Criticals.onAttack(target);
    }

    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void emperor$broke(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) Session.onBlockBroken();
    }
}
