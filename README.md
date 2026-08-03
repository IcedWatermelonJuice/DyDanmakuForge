# DyDanmakuForge

[![OpenJDK 17](https://img.shields.io/badge/OpenJDK-17-437291?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![OpenJDK 21](https://img.shields.io/badge/OpenJDK-21-437291?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Forge](https://img.shields.io/badge/Forge-1.20.1%20%7C%201.21.1-F16436?logo=curseforge&logoColor=white)](https://files.minecraftforge.net/net/minecraftforge/forge/)
[![NeoForge](https://img.shields.io/badge/NeoForge-1.21.1-FC8D1A?logo=curseforge&logoColor=white)](https://neoforged.net/)
[![GitHub](https://img.shields.io/badge/tiangalon-DyDanmakuForge-181717?logo=github&logoColor=white)](https://github.com/tiangalon/DyDanmakuForge)
[![GitHub](https://img.shields.io/badge/tiangalon-DyDanmaku-181717?logo=github&logoColor=white)](https://github.com/tiangalon/DyDanmaku)

在 Minecraft 客户端聊天框中显示抖音直播弹幕的 Forge/NeoForge Mod。

本仓库 Fork 自 [tiangalon/DyDanmakuForge（Forge 分支仓库）](https://github.com/tiangalon/DyDanmakuForge)，并同步 [tiangalon/DyDanmaku（Fabric 主仓库）](https://github.com/tiangalon/DyDanmaku) 的**核心功能与文件**，修复 Fabric 相关 API 与 Forge/NeoForge API 不一致的问题。
本仓库主要用于 **个人使用** 和维护，并根据实际使用需求补充 Forge/NeoForge 版本的修复与功能。
当前仅维护 Minecraft 1.20.1 和 1.21.1，各版本与加载器使用独立分支。
本人贡献：将上述核心逻辑迁移至 Forge/NeoForge 环境，修复加载器兼容性问题，并进行 UI 交互适配。

## 维护版本

| Minecraft | 加载器 | Java | 分支 |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.244 | 21 | [`neoforge/1.21.1-0.1.7`](https://github.com/IcedWatermelonJuice/DyDanmakuForge/tree/neoforge/1.21.1-0.1.7) |
| 1.21.1 | Forge 52.1.16 | 21 | [`forge/1.21.1-0.1.7`](https://github.com/IcedWatermelonJuice/DyDanmakuForge/tree/forge/1.21.1-0.1.7) |
| 1.20.1 | Forge 47.2.32 | 17 | [`forge/1.20.1-0.1.7`](https://github.com/IcedWatermelonJuice/DyDanmakuForge/tree/forge/1.20.1-0.1.7) |

其他 Minecraft 版本暂不在维护范围内。

## 下载与 Java 版本

已构建的 Mod 将发布在 [GitHub Releases](https://github.com/IcedWatermelonJuice/DyDanmakuForge/releases)。请根据 Minecraft 版本下载对应的 JAR 文件。

Minecraft 1.20.1 版本的 Mod 基于 Java 17 构建，Minecraft 1.21.1 的 Forge 与 NeoForge 版本均基于 Java 21 构建。运行 Mod 时也建议为对应的 Minecraft/Forge/NeoForge 实例使用相同的 Java 版本，以避免类版本不兼容或其他运行问题。

## 使用方法

下载与 Minecraft 版本及加载器对应的 Mod，将 JAR 文件放入客户端的 `mods` 目录。该 Mod 仅需安装在客户端，服务器无需安装。

进入游戏后按 `F7` 打开控制界面，可以输入以下任一格式：

```text
https://live.douyin.com/12345678
12345678
```

也可以使用客户端命令：

```text
/dydanmaku connect <直播间 URL 或房间号>
/dydanmaku disconnect
/dydanmaku status
```

消息过滤命令：

```text
/dydanmaku filter <chat|member|stats|like|gift|fansclub|all>
/dydanmaku unfilter <chat|member|stats|like|gift|fansclub|all>
/dydanmaku filter status
```

`filter` 用于屏蔽消息类型，`unfilter` 用于恢复显示，`filter status` 用于查看当前允许显示的消息类型。

## 构建

请先切换到对应的版本分支，再运行 PowerShell 构建脚本：

```powershell
.\build.ps1

# 清理后重新构建
.\build.ps1 -Clean
```

构建脚本会询问是否使用自定义 Java 路径。直接回车时，将依次尝试对应版本的 `JAVA_17_HOME` 或 `JAVA_21_HOME`、`JAVA_HOME`，最后使用 `Path` 中的 `java`。

最终安装包位于：

```text
build/libs/
```

## 说明

- 本项目与抖音官方无关，仅用于在 Minecraft 客户端内显示公开的直播间消息。
- 抖音网页结构和签名算法可能随平台更新而变化；连接失败时请检查网络状态，并查看游戏日志中的 `[DyDanmaku]` 信息。
- 如需了解原始 Forge 实现，请访问 [tiangalon/DyDanmakuForge](https://github.com/tiangalon/DyDanmakuForge)；如需了解当前功能基线，请访问 [tiangalon/DyDanmaku](https://github.com/tiangalon/DyDanmaku)。
