# DyDanmaku Forge 1.21.1

在 Minecraft 客户端聊天框中显示抖音直播间消息。本分支面向 Minecraft 1.21.1，并与 DyDanmaku Fabric `0.1.7` 的功能保持一致。

## 环境

- Minecraft 1.21.1
- Forge 52.1.15
- Java 21

## 使用

将 `DyDanmaku-forge-1.21.1-0.1.7.jar` 放入客户端的 `mods` 目录。进入游戏后按 `F7` 打开控制界面，输入抖音直播间号并连接；也可以使用客户端命令：

```text
/dydanmaku connect <直播间号>
/dydanmaku disconnect
/dydanmaku status
```

首次启动会创建 `config/dydanmaku/DyDanmakuSettings.toml`，可配置 `DySessionId`、消息类型开关、关键词过滤、用户等级过滤和输出模板。

## 构建

PowerShell 构建脚本读取 `JAVA_21_HOME`，仅在脚本进程内临时切换 `JAVA_HOME` 和 `Path`，结束后会恢复原值：

```powershell
.\build.ps1

# 完整清理后构建
.\build.ps1 -Clean
```

变量读取优先级为当前进程、用户环境变量、系统环境变量。构建产物位于：

```text
build/libs/DyDanmaku-forge-1.21.1-0.1.7.jar
```

## 说明

- Mod 仅在客户端加载，不要求服务器安装。
- Protobuf、Nashorn、ASM、TOML 和所需的 Netty HTTP 类已包含在发行 JAR 中，无需另外安装。
- 抖音网页结构和签名算法可能随平台更新而变化；连接失败时请查看游戏日志中的 `[DyDanmaku]` 信息。
