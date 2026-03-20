# AircraftWar

当前仓库同时保留了两套代码：

- `src/edu/hitsz/...`
  旧的 Java PC 单机版 Swing 实现，作为参考基线保留。
- `game-core/`
  新拆分出的纯 Java 规则层，不依赖 Swing、AWT、图片文件路径或桌面音频 API。
- `android-client/app/`
  新增的 Android 客户端骨架，使用 `Activity + Fragment + SurfaceView + GameLoop` 接入 `game-core`。

## 已完成的拆分

`game-core` 已经抽出了以下内容：

- 飞行对象、飞机、子弹、道具实体
- 子弹工厂、敌机工厂、道具工厂
- 炸弹事件发布机制
- 难度模式与动态参数调整
- 游戏主循环状态 `GameWorld`
- 面向渲染层的快照对象 `GameSnapshot`

核心层现在通过 `SpriteId` 和 `GameSessionConfig` 处理对象尺寸和世界边界，不再依赖 `BufferedImage` 和 `Main.WINDOW_WIDTH/HEIGHT`。

## Android 客户端骨架

`android-client` 当前包含：

- `MainActivity`
- `LoginFragment`
- `MainMenuFragment`
- `FeatureFragment`
- `GameFragment`
- `AircraftWarSurfaceView`
- `GameLoopThread`
- `AndroidGameRenderer`
- `BitmapSkinManager`
- `AndroidAudioPlayer`

当前资源全部走 `res/drawable` 占位图，方便后续替换为真实皮肤资源。

## 当前限制

- 本机环境没有 Android SDK / Gradle Wrapper，因此这次只验证了 `game-core` 的 `javac` 编译。
- `AndroidAudioPlayer` 已接入 `SoundPool` 和 `MediaPlayer`，但还没有绑定真实 `res/raw` 音频资源。
- 好友、商城、房间仍是 Android 端占位页面，后续接 Spring Boot 内存服务即可。
