# Android 客户端设计文档

## 模块概述

Android 客户端是飞机大战游戏的用户交互界面，负责游戏渲染、用户输入处理、网络通信和本地数据持久化。

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│  (MainActivity, Fragments, UI Components)   │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│            Business Logic Layer             │
│  (Game Views, Audio Player, Managers)       │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│              Data Layer                     │
│  (Network, Database, Local Storage)         │
└─────────────────────────────────────────────┘
```

## 核心模块

### 1. UI 模块 (ui/)

#### 1.1 MainActivity
- **职责**: 应用主入口，管理 Fragment 切换
- **功能**:
  - Fragment 导航
  - WebSocket 连接管理
  - 全局状态管理
  - Toast 消息显示

#### 1.2 Fragments
- **MainMenuFragment**: 主菜单
- **LoginFragment**: 登录界面
- **RegisterFragment**: 注册界面
- **RoomsFragment**: 联机房间管理
- **PvpGameFragment**: 联机游戏界面
- **LeaderboardFragment**: 排行榜
- **ShopFragment**: 商城
- **SettingsFragment**: 设置（包含局域网扫描）

#### 1.3 UI 工具类
- **UiUtils**: 统一的 UI 组件创建工具
  - 创建按钮、文本、面板等
  - 统一样式管理
  - dp 转 px 工具

### 2. 游戏模块 (game/)

#### 2.1 游戏视图
- **AircraftWarSurfaceView**: 单机游戏视图
  - 游戏循环
  - 碰撞检测
  - 渲染管线
  
- **PvpBattleView**: 联机游戏视图
  - 状态同步渲染
  - 双人显示
  - 子弹轨迹渲染

#### 2.2 游戏组件
- **GameLoopThread**: 游戏循环线程
- **AndroidGameRenderer**: 游戏渲染器
- **BitmapSkinManager**: 皮肤管理器
- **GameViewport**: 视口管理

#### 2.3 游戏逻辑
```
游戏循环 (60 FPS):
1. 处理输入
2. 更新游戏状态
   - 移动实体
   - 检测碰撞
   - 生成敌机
   - 更新子弹
3. 渲染画面
4. 计算帧时间
```

### 3. 网络模块 (network/)

#### 3.1 HTTP 客户端
- **HttpApiClient**: REST API 调用
  - 用户注册/登录
  - 好友管理
  - 商城操作
  - 排行榜查询
  - 游戏结算

#### 3.2 WebSocket 客户端
- **连接管理**: 自动重连、心跳检测
- **消息类型**:
  - AUTH: 认证
  - MATCH_RANDOM: 随机匹配
  - CREATE_ROOM: 创建房间
  - JOIN_ROOM: 加入房间
  - START_GAME: 开始游戏
  - MOVE: 移动
  - FIRE: 开火
  - BATTLE_STATE: 战斗状态
  - GAME_OVER: 游戏结束

#### 3.3 局域网发现
- **LanServerScanner**: 局域网服务器扫描
  - UDP 广播
  - 服务器响应解析
  - 延迟测试

#### 3.4 网络执行器
- **NetworkExecutor**: 后台线程执行网络请求

### 4. 数据库模块 (database/)

#### 4.1 数据库设计

##### 游戏记录表 (game_records)
```sql
CREATE TABLE game_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    game_mode VARCHAR NOT NULL,      -- 'SINGLE', 'PVP', 'COOP'
    score INTEGER NOT NULL,
    coins INTEGER NOT NULL,
    duration INTEGER NOT NULL,        -- 游戏时长（秒）
    enemies_killed INTEGER NOT NULL,
    max_combo INTEGER NOT NULL,       -- 最大连击
    play_time DATETIME NOT NULL,      -- 游戏时间
    is_win BOOLEAN,                   -- 是否胜利（联机模式）
    room_id INTEGER                   -- 房间ID（联机模式）
);
```

##### 用户统计表 (user_stats)
```sql
CREATE TABLE user_stats (
    user_id INTEGER PRIMARY KEY,
    total_games INTEGER DEFAULT 0,
    total_score INTEGER DEFAULT 0,
    total_coins INTEGER DEFAULT 0,
    total_playtime INTEGER DEFAULT 0,
    best_score INTEGER DEFAULT 0,
    pvp_wins INTEGER DEFAULT 0,
    pvp_losses INTEGER DEFAULT 0,
    coop_games INTEGER DEFAULT 0,
    last_update DATETIME
);
```

##### 成就表 (achievements)
```sql
CREATE TABLE achievements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    achievement_type VARCHAR NOT NULL,
    achievement_value INTEGER NOT NULL,
    unlock_time DATETIME NOT NULL
);
```

#### 4.2 数据库助手
- **GameDatabaseHelper**: SQLite 数据库管理
  - 单例模式
  - CRUD 操作
  - 统计查询
  - 数据迁移

### 5. 音频模块 (audio/)

- **AndroidAudioPlayer**: 音频播放器
  - 背景音乐
  - 音效播放
  - 音量控制

### 6. 数据模型 (network/model/)

- **FriendRequestItem**: 好友请求
- **LeaderboardEntry**: 排行榜条目
- **ShopCatalog**: 商城目录
- **LocalInventorySnapshot**: 本地库存快照

## 数据流设计

### 单机游戏流程
```
1. 用户启动游戏
2. 加载游戏资源
3. 初始化游戏状态
4. 游戏循环:
   - 用户输入 → 更新状态 → 渲染
5. 游戏结束
6. 保存记录到本地数据库
7. 更新用户统计
```

### 联机游戏流程
```
1. 连接 WebSocket
2. 认证
3. 创建/加入房间
4. 等待对手
5. 开始游戏
6. 游戏循环:
   - 用户输入 → WebSocket → 服务器
   - 服务器 → WebSocket → 更新状态 → 渲染
