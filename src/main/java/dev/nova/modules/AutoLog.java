package dev.nova.modules;

import dev.nova.module.Category;
import dev.nova.module.Module;
import dev.nova.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/** Disconnects when health is low or another player comes close. */
public class AutoLog extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", 6, 1, 20, 1));
    private final NumberSetting playerRange = add(new NumberSetting("Player range (0 = off)", 0, 0, 64, 4));

    public AutoLog() {
        super("AutoLog", "Leaves the server on low health or when a player gets close.", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        if (mc.player.getHealth() <= health.f()) {
            logout("health " + (int) mc.player.getHealth());
            return;
        }
        if (playerRange.i() > 0) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                if (mc.player.distanceTo(p) <= playerRange.f()) {
                    logout(p.getName().getString() + " is close");
                    return;
                }
            }
        }
    }

    private void logout(String why) {
        setEnabled(false);
        mc.getNetworkHandler().getConnection().disconnect(Text.literal("[Nova] AutoLog: " + why));
    }
}
