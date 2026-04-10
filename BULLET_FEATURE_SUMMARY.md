# 联机模式子弹轨迹功能实现总结

## 实现时间
2026年4月10日

## 功能描述

为联机对战模式添加了可见的子弹发射轨迹，与单机模式保持一致的视觉效果。玩家开火时会发射子弹，子弹向上飞行并与敌机进行碰撞检测。

## 实现方案

### 1. 服务器端实现

#### 1.1 添加子弹状态类
```java
private static final class BulletState {
    private final int id;
    private final long ownerId; // 发射子弹的玩家ID
    private float x;
    private float y;
    private final float speedY;
}
```

#### 1.2 在BattleRoomState中管理子弹
- 添加 `ConcurrentHashMap<Integer, BulletState> bullets` 存储所有子弹
- 添加 `nextBulletId` 计数器生成唯一子弹ID

#### 1.3 修改开火逻辑
**之前**: 开火时直接计算并击中最近的敌机（即时命中）  
**现在**: 开火时创建子弹对象，子弹从玩家位置发射

```java
private void handleFire(WebSocketSession session) {
    // 创建子弹
    int bulletId = battle.nextBulletId++;
    float bulletX = player.x;
    float bulletY = player.y - 30; // 从飞机前方发射
    float bulletSpeed = -10f; // 向上飞行（y减小）
    
    battle.bullets.put(bulletId, new BulletState(bulletId, userId, bulletX, bulletY, bulletSpeed));
}
```

#### 1.4 在游戏循环中更新子弹
在 `tickBattles` 方法中添加子弹更新逻辑：
- 更新子弹位置（每tick移动）
- 检测子弹是否飞出屏幕（移除）
- 检测子弹与敌机的碰撞
- 碰撞时扣除敌机HP，移除子弹
- 敌机被击毁时给发射者加分

```java
// 更新子弹位置并检测碰撞
Iterator<BulletState> bulletIt = battle.bullets.values().iterator();
while (bulletIt.hasNext()) {
    BulletState bullet = bulletIt.next();
    bullet.y += bullet.speedY; // 向上移动
    
    // 飞出屏幕，移除
    if (bullet.y < -40 || bullet.y > WORLD_HEIGHT + 40) {
        bulletIt.remove();
        continue;
    }
    
    // 检测与敌机碰撞
    for (EnemyState enemy : battle.enemies.values()) {
        if (distance(bullet.x, bullet.y, enemy.x, enemy.y) < 25.0) {
            enemy.hp -= 40;
            bulletIt.remove();
            // 敌机被击毁时给发射者加分
            if (enemy.hp <= 0) {
                // 加分逻辑
            }
            break;
        }
    }
}
```

#### 1.5 广播子弹状态
在 `broadcastBattleState` 方法中添加子弹信息：

```java
JSONArray bullets = new JSONArray();
for (BulletState b : battle.bullets.values()) {
    JSONObject item = new JSONObject();
    item.put("id", b.id);
    item.put("ownerId", b.ownerId);
    item.put("x", b.x);
    item.put("y", b.y);
    bullets.add(item);
}
payload.put("bullets", bullets);
```

---

### 2. 客户端实现

#### 2.1 添加子弹状态类
在 `PvpBattleView.java` 中添加：

```java
public static final class BulletState {
    public int id;
    public long ownerId; // 发射子弹的玩家ID
    public float x;
    public float y;
}
```

#### 2.2 加载子弹图片资源
```java
private final Bitmap heroBulletBitmap;

public PvpBattleView(Context context, @Nullable AttributeSet attrs) {
    // ...
    heroBulletBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pc_bullet_hero);
}
```

#### 2.3 存储和更新子弹列表
```java
private final List<BulletState> bullets = new ArrayList<>();

public void updateState(List<PlayerState> players, List<EnemyState> enemies, List<BulletState> bullets) {
    this.bullets.clear();
    this.bullets.addAll(bullets);
    // ...
}
```

#### 2.4 渲染子弹
在 `onDraw` 方法中添加子弹渲染：

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    drawBackground(canvas);
    drawCombatLane(canvas);
    drawBullets(canvas);  // 在敌机和玩家之前绘制
    drawEnemies(canvas);
    drawPlayers(canvas);
    drawHud(canvas);
}

private void drawBullets(Canvas canvas) {
    for (BulletState bullet : bullets) {
        drawSprite(canvas, heroBulletBitmap, 
            worldToScreenX(bullet.x), worldToScreenY(bullet.y), 
            12f, 24f);  // 子弹大小：12x24
    }
}
```

#### 2.5 解析服务器发送的子弹数据
在 `PvpGameFragment.java` 的 `onBattleState` 方法中：

```java
// 解析子弹状态
List<PvpBattleView.BulletState> bullets = new ArrayList<>();
JSONArray bulletsJson = payload.optJSONArray("bullets");
if (bulletsJson != null) {
    for (int i = 0; i < bulletsJson.length(); i++) {
        JSONObject item = bulletsJson.optJSONObject(i);
        if (item == null) continue;
        PvpBattleView.BulletState b = new PvpBattleView.BulletState();
        b.id = item.optInt("id", 0);
        b.ownerId = item.optLong("ownerId", 0L);
        b.x = (float) item.optDouble("x", 0.0);
        b.y = (float) item.optDouble("y", 0.0);
        bullets.add(b);
    }
}

