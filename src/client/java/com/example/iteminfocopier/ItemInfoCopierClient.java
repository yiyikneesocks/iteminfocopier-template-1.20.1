package com.example.iteminfocopier;

import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;
import com.example.iteminfocopier.config.IICConfig;
import com.example.iteminfocopier.mixin.client.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ItemInfoCopierClient implements ClientModInitializer {
    public static final IICConfig CONFIG = IICConfig.load();
    private boolean wasPressed = false;
    private static final Map<String, Integer> KEY_MAP = new HashMap<>();
    private static final Map<String, String> COPY_CACHE = new HashMap<>();

    static {
        // ... (key map initialization remains the same)
        // Alphabet keys
        for (char c = 'a'; c <= 'z'; c++) {
            KEY_MAP.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'a'));
        }
        // Number keys
        for (int i = 0; i <= 9; i++) {
            KEY_MAP.put(String.valueOf(i), GLFW.GLFW_KEY_0 + i);
        }
        // Function keys
        for (int i = 1; i <= 25; i++) {
            KEY_MAP.put("f" + i, GLFW.GLFW_KEY_F1 + i - 1);
        }
        // Special keys
        KEY_MAP.put("grave", GLFW.GLFW_KEY_GRAVE_ACCENT);
        KEY_MAP.put("minus", GLFW.GLFW_KEY_MINUS);
        KEY_MAP.put("equal", GLFW.GLFW_KEY_EQUAL);
        KEY_MAP.put("backspace", GLFW.GLFW_KEY_BACKSPACE);
        KEY_MAP.put("tab", GLFW.GLFW_KEY_TAB);
        KEY_MAP.put("left_bracket", GLFW.GLFW_KEY_LEFT_BRACKET);
        KEY_MAP.put("right_bracket", GLFW.GLFW_KEY_RIGHT_BRACKET);
        KEY_MAP.put("backslash", GLFW.GLFW_KEY_BACKSLASH);
        KEY_MAP.put("semicolon", GLFW.GLFW_KEY_SEMICOLON);
        KEY_MAP.put("apostrophe", GLFW.GLFW_KEY_APOSTROPHE);
        KEY_MAP.put("comma", GLFW.GLFW_KEY_COMMA);
        KEY_MAP.put("period", GLFW.GLFW_KEY_PERIOD);
        KEY_MAP.put("slash", GLFW.GLFW_KEY_SLASH);
        KEY_MAP.put("space", GLFW.GLFW_KEY_SPACE);
        KEY_MAP.put("escape", GLFW.GLFW_KEY_ESCAPE);
        KEY_MAP.put("enter", GLFW.GLFW_KEY_ENTER);
        KEY_MAP.put("delete", GLFW.GLFW_KEY_DELETE);
        KEY_MAP.put("home", GLFW.GLFW_KEY_HOME);
        KEY_MAP.put("end", GLFW.GLFW_KEY_END);
        KEY_MAP.put("page_up", GLFW.GLFW_KEY_PAGE_UP);
        KEY_MAP.put("page_down", GLFW.GLFW_KEY_PAGE_DOWN);
        KEY_MAP.put("up", GLFW.GLFW_KEY_UP);
        KEY_MAP.put("down", GLFW.GLFW_KEY_DOWN);
        KEY_MAP.put("left", GLFW.GLFW_KEY_LEFT);
        KEY_MAP.put("right", GLFW.GLFW_KEY_RIGHT);
    }

    private int getKeycode(String key) {
        return KEY_MAP.getOrDefault(key.toLowerCase(), -1);
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("iteminfocopy")
                        .then(argument("copyId", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String copyId = StringArgumentType.getString(context, "copyId");
                                    String value = COPY_CACHE.get(copyId);
                                    if (value != null) {
                                        MinecraftClient client = MinecraftClient.getInstance();
                                        client.keyboard.setClipboard(value);
                                        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));
                                    }
                                    return 1;
                                }))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            String hotkey = CONFIG.copyHotkey;
            if (hotkey == null || hotkey.trim().isEmpty()) {
                hotkey = "ctrl+c"; // Fallback to default
            }

            long handle = client.getWindow().getHandle();
            String[] parts = hotkey.toLowerCase().split("\\+");

            boolean ctrlRequired = false;
            boolean altRequired = false;
            boolean shiftRequired = false;
            int mainKeyCode = -1;

            for (String part : parts) {
                switch (part.trim()) {
                    case "ctrl" -> ctrlRequired = true;
                    case "alt" -> altRequired = true;
                    case "shift" -> shiftRequired = true;
                    default -> mainKeyCode = getKeycode(part.trim());
                }
            }
            if (mainKeyCode == -1) return; // Invalid hotkey in config

            boolean actualCtrlDown = Screen.hasControlDown();
            boolean actualAltDown = Screen.hasAltDown();
            boolean actualShiftDown = Screen.hasShiftDown();

            if (ctrlRequired != actualCtrlDown || altRequired != actualAltDown || shiftRequired != actualShiftDown) {
                wasPressed = false;
                return;
            }

            boolean mainKeyPressed = InputUtil.isKeyPressed(handle, mainKeyCode);

            if (mainKeyPressed) {
                if (!wasPressed) {
                    if (client.currentScreen == null || client.currentScreen instanceof HandledScreen) {
                        handleCopyLogic(client);
                    }
                    wasPressed = true;
                }
            } else {
                wasPressed = false;
            }
        });
    }

    private void handleCopyLogic(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        ItemStack stack = ItemStack.EMPTY;
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            if (!CONFIG.enableInventoryCopy) return;
            Slot slot = ((HandledScreenAccessor) handledScreen).getFocusedSlot();
            if (slot != null && slot.hasStack()) {
                stack = slot.getStack();
            }
        } else if (client.currentScreen == null) {
            if (!CONFIG.enableHandCopy) return;
            stack = player.getMainHandStack();
        }

        if (stack.isEmpty()) return;
        
        COPY_CACHE.clear();

        String lang = client.getLanguageManager().getLanguage();
        boolean isZh = lang != null && lang.startsWith("zh");

        String registryId = Registries.ITEM.getId(stack.getItem()).toString();
        String translationKey = stack.getItem().getTranslationKey();
        String displayName = stack.getName().getString();
        int rawId = Registries.ITEM.getRawId(stack.getItem());
        NbtCompound nbt = stack.getNbt();
        String nbtString = (nbt != null) ? nbt.toString() : "{}";

        StringBuilder allInfoBuilder = new StringBuilder();

        client.keyboard.setClipboard(registryId);
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));

        player.sendMessage(Text.literal("\n" + getI18n(isZh, "§b[物品信息复制] ", "§b[Item Info Copy] ") + "§f" + displayName), false);

        if (CONFIG.copyItemName) {
            player.sendMessage(createCopyableLineWithCommand(getI18n(isZh, "名称", "Name"), displayName, isZh), false);
            allInfoBuilder.append("Name: ").append(displayName).append("\n");
        }
        
        if (CONFIG.copyTranslationKey) {
            if (isZh || !"en_us".equals(lang)) {
                player.sendMessage(createCopyableLineWithCommand(getI18n(isZh, "翻译键", "Translation Key"), translationKey, isZh), false);
            }
            allInfoBuilder.append("Key: ").append(translationKey).append("\n");
        }

        if (CONFIG.copyItemId) {
            player.sendMessage(createCopyableLineWithCommand(getI18n(isZh, "命名空间 ID", "Namespace ID"), registryId, isZh), false);
            allInfoBuilder.append("ID: ").append(registryId).append("\n");
        }

        if (CONFIG.copyNumericId) {
            player.sendMessage(createCopyableLineWithCommand(getI18n(isZh, "数字 ID", "Numeric ID"), String.valueOf(rawId), isZh), false);
            allInfoBuilder.append("RawID: ").append(rawId).append("\n");
        }

        if (CONFIG.copyNbt && nbt != null) {
            player.sendMessage(createCopyableLineWithCommand("NBT", nbtString, isZh), false);
            allInfoBuilder.append("NBT: ").append(nbtString);
        }

        String allInfo = allInfoBuilder.toString();
        String copyAllId = UUID.randomUUID().toString();
        COPY_CACHE.put(copyAllId, allInfo);

        MutableText copyAllBtn = Text.literal(getI18n(isZh, "§6 >>> [点击复制全部信息] <<<", "§6 >>> [Click to Copy All] <<<"))
            .styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/iteminfocopy " + copyAllId))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(getI18n(isZh, "汇总并复制", "Copy summary"))))
                .withUnderline(true));
        
        player.sendMessage(copyAllBtn.append(Text.literal("\n")), false);
    }

    private MutableText createCopyableLineWithCommand(String label, String value, boolean isZh) {
        String copyId = UUID.randomUUID().toString();
        COPY_CACHE.put(copyId, value);

        String copyLabel = isZh ? "[复制]" : "[Copy]";
        String hoverHint = isZh ? "点击复制内容" : "Click to copy";
        String displayValue = value.length() > 50 ? value.substring(0, 47) + "..." : value;

        return Text.literal("§f " + label + ": §e" + displayValue + " ")
                .append(Text.literal("§b" + copyLabel)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/iteminfocopy " + copyId))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverHint)))
                        ));
    }

    private String getI18n(boolean isZh, String zh, String en) {
        return isZh ? zh : en;
    }
}