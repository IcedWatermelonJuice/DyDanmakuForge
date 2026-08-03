package top.tiangalon.dydanmakuforge.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import top.tiangalon.dydanmakuforge.client.ClientRuntime;
import top.tiangalon.dydanmakuforge.config.ConfigManager;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static top.tiangalon.dydanmakuforge.DyDanmakuForge.LOGGER;

/** 接收主播自建 bridge 转发的抖音官方 OpenAPI 互动数据。 */
public final class OfficialApiWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handshaker;
    private final StringBuilder danmakuList = new StringBuilder();
    private ChannelPromise handshakeFuture;

    public OfficialApiWebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object frame) {
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) frame);
                handshakeFuture.setSuccess();
                LOGGER.info("[DyDanmaku]官方 OpenAPI bridge WSS 连接成功");
            } catch (WebSocketHandshakeException exception) {
                handshakeFuture.setFailure(exception);
                ctx.close();
            }
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            ctx.close();
        } else if (frame instanceof PingWebSocketFrame ping) {
            ctx.writeAndFlush(new PongWebSocketFrame(ping.content().retain()));
        } else if (frame instanceof TextWebSocketFrame text) {
            processJson(text.text());
        } else if (frame instanceof BinaryWebSocketFrame binary) {
            processJson(binary.content().readableBytes() == 0
                    ? "" : binary.content().toString(StandardCharsets.UTF_8));
        }
    }

    private void processJson(String json) {
        if (json == null || json.isBlank()) return;
        try {
            processElement(JsonParser.parseString(json));
        } catch (Exception exception) {
            LOGGER.warn("[DyDanmaku]忽略 bridge 发来的无效 JSON", exception);
        }
    }

    private void processElement(JsonElement element) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) processElement(item);
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();

        JsonElement payload = object.get("payload");
        if (payload != null && (payload.isJsonArray() || payload.isJsonObject())) {
            processElement(payload);
            return;
        }
        JsonObject params = asObject(object.get("params"));
        if (params != null && params.has("payload")) {
            processElement(params.get("payload"));
            return;
        }
        JsonElement data = object.get("data");
        if (data != null && data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) {
            processJson(data.getAsString());
            return;
        }

        String type = stringValue(object, "msg_type_str");
        if (type.isEmpty()) type = stringValue(object, "msg_type");
        switch (normalizeType(type)) {
            case "live_comment" -> outputComment(object);
            case "live_like" -> outputLike(object);
            case "live_gift" -> outputGift(object);
            case "live_fansclub" -> outputFansclub(object);
            default -> LOGGER.debug("[DyDanmaku]忽略 bridge 未支持的消息类型：{}", type);
        }
    }

    private void outputComment(JsonObject object) {
        Map<String, String> vars = userVars(object);
        vars.put("content", stringValue(object, "content"));
        output("WebcastChatMessage", vars, "§b[聊天]§f${nickname}：${content}", object, true);
    }

    private void outputLike(JsonObject object) {
        Map<String, String> vars = userVars(object);
        vars.put("count", numberValue(object, "like_num", "1"));
        output("WebcastLikeMessage", vars, "§d[点赞]§f${nickname} 点了${count}个赞", object, true);
    }

    private void outputGift(JsonObject object) {
        Map<String, String> vars = userVars(object);
        String giftName = stringValue(object, "gift_name");
        if (giftName.isEmpty()) giftName = stringValue(object, "sec_gift_id");
        String count = numberValue(object, "gift_num", "1");
        vars.put("giftName", giftName.isEmpty() ? "礼物" : giftName);
        vars.put("giftCombo", "1".equals(count) ? "" : "x" + count);
        vars.put("comboCount", count);
        vars.put("repeatCount", count);
        vars.put("giftId", stringValue(object, "sec_gift_id"));
        vars.put("giftDescribe", "");
        vars.put("giftDiamondCount", "");
        vars.put("giftType", "");
        output("WebcastGiftMessage", vars, "§a[礼物]§f${nickname} 送出了${giftName}${giftCombo}", object, true);
    }

    private void outputFansclub(JsonObject object) {
        Map<String, String> vars = userVars(object);
        String reason = switch (numberValue(object, "fansclub_reason_type", "0")) {
            case "1" -> "粉丝团升级";
            case "2" -> "加入粉丝团";
            case "16" -> "退出粉丝团";
            default -> "粉丝团状态更新";
        };
        vars.put("content", vars.getOrDefault("nickname", "用户") + " " + reason);
        output("WebcastFansclubMessage", vars, "§6[粉丝团]§f${content}", object, false);
    }

    private void output(String method, Map<String, String> vars, String fallback,
                        JsonObject source, boolean applyUserFilter) {
        String configDir = ClientRuntime.getConfigDir().toString();
        if (!ConfigManager.getMethodVisibilityConfig(configDir).isMethodEnabled(method)) return;
        if (applyUserFilter && !shouldDisplayUser(source, configDir)) return;
        String template = ConfigManager.getTemplateConfig(configDir).getTemplate(method);
        String message = template == null ? fallback : template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            message = message.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        message = message.replaceAll("\\$\\{[^}]+}", "");
        if (!shouldDisplay(message, configDir)) return;
        ClientRuntime.output(message);
        appendDanmaku(message + "\n");
    }

    private boolean shouldDisplay(String message, String configDir) {
        ConfigManager.FilterConfig filter = ConfigManager.getFilterConfig(configDir);
        if (!filter.isEnabled()) return true;
        String lower = message.toLowerCase();
        boolean matched = filter.keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isEmpty())
                .anyMatch(keyword -> lower.contains(keyword.toLowerCase()));
        return "whitelist".equals(filter.mode) ? matched : !matched;
    }

    private boolean shouldDisplayUser(JsonObject object, String configDir) {
        ConfigManager.UserFilterConfig filter = ConfigManager.getUserFilterConfig(configDir);
        if (!filter.enabled) return true;
        int fansLevel = intValue(object, "fansclub_level", 0);
        int privilegeLevel = intValue(object, "user_privilege_level", 0);
        if (filter.requireFanClub && (fansLevel <= 0 || fansLevel < filter.fanClubMinLevel)) return false;
        return !filter.requirePayGrade
                || (privilegeLevel > 0 && privilegeLevel >= filter.payGradeMinLevel);
    }

    private Map<String, String> userVars(JsonObject object) {
        Map<String, String> vars = new HashMap<>();
        vars.put("nickname", stringValue(object, "nickname"));
        vars.put("fansClubLevel", numberValue(object, "fansclub_level", ""));
        vars.put("payGradeLevel", numberValue(object, "user_privilege_level", ""));
        return vars;
    }

    private synchronized void appendDanmaku(String message) {
        if (danmakuList.length() > 10000) {
            int firstLineEnd = danmakuList.indexOf("\n");
            danmakuList.delete(0, firstLineEnd >= 0 ? firstLineEnd + 1 : danmakuList.length());
        }
        danmakuList.append(message);
    }

    public synchronized String getDanmakuText() {
        return danmakuList.toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("[DyDanmaku]官方 OpenAPI bridge WSS 处理失败", cause);
        if (handshakeFuture != null && !handshakeFuture.isDone()) handshakeFuture.setFailure(cause);
        ctx.close();
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static String numberValue(JsonObject object, String key, String fallback) {
        String value = stringValue(object, key);
        return value.isEmpty() ? fallback : value;
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalizeType(String type) {
        return switch (type) {
            case "1" -> "live_like";
            case "2" -> "live_comment";
            case "3" -> "live_gift";
            case "4" -> "live_fansclub";
            default -> type;
        };
    }
}
