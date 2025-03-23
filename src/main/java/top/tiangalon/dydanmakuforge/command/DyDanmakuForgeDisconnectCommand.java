package top.tiangalon.dydanmakuforge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.mojang.text2speech.Narrator.LOGGER;
import static top.tiangalon.dydanmakuforge.DyDanmakuForge.websocket;

public class DyDanmakuForgeDisconnectCommand implements Command<CommandSourceStack> {

    public static DyDanmakuForgeDisconnectCommand instance = new DyDanmakuForgeDisconnectCommand();

        @Override
        public int run(CommandContext<CommandSourceStack> context) {
            if (!websocket.isConnected()) {
                context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]尚未连接到房间，无法断开连接"));
                LOGGER.info("[DyDanmaku]尚未连接到房间，无法断开连接");
                return Command.SINGLE_SUCCESS;
            }
            try {
                websocket.close();
            } catch (InterruptedException e) {
                context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]断开连接失败"));
                throw new RuntimeException(e);
            }
            context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]已经断开直播间连接"));
            LOGGER.info("[DyDanmaku]已经断开直播间连接");
            return Command.SINGLE_SUCCESS;
    }
}
