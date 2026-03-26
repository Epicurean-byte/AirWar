# AirWar Server

飞机大战游戏服务端，基于 Spring Boot 3 实现好友系统、排行榜、双人联机和皮肤商城功能。

## 技术栈

| 依赖 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.2.3 | 框架基础 |
| Spring WebSocket | — | 双人联机实时同步 |
| Lombok | — | 简化样板代码 |
| FastJSON2 | 2.0.47 | WebSocket 消息解析 |
| Jackson | 内置 | HTTP JSON + 文件持久化 |

## 快速启动

**前提：JDK 17+、Maven 3.6+**

```bash
cd AirWarServer
mvn spring-boot:run
```

服务默认监听 `http://localhost:8080`，WebSocket 地址为 `ws://localhost:8080/ws/game`。

数据文件自动存储在启动目录下的 `./data/` 文件夹（可在 `application.yml` 中修改 `app.data-dir`）。

---

## 项目结构

```
src/main/java/com/planewar/server/
├── PlaneWarServerApplication.java      Spring Boot 启动类
├── config/
│   ├── WebSocketConfig.java            注册 /ws/game 端点
│   └── CorsConfig.java                 全局跨域（允许所有来源）
├── datastore/
│   └── InMemoryDataStore.java          内存数据库 + JSON 文件持久化
├── model/
│   ├── entity/
│   │   ├── User.java                   用户（金币、分数、皮肤、好友列表）
│   │   ├── FriendRequest.java          好友申请（PENDING/ACCEPTED/REJECTED）
│   │   ├── Room.java                   联机房间（WAITING/IN_GAME/FINISHED）
│   │   └── SkinConfig.java             商城皮肤配置
│   └── dto/
│       ├── ApiResponse.java            统一 HTTP 返回格式 {code, message, data}
│       └── WsMessageDto.java           WebSocket 消息体 {type, roomId, userId, payload}
├── controller/
│   ├── UserController.java             注册/登录/搜索/好友申请
│   ├── LeaderboardController.java      分数榜/金币榜（Top 50）
│   ├── ShopController.java             商城查询/购买/装备皮肤
│   └── GameSettleController.java       单人/PvP 结算
└── websocket/
    ├── MatchManager.java               匹配池、会话映射、房间调度
    └── GameWebSocketHandler.java       消息路由与转发
```

---

## HTTP 接口文档

所有接口返回统一格式：
```json
{ "code": 200, "message": "success", "data": { ... } }
```

### 用户系统 `/api/user`

| 方法 | 路径 | 说明 | 请求体 |
|---|---|---|---|
| POST | `/register` | 注册 | `{username, password, nickname?}` |
| POST | `/login` | 登录 | `{username, password}` |
| POST | `/logout` | 登出 | `?userId=` |
| GET | `/info` | 获取用户信息 | `?userId=` |
| GET | `/search` | 搜索玩家 | `?keyword=` |
| GET | `/friends` | 好友列表 | `?userId=` |
| POST | `/friend/request` | 发送好友申请 | `{fromUserId, toUserId}` |
| GET | `/friend/requests` | 收到的好友申请 | `?userId=` |
| POST | `/friend/respond` | 处理好友申请 | `{requestId, accept: true/false}` |

### 排行榜 `/api/leaderboard`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/score` | 历史最高分榜（Top 50） |
| GET | `/coins` | 累计金币榜（Top 50） |

### 商城 `/api/shop`

| 方法 | 路径 | 说明 | 请求体 |
|---|---|---|---|
| GET | `/info` | 查询商城（含余额、皮肤列表、已拥有标记） | `?userId=` |
| POST | `/buy` | 购买皮肤 | `{userId, skinId}` |
| POST | `/equip` | 装备皮肤 | `{userId, skinId}` |

### 游戏结算 `/api/game`

| 方法 | 路径 | 说明 | 请求体 |
|---|---|---|---|
| POST | `/settle/single` | 单人模式结算 | `{userId, score, coins}` |
| POST | `/settle/pvp` | 联机模式结算 | `{roomId, userId, score, coins}` |

> **胜负公式**：`最终成绩 = 10% × 分数 + 90% × 金币`，赢家额外奖励 200 金币。

---

## WebSocket 协议

连接地址：`ws://localhost:8080/ws/game`

