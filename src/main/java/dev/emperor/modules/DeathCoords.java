package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.util.Chat;

public class DeathCoords extends Module {
    private boolean reported;

    public DeathCoords() {
        super("DeathCoords", "Prints the coordinates where you died.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.getHealth() <= 0f || mc.player.isDead()) {
            if (reported) return;
            reported = true;
            String dim = mc.world == null ? "?" : mc.world.getRegistryKey().getValue().getPath();
            Chat.info("You died at " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " "
                    + mc.player.getBlockZ() + " (" + dim + ")");
        } else {
            reported = false;
        }
    }
}
