package top.tiangalon.dydanmakuforge.client;

import top.tiangalon.dydanmakuforge.net.DyDanmakuRequest;
import top.tiangalon.dydanmakuforge.net.WebSocketClientNetty;
import top.tiangalon.dydanmakuforge.config.ConfigManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.tiangalon.dydanmakuforge.DyDanmakuForge.LOGGER;

public final class DyDanmakuController {
    private static final String DOUYIN_LIVE_URL_PREFIX = "https://live.douyin.com/";

    private final WebSocketClientNetty websocket = new WebSocketClientNetty();
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "DyDanmaku-Worker");
        thread.setDaemon(true);
        return thread;
    });

    public void connect(String liveId) {
        ConfigManager.OfficialApiConfig officialConfig = ConfigManager.getOfficialApiConfig(
                ClientRuntime.getConfigDir().toString());
        String normalizedId = normalizeLiveId(liveId);
        if (!officialConfig.enabled && normalizedId.isEmpty()) {
            ClientRuntime.output("[DyDanmaku]请输入直播间 URL 或房间号");
            return;
        }
        if (websocket.isConnected()) {
            ClientRuntime.output("[DyDanmaku]已经连接到直播间，无法重复连接");
            return;
        }
        if (!connecting.compareAndSet(false, true)) {
            ClientRuntime.output("[DyDanmaku]正在连接，请稍候");
            return;
        }

        ClientRuntime.output(officialConfig.enabled
                ? "[DyDanmaku]正在连接抖音官方 OpenAPI bridge WSS"
                : "[DyDanmaku]正在通过原有直连方式连接直播间：" + normalizedId);
        worker.submit(() -> {
            try {
                if (officialConfig.enabled) {
                    websocket.initOfficial(officialConfig);
                } else {
                    Map<String, String> params = DyDanmakuRequest.getParams(normalizedId);
                    if (params == null) {
                        ClientRuntime.output("[DyDanmaku]无法获取直播间参数，请检查网络环境或房间号");
                        return;
                    }
                    websocket.init(params);
                }
                websocket.run();
                ClientRuntime.output(officialConfig.enabled
                        ? "[DyDanmaku]已连接抖音官方 OpenAPI bridge WSS"
                        : "[DyDanmaku]已经连接到直播间：" + normalizedId);
                websocket.liveStatusOutput();
            } catch (Exception exception) {
                LOGGER.error("[DyDanmaku]连接弹幕数据源失败", exception);
                ClientRuntime.output("[DyDanmaku]连接弹幕数据源失败：" + exception.getMessage());
                closeQuietly();
            } finally {
                connecting.set(false);
            }
        });
    }

    public void disconnect() {
        if (!websocket.isConnected() && !connecting.get()) {
            ClientRuntime.output("[DyDanmaku]尚未连接到直播间");
            return;
        }
        worker.submit(() -> {
            try {
                websocket.close();
                ClientRuntime.output("[DyDanmaku]已经断开直播间连接");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                ClientRuntime.output("[DyDanmaku]断开直播间连接失败");
            } finally {
                connecting.set(false);
            }
        });
    }

    public void status() {
        if (!websocket.isConnected()) {
            ClientRuntime.output(connecting.get()
                    ? "[DyDanmaku]正在连接直播间"
                    : "[DyDanmaku]尚未连接到直播间");
            return;
        }
        websocket.liveStatusOutput();
    }

    public boolean isConnecting() {
        return connecting.get();
    }

    public WebSocketClientNetty getWebsocket() {
        return websocket;
    }

    static String normalizeLiveId(String liveId) {
        String normalizedId = liveId == null ? "" : liveId.trim();
        if (!normalizedId.startsWith(DOUYIN_LIVE_URL_PREFIX)) {
            return normalizedId;
        }

        normalizedId = normalizedId.substring(DOUYIN_LIVE_URL_PREFIX.length());
        int delimiterIndex = normalizedId.length();
        for (char delimiter : new char[]{'/', '?', '#'}) {
            int index = normalizedId.indexOf(delimiter);
            if (index >= 0) {
                delimiterIndex = Math.min(delimiterIndex, index);
            }
        }
        return normalizedId.substring(0, delimiterIndex).trim();
    }

    private void closeQuietly() {
        try {
            websocket.close();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
