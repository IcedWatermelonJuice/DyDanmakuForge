package top.tiangalon.dydanmakuforge.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import top.tiangalon.dydanmakuforge.client.ClientRuntime;
import top.tiangalon.dydanmakuforge.config.ConfigManager;
import top.tiangalon.dydanmakuforge.douyin.DySignEngine;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static top.tiangalon.dydanmakuforge.DyDanmakuForge.LOGGER;

public final class WebSocketClientNetty {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private volatile Channel channel;
    private volatile EventLoopGroup eventLoopGroup;
    private String uri;
    private String ttwid;
    private boolean officialApi;
    private String officialApiKey;
    public volatile Map<String, String> params;
    public volatile WebSocketClientHandler handler;
    public volatile OfficialApiWebSocketClientHandler officialHandler;

    public void init(Map<String, String> params) {
        this.officialApi = false;
        this.officialApiKey = null;
        this.officialHandler = null;
        this.params = Objects.requireNonNull(params, "params");
        String roomId = requireParam(params, "roomId");
        String userUniqueId = requireParam(params, "user_unique_id");
        this.ttwid = requireParam(params, "ttwid");

        String avatarUrl = params.get("avatar");
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            Thread avatarThread = new Thread(
                    () -> DyDanmakuRequest.downloadAvatar(avatarUrl, roomId),
                    "DyDanmaku-Avatar"
            );
            avatarThread.setDaemon(true);
            avatarThread.start();
        }

        String signature = DySignEngine.generateSignature(roomId, userUniqueId);
        String internalExt = "internal_ext=internal_src:dim|wss_push_room_id:" + roomId
                + "|wss_push_did:" + userUniqueId
                + "|first_req_ms:1732882891041|fetch_time:1732882891133|seq:1|"
                + "wss_info:0-1732882891133-0-0|wrds_v:7442675340347970811&";

