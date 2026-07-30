# DyDanmaku Forge 1.20.1

在 Minecraft 客户端聊天框中显示抖音直播间消息。本分支面向 Minecraft 1.20.1，并与 DyDanmaku Fabric `0.1.7` 的功能保持一致。

## 环境

- Minecraft 1.20.1
- Forge 47.4.22
- Java 17

## 使用

将 `DyDanmaku-forge-1.20.1-0.1.7.jar` 放入客户端的 `mods` 目录。进入游戏后按 `F7` 打开控制界面，输入抖音直播间号并连接；也可以使用客户端命令：

```text
/dydanmaku connect <直播间号>
/dydanmaku disconnect
/dydanmaku status
```

首次启动会创建 `config/dydanmaku/DyDanmakuSettings.toml`，可配置 `DySessionId`、消息类型开关、关键词过滤、用户等级过滤和输出模板。

## 构建

PowerShell 构建脚本启动后会询问自定义 Java 路径，直接回车即可自动选择。Java 的选择优先级为：自定义路径 → `JAVA_17_HOME` → `JAVA_HOME` → `Path` 中的 `java`。仅在脚本进程内临时切换 `JAVA_HOME` 和 `Path`，结束后会恢复原值：

```powershell
.\build.ps1

# 完整清理后构建
.\build.ps1 -Clean

# 也可以直接通过参数指定，不再交互询问
.\build.ps1 -Clean -JavaHome 'C:\path\to\jdk-17'
```

环境变量的读取优先级为当前进程、用户环境变量、系统环境变量。脚本会校验所选 Java 是否为 Java 17。构建产物位于：

```text
build/libs/DyDanmaku-forge-1.20.1-0.1.7.jar
```

构建过程中还会生成带 `-slim.jar` 后缀的中间包。该文件不含第三方依赖，不要作为游戏安装包使用。所有 JAR 均已由 `.gitignore` 排除。

## 说明

- Mod 仅在客户端加载，不要求服务器安装。
- Protobuf、TOML 和所需的 Netty HTTP 类已包含在发行 JAR 中；Nashorn 及其 ASM 依赖复用 Forge 自带版本，避免 Java 模块冲突。
- 抖音网页结构和签名算法可能随平台更新而变化；连接失败时请查看游戏日志中的 `[DyDanmaku]` 信息。
