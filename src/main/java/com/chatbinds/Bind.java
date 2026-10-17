package com.chatbinds;

/** Один бинд. Поля публичные, чтобы Gson мог их сериализовать. */
public class Bind {
    public enum Mode { ONCE, TOGGLE, HOLD }

    public String name = "";
    /** Имя клавиши в формате InputConstants, например "key.keyboard.k" или "key.mouse.left". */
    public String key = KeyUtil.NO_KEY;
    /** Битовая маска модификаторов GLFW (Shift/Ctrl/Alt). */
    public int mods = 0;
    /** Сообщение, команда или цепочка шагов через ";;". */
    public String message = "";
    public boolean enabled = true;
    /** Режим выполнения цепочки. */
    public Mode mode = Mode.ONCE;
    /** Задержка между шагами в тиках (20 тиков = 1 секунда). */
    public int interval = 5;

    public Bind() {}
}
