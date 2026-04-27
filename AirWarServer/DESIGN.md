# 服务器端设计文档

## 模块概述

服务器端基于 Spring Boot 框架，提供 REST API 和 WebSocket 服务，负责用户管理、游戏房间管理、实时游戏状态同步和数据存储。

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────────┐
│          Controller Layer                   │
│  (REST Controllers, WebSocket Handler)      │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│           Service Layer                     │
│  (Business Logic, Game Logic)               │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│            Data Layer                       │
│  (InMemoryDataStore, File Storage)          │
└─────────────────────────────────────────────┘
```

## 核心模块

### 1. 配置模块 (config/)

#### 1.1 CorsConfig
- **职责**: 跨域资源共享配置
- **功能**:
  - 允许所有来源访问
  - 支持所有 HTTP 方法
  - 允许携带凭证

#### 1.2 WebSocketConfig
- **职责**: WebSocket 配置
- **功能**:
  - 注册 WebSocket 端点
  - 配置握手拦截器
  - 设置消息大小限制

#### 1.3 ServerStartupListener
- **职责**: 服务器启动监听
- **功能**:
  - 打印服务器信息
  - 显示访问地址
  - 初始化数据

### 2. 控制器模块 (controller/)

#### 2.1 UserController
- **端点**:
  - `POST /api/user/register`: 用户注册
  - `POST /api/user/login`: 用户登录
  - `GET /api/user/{id}`: 获取用户信息
  - `POST /api/user/friend/request`: 发送好友请求
  - `POST /api/user/friend/accept`: 接受好友请求
  - `GET /api/user/{id}/friends`: 获取好友列表
  - `GET /api/user/{id}/friend-requests`: 获取好友请求列表

#### 2.2 ShopController
- **端点**:
  - `GET /api/shop/catalog`: 获取商城目录
  - `POST /api/shop/buy`: 购买物品
  - `GET /api/shop/inventory/{userId}`: 获取用户库存
  - `POST /api/shop/equip`: 装备皮肤

#### 2.3 LeaderboardController
- **端点**:
  - `GET /api/leaderboard/global`: 全局排行榜
  - `GET /api/leaderboard/friends/{userId}`: 好友排行榜

#### 2.4 GameSettleController
- **端点**:
  - `POST /api/game/settle/single`: 单机游戏结算
  - `POST /api/game/settle/pvp`: 联机游戏结算

### 3. WebSocket 模块 (websocket/)

#### 3.1 GameWebSocketHandler
- **职责**: 处理 WebSocket 连接和消息
- **消息类型**:
  - `AUTH`: 用户认证
  - `MATCH_RANDOM`: 随机匹配
  - `CREATE_ROOM`: 创建房间
  - `JOIN_ROOM`: 加入房间
  - `START_GAME`: 开始游戏
  - `MOVE`: 玩家移动
  - `FIRE`: 玩家开火
  - `GAME_OVER`: 游戏结束

#### 3.2 MatchManager
- **职责**: 管理游戏匹配和房间
- **功能**:
  - 用户会话管理
  - 随机匹配
  - 房间创建和管理
  - 游戏开始控制

#### 3.3 游戏状态管理

##### BattleRoomState
```java
class BattleRoomState {
    long roomId;
    long player1Id;
    long player2Id;
    GameMode gameMode;  // PVP 或 COOP
    Map<Long, PlayerState> players;
    Map<Integer, EnemyState> enemies;
    Map<Integer, BulletState> bullets;
    Random random;
    boolean finished;
    int tickCount;
}
```

##### 游戏循环
```
每 50ms 执行一次 (20 TPS):
1. 生成敌机
2. 更新敌机位置
3. 更新子弹位置
4. 碰撞检测:
   - 玩家与敌机
   - 子弹与敌机
