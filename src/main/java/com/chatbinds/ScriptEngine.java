package com.chatbinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Выполняет цепочки шагов: чат, клики мыши, смена слота, паузы. */
public final class ScriptEngine {
    private static final List<Runner> RUNNERS = new ArrayList<>();

    private ScriptEngine() {}

    /** Вызывается при нажатии клавиши бинда. */
    public static void trigger(Bind bind) {
        Runner running = null;
        for (Runner r : RUNNERS) {
            if (r.bind == bind) {
                running = r;
                break;
            }
        }
        if (running != null) {
            if (bind.mode == Bind.Mode.TOGGLE) {
                running.release();
                RUNNERS.remove(running);
                notice(bind, false);
            }
            return; // ONCE / HOLD: уже выполняется
        }

        List<Script.Step> steps = Script.parse(bind.message);
        if (steps.isEmpty()) return;
        RUNNERS.add(new Runner(bind, steps));
        if (bind.mode == Bind.Mode.TOGGLE) notice(bind, true);
    }

    /** Вызывается каждый клиентский тик (до обработки клавиш игрой). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            stopAll();
            return;
        }
        Iterator<Runner> it = RUNNERS.iterator();
        while (it.hasNext()) {
            Runner r = it.next();
            r.tick(mc);
            if (r.finished) {
                r.release();
                it.remove();
            }
        }
    }

    private static void stopAll() {
        for (Runner r : RUNNERS) r.release();
        RUNNERS.clear();
    }

    private static void notice(Bind bind, boolean on) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        String shown = bind.name.isBlank() ? bind.message : bind.name;
        p.displayClientMessage(Component.translatable(on ? "chatbinds.toggle.on" : "chatbinds.toggle.off", shown), true);
    }

    private static boolean physicallyDown(Minecraft mc, String keyName) {
        long window = mc.getWindow().getWindow();
        InputConstants.Key k = KeyUtil.keyOf(keyName);
        return switch (k.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, k.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    private static void send(LocalPlayer player, String raw) {
        String msg = raw.replace("{x}", String.valueOf(player.getBlockX()))
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
    }

    // ------------------------------------------------------------------

    private static final class Runner {
        final Bind bind;
        final List<Script.Step> steps;
        int index = 0;
        int wait = 0;
        int holdLeft = 0;
        KeyMapping heldKey = null;
        boolean finished = false;

        Runner(Bind bind, List<Script.Step> steps) {
            this.bind = bind;
            this.steps = steps;
        }

        void release() {
            if (heldKey != null) {
                heldKey.setDown(false);
                heldKey = null;
            }
            holdLeft = 0;
        }

        void tick(Minecraft mc) {
            LocalPlayer player = mc.player;
            if (player == null || !bind.enabled || !BindManager.all().contains(bind)) {
                finished = true;
                return;
            }
            if (bind.mode == Bind.Mode.HOLD && !physicallyDown(mc, bind.key)) {
                finished = true;
                return;
            }
            if (mc.screen != null) return; // пауза, пока открыто окно

            if (holdLeft > 0) {
                heldKey.setDown(true);
                if (--holdLeft == 0) release();
            }
            if (wait > 0) {
                wait--;
                return;
            }

            if (index >= steps.size()) {
                if (bind.mode == Bind.Mode.ONCE) {
                    finished = true;
                    return;
                }
                index = 0;
            }

            Script.Step s = steps.get(index++);
            int interval = Math.max(1, bind.interval);
            int duration = interval;

            switch (s.type()) {
                case CHAT -> send(player, s.text());
                case SLOT -> player.getInventory().selected = Math.max(1, Math.min(9, s.arg())) - 1;
                case WAIT -> {
                    if (s.arg() > 0) duration = s.arg();
                }
                case USE, ATTACK -> {
                    KeyMapping key = s.type() == Script.Type.USE ? mc.options.keyUse : mc.options.keyAttack;
                    if (s.arg() > 0) {
                        release();
                        heldKey = key;
                        holdLeft = s.arg();
                        key.setDown(true);
                        duration = Math.max(interval, s.arg());
                    } else {
                        KeyMapping.click(key.getKey());
                    }
                }
            }
            wait = duration - 1;
        }
    }
  }
