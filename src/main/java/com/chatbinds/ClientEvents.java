package com.chatbinds;

import com.chatbinds.gui.BindListScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static KeyMapping OPEN_MENU;

    private ClientEvents() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_MENU = new KeyMapping("key.chatbinds.open", GLFW.GLFW_KEY_K, "key.categories.chatbinds");
        event.register(OPEN_MENU);
    }

    public static void onKey(InputEvent.Key event) {
        String name = InputConstants.getKey(event.getKey(), event.getScanCode()).getName();
        trigger(name, KeyUtil.modBit(event.getKey()), event.getModifiers(), event.getAction());
    }

    public static void onMouse(InputEvent.MouseButton.Post event) {
        String name = InputConstants.Type.MOUSE.getOrCreate(event.getButton()).getName();
        trigger(name, 0, event.getModifiers(), event.getAction());
    }

    private static void trigger(String keyName, int ownBit, int modifiers, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;

        int mods = modifiers & KeyUtil.MASK & ~ownBit;
        for (Bind b : BindManager.all()) {
            if (b.enabled && b.mods == mods && b.key.equals(keyName)) {
                ChatSender.run(b);
            }
        }
    }

    public static void onTick(ClientTickEvent.Post event) {
        ChatSender.tick();
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_MENU != null && OPEN_MENU.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new BindListScreen(null));
            }
        }
    }
}
