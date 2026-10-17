package com.chatbinds.gui;

import com.chatbinds.Bind;
import com.chatbinds.BindManager;
import com.chatbinds.KeyUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class BindEditScreen extends Screen {
    private final Screen parent;
    private final Bind editing; // null = создаём новый

    private String name;
    private String message;
    private String key;
    private int mods;
    private boolean enabled;
    private Bind.Mode mode;
    private int interval;

    private EditBox nameBox;
    private EditBox messageBox;
    private EditBox intervalBox;
    private Button keyButton;
    private Button saveButton;

    private boolean listening = false;
    private int pendingModifierKey = -1;

    public BindEditScreen(Screen parent, Bind editing) {
        super(Component.translatable(editing == null ? "chatbinds.edit.title_new" : "chatbinds.edit.title_edit"));
        this.parent = parent;
        this.editing = editing;
        if (editing != null) {
            name = editing.name;
            message = editing.message;
            key = editing.key;
            mods = editing.mods;
            enabled = editing.enabled;
            mode = editing.mode;
            interval = editing.interval;
        } else {
            name = "";
            message = "";
            key = KeyUtil.NO_KEY;
            mods = 0;
            enabled = true;
            mode = Bind.Mode.ONCE;
            interval = 5;
        }
    }

    private int top() {
        return Math.max(4, this.height / 2 - 131);
    }

    private static String token(String langKey) {
        return Component.translatable(langKey).getString();
    }

    private void insert(String piece) {
        String cur = messageBox.getValue();
        String sep;
        if (cur.isBlank()) sep = "";
        else if (cur.trim().endsWith(";;")) sep = " ";
        else sep = " ;; ";
        messageBox.setValue(cur + sep + piece);
        setFocused(messageBox);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int left = cx - 150;
        int top = top();

        // Название
        nameBox = new EditBox(font, left, top + 24, 300, 20, Component.translatable("chatbinds.edit.name"));
        nameBox.setMaxLength(64);
        nameBox.setHint(Component.translatable("chatbinds.edit.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(name);
        nameBox.setResponder(s -> name = s);
        addRenderableWidget(nameBox);

        // Сообщение / цепочка действий
        messageBox = new EditBox(font, left, top + 58, 300, 20, Component.translatable("chatbinds.edit.message"));
        messageBox.setMaxLength(512);
        messageBox.setHint(Component.translatable("chatbinds.edit.message_hint").withStyle(ChatFormatting.DARK_GRAY));
        messageBox.setValue(message);
        messageBox.setResponder(s -> {
            message = s;
            updateSaveButton();
        });
        addRenderableWidget(messageBox);

        // Кнопки быстрой вставки шагов
        addRenderableWidget(Button.builder(Component.translatable("chatbinds.edit.add_rmb"),
                        b -> insert("[" + token("chatbinds.token.rmb") + "]"))
                .bounds(left, top + 82, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("chatbinds.edit.add_rmb.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("chatbinds.edit.add_lmb"),
                        b -> insert("[" + token("chatbinds.token.lmb") + "]"))
                .bounds(left + 101, top + 82, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("chatbinds.edit.add_lmb.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("chatbinds.edit.add_wait"),
                        b -> insert("[" + token("chatbinds.token.wait") + " 10]"))
                .bounds(left + 202, top + 82, 98, 20)
                .tooltip(Tooltip.create(Component.translatable("chatbinds.edit.add_wait.tip"))).build());

        // Слоты хотбара 1-9
        for (int i = 1; i <= 9; i++) {
            final int n = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(n)),
                            b -> insert("[" + token("chatbinds.token.slot") + " " + n + "]"))
                    .bounds(left + (i - 1) * 33, top + 104, 30, 20)
                    .tooltip(Tooltip.create(Component.translatable("chatbinds.edit.add_slot.tip", n))).build());
        }

        // Задержка между шагами
        intervalBox = new EditBox(font, left + 200, top + 168, 100, 20, Component.translatable("chatbinds.edit.interval"));
        intervalBox.setMaxLength(4);
        intervalBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        intervalBox.setValue(String.valueOf(interval));
        intervalBox.setResponder(s -> {
            try {
                interval = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                interval = 0;
            }
        });
        addRenderableWidget(intervalBox);

        // Клавиша
        keyButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            listening = true;
            pendingModifierKey = -1;
            updateKeyButton();
        }).bounds(left, top + 192, 300, 20).build());

        // Режим и включение
        addRenderableWidget(CycleButton.builder((Bind.Mode m) ->
                        Component.translatable("chatbinds.mode." + m.name().toLowerCase(Locale.ROOT)))
                .withValues(Bind.Mode.values())
                .withInitialValue(mode)
                .create(left, top + 216, 148, 20, Component.translatable("chatbinds.edit.mode"),
                        (btn, value) -> mode = value));
        addRenderableWidget(CycleButton.onOffBuilder(enabled).create(left + 152, top + 216, 148, 20,
                Component.translatable("chatbinds.edit.enabled"), (btn, value) -> enabled = value));

        // Сохранить / отмена
        saveButton = addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.save"),
                b -> save()).bounds(left, top + 242, 148, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.cancel"),
                b -> onClose()).bounds(left + 152, top + 242, 148, 20).build());

        setInitialFocus(editing == null ? nameBox : messageBox);
        updateKeyButton();
        updateSaveButton();
    }

    private void updateKeyButton() {
        Component shown;
        if (listening) {
            shown = Component.translatable("chatbinds.edit.press_key").withStyle(ChatFormatting.YELLOW);
        } else {
            shown = KeyUtil.label(key, mods);
        }
        keyButton.setMessage(listening ? shown : Component.translatable("chatbinds.edit.key", shown));
    }

    private void updateSaveButton() {
        saveButton.active = !message.isBlank() && !KeyUtil.NO_KEY.equals(key);
    }

    private void setKey(String newKey, int newMods) {
        key = newKey;
        mods = newMods & KeyUtil.MASK;
        listening = false;
        pendingModifierKey = -1;
        updateKeyButton();
        updateSaveButton();
    }

    private void save() {
        Bind b = editing != null ? editing : new Bind();
        b.name = name.trim();
        b.message = message.trim();
        b.key = key;
        b.mods = mods;
        b.enabled = enabled;
        b.mode = mode;
        b.interval = Math.max(1, Math.min(1200, interval));
        if (editing == null) BindManager.all().add(b);
        BindManager.save();
        if (parent instanceof BindListScreen list) list.select(b);
        minecraft.setScreen(parent);
    }

    // ---------- Захват клавиш ----------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                pendingModifierKey = -1;
                updateKeyButton();
                return true;
            }
            if (KeyUtil.modBit(keyCode) != 0) {
                // Возможно, это просто модификатор (Ctrl+K) — ждём следующее нажатие/отпускание
                pendingModifierKey = keyCode;
                return true;
            }
            setKey(InputConstants.getKey(keyCode, scanCode).getName(), modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (listening && pendingModifierKey == keyCode) {
            // Модификатор нажали и отпустили без других клавиш — привязываем его самого
            setKey(InputConstants.getKey(keyCode, scanCode).getName(), 0);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (listening) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listening) {
            int m = 0;
            if (hasShiftDown()) m |= GLFW.GLFW_MOD_SHIFT;
            if (hasControlDown()) m |= GLFW.GLFW_MOD_CONTROL;
            if (hasAltDown()) m |= GLFW.GLFW_MOD_ALT;
            setKey(InputConstants.Type.MOUSE.getOrCreate(button).getName(), m);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------- Отрисовка ----------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = width / 2 - 150;
        int top = top();

        g.drawCenteredString(font, title, width / 2, top + 4, 0xFFFFFF);
        g.drawString(font, Component.translatable("chatbinds.edit.name"), left, top + 14, 0xA0A0A0, false);
        g.drawString(font, Component.translatable("chatbinds.edit.message"), left, top + 48, 0xA0A0A0, false);
        g.drawWordWrap(font, Component.translatable("chatbinds.edit.help"), left, top + 128, 300, 0x707070);
        g.drawString(font, Component.translatable("chatbinds.edit.interval"), left, top + 174, 0xA0A0A0, false);

        Component warning = conflictWarning();
        if (warning != null && !listening) {
            g.drawCenteredString(font, warning, width / 2, top + 157, 0xFFAA00);
        }
    }

    private Component conflictWarning() {
        if (KeyUtil.NO_KEY.equals(key)) return null;
        for (Bind other : BindManager.all()) {
            if (other != editing && other.key.equals(key) && other.mods == mods) {
                String n = other.name.isBlank() ? other.message : other.name;
                return Component.translatable("chatbinds.edit.conflict_bind", n);
            }
        }
        if (mods == 0) {
            for (KeyMapping km : minecraft.options.keyMappings) {
                if (!km.isUnbound() && km.saveString().equals(key)) {
                    return Component.translatable("chatbinds.edit.conflict_vanilla",
                            Component.translatable(km.getName()));
                }
            }
        }
        return null;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
