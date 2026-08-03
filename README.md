# DyDanmakuForge

[![OpenJDK 17](https://img.shields.io/badge/OpenJDK-17-437291?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![OpenJDK 21](https://img.shields.io/badge/OpenJDK-21-437291?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Forge](https://img.shields.io/badge/Forge-1.20.1%20%7C%201.21.1-F16436?logo=curseforge&logoColor=white)](https://files.minecraftforge.net/net/minecraftforge/forge/)
[![NeoForge](https://img.shields.io/badge/NeoForge-1.21.1-FC8D1A?logo=curseforge&logoColor=white)](https://neoforged.net/)
[![GitHub](https://img.shields.io/badge/tiangalon-DyDanmakuForge-181717?logo=github&logoColor=white)](https://github.com/tiangalon/DyDanmakuForge)
[![GitHub](https://img.shields.io/badge/tiangalon-DyDanmaku-181717?logo=github&logoColor=white)](https://github.com/tiangalon/DyDanmaku)

在 Minecraft 客户端聊天框中显示抖音直播弹幕的 Forge/NeoForge Mod。

本仓库 Fork 自 [tiangalon/DyDanmakuForge（Forge 分支仓库）](https://github.com/tiangalon/DyDanmakuForge)，并同步 [tiangalon/DyDanmaku（Fabric 主仓库）](https://github.com/tiangalon/DyDanmaku) 的**核心功能与文件**，修复 Fabric 相关 API 与 Forge/NeoForge API 不一致的问题并适配 **UI页面**，**未增删改其原本的核心逻辑**。
本仓库主要用于 **个人使用** 和维护，并根据实际使用需求补充 Forge/NeoForge 版本的修复与功能。
当前仅维护 Minecraft 1.20.1 和 1.21.1，各版本与加载器使用独立分支。

## 维护版本

| Minecraft | 加载器 | Java | 分支 |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.244 | 21 | [`neoforge/1.21.1-0.1.7`](https://github.com/IcedWatermelonJuice/DyDanmakuForge/tree/neoforge/1.21.1-0.1.7) |
| 1.21.1 | Forge 52.1.16 | 21 | [`forge/1.21.1-0.1.7`](https://github.com/IcedWatermelonJuice/DyDanmakuForge/tree/forge/1.21.1-0.1.7) |
| 1.20.1 | Forge 47.2.32 | 17 | [`forge/1.20.1-0.1.7`](https://github.com/IcedWatermelonJuice/DyDanmakuForge/tree/forge/1.20.1-0.1.7) |

其他 Minecraft 版本暂不在维护范围内。

## 下载与 Java 版本

Minecraft 1.20.1 版本的 Mod 基于 Java 17 构建，Minecraft 1.21.1 的 Forge 与 NeoForge 版本均基于 Java 21 构建。运行 Mod 时也建议为对应的 Minecraft/Forge/NeoForge 实例使用相同的 Java 版本，以避免类版本不兼容或其他运行问题。

## 使用方法

下载与 Minecraft 版本及加载器对应的 Mod，将 JAR 文件放入客户端的 `mods` 目录。该 Mod 仅需安装在客户端，服务器无需安装。

进入游戏后按 `F7` 打开控制界面。Mod 默认使用基于抖音官方 OpenAPI 的 bridge WSS 接入。原有输入房间号网页代理方式直连属于原仓库**遗留**能力，不进行维护，若直连出现问题，请联系 [Mod 原作者](https://github.com/tiangalon)。。

消息过滤命令：

```text
/dydanmaku filter <chat|member|stats|like|gift|fansclub|all>
/dydanmaku unfilter <chat|member|stats|like|gift|fansclub|all>
/dydanmaku filter status
```

`filter` 用于屏蔽消息类型，`unfilter` 用于恢复显示，`filter status` 用于查看当前允许显示的消息类型。

## 抖音官方 OpenAPI bridge WSS 接入（默认）

此功能用于合规接收弹幕玩法互动数据。**使用者（主播/玩法开发者）必须自行在[抖音开放平台](https://developer.open-douyin.com/)创建直播弹幕玩法，自行申请直播间评论、点赞、礼物、粉丝团等所需互动数据权限，并自行搭建、部署及维护 bridge WSS 服务。** 本项目不代申请权限、不提供公共 bridge，也不提供任何绕过抖音官方授权或风控的能力。

使用者自建的 bridge 负责通过抖音官方 OpenAPI 启动数据推送任务、接收官方回调，再通过 WSS 转发给 Mod。请参考抖音官方的[直播间评论互动能力](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/jierushuoming/hudongshuju/pinglunshuju)和[抖音云弹幕玩法接入指南](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/douyincloud/guide)，并自行确保玩法、bridge、数据处理与存储符合平台协议、隐私要求及适用法律法规。

可在游戏内“更多设置 → 接入设置”编辑以下配置：

```toml
Dy_Official_API = true            # 默认开启；false 时使用原有网页直连
Dy_Official_API_Endpoint = ""     # 使用者自建 bridge 的 wss:// 接入点
Dy_Official_API_Key = ""          # 通过 Authorization: Bearer <key> 发送
DySessionId = ""                  # 仅原有网页直连使用
```

bridge 可转发抖音官方开放数据格式的 `live_comment`、`live_like`、`live_gift`、`live_fansclub` 消息。Mod 收到数据后继续使用原有的消息类型开关、输出模板、关键词过滤、用户等级过滤与游戏内显示逻辑。

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

- 本项目不是抖音官方产品；“官方 OpenAPI 接入”表示数据来源与授权链路基于抖音开放平台官方 OpenAPI，并不表示本项目获得抖音官方背书。
- 如需了解原始 Forge 实现，请访问 [tiangalon/DyDanmakuForge](https://github.com/tiangalon/DyDanmakuForge)；如需了解当前功能基线，请访问 [tiangalon/DyDanmaku](https://github.com/tiangalon/DyDanmaku)。
