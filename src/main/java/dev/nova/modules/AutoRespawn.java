package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Respawns instantly after death.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.currentScreen instanceof DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
