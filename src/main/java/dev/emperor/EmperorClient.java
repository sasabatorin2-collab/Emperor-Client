package dev.emperor;

import dev.emperor.gui.ClickGuiScreen;
import dev.emperor.hud.Hud;
import dev.emperor.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class EmperorClient implements ClientModInitializer {
    public static final String NAME = "Emperor";
    public static final String VERSION = "1.0.0";

    private static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.emperor.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.emperor"));

        ClientTickEvents.START_CLIENT_TICK.register(ModuleManager::preTick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
            }
            ModuleManager.tick(client);
        });

        HudRenderCallback.EVENT.register(Hud::render);
        WorldRenderEvents.LAST.register(ModuleManager::render3d);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModuleManager.save());
    }
}
