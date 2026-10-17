package com.chatbinds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Хранит список биндов и сохраняет его в config/chatbinds.json. */
public final class BindManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatBinds");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Bind> BINDS = new ArrayList<>();

    private BindManager() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("chatbinds.json");
    }

    public static List<Bind> all() {
        return BINDS;
    }

    public static void load() {
        BINDS.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            List<Bind> loaded = GSON.fromJson(r, new TypeToken<List<Bind>>() {}.getType());
            if (loaded != null) {
                for (Bind b : loaded) {
                    if (b == null) continue;
                    if (b.name == null) b.name = "";
                    if (b.key == null) b.key = KeyUtil.NO_KEY;
                    if (b.message == null) b.message = "";
                    if (b.mode == null) b.mode = Bind.Mode.ONCE;
                    if (b.interval < 1) b.interval = 5;
                    BINDS.add(b);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось загрузить {}", f, e);
        }
    }

    public static void save() {
        Path f = file();
        try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
            GSON.toJson(BINDS, w);
        } catch (IOException e) {
            LOGGER.error("Не удалось сохранить {}", f, e);
        }
    }
}
