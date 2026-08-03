# DyDanmaku Forge 1.20.1

在 Minecraft 客户端聊天框中显示抖音直播间消息。本分支面向 Minecraft 1.20.1，并与 DyDanmaku Fabric `0.1.7` 的功能保持一致。

## 环境

- Minecraft 1.20.1
- Forge 47.2.32 或更高版本
- Java 17

## 使用

将 `DyDanmaku-forge-1.20.1-0.1.7.2.jar` 放入客户端的 `mods` 目录。进入游戏后按 `F7` 打开控制界面。Mod 默认启用基于抖音官方 OpenAPI 的 bridge WSS 接入；原有的直播间 URL/房间号直连能力仍然保留，可在“更多设置 → 接入设置”中关闭官方接入后使用。

## 抖音官方 OpenAPI bridge WSS 接入（默认）

此模式用于合规接收弹幕玩法互动数据。**使用者（主播/玩法开发者）必须自行在[抖音开放平台](https://developer.open-douyin.com/)创建并申请直播弹幕玩法，申请所需的直播间评论、点赞、礼物、粉丝团等互动数据权限，并自行搭建、部署和维护 bridge WSS 服务。** 本项目不代申请权限、不提供公共 bridge，也不提供绕过抖音官方授权或风控的能力。

bridge 负责通过抖音官方 OpenAPI 启动直播间数据推送任务、接收官方回调，再把互动数据通过 WSS 转发给本 Mod。可参考抖音官方文档：[直播间评论互动能力](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/jierushuoming/hudongshuju/pinglunshuju)及[抖音云弹幕玩法接入指南](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/douyincloud/guide)。使用者还应自行确保玩法、bridge、数据处理和存储符合抖音开放平台协议、隐私要求及适用法律法规。

在“更多设置 → 接入设置”中填写：

- `Dy_Official_API`：是否使用官方 OpenAPI bridge，默认 `true`。
- `Dy_Official_API_Endpoint`：使用者自建 bridge 的 `wss://` 接入点。
- `Dy_Official_API_Key`：bridge 鉴权 key；Mod 通过 `Authorization: Bearer <key>` 握手头发送，请勿公开或提交真实 key。

bridge 下行消息应使用抖音官方开放数据字段（例如 `msg_type_str` 为 `live_comment`、`live_like`、`live_gift` 或 `live_fansclub`）。Mod 支持单条 JSON、JSON 数组、`{"payload": [...]}`，以及抖音云示例中 `{"msg_type": "...", "data": "[...]"}` 的包装形式。收到消息后会继续使用原有的消息类型开关、输出模板、关键词过滤、用户等级过滤及游戏内显示逻辑。

首次启用时必须先配置有效的 WSS 接入点和 key，否则连接会给出明确提示。官方模式下无需在主界面输入直播间房间号，直播间与主播身份由使用者的玩法和 bridge 负责绑定。

## 原有网页直连接入

在“更多设置 → 接入设置”关闭“抖音官方 OpenAPI bridge”后，Mod 会继续使用原有直连实现，输入抖音直播间 URL 或房间号即可连接。此模式没有被删除。

输入可以是 URL 或者房间号。示例 URL：`https://live.douyin.com/594357732923`；示例房间号：`594357732923`。

也可以使用客户端命令：

```text
/dydanmaku connect <直播间 URL 或房间号>
/dydanmaku disconnect
/dydanmaku status
```

控制界面的消息类型复选框默认全部勾选，分别控制消息、入场、统计、点赞、礼物和粉丝团消息。选择会保存到 `DyDanmakuSettings.toml`。也可以使用命令调整过滤：

点击控制界面的“更多设置”可在“接入设置”页切换官方 bridge/原有直连并编辑 WSS 接入点、key 和 `DySessionId`，也可在“弹幕过滤”页切换弹幕关键词过滤器的关闭、黑名单、白名单模式，以及新增、修改、删除过滤关键词。接入配置在下次连接时生效。

```text
/dydanmaku filter <chat|member|stats|like|gift|fansclub|all>
/dydanmaku unfilter <chat|member|stats|like|gift|fansclub|all>
/dydanmaku filter status
```

`filter` 表示屏蔽，`unfilter` 表示恢复显示；`all` 表示全部类型。`filter status` 会显示当前允许的消息类型。

首次启动会创建 `config/dydanmaku/DyDanmakuSettings.toml`，可配置官方 OpenAPI bridge 开关、接入点、key、`DySessionId`、消息类型开关、关键词过滤、用户等级过滤和输出模板。默认配置如下：

```toml
Dy_Official_API = true
Dy_Official_API_Endpoint = ""
Dy_Official_API_Key = ""
DySessionId = ""
```

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
build/libs/DyDanmaku-forge-1.20.1-0.1.7.2.jar
```

构建过程中还会生成带 `-slim.jar` 后缀的中间包。该文件不含第三方依赖，不要作为游戏安装包使用。所有 JAR 均已由 `.gitignore` 排除。

## 说明

- Mod 仅在客户端加载，不要求服务器安装。
- Protobuf、TOML 和所需的 Netty HTTP 类已包含在发行 JAR 中；Nashorn 及其 ASM 依赖复用 Forge 自带版本，避免 Java 模块冲突。
- 抖音网页结构和签名算法可能随平台更新而变化；连接失败时请查看游戏日志中的 `[DyDanmaku]` 信息。
