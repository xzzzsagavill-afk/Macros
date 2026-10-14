package com.chatbinds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Очередь отправки. Несколько сообщений ("a ;; b ;; c") уходят с небольшой паузой,
 * чтобы сервер не кикнул за спам.
 */
public final class ChatSender {
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static final int DELAY_TICKS = 4;
    private static int cooldown = 0;

    private ChatSender() {}

    public static void run(Bind bind) {
        for (String part : bind.message.split(";;")) {
            String s = part.trim();
            if (!s.isEmpty()) QUEUE.add(s);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            QUEUE.clear();
            cooldown = 0;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        String msg = QUEUE.poll();
        if (msg == null) return;

        msg = msg.replace("{x}", String.valueOf(player.getBlockX()))
                .replace("{y}", String.valueOf(player.getBlockY()))
                .replace("{z}", String.valueOf(player.getBlockZ()))
                .replace("{player}", player.getGameProfile().getName());
        if (msg.length() > 256) msg = msg.substring(0, 256);

        if (msg.startsWith("/")) {
            String cmd = msg.substring(1);
            if (!cmd.isEmpty()) player.connection.sendCommand(cmd);
        } else {
            player.connection.sendChat(msg);
        }
        cooldown = DELAY_TICKS;
    }
}
