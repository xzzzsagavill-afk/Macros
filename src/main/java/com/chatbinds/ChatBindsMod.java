package com.chatbinds;

import com.chatbinds.gui.BindListScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ChatBindsMod.MODID, dist = Dist.CLIENT)
public class ChatBindsMod {
    public static final String MODID = "chatbinds";

    public ChatBindsMod(IEventBus modBus, ModContainer container) {
        BindManager.load();

        modBus.addListener(ClientEvents::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onKey);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onMouse);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onTick);

        // Кнопка "Настроить" в списке модов открывает меню биндов
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (a, parent) -> new BindListScreen(parent));
    }
}
