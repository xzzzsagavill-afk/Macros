package com.chatbinds;

/** Один бинд. Поля публичные, чтобы Gson мог их сериализовать. */
public class Bind {
    public String name = "";
    /** Имя клавиши в формате InputConstants, например "key.keyboard.k" или "key.mouse.left". */
    public String key = KeyUtil.NO_KEY;
    /** Битовая маска модификаторов GLFW (Shift/Ctrl/Alt). */
    public int mods = 0;
    public String message = "";
    public boolean enabled = true;

    public Bind() {}
}
