package top.tiangalon.dydanmakuforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



@Mod.EventBusSubscriber
public class CommandEventHandler {

    @SubscribeEvent
    public static void onSeverStaring(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralCommandNode<CommandSourceStack> dydanmaku_cmd = dispatcher.register(
                Commands.literal("dydanmaku").requires(source -> source.hasPermission(0)).executes(
                        command -> {
                            command.getSource().sendSystemMessage(Component.nullToEmpty("[DyDanmaku]Called /dydanmaku with no arguments."));
                            return 0;
                        }
                ).then(
                        Commands.literal("connect").then(
                                Commands.argument("live_id", StringArgumentType.string()).requires(
                                        source -> source.hasPermission(0)).executes(
                                                DyDanmakuForgeConnectCommand.instance
                                )
                        )
                ).then(
                        Commands.literal("disconnect").requires(
                                source -> source.hasPermission(0)).executes(
                                DyDanmakuForgeDisconnectCommand.instance
                        )
                ).then(
                        Commands.literal("status")
                                .executes(DyDanmakuForgeStatusCommand.instance)
                )
        );
        dispatcher.register(Commands.literal("dydanmaku").redirect(dydanmaku_cmd));
    }
}