battleView.updateState(players, enemies, bullets);
```

---

## 技术参数

### 子弹属性
- **大小**: 12 x 24 像素
- **速度**: -10 像素/tick（向上飞行）
- **伤害**: 40 HP
- **碰撞半径**: 25 像素
- **发射位置**: 玩家飞机前方 30 像素

### 游戏循环
- **更新频率**: 50ms/tick (20 FPS)
- **子弹生命周期**: 从发射到飞出屏幕或击中目标

### 碰撞检测
- **子弹 vs 敌机**: 距离 < 25 像素
- **检测频率**: 每个游戏tick

---

## 与单机模式的对比

| 特性 | 单机模式 | 联机模式（新实现） |
|------|---------|------------------|
| 子弹可见性 | ✅ 有轨迹 | ✅ 有轨迹 |
| 子弹物理 | 客户端计算 | 服务器计算 |
| 碰撞检测 | 客户端 | 服务器 |
| 网络同步 | 无 | 实时广播 |
| 视觉效果 | 流畅 | 流畅（取决于网络） |

---

## 优势

### 1. 视觉一致性
- 联机模式和单机模式的视觉效果保持一致
- 玩家可以看到自己和队友的子弹轨迹

### 2. 游戏体验
- 更直观的反馈，玩家可以看到子弹飞行
- 增加游戏的沉浸感和真实感
- 便于判断射击是否命中

### 3. 公平性
- 服务器端权威，防止作弊
- 所有玩家看到相同的子弹状态
- 碰撞检测统一在服务器端进行

---

## 性能考虑

### 网络带宽
- 每个子弹约 40 字节（JSON格式）
- 假设同时有 10 个子弹在屏幕上
- 每次更新约 400 字节
- 更新频率 20 Hz = 8 KB/s

### 服务器性能
- 子弹更新：O(n) 其中 n 是子弹数量
- 碰撞检测：O(n * m) 其中 n 是子弹数量，m 是敌机数量
- 实际影响：可忽略不计（子弹和敌机数量都很少）

### 客户端性能
- 渲染子弹：O(n) 其中 n 是子弹数量
- 使用硬件加速的Canvas绘制
- 性能影响：可忽略不计

---

## 测试建议

### 功能测试
- [ ] 测试子弹是否正确发射
- [ ] 测试子弹是否向上飞行
- [ ] 测试子弹是否能击中敌机
- [ ] 测试子弹飞出屏幕后是否被移除
- [ ] 测试多个玩家同时开火

### 视觉测试
- [ ] 确认子弹图片正确显示
- [ ] 确认子弹大小合适
- [ ] 确认子弹轨迹流畅
- [ ] 确认击中敌机时子弹消失

### 网络测试
- [ ] 测试不同网络延迟下的表现
- [ ] 测试子弹状态同步是否正确
- [ ] 测试多个玩家的子弹是否都能看到

---

## 已知限制

1. **网络延迟影响**: 在高延迟网络下，子弹可能会有轻微的跳跃感
2. **子弹数量限制**: 当前没有限制同时存在的子弹数量，理论上可能导致性能问题（实际不太可能）
3. **子弹类型**: 当前只支持直线子弹，未来可以扩展支持散射、环形等

---

## 未来改进方向

1. **客户端预测**: 在客户端立即显示子弹，减少延迟感
2. **子弹插值**: 平滑子弹位置更新，减少网络抖动
3. **多种子弹类型**: 支持不同的子弹模式（散射、追踪等）
4. **子弹特效**: 添加发射特效、击中特效
5. **音效**: 添加开火音效和击中音效

---

## 修改文件列表

### 服务器端
- `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`
  - 添加 `BulletState` 内部类
  - 修改 `BattleRoomState` 添加子弹管理
  - 修改 `handleFire` 方法创建子弹
  - 修改 `tickBattles` 方法更新子弹和碰撞检测
  - 修改 `broadcastBattleState` 方法广播子弹状态

### 客户端
- `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/game/PvpBattleView.java`
  - 添加 `BulletState` 内部类
  - 加载子弹图片资源
  - 添加 `drawBullets` 方法
  - 修改 `updateState` 方法接收子弹数据
  - 修改 `onDraw` 方法渲染子弹

- `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/PvpGameFragment.java`
  - 修改 `onBattleState` 方法解析子弹数据
  - 传递子弹数据给 `PvpBattleView`

---

## 版本信息

**版本:** 2.1.0  
**发布日期:** 2026年4月10日  
**新增功能:** 联机模式子弹轨迹可视化
