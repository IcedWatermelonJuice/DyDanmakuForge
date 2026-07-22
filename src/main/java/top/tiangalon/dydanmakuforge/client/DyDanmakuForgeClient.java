package top.tiangalon.dydanmakuforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import top.tiangalon.dydanmakuforge.config.ConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static top.tiangalon.dydanmakuforge.DyDanmakuForge.LOGGER;

public final class DyDanmakuForgeClient {
    public static final DyDanmakuController CONTROLLER = new DyDanmakuController();
    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key.dydanmaku.gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "key.category.dydanmaku.dydanmakukey"
    );

    private DyDanmakuForgeClient() {
    }

    public static void register(IEventBus modEventBus) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("dydanmaku");
        try {
            Files.createDirectories(configDir);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建 DyDanmaku 配置目录", exception);
        }

        ClientRuntime.initialize(
                configDir,
                DyDanmakuForgeClient::sendChatMessage,
                DyDanmakuScreen::setAvatar
        );
        ConfigManager.createDefaultConfig(configDir.toString());

        modEventBus.addListener(DyDanmakuForgeClient::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(DyDanmakuForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(DyDanmakuForgeClient::registerClientCommands);
        LOGGER.info("[DyDanmaku]配置目录：{}", configDir.toAbsolutePath());
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !OPEN_GUI.consumeClick()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DyDanmakuScreen) {
            minecraft.setScreen(null);
        } else {
            minecraft.setScreen(new DyDanmakuScreen(CONTROLLER));
        }
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dydanmaku")
                        .executes(context -> {
                            ClientRuntime.output("[DyDanmaku]用法：/dydanmaku <connect|disconnect|status>");
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("connect")
                                .then(Commands.argument("live_id", StringArgumentType.string())
                                        .executes(context -> {
                                            CONTROLLER.connect(StringArgumentType.getString(context, "live_id"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("disconnect")
                                .executes(context -> {
                                    CONTROLLER.disconnect();
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    CONTROLLER.status();
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

    private static void sendChatMessage(String message) {
        LOGGER.info("{}", message);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.getChat().addMessage(Component.literal(message)));
    }
}
