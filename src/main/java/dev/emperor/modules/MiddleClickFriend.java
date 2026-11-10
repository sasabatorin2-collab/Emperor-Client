package dev.emperor.modules;

import dev.emperor.friend.Friends;
import dev.emperor.hud.Notifications;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public class MiddleClickFriend extends Module {
    private boolean wasDown;

    public MiddleClickFriend() {
        super("MiddleClickFriend", "Middle-click a player to add or remove a friend.", Category.MISC);
    }

    @Override
    public void onTick() {
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (down && !wasDown && mc.currentScreen == null
                && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity p) {
            String name = p.getGameProfile().getName();
            boolean added = Friends.toggle(name);
            Notifications.push(name, added ? "Added to friends" : "Removed from friends",
                    added ? Notifications.Kind.ENABLED : Notifications.Kind.DISABLED);
        }
        wasDown = down;
    }
}
