package top.tiangalon.dydanmakuforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import top.tiangalon.dydanmakuforge.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

/** 游戏内可编辑的 DyDanmaku 配置界面。 */
public final class DyDanmakuSettingsScreen extends Screen {
    private static final int MAX_VISIBLE_KEYWORDS = 6;
    private static final List<String> FILTER_MODES = List.of("disabled", "blacklist", "whitelist");

    private final Screen parent;
    private final String configDir;
    private final List<String> keywords = new ArrayList<>();
    private final List<Button> keywordButtons = new ArrayList<>();
    private boolean connectionPage = true;
    private boolean officialApiEnabled;
    private String officialEndpoint;
    private String officialKey;
    private String directSessionId;
    private EditBox endpointInput;
    private EditBox keyInput;
    private EditBox sessionIdInput;
    private EditBox keywordInput;
    private Button officialApiButton;
    private Button modeButton;
    private Button editButton;
    private Button deleteButton;
    private String filterMode;
    private String statusMessage = "";
    private int selectedKeyword = -1;
    private int keywordScroll;
    private int visibleKeywordRows;

    public DyDanmakuSettingsScreen(Screen parent) {
        super(Component.literal("DyDanmaku 设置"));
        this.parent = parent;
        this.configDir = ClientRuntime.getConfigDir().toString();
        ConfigManager.OfficialApiConfig official = ConfigManager.getOfficialApiConfig(configDir);
        this.officialApiEnabled = official.enabled;
        this.officialEndpoint = official.endpoint;
        this.officialKey = official.key;
        String sessionId = ConfigManager.getSessionId(configDir);
        this.directSessionId = sessionId == null ? "" : sessionId;
        ConfigManager.FilterConfig filter = ConfigManager.getFilterConfig(configDir);
        this.filterMode = filter.mode;
        this.keywords.addAll(filter.keywords);
    }