5. 清理离屏实体
6. 广播游戏状态
7. 检查游戏结束条件
```

### 4. 数据模型 (model/)

#### 4.1 实体类 (entity/)

##### User
```java
class User {
    long userId;
    String username;
    String password;
    long totalScore;
    long totalCoins;
    int equippedSkinId;
    boolean online;
    Set<Long> friendIds;
    List<Integer> ownedSkinIds;
}
```

##### Room
```java
class Room {
    long id;
    long player1Id;
    long player2Id;
    State state;  // WAITING, PLAYING, FINISHED
    GameMode gameMode;  // PVP, COOP
    long gameSeed;
    int player1SkinId;
    int player2SkinId;
}
```

##### FriendRequest
```java
class FriendRequest {
    long id;
    long fromUserId;
    long toUserId;
    Status status;  // PENDING, ACCEPTED, REJECTED
    long timestamp;
}
```

##### SkinConfig
```java
class SkinConfig {
    int skinId;
    String name;
    String description;
    int price;
    String imageUrl;
}
```

##### GameMode
```java
enum GameMode {
    PVP,   // 对战模式
    COOP   // 合作模式
}
```

#### 4.2 DTO 类 (dto/)

##### ApiResponse
```java
class ApiResponse<T> {
    boolean success;
    String message;
    T data;
}
```

##### WsMessageDto
```java
class WsMessageDto {
    String type;
    long roomId;
    long userId;
    String payload;  // JSON 字符串
}
```

### 5. 数据存储 (datastore/)

#### InMemoryDataStore
- **职责**: 内存数据存储
- **数据结构**:
  - `Map<Long, User> users`: 用户数据
  - `Map<Long, Room> rooms`: 房间数据
  - `List<FriendRequest> friendRequests`: 好友请求
  - `List<SkinConfig> skinCatalog`: 皮肤目录

- **持久化**:
  - 定期保存到 JSON 文件
  - 启动时从文件加载
  - 文件位置: `data/users.json`, `data/friend_requests.json`

## 游戏逻辑设计

### 1. 匹配系统

#### 随机匹配流程
```
1. 用户发送 MATCH_RANDOM
2. 检查是否有等待中的房间
3. 如果有:
   - 加入房间
   - 通知双方匹配成功
4. 如果没有:
   - 创建新房间
   - 返回等待状态
```

#### 房间管理
```
房间状态转换:
WAITING → PLAYING → FINISHED

创建房间:
1. 生成唯一房间 ID
2. 设置房主
3. 设置游戏模式
4. 保存房间信息

加入房间:
1. 验证房间存在
2. 验证房间未满
3. 添加玩家2
4. 通知双方

开始游戏:
1. 验证房间已满
2. 验证请求者是房主
3. 生成游戏种子
4. 创建战斗状态
5. 广播游戏开始
```

### 2. 游戏同步

#### 状态广播
```
每 50ms 广播一次:
{
  "tick": 123,
  "roomId": 456,
  "players": [
    {"userId": 1, "x": 256, "y": 600, "hp": 80, "score": 1000, "coins": 50},
    {"userId": 2, "x": 300, "y": 650, "hp": 100, "score": 800, "coins": 30}
  ],
  "enemies": [
    {"id": 1, "type": 0, "x": 200, "y": 100, "hp": 50},
    {"id": 2, "type": 1, "x": 350, "y": 150, "hp": 80}
  ],
  "bullets": [
    {"id": 1, "ownerId": 1, "x": 256, "y": 550},
    {"id": 2, "ownerId": 2, "x": 300, "y": 600}
  ]
}
```

#### 输入处理
```
客户端输入 → WebSocket → 服务器:
1. MOVE: 更新玩家位置
2. FIRE: 创建子弹
3. 服务器验证输入合法性
4. 更新游戏状态
5. 下一个 tick 广播
```

### 3. 碰撞检测

#### 玩家与敌机碰撞
```java
for (Enemy enemy : enemies) {
    for (Player player : players) {
        if (distance(enemy, player) < COLLISION_RADIUS) {
            player.hp -= COLLISION_DAMAGE;
            removeEnemy(enemy);
        }
    }
}
```

#### 子弹与敌机碰撞
```java
for (Bullet bullet : bullets) {
    for (Enemy enemy : enemies) {
        if (distance(bullet, enemy) < HIT_RADIUS) {
            enemy.hp -= BULLET_DAMAGE;
            removeBullet(bullet);
            if (enemy.hp <= 0) {
                player.score += enemy.scoreValue;
                player.coins += randomCoins();
                removeEnemy(enemy);
            }
        }
    }
}
```

### 4. 游戏模式

#### PVP 模式
- 玩家1在屏幕上半部分（y < 384）
- 玩家2在屏幕下半部分（y > 384）
- 各自击杀敌机获得分数
- 游戏结束时比较分数

#### COOP 模式
- 两个玩家并排在屏幕下方
- 共同对抗敌机
- 分数和金币独立计算
- 任一玩家死亡游戏结束

## 性能优化

### 1. 并发处理
- 使用 `ConcurrentHashMap` 存储游戏状态
- 游戏循环使用单独的线程池
- WebSocket 消息异步处理

### 2. 内存管理
- 定期清理已结束的房间
- 限制最大房间数量
- 限制最大敌机数量

### 3. 网络优化
- 固定频率广播（50ms）
- 批量发送消息
- 压缩 JSON 数据

## 安全性

### 1. 认证授权
- WebSocket 连接前必须认证
- 每个操作验证用户身份
- 防止越权操作

### 2. 输入验证
- 验证所有客户端输入
- 防止注入攻击
- 限制输入范围

### 3. 防作弊
- 服务器权威
- 验证移动合法性
- 检测异常行为

## 配置文件

### application.yml
```yaml
server:
  port: 18080
  address: 0.0.0.0

