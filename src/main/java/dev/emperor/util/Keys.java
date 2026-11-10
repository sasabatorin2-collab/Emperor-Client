package dev.emperor.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Bind codes: -1 = none, 0..999 = GLFW keyboard key, 1000+n = mouse button n
 * (2 = middle, 3/4 = side buttons).
 */
public final class Keys {
    public static final int NONE = -1;
    private static final int MOUSE = 1000;

    private Keys() {
    }

    public static int mouse(int button) {
        return MOUSE + button;
    }

    public static boolean isMouse(int code) {
        return code >= MOUSE;
    }

    public static boolean isDown(int code) {
        if (code < 0) return false;
        long w = MinecraftClient.getInstance().getWindow().getHandle();
        if (isMouse(code)) return GLFW.glfwGetMouseButton(w, code - MOUSE) == GLFW.GLFW_PRESS;
        return InputUtil.isKeyPressed(w, code);
    }

    public static String name(int code) {
        if (code < 0) return "NONE";
        if (isMouse(code)) {
            return switch (code - MOUSE) {
                case 2 -> "MMB";
                case 3 -> "MOUSE 4";
                case 4 -> "MOUSE 5";
                default -> "MOUSE " + (code - MOUSE + 1);
            };
        }
        try {
            return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "?";
        }
    }

    /** Parses names like "r", "f6", "lshift", "mouse4" or "mmb"; returns NONE if unknown. */
    public static int parse(String s) {
        String k = s.toLowerCase(Locale.ROOT);
        switch (k) {
            case "mmb", "mouse3", "middle" -> {
                return mouse(2);
            }
            case "mouse4", "mb4" -> {
                return mouse(3);
            }
            case "mouse5", "mb5" -> {
                return mouse(4);
            }
            case "lshift" -> k = "left.shift";
            case "rshift" -> k = "right.shift";
            case "lctrl" -> k = "left.control";
            case "rctrl" -> k = "right.control";
            case "lalt" -> k = "left.alt";
            case "ralt" -> k = "right.alt";
            default -> {
            }
        }
        InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + k);
        return key.getCode() < 0 ? NONE : key.getCode();
    }
}