    @Override
    protected void init() {
        rememberInputs();
        keywordButtons.clear();
        int left = Math.max(20, width / 2 - 190);
        int contentWidth = Math.min(380, width - 40);

        addRenderableWidget(Button.builder(Component.literal("接入设置"), button -> switchPage(true))
                .bounds(left, 28, contentWidth / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("弹幕过滤"), button -> switchPage(false))
                .bounds(left + contentWidth / 2 + 2, 28, contentWidth / 2 - 2, 20).build());

        if (connectionPage) {
            initConnectionPage(left, contentWidth);
        } else {
            initFilterPage(left, contentWidth);
        }
        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
                .bounds(left + contentWidth - 80, height - 30, 80, 20).build());
    }

    private void initConnectionPage(int left, int contentWidth) {
        officialApiButton = Button.builder(officialApiText(), button -> {
            officialApiEnabled = !officialApiEnabled;
            officialApiButton.setMessage(officialApiText());
            updateConnectionFields();
        }).bounds(left, 56, contentWidth, 20).build();
        addRenderableWidget(officialApiButton);

        endpointInput = new EditBox(font, left, 88, contentWidth, 20,
                Component.literal("WSS bridge 接入点"));
        endpointInput.setMaxLength(2048);
        endpointInput.setValue(officialEndpoint);
        addRenderableWidget(endpointInput);

        keyInput = new EditBox(font, left, 120, contentWidth, 20, Component.literal("Bridge key"));
        keyInput.setMaxLength(4096);
        keyInput.setValue(officialKey);
        addRenderableWidget(keyInput);

        sessionIdInput = new EditBox(font, left, 152, contentWidth, 20, Component.literal("DySessionId"));
        sessionIdInput.setMaxLength(4096);
        sessionIdInput.setValue(directSessionId);
        addRenderableWidget(sessionIdInput);

        addRenderableWidget(Button.builder(Component.literal("保存接入设置"), button -> saveConnectionSettings())
                .bounds(left, 180, contentWidth, 20).build());
        updateConnectionFields();
    }

    private void initFilterPage(int left, int contentWidth) {
        modeButton = Button.builder(filterModeText(), button -> cycleFilterMode())
                .bounds(left, 58, contentWidth, 20).build();
        addRenderableWidget(modeButton);

        keywordInput = new EditBox(font, left, 102, contentWidth - 225, 20, Component.literal("过滤关键词"));
        keywordInput.setMaxLength(256);
        addRenderableWidget(keywordInput);
        addRenderableWidget(Button.builder(Component.literal("新增"), button -> addKeyword())
                .bounds(left + contentWidth - 220, 102, 70, 20).build());
        editButton = Button.builder(Component.literal("修改"), button -> editKeyword())
                .bounds(left + contentWidth - 145, 102, 70, 20).build();
        addRenderableWidget(editButton);
        deleteButton = Button.builder(Component.literal("删除"), button -> deleteKeyword())
                .bounds(left + contentWidth - 70, 102, 70, 20).build();
        addRenderableWidget(deleteButton);

        visibleKeywordRows = Math.max(1, Math.min(MAX_VISIBLE_KEYWORDS, (height - 175) / 22));
        for (int row = 0; row < visibleKeywordRows; row++) {
            final int rowIndex = row;
            Button keywordButton = Button.builder(Component.empty(),
                            button -> selectKeyword(keywordScroll + rowIndex))
                    .bounds(left, 130 + row * 22, contentWidth, 20).build();
            keywordButtons.add(keywordButton);
            addRenderableWidget(keywordButton);
        }
        refreshKeywordButtons();
    }

    private void switchPage(boolean connectionPage) {
        if (this.connectionPage == connectionPage) return;
        rememberInputs();
        this.connectionPage = connectionPage;
        clearWidgets();
        init();
    }

    private void rememberInputs() {
        if (endpointInput != null) officialEndpoint = endpointInput.getValue();
        if (keyInput != null) officialKey = keyInput.getValue();
        if (sessionIdInput != null) directSessionId = sessionIdInput.getValue();
    }

    private void updateConnectionFields() {
        if (endpointInput != null) endpointInput.setEditable(officialApiEnabled);
        if (keyInput != null) keyInput.setEditable(officialApiEnabled);
        if (sessionIdInput != null) sessionIdInput.setEditable(!officialApiEnabled);
    }

    private void saveConnectionSettings() {
        rememberInputs();
        boolean officialSaved = ConfigManager.setOfficialApiConfig(
                configDir, officialApiEnabled, officialEndpoint, officialKey);
        boolean sessionSaved = ConfigManager.setSessionId(configDir, directSessionId);
        statusMessage = officialSaved && sessionSaved
                ? "接入设置已保存，下次连接时生效"
                : "接入设置保存失败，请查看日志";
    }

    private Component officialApiText() {
        return Component.literal("抖音官方 OpenAPI bridge：" + (officialApiEnabled ? "开启（默认）" : "关闭（原有直连）"));
    }

    private void cycleFilterMode() {
        int index = FILTER_MODES.indexOf(filterMode);
        filterMode = FILTER_MODES.get((index + 1) % FILTER_MODES.size());
        modeButton.setMessage(filterModeText());
        saveFilter("过滤模式已保存");
    }

    private void addKeyword() {
        String keyword = keywordInput.getValue().trim();
        if (keyword.isEmpty()) { statusMessage = "请输入要新增的关键词"; return; }
        if (keywords.contains(keyword)) { statusMessage = "该关键词已存在"; return; }
        keywords.add(keyword);
        selectedKeyword = keywords.size() - 1;
        ensureSelectedKeywordVisible();
        keywordInput.setValue("");
        saveFilter("关键词已新增");
        refreshKeywordButtons();
    }

    private void editKeyword() {
        if (selectedKeyword < 0 || selectedKeyword >= keywords.size()) {
            statusMessage = "请先在下方选择一个关键词"; return;
        }
        String keyword = keywordInput.getValue().trim();
        if (keyword.isEmpty()) { statusMessage = "请输入修改后的关键词"; return; }
        int duplicateIndex = keywords.indexOf(keyword);
        if (duplicateIndex >= 0 && duplicateIndex != selectedKeyword) {
            statusMessage = "该关键词已存在"; return;
        }
        keywords.set(selectedKeyword, keyword);
        keywordInput.setValue("");
        saveFilter("关键词已修改");
        refreshKeywordButtons();
    }

    private void deleteKeyword() {
        if (selectedKeyword < 0 || selectedKeyword >= keywords.size()) {
            statusMessage = "请先在下方选择一个关键词"; return;
        }
        keywords.remove(selectedKeyword);
        selectedKeyword = keywords.isEmpty() ? -1 : Math.min(selectedKeyword, keywords.size() - 1);
        keywordScroll = Math.min(keywordScroll, Math.max(0, keywords.size() - visibleKeywordRows));
        keywordInput.setValue("");
        saveFilter("关键词已删除");
        refreshKeywordButtons();
    }

    private void selectKeyword(int index) {
        if (index < 0 || index >= keywords.size()) return;
        selectedKeyword = index;
        keywordInput.setValue(keywords.get(index));
        refreshKeywordButtons();
    }

    private void saveFilter(String successMessage) {
        statusMessage = ConfigManager.setFilterConfig(configDir, filterMode, keywords)
                ? successMessage : "过滤器设置保存失败，请查看日志";
    }

    private void ensureSelectedKeywordVisible() {
        if (selectedKeyword < keywordScroll) keywordScroll = selectedKeyword;
        else if (selectedKeyword >= keywordScroll + visibleKeywordRows)
            keywordScroll = selectedKeyword - visibleKeywordRows + 1;
    }

    private void refreshKeywordButtons() {
        editButton.active = selectedKeyword >= 0;
        deleteButton.active = selectedKeyword >= 0;
        for (int row = 0; row < keywordButtons.size(); row++) {
            Button button = keywordButtons.get(row);
            int index = keywordScroll + row;
            button.visible = index < keywords.size();
            button.active = button.visible;
            if (button.visible) {
                String prefix = index == selectedKeyword ? "§a▶ §f" : "";
                button.setMessage(Component.literal(prefix + keywords.get(index)));
            }
        }
    }

    private Component filterModeText() {
        String displayName = switch (filterMode) {
            case "blacklist" -> "黑名单（屏蔽匹配关键词）";
            case "whitelist" -> "白名单（仅显示匹配关键词）";
            default -> "关闭（不过滤弹幕）";
        };
        return Component.literal("弹幕过滤器：" + displayName);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int left = Math.max(20, width / 2 - 190);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        if (connectionPage) {
            graphics.drawString(font, "WSS bridge 接入点", left, 78, 0xFFAAAAAA, false);
            graphics.drawString(font, "Bridge key（作为 Bearer Token 发送）", left, 110, 0xFFAAAAAA, false);
            graphics.drawString(font, "DySessionId（仅原有直连模式使用）", left, 142, 0xFFAAAAAA, false);
        } else {
            graphics.drawString(font, "过滤模式（点击切换）", left, 48, 0xFFAAAAAA, false);
            graphics.drawString(font, "关键词（选择列表项后可修改或删除）", left, 92, 0xFFAAAAAA, false);
        }
        if (!statusMessage.isEmpty()) {
            graphics.drawString(font, statusMessage, left, height - 27, 0xFF55FF55, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!connectionPage && mouseY >= 126 && mouseY <= 130 + visibleKeywordRows * 22
                && keywords.size() > visibleKeywordRows) {
            int maxScroll = keywords.size() - visibleKeywordRows;
            keywordScroll = Math.max(0, Math.min(maxScroll,
                    keywordScroll + (verticalAmount < 0 ? 1 : -1)));
            refreshKeywordButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!connectionPage && keyCode == GLFW.GLFW_KEY_ENTER && keywordInput.isFocused()) {
            if (selectedKeyword >= 0) editKeyword(); else addKeyword();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
