package com.example.iteminfocopier.integration;

import java.util.List;
import java.util.Optional;

import com.example.iteminfocopier.ItemInfoCopierClient;
import com.example.iteminfocopier.screen.KeyRecorderScreen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public final class HotkeyConfigScreen {

    public static Screen create(Screen parent) {
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

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.copyItemName"), ItemInfoCopierClient.CONFIG.copyItemName)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.copyItemName = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.copyItemId"), ItemInfoCopierClient.CONFIG.copyItemId)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.copyItemId = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.copyTranslationKey"), ItemInfoCopierClient.CONFIG.copyTranslationKey)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.copyTranslationKey = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.copyNumericId"), ItemInfoCopierClient.CONFIG.copyNumericId)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.copyNumericId = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.iteminfocopier.copyNbt"), ItemInfoCopierClient.CONFIG.copyNbt)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ItemInfoCopierClient.CONFIG.copyNbt = newValue)
                .build());

        general.addEntry(new HotkeyEntry(parent));

        builder.setSavingRunnable(ItemInfoCopierClient.CONFIG::save);
        return builder.build();
    }

    @SuppressWarnings("deprecation")  // TooltipListEntry constructor is deprecated but still functional
    private static class HotkeyEntry extends TooltipListEntry<Object> {
        private final TextWidget hotkeyText;
        private final ButtonWidget recordButton;
        private final ButtonWidget clearButton;

        protected HotkeyEntry(Screen parent) {
            super(Text.translatable("text.iteminfocopier.hotkey_entry_title"), () -> Optional.empty());

            hotkeyText = new TextWidget(0, 0, 200, 20, getHotkeyText(), MinecraftClient.getInstance().textRenderer);

            recordButton = ButtonWidget.builder(Text.translatable("button.iteminfocopier.record"), button -> {
                MinecraftClient.getInstance().setScreen(new KeyRecorderScreen(MinecraftClient.getInstance().currentScreen, new KeyRecorderScreen.KeyRecordCallback() {
                    @Override
                    public void onKeyRecorded(String keyCombo) {
                        ItemInfoCopierClient.CONFIG.copyHotkey = keyCombo;
                        hotkeyText.setMessage(getHotkeyText());
                    }

                    @Override
                    public void onCancel() {
                        // Re-set the screen to ensure focus is regained.
                        MinecraftClient.getInstance().setScreen(parent);
                    }
                }));
            }).dimensions(0, 0, 60, 20).build();

            clearButton = ButtonWidget.builder(Text.translatable("button.iteminfocopier.clear"), button -> {
                ItemInfoCopierClient.CONFIG.copyHotkey = "";
                hotkeyText.setMessage(getHotkeyText());
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 0.7F));
            }).dimensions(0, 0, 40, 20).build();
        }

        private Text getHotkeyText() {
            String key = ItemInfoCopierClient.CONFIG.copyHotkey;
            Text keyText = key.isEmpty() ? Text.translatable("text.iteminfocopier.hotkey_none") : Text.literal(key);
            return Text.translatable("text.iteminfocopier.hotkey_display", keyText);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float partialTick) {
            super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, partialTick);
            
            // Adjust positions based on layout
            hotkeyText.setX(x + 20);
            hotkeyText.setY(y + 2);
            
            clearButton.setX(x + entryWidth - clearButton.getWidth() - 4);
            clearButton.setY(y);

            recordButton.setX(clearButton.getX() - recordButton.getWidth() - 4);
            recordButton.setY(y);
            
            // Render widgets
            hotkeyText.render(context, mouseX, mouseY, partialTick);
            recordButton.render(context, mouseX, mouseY, partialTick);
            clearButton.render(context, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(hotkeyText, recordButton, clearButton);
        }

        @Override
        public List<? extends net.minecraft.client.gui.Selectable> narratables() {
            return List.of(hotkeyText, recordButton, clearButton);
        }

        // These methods are required but we don't need them for this entry.
        @Override
        public Object getValue() { return null; }
        @Override
        public Optional<Object> getDefaultValue() { return Optional.empty(); }
        @Override
        public void save() {}
    }
}