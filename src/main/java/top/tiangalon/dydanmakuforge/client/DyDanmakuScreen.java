package top.tiangalon.dydanmakuforge.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import top.tiangalon.dydanmakuforge.config.ConfigManager;
import top.tiangalon.dydanmakuforge.net.WebSocketClientNetty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static top.tiangalon.dydanmakuforge.DyDanmakuForge.LOGGER;
import static top.tiangalon.dydanmakuforge.client.DyDanmakuForgeClient.OPEN_GUI;

public final class DyDanmakuScreen extends Screen {
    private static final String EXAMPLE_LIVE_ID = "594357732923";
    private static final ResourceLocation AVATAR_ID = ResourceLocation.parse("dydanmaku:avatar");
    private static final ResourceLocation LOADING_ID =
            ResourceLocation.fromNamespaceAndPath("dydanmaku", "textures/gui/sprite/loading.png");
    private static volatile Path avatarPath;

    private final DyDanmakuController controller;
    private EditBox liveIdInput;
    private Button connectButton;
    private DynamicTexture avatarTexture;
    private boolean avatarRegistered;
    private int scrollOffset;
    private int currentFrame;
    private long lastFrameTime;

    public DyDanmakuScreen(DyDanmakuController controller) {
        super(Component.literal("DyDanmaku"));
        this.controller = controller;
    }

    public static void setAvatar(Path path) {
        avatarPath = path;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof DyDanmakuScreen screen) {
                screen.loadAvatar(path);
            }
        });
    }

    @Override
    protected void init() {
        String initialLiveId = liveIdInput == null ? EXAMPLE_LIVE_ID : liveIdInput.getValue();
        liveIdInput = new EditBox(font, 40, 70, 120, 20, Component.literal("输入直播间 ID"));
        liveIdInput.setMaxLength(32);
        liveIdInput.setValue(initialLiveId);
        addRenderableWidget(liveIdInput);

        connectButton = Button.builder(Component.literal("连接"), button -> {
            if (controller.getWebsocket().isConnected()) {
                controller.disconnect();
            } else {
                controller.connect(liveIdInput.getValue());
            }
        }).bounds(170, 70, 70, 20).build();
        addRenderableWidget(connectButton);

        Path pendingAvatar = avatarPath;
        if (pendingAvatar != null && Files.isRegularFile(pendingAvatar)) {
            loadAvatar(pendingAvatar);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        WebSocketClientNetty websocket = controller.getWebsocket();
        boolean connected = websocket.isConnected();

        graphics.drawString(font, "DyDanmaku Forge", 40, 25, 0xFFFFFFFF, true);
        graphics.drawString(
                font,
                controller.isConnecting() ? "直播间状态：连接中" : connected ? "直播间状态：已连接" : "直播间状态：未连接",
                40,
                45,
                connected ? 0xFF55FF55 : 0xFFFFFFFF,
                true
        );

        liveIdInput.setEditable(!connected && !controller.isConnecting());
        connectButton.setMessage(Component.literal(
                controller.isConnecting() ? "连接中" : connected ? "断开" : "连接"));

        if (!connected || websocket.params == null) {
            graphics.drawString(font, "按 F7 可关闭此界面", 40, 100, 0xFFAAAAAA, false);
            return;
        }

        liveIdInput.setValue(websocket.params.getOrDefault("live_id", ""));
        String sessionId = ConfigManager.getSessionId(ClientRuntime.getConfigDir().toString());
        if (sessionId == null || sessionId.isBlank()) {
            graphics.drawString(font, "未设置抖音 sessionid，可能无法收到礼物信息", 250, 75, 0xFFFF5555, true);
        }

        drawAvatar(graphics);
        graphics.drawString(font, "主播：" + websocket.params.getOrDefault("nickname", ""), 100, 100, 0xFFFFFFFF, true);
        graphics.drawString(font, "标题：" + websocket.params.getOrDefault("live_title", ""), 100, 115, 0xFFFFFFFF, true);
        drawDanmaku(graphics, websocket.getDanmakuText());
    }

    private void drawAvatar(GuiGraphics graphics) {
        if (avatarRegistered) {
            graphics.blit(AVATAR_ID, 40, 100, 0, 0, 50, 50, 50, 50);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > 33) {
            currentFrame = (currentFrame + 1) % 30;
            lastFrameTime = now;
        }
        graphics.blit(LOADING_ID, 40, 100, 0, currentFrame * 50, 50, 50, 50, 1500);
    }

    private void drawDanmaku(GuiGraphics graphics, String text) {
        int top = 160;
        int bottom = height - 15;
        int maxLines = Math.max(1, (bottom - top) / font.lineHeight);
        List<String> lines = text.isEmpty() ? List.of() : Arrays.asList(text.split("\\R"));
        int maxOffset = Math.max(0, lines.size() - maxLines);
        scrollOffset = Math.min(scrollOffset, maxOffset);
        int end = Math.max(0, lines.size() - scrollOffset);
        int start = Math.max(0, end - maxLines);

        graphics.fill(40, top - 5, width - 40, bottom + 5, 0x80000000);
        graphics.enableScissor(40, top - 5, width - 40, bottom + 5);
        for (int index = start; index < end; index++) {
            int y = top + (index - start) * font.lineHeight;
            graphics.drawString(font, lines.get(index), 45, y, 0xFFFFFFFF, false);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= 150) {
            scrollOffset = Math.max(0, scrollOffset + (verticalAmount > 0 ? 2 : -2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (OPEN_GUI.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && liveIdInput.isFocused()) {
            controller.connect(liveIdInput.getValue());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (avatarTexture != null) {
            avatarTexture.close();
            avatarTexture = null;
            avatarRegistered = false;
        }
        super.removed();
    }

    private void loadAvatar(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            if (avatarTexture != null) {
                avatarTexture.close();
            }
            avatarTexture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register("dydanmaku:avatar", avatarTexture);
            avatarRegistered = true;
        } catch (IOException exception) {
            LOGGER.warn("[DyDanmaku]加载主播头像失败：{}", path, exception);
        }
    }
}
