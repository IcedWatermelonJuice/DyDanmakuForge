package top.tiangalon.dydanmakuforge;

import DyDanmaku.WebSocketClientNetty;
import DyDanmaku.WebSocketClientNetty;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import top.tiangalon.dydanmakuforge.command.DyDanmakuForgeConnectCommand;

import java.io.File;
import java.io.IOException;

import static DyDanmaku.WebSocketClientNetty.getSignFile;

@Mod("dydanmakuforge")
public class DyDanmakuForge {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static WebSocketClientNetty websocket = new WebSocketClientNetty();
    public static String DyDanmakuPath = WebSocketClientNetty.getPath();
    public static String ConfigDirPath = DyDanmakuPath.substring(0, DyDanmakuPath.lastIndexOf("/")) + "/config/DyDanmaku";

    public DyDanmakuForge() {
        LOGGER.info("[DyDanmaku]DyDanmakuForge Mod Loaded");
        LOGGER.info(DyDanmakuPath);
        LOGGER.info(DyDanmakuForgeConnectCommand.class.getResource("").getProtocol());
        FileInit();
    }

    public static boolean isRunInJar() {
        String runType = String.valueOf(WebSocketClientNetty.class.getResource("WebSocketClientNetty.class"));
        return runType != null && runType.startsWith("jar:");
    }

    public static void FileInit() {
        File ConfigDir = new File(ConfigDirPath);
        if  (!ConfigDir.exists()  && !ConfigDir.isDirectory()) {
            LOGGER.info("[DyDanmaku]/config/DyDanmaku不存在,创建目录");
            ConfigDir.mkdirs();
        } else {
            LOGGER.info("[DyDanmaku]/config/DyDanmaku目录存在");
        }
        String SignFilePath = ConfigDirPath + "/Signature.exe";
        File SignFile = new File(SignFilePath);
        if(!SignFile.exists()) {
            LOGGER.info("[DyDanmaku]Signature.exe不存在,创建文件");
            try {
                getSignFile(SignFilePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.info("[DyDanmaku]Signature.exe文件存在");
        }
    }

}