所有消息均为 JSON，格式如下：
```json
{
  "type":    "消息类型",
  "roomId":  0,
  "userId":  12345,
  "payload": "{...}"
}
```

### 连接流程

```
客户端连接
    │
    ▼
发送 AUTH（必须是第一条消息）
    │
    ▼
收到 AUTH_OK
    │
    ├── 随机匹配 → MATCH_RANDOM → 等待 MATCH_SUCCESS
    │
    └── 好友房间 → CREATE_ROOM → 收到 ROOM_CREATED
                          │
                  对方发 JOIN_ROOM → 双方收到 ROOM_JOINED
                          │
                  房主发 START_GAME → 双方收到 GAME_START（含种子和难度）
```

### 消息类型一览

| 方向 | type | 说明 |
|---|---|---|
| 客→服 | `AUTH` | 认证，payload 无需填写，userId 填登录后的 ID |
| 客→服 | `MATCH_RANDOM` | 加入随机匹配池 |
| 客→服 | `CREATE_ROOM` | 创建好友房间 |
| 客→服 | `JOIN_ROOM` | 加入指定房间（roomId 填目标房间） |
| 客→服 | `START_GAME` | 房主开始游戏（roomId 填自己的房间） |
| 客→服 | `MOVE` | 上报玩家坐标（转发给对手） |
| 客→服 | `FIRE` | 上报开火事件（转发给对手） |
| 客→服 | `PICKUP` | 上报道具拾取（转发给对手） |
| 客→服 | `SCORE_UPDATE` | 上报分数/血量变化（广播到全房间） |
| 客→服 | `GAME_OVER` | 本方游戏结束（通知对手） |
| 服→客 | `AUTH_OK` | 认证成功 |
| 服→客 | `MATCH_SUCCESS` | 随机匹配成功，payload 含 roomId/player1Id/player2Id |
| 服→客 | `ROOM_CREATED` | 房间创建成功，payload 含 roomId |
| 服→客 | `ROOM_JOINED` | 有玩家加入房间 |
| 服→客 | `GAME_START` | 游戏开始，payload 含 seed 和 difficulty 配置 |
| 服→客 | `OPPONENT_DISCONNECTED` | 对手断线 |
| 服→客 | `ERROR` | 错误，payload 含 reason |

### GAME_START payload 示例

```json
{
  "roomId": 1,
  "seed": 1711234567890,
  "player1Id": 1,
  "player2Id": 2,
  "difficulty": {
    "enemyHpMultiplier": 1.5,
    "spawnRate": 1.3,
    "bulletDensity": 1.2
  }
}
```

---

## 数据持久化

| 文件 | 内容 | 时机 |
|---|---|---|
| `data/users.json` | 全部用户数据 | 注册、购买、装备、结算后立即写入 |
| `data/friend_requests.json` | 全部好友申请记录 | 发送申请、处理申请后立即写入 |

- 服务重启后自动从文件恢复数据，ID 生成器同步推进，不会重复。
- 重启后所有用户 `online` 字段自动重置为 `false`。
- 房间（`Room`）为会话级数据，不持久化。

---

## 商城皮肤列表（内置）

| skinId | 名称 | 价格 | 贴图资源名 |
|---|---|---|---|
| 0 | 默认战机 | 0（免费） | `plane_default` |
| 1 | 赤焰战机 | 500 金币 | `plane_red` |
| 2 | 幽灵战机 | 800 金币 | `plane_ghost` |
| 3 | 极光战机 | 1200 金币 | `plane_aurora` |

新增皮肤在 `InMemoryDataStore` 构造函数中的 `skinCatalog` 列表追加即可。

---

## 给客户端开发的接入注意事项

1. **登录后保存 userId**，后续所有接口和 WebSocket 消息都依赖它。
2. **WebSocket 首条消息必须是 `AUTH`**，否则后续操作会返回 `ERROR`。
3. **皮肤贴图**：根据 `equippedSkinId` 动态加载对应 `assetName` 的 Android 贴图资源。
4. **随机种子**：收到 `GAME_START` 后，双方用同一 `seed` 初始化随机数生成器，保证敌机生成序列一致。
5. **结算时序**：游戏结束后先调用 HTTP `/api/game/settle/pvp`（上报分数金币），再展示结算界面。
6. **好友列表**展示字段：`nickname`、`online`、`highScore`、`coins`、`equippedSkinId`。
