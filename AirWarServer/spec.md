│
├── pom.xml                                  <-- Maven 依赖配置（包含 Web, WebSocket, Lombok 等）
│
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── planewar
    │   │           └── server
    │   │               ├── PlaneWarServerApplication.java  <-- Spring Boot 启动类
    │   │               │
    │   │               ├── config/                         <-- 【配置层】
    │   │               │   ├── WebSocketConfig.java        <-- 注册 WebSocket 端点及拦截器
    │   │               │   └── CorsConfig.java             <-- 跨域配置（方便后期联调）
    │   │               │
    │   │               ├── datastore/                      <-- 【核心存储层】（纯内存数据库）
    │   │               │   └── InMemoryDataStore.java      <-- 全局单例，包含 ConcurrentHashMap 等
    │   │               │
    │   │               ├── model/                          <-- 【数据模型层】
    │   │               │   ├── entity/
    │   │               │   │   ├── User.java               <-- 用户实体（金币、分数、皮肤）
    │   │               │   │   ├── FriendRequest.java      <-- 好友申请实体
    │   │               │   │   ├── Room.java               <-- 联机房间实体（记录玩家1, 玩家2及对战状态）
    │   │               │   │   └── SkinConfig.java         <-- 商城皮肤配置实体
    │   │               │   └── dto/
    │   │               │       ├── ApiResponse.java        <-- 统一 JSON 返回格式封装
    │   │               │       └── WsMessageDto.java       <-- WebSocket 通讯消息的统一封装体
    │   │               │
    │   │               ├── controller/                     <-- 【HTTP 接口层】（第一部分：常规业务）
    │   │               │   ├── UserController.java         <-- 注册、登录、搜索、好友申请相关接口
    │   │               │   ├── LeaderboardController.java  <-- 分数榜、金币榜接口
    │   │               │   ├── ShopController.java         <-- 商城查询、购买、装备皮肤接口
    │   │               │   └── GameSettleController.java   <-- 联机对战结束后的结算上报接口
    │   │               │
    │   │               └── websocket/                      <-- 【WebSocket 长连接层】（第二部分：对战同步）
    │   │                   ├── GameWebSocketHandler.java   <-- 处理坐标移动、子弹/敌机同步、伤害事件
    │   │                   └── MatchManager.java           <-- 匹配池管理（随机匹配/好友开房调度逻辑）
    │   │
    │   └── resources
    │       ├── application.yml                             <-- Spring Boot 核心配置文件
    │       └── static/                                     <-- （可选）存放默认皮肤图标等静态资源
    │
    └── test                                                <-- 单元测试目录


网络模块 (Network Module)

**核心策略：**服务端推荐使用 Spring Boot，客户端采用标准 HTTP 接口处理常规业务，使用 WebSocket 处理高频实时的对战状态同步。

### 1. 好友系统 (Friend System)
- **技术栈：** HTTP 接口。
- **功能：** 用户注册/登录、搜索玩家、发送/接收/同意/拒绝好友申请。
- **数据结构：** 在服务端内存中维护 `用户列表`、`好友申请表`、`好友关系映射(Map)`。
- **展示：** 在 Android 端好友列表中展示好友昵称、在线状态、历史最高分、金币数。

### 2. 双维度排行榜系统 (Leaderboard System)
- **技术栈：** HTTP 接口。
- **功能扩展：** 游戏引入“击毁敌机随机掉落金币”机制。每局结束后客户端上传得分与金币。
- **两个榜单：**
  - 分数排行榜：按单局最高分或历史累计最高分排序。
  - 金币排行榜：按累计获得金币总数排序。
- **逻辑：** 服务端接收数据，校验合法性后更新内存中的玩家数据，对列表进行降序排序后返回 Top N。

### 3. 双人联机竞争模式 (Co-op PvP Mode)
- **技术栈：** WebSocket 长连接 + 房间制。
- **房间机制：** 支持邀请好友或随机匹配，满 2 人由房主开始游戏。服务端下发初始化指令与随机种子/难度配置。
- **实时同步 (WebSocket)：**
  - 客户端上报：玩家移动坐标、开火、道具拾取。
  - 服务端统一下发：敌机/Boss的生成与移动逻辑、金币/道具掉落、双方分数/血量状态。
- **竞争与结算规则：** 
  - 玩家在同一战场共享敌机，但相互独立获取分数和金币。
  - 难度会随联机模式动态调高（提高敌机血量、刷新率、子弹密度等）。
  - **胜负判定公式：** `最终排行成绩 = 10% * 分数 + 90% * 金币`，赢家将获得额外金币奖励。本局成绩同步更新至全局排行榜。

### 4. 在线金币商城与皮肤购买系统 (Online Shop & Skin System)
- **技术栈：** HTTP 接口，JSON 数据交互。
- **功能：** 消耗游戏内积累的金币购买飞机皮肤，并在个人界面装备。
- **逻辑链路：**
  1. 获取商城数据：返回用户余额、可购皮肤列表、已拥有皮肤。
  2. 购买校验：检查用户是否登录、皮肤是否存在、是否已拥有、余额是否充足。扣款并在内存中记录购买状态。
  3. 装备与同步：玩家选择皮肤后，服务端更新其当前装备状态；玩家每次进入游戏前拉取装备状态，以动态加载对应的 Android 贴图资源。
- **联动：** 装备的皮肤需要在好友列表和双人联机房间中向其他玩家展示。
