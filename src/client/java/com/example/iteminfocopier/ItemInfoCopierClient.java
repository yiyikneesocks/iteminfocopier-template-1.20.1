package com.example.iteminfocopier;

import org.lwjgl.glfw.GLFW;
import com.example.iteminfocopier.config.IICConfig;
import com.example.iteminfocopier.mixin.client.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
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

public class ItemInfoCopierClient implements ClientModInitializer {
    // 加载配置实例
    public static final IICConfig CONFIG = IICConfig.load();
    private boolean wasPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 检测 Ctrl + C
            boolean isCtrlDown = Screen.hasControlDown();
            boolean isCPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_C);

            if (isCtrlDown && isCPressed) {
                if (!wasPressed) {
                    // 仅在没有打开聊天框等输入框时触发
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
        boolean isFromInventory = false;

        // 1. 获取物品并检查配置开关
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            // 如果物品栏复制被禁用，直接返回
            if (!CONFIG.enableInventoryCopy) return;
            
            Slot slot = ((HandledScreenAccessor) handledScreen).getFocusedSlot();
            if (slot != null && slot.hasStack()) {
                stack = slot.getStack();
            }
            isFromInventory = true;
        } else if (client.currentScreen == null) {
            // 如果手持复制被禁用，直接返回
            if (!CONFIG.enableHandCopy) return;
            
            stack = player.getMainHandStack();
        }

        if (stack.isEmpty()) return;

        // 2. 语言适配检测
        String lang = client.getLanguageManager().getLanguage();
        boolean isZh = lang != null && lang.startsWith("zh");

        // 3. 准备数据
        String registryId = Registries.ITEM.getId(stack.getItem()).toString();
        String translationKey = stack.getItem().getTranslationKey();
        String displayName = stack.getName().getString();
        int rawId = Registries.ITEM.getRawId(stack.getItem());
        NbtCompound nbt = stack.getNbt();
        String nbtString = (nbt != null) ? nbt.toString() : "{}";

        // 汇总信息用于一键复制
        String allInfo = String.format("Name: %s\nID: %s\nKey: %s\nRawID: %d\nNBT: %s", 
                                        displayName, registryId, translationKey, rawId, nbtString);

        // 4. 执行默认操作：复制 ID 并播放声音
        client.keyboard.setClipboard(registryId);
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.2f));

        // 5. 构建并发送聊天框消息
        player.sendMessage(Text.literal("\n" + getI18n(isZh, "§b[物品信息复制] ", "§b[Item Info Copy] ") + "§f" + displayName), false);

        // 复制行：名称
        player.sendMessage(createCopyableLine(getI18n(isZh, "名称", "Name"), displayName, isZh), false);
        
        // 复制行：翻译键 (修复空指针：常量在前)
        if (isZh || !"en_us".equals(lang)) {
            player.sendMessage(createCopyableLine(getI18n(isZh, "翻译键", "Translation Key"), translationKey, isZh), false);
        }

        // 复制行：命名空间 ID
        player.sendMessage(createCopyableLine(getI18n(isZh, "命名空间 ID", "Namespace ID"), registryId, isZh), false);

        // 复制行：数字 ID
        player.sendMessage(createCopyableLine(getI18n(isZh, "数字 ID", "Numeric ID"), String.valueOf(rawId), isZh), false);

        // 复制行：NBT
        if (nbt != null) {
            player.sendMessage(createCopyableLine("NBT", nbtString, isZh), false);
        }

        // 一键复制按钮
        MutableText copyAllBtn = Text.literal(getI18n(isZh, "§6 >>> [点击复制全部信息] <<<", "§6 >>> [Click to Copy All] <<<"))
            .styled(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, allInfo))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(getI18n(isZh, "汇总并复制", "Copy summary"))))
                .withUnderline(true));
        
        player.sendMessage(copyAllBtn.append(Text.literal("\n")), false);
    }

    private MutableText createCopyableLine(String label, String value, boolean isZh) {
        String copyLabel = isZh ? "[复制]" : "[Copy]";
        String hoverHint = isZh ? "点击复制内容" : "Click to copy";
        String displayValue = value.length() > 50 ? value.substring(0, 47) + "..." : value;

        return Text.literal("§f " + label + ": §e" + displayValue + " ")
            .append(Text.literal("§b" + copyLabel)
                .styled(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hoverHint)))
                ));
    }

    private String getI18n(boolean isZh, String zh, String en) {
        return isZh ? zh : en;
    }
}
