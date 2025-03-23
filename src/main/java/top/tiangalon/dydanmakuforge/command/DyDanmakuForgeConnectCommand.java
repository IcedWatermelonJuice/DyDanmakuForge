package top.tiangalon.dydanmakuforge.command;

import DyDanmaku.DyDanmakuRequest;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import javax.net.ssl.SSLException;
import java.net.URISyntaxException;
import java.util.Map;

import static top.tiangalon.dydanmakuforge.DyDanmakuForge.websocket;
import static top.tiangalon.dydanmakuforge.DyDanmakuForge.LOGGER;

public class DyDanmakuForgeConnectCommand implements Command<CommandSourceStack> {
    public static DyDanmakuForgeConnectCommand instance = new DyDanmakuForgeConnectCommand();

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if(websocket.isConnected()) {
            context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]已经连接到房间，无法重复连接"));
            LOGGER.info("[DyDanmaku]已经连接到房间，无法重复连接");
            return Command.SINGLE_SUCCESS;
        }
        String live_id = StringArgumentType.getString(context, "live_id");
        context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]尝试连接到房间号：" + live_id));
        Map<String, String> params = DyDanmakuRequest.getParams(live_id);
        if (params == null) {
            context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]无法获取房间号：" + live_id + " 的参数,请检查网络环境或房间号是否正确"));
            LOGGER.info("[DyDanmaku]无法获取房间号：" + live_id + " 的参数,请检查网络环境或房间号是否正确");
            return Command.SINGLE_SUCCESS;
        }
        if (websocket.isConnected()) {
            context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]已经连接到房间号：" + live_id + "，无法重复连接"));
            LOGGER.info("[DyDanmaku]已经连接到房间号：" + live_id + "，无法重复连接");
            return Command.SINGLE_SUCCESS;
        }
        try {
            websocket.init(params, context.getSource());
            new Thread(() -> {
                try {
                    websocket.run();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }).start();
        } catch (Exception e) {
            LOGGER.info("[DyDanmaku]无法连接房间：" + live_id, e);
            context.getSource().getPlayer().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]无法连接房间：" + live_id));
            throw new RuntimeException(e);
        }
        context.getSource().getPlayer().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]已经连接到房间号：" + live_id));
        LOGGER.info("[DyDanmaku]已经连接到房间号：" + live_id);
        websocket.LiveStatusOutput();
        return Command.SINGLE_SUCCESS;
    }
}