7. 游戏结束
8. 结算 → HTTP API → 服务器
9. 保存记录到本地数据库
```

### 数据同步流程
```
1. 用户登录 → 获取服务器数据
2. 合并本地数据和服务器数据
3. 游戏过程中实时更新
4. 定期同步到服务器
5. 离线时保存到本地
6. 上线后批量同步
```

## 性能优化

### 1. 渲染优化
- 使用 SurfaceView 进行游戏渲染
- 双缓冲技术
- 脏矩形更新
- Bitmap 缓存和复用

### 2. 内存优化
- 及时释放 Bitmap
- 使用对象池
- 避免内存泄漏
- 监控内存使用

### 3. 网络优化
- 批量发送消息
- 压缩数据
- 减少不必要的请求
- 使用缓存

### 4. 数据库优化
- 使用索引
- 批量操作
- 异步读写
- 定期清理旧数据

## 用户体验设计

### 1. 界面设计
- 简洁直观的 UI
- 统一的视觉风格
- 流畅的动画效果
- 响应式布局

### 2. 交互设计
- 触摸控制优化
- 即时反馈
- 错误提示
- 加载状态显示

### 3. 音效设计
- 背景音乐
- 射击音效
- 爆炸音效
- UI 交互音效

## 错误处理

### 1. 网络错误
- 连接超时重试
- 断线重连
- 错误提示
- 降级处理

### 2. 数据错误
- 数据验证
- 异常捕获
- 日志记录
- 用户提示

### 3. 游戏错误
- 状态恢复
- 自动保存
- 崩溃报告

## 安全性

### 1. 数据安全
- 敏感数据加密
- 安全存储
- 防止注入攻击

### 2. 网络安全
- HTTPS 通信（待实现）
- 数据签名验证
- 防重放攻击

## 测试策略

### 1. 单元测试
- 工具类测试
- 数据模型测试
- 业务逻辑测试

### 2. UI 测试
- Espresso 测试
- 界面交互测试
- 兼容性测试

### 3. 性能测试
- 帧率测试
- 内存泄漏检测
- 网络延迟测试

## 构建配置

### Gradle 配置
```gradle
android {
    compileSdk 34
    defaultConfig {
        minSdk 26
        targetSdk 34
    }
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
        }
    }
}

dependencies {
    // Android 核心库
    implementation 'androidx.appcompat:appcompat:1.6.1'
    
    // 网络库
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'org.java-websocket:Java-WebSocket:1.5.3'
    
    // JSON 解析
    implementation 'org.json:json:20231013'
}
```

## 目录结构

```
android-client/app/src/main/
├── java/edu/hitsz/aircraftwar/android/
│   ├── audio/
│   │   └── AndroidAudioPlayer.java
│   ├── game/
│   │   ├── AircraftWarSurfaceView.java
│   │   ├── PvpBattleView.java
│   │   ├── AndroidGameRenderer.java
│   │   ├── BitmapSkinManager.java
│   │   ├── GameLoopThread.java
│   │   └── GameViewport.java
│   ├── network/
│   │   ├── HttpApiClient.java
│   │   ├── LanServerScanner.java
│   │   ├── NetworkExecutor.java
│   │   ├── ConnectionTester.java
│   │   ├── LocalInventoryManager.java
│   │   └── model/
│   │       ├── FriendRequestItem.java
│   │       ├── LeaderboardEntry.java
│   │       ├── ShopCatalog.java
│   │       └── LocalInventorySnapshot.java
│   ├── ui/
│   │   ├── MainActivity.java
│   │   ├── MainMenuFragment.java
│   │   ├── LoginFragment.java
│   │   ├── RegisterFragment.java
│   │   ├── RoomsFragment.java
│   │   ├── PvpGameFragment.java
│   │   ├── LeaderboardFragment.java
│   │   ├── ShopFragment.java
│   │   ├── SettingsFragment.java
│   │   ├── UiUtils.java
│   │   └── ShopItemVisuals.java
│   ├── database/
│   │   ├── GameDatabaseHelper.java
│   │   └── model/
│   │       ├── GameRecord.java
│   │       ├── UserStats.java
│   │       └── Achievement.java
│   └── MainActivity.java
├── res/
│   ├── drawable/        # 图片资源
│   ├── layout/          # 布局文件
│   ├── values/          # 值资源
│   └── raw/             # 音频文件
└── AndroidManifest.xml
```

## 未来改进

### 功能扩展
- [ ] 添加更多游戏模式
- [ ] 实现成就系统
- [ ] 添加社交分享
- [ ] 实现回放功能

### 性能优化
- [ ] 使用 OpenGL ES 渲染
- [ ] 实现资源预加载
- [ ] 优化网络协议
- [ ] 减少内存占用

### 用户体验
- [ ] 添加新手引导
- [ ] 优化触摸控制
- [ ] 添加更多音效
- [ ] 实现自定义皮肤

## 开发规范

### 命名规范
- Activity/Fragment: XxxActivity, XxxFragment
- View: XxxView
- Adapter: XxxAdapter
- Utils: XxxUtils
- 常量: UPPER_SNAKE_CASE
- 变量: lowerCamelCase

### 代码规范
- 每个类不超过 500 行
- 每个方法不超过 50 行
- 添加必要的注释
- 使用有意义的变量名

### 资源规范
- 布局文件: activity_xxx.xml, fragment_xxx.xml
- 图片资源: ic_xxx.png, bg_xxx.png
- 字符串: 使用 strings.xml
- 颜色: 使用 colors.xml
