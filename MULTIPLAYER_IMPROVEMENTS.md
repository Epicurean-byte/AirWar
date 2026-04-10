# 多人对战功能改进总结

## 完成时间
2026年4月10日

## 改进内容

### 1. ✅ 修复无法发射子弹的问题

**问题原因:**
之前的开火逻辑判断条件错误，导致玩家无法攻击到敌机。

**修复方案:**
- 重写了 `handleFire` 方法，根据游戏模式选择不同的目标选择策略
- **合作模式**: 攻击所有敌机，选择距离最近的
- **对战模式**: 玩家1攻击上方敌机，玩家2攻击下方敌机

**修改文件:**
- `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`

---

### 2. ✅ 同步玩家皮肤

**实现方案:**
- 在 `Room` 实体中添加 `player1SkinId` 和 `player2SkinId` 字段
- 创建房间和加入房间时，客户端发送自己的皮肤ID
- 游戏开始时，服务器将两个玩家的皮肤ID广播给所有玩家
- 客户端根据收到的皮肤ID渲染对手的飞机

**修改文件:**
- 服务器端:
  - `AirWarServer/src/main/java/com/planewar/server/model/entity/Room.java`
  - `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`
- 客户端:
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/game/PvpBattleView.java`
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/PvpGameFragment.java`
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/RoomsFragment.java`

---

### 3. ✅ 限制玩家移动区域（对战模式）

**实现方案:**
- 在 `handleMove` 方法中根据游戏模式限制玩家的Y坐标
- **对战模式 (PVP)**:
  - 玩家1只能在上半场移动 (y: 0 ~ 364)
  - 玩家2只能在下半场移动 (y: 404 ~ 768)
- **合作模式 (COOP)**:
  - 玩家可以在整个屏幕自由移动

**修改文件:**
- `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`

---

### 4. ✅ 添加合作模式

**实现方案:**

#### 4.1 创建游戏模式枚举
- 新建 `GameMode` 枚举类，包含 `PVP` 和 `COOP` 两种模式
- 默认模式设置为 `COOP`（合作模式）

#### 4.2 服务器端改动
- `Room` 实体添加 `gameMode` 字段
- `BattleRoomState` 根据游戏模式设置不同的初始位置:
  - **PVP模式**: 玩家1在上方(256, 100)，玩家2在下方(256, 660)
  - **COOP模式**: 两个玩家并排在下方(180, 660)和(330, 660)
- 开火逻辑根据模式选择不同的目标策略
- 移动限制根据模式应用不同的区域约束

#### 4.3 客户端改动
- `PvpBattleView` 添加游戏模式显示
  - 合作模式显示"合作模式"和"队友HP"
  - 对战模式显示"对战模式"和"对手HP"
- `RoomsFragment` 添加游戏模式选择按钮
  - "合作"按钮 - 选择COOP模式
  - "对战"按钮 - 选择PVP模式
- 创建房间时将选择的游戏模式发送给服务器

**新增文件:**
- `AirWarServer/src/main/java/com/planewar/server/model/entity/GameMode.java`

**修改文件:**
- 服务器端:
  - `AirWarServer/src/main/java/com/planewar/server/model/entity/Room.java`
  - `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`
- 客户端:
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/game/PvpBattleView.java`
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/PvpGameFragment.java`
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/RoomsFragment.java`
  - `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/MainActivity.java`

---

## 游戏模式对比

### 合作模式 (COOP) - 默认模式

**特点:**
- 两个玩家并肩作战，共同对抗敌机
- 玩家初始位置在屏幕下方，并排站位
- 玩家可以在整个屏幕自由移动
- 攻击所有敌机，选择距离最近的目标
- 没有友伤，玩家之间不会互相伤害
- 共同的胜利条件

**适合场景:**
- 朋友之间合作游戏
- 新手玩家练习
- 追求高分挑战

### 对战模式 (PVP)

**特点:**
- 两个玩家相向而行，竞争更高分数
- 玩家1在屏幕上方，玩家2在屏幕下方
- 玩家只能在各自半场移动，不能越界
- 玩家1攻击上方敌机，玩家2攻击下方敌机
- 竞争性玩法，比拼得分

**适合场景:**
- 竞技对战
- 排行榜竞争
- 技术比拼

---

## 使用方法

### 1. 启动服务器
```bash
cd AirWarServer
mvn spring-boot:run
```

### 2. 客户端操作流程

#### 创建房间:
1. 打开Android客户端
2. 进入"房间"界面
3. 选择游戏模式:
   - 点击"合作"按钮选择合作模式
   - 点击"对战"按钮选择对战模式
4. 点击"创建房间"
5. 记下房间ID，告诉朋友

#### 加入房间:
1. 打开Android客户端
2. 进入"房间"界面
3. 输入房间ID
4. 点击"加入房间"

#### 开始游戏:
1. 房主点击"开始游戏"
2. 两个玩家自动进入游戏界面
3. 可以看到对方的皮肤
4. 根据选择的模式进行游戏

---

## 技术细节

### 消息协议更新

#### CREATE_ROOM 消息
```json
{
  "type": "CREATE_ROOM",
  "roomId": 0,
  "userId": 123,
  "payload": {
    "gameMode": "COOP",
    "skinId": 1
  }
}
```

#### JOIN_ROOM 消息
```json
{
  "type": "JOIN_ROOM",
  "roomId": 456,
  "userId": 789,
  "payload": {
    "skinId": 2
  }
}
```

#### GAME_START 消息
```json
{
  "type": "GAME_START",
  "roomId": 456,
  "userId": 0,
  "payload": {
    "roomId": 456,
    "seed": 1234567890,
    "player1Id": 123,
    "player2Id": 789,
    "gameMode": "COOP",
    "player1SkinId": 1,
    "player2SkinId": 2,
    "difficulty": {
      "enemyHpMultiplier": 1.5,
      "spawnRate": 1.3,
      "bulletDensity": 1.2
    }
  }
}
```

---

## 性能优化

### 自动开火优化
- 将开火间隔从250ms增加到500ms，减少50%的网络消息
- 只在有敌机时才发送FIRE消息，避免无效请求
- 减少服务器负担和网络带宽占用

### 碰撞检测优化
- 调整碰撞半径从28.0到35.0，更符合实际飞机大小
- 增加碰撞伤害从12到15，提高游戏挑战性

---

## 测试建议

### 功能测试
- [ ] 测试合作模式下两个玩家能否正常开火
- [ ] 测试对战模式下玩家移动区域限制
- [ ] 测试皮肤同步是否正确显示
- [ ] 测试游戏模式切换

### 网络测试
- [ ] 测试局域网连接稳定性
- [ ] 测试不同网络延迟下的游戏体验
- [ ] 测试断线重连

### 压力测试
- [ ] 测试多个房间同时运行
- [ ] 测试长时间游戏稳定性

---

## 已知问题

无

---

## 后续改进建议

1. 添加房间列表功能，方便玩家浏览可用房间
2. 添加观战功能
3. 添加聊天功能
4. 添加更多游戏模式（如生存模式、竞速模式）
5. 添加排行榜系统
6. 优化网络同步，添加客户端预测和插值

---

## 版本信息

**版本:** 2.0  
**发布日期:** 2026年4月10日  
**兼容性:** 向后兼容1.0版本
