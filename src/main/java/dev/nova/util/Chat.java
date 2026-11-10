package dev.nova.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class Chat {
    private Chat() {
    }

    public static void info(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal("§d[Nova] §f" + msg), false);
    }
}
