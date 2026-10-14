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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BindEditScreen extends Screen {
    private final Screen parent;
    private final Bind editing; // null = создаём новый

    private String name;
    private String message;
    private String key;
    private int mods;
    private boolean enabled;

    private EditBox nameBox;
    private EditBox messageBox;
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
        } else {
            name = "";
            message = "";
            key = KeyUtil.NO_KEY;
            mods = 0;
            enabled = true;
        }
    }

    private int top() {
        return Math.max(8, this.height / 2 - 105);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int left = cx - 150;
        int top = top();

        nameBox = new EditBox(font, left, top + 34, 300, 20, Component.translatable("chatbinds.edit.name"));
        nameBox.setMaxLength(64);
        nameBox.setHint(Component.translatable("chatbinds.edit.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(name);
        nameBox.setResponder(s -> name = s);
        addRenderableWidget(nameBox);

        messageBox = new EditBox(font, left, top + 74, 300, 20, Component.translatable("chatbinds.edit.message"));
        messageBox.setMaxLength(256);
        messageBox.setHint(Component.translatable("chatbinds.edit.message_hint").withStyle(ChatFormatting.DARK_GRAY));
        messageBox.setValue(message);
        messageBox.setResponder(s -> {
            message = s;
            updateSaveButton();
        });
        addRenderableWidget(messageBox);

        keyButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            listening = true;
            pendingModifierKey = -1;
            updateKeyButton();
        }).bounds(left, top + 130, 300, 20).build());

        addRenderableWidget(CycleButton.onOffBuilder(enabled).create(left, top + 154, 300, 20,
                Component.translatable("chatbinds.edit.enabled"), (btn, value) -> enabled = value));

        saveButton = addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.save"),
                b -> save()).bounds(left, top + 184, 148, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.cancel"),
                b -> onClose()).bounds(left + 152, top + 184, 148, 20).build());

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

        g.drawCenteredString(font, title, width / 2, top + 6, 0xFFFFFF);
        g.drawString(font, Component.translatable("chatbinds.edit.name"), left, top + 22, 0xA0A0A0, false);
        g.drawString(font, Component.translatable("chatbinds.edit.message"), left, top + 62, 0xA0A0A0, false);
        g.drawWordWrap(font, Component.translatable("chatbinds.edit.help"), left, top + 98, 300, 0x707070);

        Component warning = conflictWarning();
        if (warning != null && !listening) {
            g.drawCenteredString(font, warning, width / 2, top + 116, 0xFFAA00);
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
