package com.chatbinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public final class KeyUtil {
    public static final String NO_KEY = "key.keyboard.unknown";
    public static final int MASK = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT;

    private KeyUtil() {}

    /** Если сама клавиша — модификатор, возвращает её бит, иначе 0. */
    public static int modBit(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> GLFW.GLFW_MOD_SUPER;
            default -> 0;
        };
    }

    public static InputConstants.Key keyOf(String name) {
        try {
            return InputConstants.getKey(name);
        } catch (IllegalArgumentException e) {
            return InputConstants.UNKNOWN;
        }
    }

    public static Component label(String key, int mods) {
        if (key == null || NO_KEY.equals(key)) {
            return Component.translatable("chatbinds.key.none");
        }
        MutableComponent c = Component.empty();
        if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) c.append("Ctrl + ");
        if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) c.append("Shift + ");
        if ((mods & GLFW.GLFW_MOD_ALT) != 0) c.append("Alt + ");
        return c.append(keyOf(key).getDisplayName());
    }
}