        this.uri = "wss://webcast5-ws-web-hl.douyin.com/webcast/im/push/v2/?"
                + "app_name=douyin_web&version_code=180800&webcast_sdk_version=1.0.14-beta.0&"
                + "update_version_code=1.0.14-beta.0&compress=gzip&device_platform=web&"
                + "cookie_enabled=true&screen_width=2560&screen_height=1440&browser_language=zh-CN&"
                + "browser_platform=Win32&browser_name=Mozilla&browser_version="
                + URLEncoder.encode(USER_AGENT, StandardCharsets.UTF_8)
                + "&browser_online=true&tz_name=Asia/Hong_Kong&"
                + "cursor=t-1732882891133_r-1_d-1_u-1_h-7442675243345155072&"
                + URLEncoder.encode(internalExt, StandardCharsets.UTF_8)
                + "host=https://live.douyin.com&aid=6383&live_id=1&did_rule=3&"
                + "endpoint=live_pc&support_wrds=1&user_unique_id=" + userUniqueId
                + "&im_path=/webcast/im/fetch/&identity=audience&need_persist_msg_count=15&"
                + "insert_task_id=&live_reason=&room_id=" + roomId
                + "&heartbeatDuration=0&signature=" + signature;
    }

    public void initOfficial(ConfigManager.OfficialApiConfig config) {
        Objects.requireNonNull(config, "config");
        String endpoint = config.endpoint == null ? "" : config.endpoint.trim();
        if (endpoint.isEmpty()) {
            throw new IllegalArgumentException("请先在更多设置中填写官方 OpenAPI bridge 接入点");
        }
        URI socketUri = URI.create(endpoint);
        if (!"wss".equalsIgnoreCase(socketUri.getScheme()) || socketUri.getHost() == null) {
            throw new IllegalArgumentException("官方 OpenAPI bridge 接入点必须是有效的 wss:// 地址");
        }
        if (config.key == null || config.key.isBlank()) {
            throw new IllegalArgumentException("请先在更多设置中填写官方 OpenAPI bridge key");
        }
        this.officialApi = true;
        this.officialApiKey = config.key.trim();
        this.params = null;
        this.handler = null;
        this.ttwid = null;
        this.uri = socketUri.toString();
    }

    public void run() throws Exception {
        URI socketUri = URI.create(Objects.requireNonNull(uri, "WebSocket 尚未初始化"));
        int port = socketUri.getPort() >= 0 ? socketUri.getPort() : 443;
        SslContext sslContext = SslContextBuilder.forClient().build();
        EventLoopGroup group = new NioEventLoopGroup(1);
        this.eventLoopGroup = group;

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("User-Agent", USER_AGENT);
        if (officialApi) {
            headers.add("Authorization", "Bearer " + officialApiKey);
        } else {
            String sessionId = ConfigManager.getSessionId(ClientRuntime.getConfigDir().toString());
            String cookie = "ttwid=" + ttwid;
            if (sessionId != null && !sessionId.isBlank()) {
                cookie += "; sessionid=" + sessionId;
            }
            headers.add("Cookie", cookie);
        }

        ChannelHandler socketHandler;
        if (officialApi) {
            OfficialApiWebSocketClientHandler officialSocketHandler = new OfficialApiWebSocketClientHandler(
                    WebSocketClientHandshakerFactory.newHandshaker(
                            socketUri, WebSocketVersion.V13, null, false, headers));
            this.officialHandler = officialSocketHandler;
            socketHandler = officialSocketHandler;
        } else {
            WebSocketClientHandler directSocketHandler = new WebSocketClientHandler(
                    WebSocketClientHandshakerFactory.newHandshaker(
                            socketUri, WebSocketVersion.V13, null, false, headers));
            this.handler = directSocketHandler;
            socketHandler = directSocketHandler;
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(
                                sslContext.newHandler(socketChannel.alloc(), socketUri.getHost(), port),
                                new HttpClientCodec(),
                                new HttpObjectAggregator(8192),
                                socketHandler
                        );
                    }
                });

        try {
            channel = bootstrap.connect(socketUri.getHost(), port).sync().channel();
            if (officialApi) {
                officialHandler.handshakeFuture().sync();
            } else {
                handler.handshakeFuture().sync();
            }
        } catch (Exception exception) {
            shutdownGroup();
            throw exception;
        }
    }

    public boolean isConnected() {
        Channel current = channel;
        if (current == null || !current.isActive()) return false;
        if (officialApi) {
            OfficialApiWebSocketClientHandler currentHandler = officialHandler;
            return currentHandler != null && currentHandler.handshakeFuture() != null
                    && currentHandler.handshakeFuture().isSuccess();
        }
        WebSocketClientHandler currentHandler = handler;
        return currentHandler != null && currentHandler.handshakeFuture() != null
                && currentHandler.handshakeFuture().isSuccess();
    }

    public synchronized void close() throws InterruptedException {
        Channel current = channel;
        try {
            if (current != null && current.isOpen()) {
                current.writeAndFlush(new CloseWebSocketFrame()).sync();
                current.close().sync();
            }
        } finally {
            channel = null;
            shutdownGroup();
        }
    }

    private void shutdownGroup() {
        EventLoopGroup group = eventLoopGroup;
        eventLoopGroup = null;
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public void liveStatusOutput() {
        if (officialApi) {
            ClientRuntime.output("—————————————§bDYDANMAKU§f—————————————");
            ClientRuntime.output("已连接主播自建的抖音官方 OpenAPI bridge WSS");
            ClientRuntime.output("—————————————————————————————————");
            return;
        }
        if (params == null) {
            ClientRuntime.output("[DyDanmaku]未获取到直播间参数");
            return;
        }
        String liveStatus = Objects.equals(params.get("live_status"), "2")
                ? "§a●直播中§f" : "§4●未开播§f";
        ClientRuntime.output("—————————————§bDYDANMAKU§f—————————————");
        ClientRuntime.output("已经连接到直播间   状态：" + liveStatus);
        ClientRuntime.output("直播间标题：" + params.get("live_title"));
        ClientRuntime.output("主播：" + params.get("nickname"));
        ClientRuntime.output("—————————————————————————————————");
    }

    public String getDanmakuText() {
        OfficialApiWebSocketClientHandler currentOfficialHandler = officialHandler;
        if (officialApi) {
            return currentOfficialHandler == null ? "" : currentOfficialHandler.getDanmakuText();
        }
        WebSocketClientHandler currentHandler = handler;
        return currentHandler == null ? "" : currentHandler.getDanmakuText();
    }

    public boolean isOfficialApi() {
        return officialApi;
    }

    private static String requireParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("直播间参数缺少 " + key);
        }
        return value;
    }
}
