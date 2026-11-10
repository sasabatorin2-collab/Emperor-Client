package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.util.Chat;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Notifier extends Module {
    private final Set<UUID> seen = new HashSet<>();

    public Notifier() {
        super("Notifier", "Chat alert when a player enters your render distance.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        Set<UUID> now = new HashSet<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            now.add(p.getUuid());
            if (!seen.contains(p.getUuid())) {
                Chat.info(p.getName().getString() + " appeared at " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ());
            }
        }
        seen.clear();
        seen.addAll(now);
    }

    @Override
    protected void onDisable() {
        seen.clear();
    }
}
