package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class SafeWalk extends Module {
    public SafeWalk() {
        super("SafeWalk", "Stops you from walking off edges, like sneaking.", Category.MOVEMENT);
    }

    public static boolean active(PlayerEntity p) {
        SafeWalk s = ModuleManager.get(SafeWalk.class);
        return s != null && s.isEnabled() && p == MinecraftClient.getInstance().player;
    }
}
