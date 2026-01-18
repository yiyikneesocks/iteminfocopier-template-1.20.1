package com.example.iteminfocopier.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class IICConfig {
    // 引入日志系统
    private static final Logger LOGGER = LoggerFactory.getLogger("ItemInfoCopier");
    
    public boolean enableHandCopy = true;
    public boolean enableInventoryCopy = true;

    // Add new options
    public boolean copyItemName = true;
    public boolean copyItemId = true;
    public boolean copyTranslationKey = true;
    public boolean copyNumericId = true;
    public boolean copyNbt = true;
    public String copyHotkey = "";

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("iteminfocopier.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static IICConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                IICConfig config = GSON.fromJson(reader, IICConfig.class);
                return config != null ? config : new IICConfig();
            } catch (IOException e) {
                // 使用 Logger 代替 printStackTrace
                LOGGER.error("Failed to load config!", e);
                return new IICConfig();
            }
        }
        return new IICConfig();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            // 使用 Logger 代替 printStackTrace
            LOGGER.error("Failed to save config!", e);
        }
    }
}
