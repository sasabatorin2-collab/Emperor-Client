package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.NumberSetting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Adds a Reconnect button to the disconnect screen; with the module on it reconnects by itself. */
public class AutoReconnect extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay (s)", 5, 1, 60, 1));
    private static ServerInfo last;

    public AutoReconnect() {
        super("AutoReconnect", "Reconnects to the server automatically after a kick.", Category.MISC);
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.getCurrentServerEntry() != null) last = client.getCurrentServerEntry();
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof DisconnectedScreen) || last == null) return;
            AutoReconnect m = ModuleManager.get(AutoReconnect.class);
            int[] ticks = {m != null && m.isEnabled() ? m.delay.i() * 20 : -1};
            ButtonWidget button = ButtonWidget.builder(label(ticks[0]), b -> reconnect(client))
                    .dimensions(w / 2 - 100, h - 32, 200, 20).build();
            Screens.getButtons(screen).add(button);
            ScreenEvents.afterTick(screen).register(s -> {
                if (ticks[0] < 0) return;
                if (--ticks[0] <= 0) reconnect(client);
                else button.setMessage(label(ticks[0]));
            });
        });
    }

    private static Text label(int ticks) {
        return Text.literal(ticks < 0 ? "♛ Reconnect" : "♛ Reconnect (" + (ticks + 19) / 20 + "s)");
    }

    private static void reconnect(MinecraftClient client) {
        if (last == null) return;
        ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), client, ServerAddress.parse(last.address), last, false, null);
    }
}