spring:
  application:
    name: planewar-server
  websocket:
    max-text-message-size: 65536
    max-binary-message-size: 65536

game:
  world:
    width: 512
    height: 768
  tick-rate: 20  # 20 TPS
  max-enemies: 12
```

## 数据持久化

### 文件存储
```
data/
├── users.json           # 用户数据
└── friend_requests.json # 好友请求
```

### 数据格式
```json
{
  "users": [
    {
      "userId": 1,
      "username": "player1",
      "password": "hashed_password",
      "totalScore": 10000,
      "totalCoins": 500,
      "equippedSkinId": 0,
      "online": false,
      "friendIds": [2, 3],
      "ownedSkinIds": [0, 1, 2]
    }
  ]
}
```

## API 文档

### REST API

#### 用户注册
```
POST /api/user/register
Content-Type: application/json

Request:
{
  "username": "player1",
  "password": "password123"
}

Response:
{
  "success": true,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "player1",
    "totalScore": 0,
    "totalCoins": 0
  }
}
```

#### 用户登录
```
POST /api/user/login
Content-Type: application/json

Request:
{
  "username": "player1",
  "password": "password123"
}

Response:
{
  "success": true,
  "message": "登录成功",
  "data": {
    "userId": 1,
    "username": "player1",
    "totalScore": 10000,
    "totalCoins": 500,
    "equippedSkinId": 0,
    "ownedSkinIds": [0, 1, 2]
  }
}
```

### WebSocket API

#### 连接
```
ws://localhost:18080/game-ws
```

#### 认证
```json
{
  "type": "AUTH",
  "userId": 1,
  "roomId": 0,
  "payload": ""
}
```

#### 创建房间
```json
{
  "type": "CREATE_ROOM",
  "userId": 1,
  "roomId": 0,
  "payload": "{\"gameMode\":\"COOP\",\"skinId\":0}"
}
```

## 错误处理

### HTTP 错误
- 400: 请求参数错误
- 401: 未认证
- 404: 资源不存在
- 500: 服务器内部错误

### WebSocket 错误
```json
{
  "type": "ERROR",
  "roomId": 0,
  "userId": 0,
  "payload": "{\"reason\":\"invalid message\"}"
}
```

## 日志记录

### 日志级别
- DEBUG: 详细调试信息
- INFO: 一般信息
- WARN: 警告信息
- ERROR: 错误信息

### 日志内容
- 用户操作
- 游戏事件
- 错误异常
- 性能指标

## 监控指标

### 性能指标
- 在线用户数
- 活跃房间数
- 平均响应时间
- 游戏 TPS

### 业务指标
- 注册用户数
- 游戏场次
- 平均游戏时长
- 商城交易量

## 部署

### 构建
```bash
mvn clean package
```

### 运行
```bash
java -jar target/planewar-server.jar
```

### Docker 部署（待实现）
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/planewar-server.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 未来改进

### 功能扩展
- [ ] 数据库持久化（MySQL/PostgreSQL）
- [ ] Redis 缓存
- [ ] 消息队列（RabbitMQ/Kafka）
- [ ] 微服务架构

### 性能优化
- [ ] 负载均衡
- [ ] 分布式部署
- [ ] CDN 加速
- [ ] 数据库优化

### 安全增强
- [ ] HTTPS 支持
- [ ] JWT 认证
- [ ] 密码加密
- [ ] 防 DDoS

## 开发规范

### 代码规范
- 遵循 Spring Boot 最佳实践
- 使用 Lombok 简化代码
- 添加必要的注释
- 单元测试覆盖率 > 80%

### API 规范
- RESTful 设计
- 统一响应格式
- 版本控制
- 完整的文档

### Git 规范
- 功能分支开发
- 代码审查
- 持续集成
