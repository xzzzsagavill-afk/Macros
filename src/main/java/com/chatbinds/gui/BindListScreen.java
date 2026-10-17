package com.chatbinds.gui;

import com.chatbinds.Bind;
import com.chatbinds.BindManager;
import com.chatbinds.KeyUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class BindListScreen extends Screen {
    private final Screen parent;
    private BindList list;
    private Button editButton;
    private Button deleteButton;
    private Button toggleButton;
    private Bind selectedBind;

    public BindListScreen(Screen parent) {
        super(Component.translatable("chatbinds.screen.title"));
        this.parent = parent;
    }

    void select(Bind bind) {
        this.selectedBind = bind;
    }

    @Override
    protected void init() {
        list = new BindList(minecraft, width, height - 32 - 60, 32, 38);
        addRenderableWidget(list);
        list.reload(selectedBind);

        int cx = width / 2;
        int y1 = height - 52;
        int y2 = height - 28;

        addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.add"),
                b -> minecraft.setScreen(new BindEditScreen(this, null)))
                .bounds(cx - 154, y1, 100, 20).build());
        editButton = addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.edit"),
                b -> editSelected())
                .bounds(cx - 50, y1, 100, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.delete"),
                b -> deleteSelected())
                .bounds(cx + 54, y1, 100, 20).build());
        toggleButton = addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.toggle"),
                b -> toggleSelected())
                .bounds(cx - 154, y2, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chatbinds.button.done"),
                b -> onClose())
                .bounds(cx + 4, y2, 150, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        boolean has = selectedBind != null;
        editButton.active = has;
        deleteButton.active = has;
        toggleButton.active = has;
    }

    private void editSelected() {
        if (selectedBind != null) {
            minecraft.setScreen(new BindEditScreen(this, selectedBind));
        }
    }

    private void toggleSelected() {
        if (selectedBind != null) {
            selectedBind.enabled = !selectedBind.enabled;
            BindManager.save();
        }
    }

    private void deleteSelected() {
        final Bind target = selectedBind;
        if (target == null) return;
        String shown = target.name.isBlank() ? target.message : target.name;
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                BindManager.all().remove(target);
                BindManager.save();
                selectedBind = null;
            }
            minecraft.setScreen(this);
        }, Component.translatable("chatbinds.delete.title"),
                Component.translatable("chatbinds.delete.text", shown)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        if (BindManager.all().isEmpty()) {
            g.drawCenteredString(font, Component.translatable("chatbinds.list.empty"),
                    width / 2, 32 + (height - 92) / 2 - 4, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ------------------------------------------------------------------

    private class BindList extends ObjectSelectionList<BindList.Row> {
        private Row lastClicked;
        private long lastClickTime;

        BindList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        void reload(Bind select) {
            double scroll = getScrollAmount();
            clearEntries();
            Row toSelect = null;
            for (Bind b : BindManager.all()) {
                Row r = new Row(b);
                addEntry(r);
                if (b == select) toSelect = r;
            }
            setSelected(toSelect);
            setScrollAmount(scroll);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        // без @Override: имя метода зависит от версии, но в 1.21.1 он именно такой
        protected int getScrollbarPosition() {
            return this.width / 2 + getRowWidth() / 2 + 6;
        }

        private class Row extends ObjectSelectionList.Entry<Row> {
            private final Bind bind;

            Row(Bind bind) {
                this.bind = bind;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                Font f = BindListScreen.this.font;
                String title = bind.name.isBlank()
                        ? (bind.message.isBlank() ? Component.translatable("chatbinds.list.unnamed").getString() : bind.message)
                        : bind.name;
                int nameColor = bind.enabled ? 0xFFFFFF : 0x808080;
                int keyColor = bind.enabled ? 0x55FF55 : 0x777777;

                Component status = Component.translatable(bind.enabled ? "chatbinds.list.on" : "chatbinds.list.off");
                int statusW = f.width(status);
                g.drawString(f, status, left + width - statusW - 6, top + 3, keyColor, false);

                g.drawString(f, f.plainSubstrByWidth(title, width - statusW - 20), left + 4, top + 3, nameColor, false);

                Component keyLabel = KeyUtil.label(bind.key, bind.mods);
                g.drawString(f, keyLabel, left + 4, top + 14, keyColor, false);
                if (bind.mode != Bind.Mode.ONCE) {
                    Component modeLabel = Component.translatable(
                            "chatbinds.mode." + bind.mode.name().toLowerCase(Locale.ROOT));
                    g.drawString(f, modeLabel, left + 4 + f.width(keyLabel) + 8, top + 14, 0xFFAA00, false);
                }

                g.drawString(f, f.plainSubstrByWidth(bind.message, width - 12), left + 4, top + 25, 0xA0A0A0, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                BindList.this.setSelected(this);
                selectedBind = bind;
                updateButtons();

                long now = Util.getMillis();
                if (lastClicked == this && now - lastClickTime < 300) {
                    editSelected();
                }
                lastClicked = this;
                lastClickTime = now;
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(bind.name.isBlank() ? bind.message : bind.name);
            }
        }
    }
}
