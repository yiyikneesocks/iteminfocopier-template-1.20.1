package com.example.iteminfocopier.screen;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class KeyRecorderScreen extends Screen {
    private final Screen parent;
    private final KeyRecordCallback callback;

    private boolean ctrl, alt, shift;
    private int mainKeyCode = -1;

    private TextWidget recordedKeyText;
    private ButtonWidget confirmButton;
    private boolean screenChangeHandled = false;

    public interface KeyRecordCallback {
        void onKeyRecorded(String keyCombo);
        void onCancel();
    }

    public KeyRecorderScreen(Screen parent, KeyRecordCallback callback) {
        super(Text.translatable("screen.iteminfocopier.record_keybind"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(new TextWidget(centerX - 100, centerY - 60, 200, 20,
                Text.translatable("text.iteminfocopier.press_any_key"), this.textRenderer));
        this.addDrawableChild(new TextWidget(centerX - 100, centerY - 40, 200, 20,
                Text.translatable("text.iteminfocopier.key_modifiers_hint"), this.textRenderer));

        recordedKeyText = new TextWidget(centerX - 100, centerY, 200, 20,
                Text.translatable("text.iteminfocopier.recorded_key_display", Text.translatable("text.iteminfocopier.hotkey_none")), this.textRenderer);
        this.addDrawableChild(recordedKeyText);

        confirmButton = ButtonWidget.builder(Text.translatable("button.iteminfocopier.confirm"), button -> {
            if (mainKeyCode != -1) {
                callback.onKeyRecorded(buildKeyComboString());
                screenChangeHandled = true;
                this.client.setScreen(parent);
            }
        }).dimensions(centerX - 100, centerY + 40, 90, 20).build();
        confirmButton.active = false; // Initially disabled
        this.addDrawableChild(confirmButton);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.iteminfocopier.cancel"), button -> {
            screenChangeHandled = true;
            this.client.setScreen(parent);
        }).dimensions(centerX + 10, centerY + 40, 90, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.iteminfocopier.clear"), button -> clearRecording())
                .dimensions(centerX - 100, centerY + 65, 200, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }

        ctrl = hasControl(this.client.getWindow().getHandle());
        alt = hasAlt(this.client.getWindow().getHandle());
        shift = hasShift(this.client.getWindow().getHandle());

        if (isModifierKey(keyCode)) {
            updateRecordedKeyText(true);
            return true;
        }

        mainKeyCode = keyCode;
        updateRecordedKeyText(false);
        confirmButton.active = true;
        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        return true;
    }

    private void clearRecording() {
        ctrl = alt = shift = false;
        mainKeyCode = -1;
        updateRecordedKeyText(false);
        confirmButton.active = false;
        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 0.7F));
    }

    private void updateRecordedKeyText(boolean modifiersOnly) {
        if (mainKeyCode == -1 && !modifiersOnly) {
            recordedKeyText.setMessage(Text.translatable("text.iteminfocopier.recorded_key_display", Text.translatable("text.iteminfocopier.hotkey_none")));
        } else {
            recordedKeyText.setMessage(Text.translatable("text.iteminfocopier.recorded_key_display", buildKeyNameText()));
        }
    }

    private Text buildKeyNameText() {
        MutableText keyNameText = Text.empty();
        if (ctrl) {
            keyNameText.append(Text.translatable("text.iteminfocopier.modifier.ctrl"));
        }
        if (alt) {
            keyNameText.append(Text.translatable("text.iteminfocopier.modifier.alt"));
        }
        if (shift) {
            keyNameText.append(Text.translatable("text.iteminfocopier.modifier.shift"));
        }
        if (mainKeyCode != -1) {
            keyNameText.append(InputUtil.fromKeyCode(mainKeyCode, -1).getLocalizedText());
        }
        return keyNameText;
    }

    private String buildKeyComboString() {
        if (mainKeyCode == -1) return "";
        
        StringBuilder result = new StringBuilder();
        if (ctrl) result.append("ctrl+");
        if (alt) result.append("alt+");
        if (shift) result.append("shift+");
        
        String keyName = InputUtil.fromKeyCode(mainKeyCode, -1).getTranslationKey();
        if(keyName.startsWith("key.keyboard.")){
            keyName = keyName.substring("key.keyboard.".length());
        }
        
        result.append(keyName.toLowerCase());
        return result.toString();
    }
    
    private static boolean isModifierKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
               keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
               keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    private static boolean hasControl(long window) {
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
    private static boolean hasAlt(long window) {
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }
    private static boolean hasShift(long window) {
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, partialTick);
    }

    @Override
    public void close() {
        screenChangeHandled = true;
        this.client.setScreen(this.parent);
    }

    @Override
    public void removed() {
        if (!screenChangeHandled) {
            callback.onCancel();
        }
        super.removed();
    }
}
