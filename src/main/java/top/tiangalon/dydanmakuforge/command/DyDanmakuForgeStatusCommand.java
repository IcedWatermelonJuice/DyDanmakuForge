package top.tiangalon.dydanmakuforge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.mojang.text2speech.Narrator.LOGGER;
import static top.tiangalon.dydanmakuforge.DyDanmakuForge.websocket;

public class DyDanmakuForgeStatusCommand implements Command<CommandSourceStack> {

    public static DyDanmakuForgeStatusCommand instance = new DyDanmakuForgeStatusCommand();

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        if (!websocket.isConnected()) {
            context.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]尚未连接到房间，无法获取状态"));
            LOGGER.info("[DyDanmaku]尚未连接到房间，无法获取状态");
            return Command.SINGLE_SUCCESS;
        }
        websocket.LiveStatusOutput();
        return Command.SINGLE_SUCCESS;
    }
}
