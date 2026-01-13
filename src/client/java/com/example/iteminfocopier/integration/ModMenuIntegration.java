package com.example.iteminfocopier.integration;

import com.example.iteminfocopier.ItemInfoCopierClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("title.iteminfocopier.config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.iteminfocopier.general"));

            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.enableHand"), ItemInfoCopierClient.CONFIG.enableHandCopy)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.enableHandCopy = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.enableInv"), ItemInfoCopierClient.CONFIG.enableInventoryCopy)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.enableInventoryCopy = newValue)
                    .build());

            builder.setSavingRunnable(() -> ItemInfoCopierClient.CONFIG.save());
            return builder.build();
        };
    }
}
