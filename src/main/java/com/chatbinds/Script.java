package com.chatbinds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбор текста бинда на шаги. Шаги разделяются через ";;". */
public final class Script {
    public enum Type { CHAT, USE, ATTACK, SLOT, WAIT }

    public record Step(Type type, int arg, String text) {}

    private static final Pattern TOKEN = Pattern.compile("^\\[\\s*(\\p{L}+)\\s*(\\d{1,5})?\\s*\\]$");

    private Script() {}

    public static List<Step> parse(String src) {
        List<Step> steps = new ArrayList<>();
        if (src == null) return steps;

        for (String raw : src.split(";;")) {
            String part = raw.trim();
            if (part.isEmpty()) continue;

            Step step = null;
            Matcher m = TOKEN.matcher(part);
            if (m.matches()) {
                String name = m.group(1).toLowerCase(Locale.ROOT);
                int arg = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
                switch (name) {
                    case "пкм", "rmb", "use", "right" -> step = new Step(Type.USE, arg, "");
                    case "лкм", "lmb", "attack", "left" -> step = new Step(Type.ATTACK, arg, "");
                    case "слот", "slot" -> step = new Step(Type.SLOT, arg, "");
                    case "ждать", "пауза", "wait", "pause" -> step = new Step(Type.WAIT, arg, "");
                    default -> { }
                }
            }
            // Всё, что не похоже на токен, — обычное сообщение или команда
            if (step == null) step = new Step(Type.CHAT, 0, part);
            steps.add(step);
        }
        return steps;
    }
                }
