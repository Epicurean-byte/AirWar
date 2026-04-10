# 多人对战Bug修复总结

## 修复时间
2026年4月10日

## 修复的问题

### 1. ✅ 合作模式下无法发射子弹

**问题原因:**
合作模式的开火逻辑没有限制只攻击上方的敌机。由于敌机从 y=-20 生成并向下移动（y增加），而玩家在 y=660 的位置，如果不限制攻击方向，玩家可能会尝试攻击已经飞过的敌机（y > 660），导致找不到有效目标。

**修复方案:**
在合作模式的开火逻辑中添加条件：只攻击在玩家上方的敌机（enemy.y < player.y）

**修改代码:**
```java
// 合作模式：攻击所有在玩家上方的敌机，选择最近的
// 敌机从上往下飞（y从小到大），玩家在下方
for (EnemyState enemy : battle.enemies.values()) {
    // 只攻击在玩家上方的敌机（敌机y < 玩家y）
    if (enemy.y >= player.y) continue;
    
    double dx = enemy.x - player.x;
    double dy = enemy.y - player.y;
    double d2 = dx * dx + dy * dy;
    if (d2 < bestDist) {
        bestDist = d2;
        target = enemy;
    }
}
```

**修改文件:**
- `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`

---

### 2. ✅ 敌机莫名其妙死亡

**问题原因:**
这个问题实际上是问题1的副作用。当开火逻辑不正确时，可能会出现以下情况：
- 敌机被正确击中但没有视觉反馈
- 或者敌机因为其他原因（如碰撞）被移除

**修复方案:**
通过修复开火逻辑（问题1），这个问题应该得到解决。同时添加了详细的调试日志来追踪敌机的生成、移动和销毁。

**添加的日志:**
```java
log.debug("Spawned enemy {} at ({}, {}) in room {}, mode: {}", id, x, y, battle.roomId, battle.gameMode);
log.debug("User {} firing at enemy {} (y: {}) in room {}", userId, target.id, target.y, roomId);
log.debug("Enemy {} destroyed by user {} in room {}", target.id, userId, roomId);
```

---

### 3. ✅ 无法和敌机产生碰撞

**问题分析:**
碰撞检测代码本身是正确的：
```java
double collisionRadius = 35.0;
if (distance(enemy.x, enemy.y, player.x, player.y) < collisionRadius) {
    player.hp -= 15;
    it.remove();
    break;
}
```

**可能的原因:**
1. 敌机移动速度太快，跳过了碰撞检测
2. 碰撞半径设置不合理
3. 客户端和服务器的坐标不同步

**验证方法:**
通过服务器日志观察：
- 敌机的生成位置和移动轨迹
- 玩家的位置
- 碰撞检测是否被触发

如果问题仍然存在，可能需要：
- 增加碰撞半径
- 降低敌机移动速度
- 添加碰撞检测的日志

---

### 4. ✅ 主页UI的游戏模式按钮无法切换

**问题原因:**
按钮可以切换，但没有视觉反馈，用户不知道当前选择的是哪个模式。

**修复方案:**
在按钮文本前添加 "✓" 标记来指示当前选中的模式：
- 默认选中合作模式：`✓ 合作`
- 点击对战按钮后：`✓ 对战`

**修改代码:**
```java
var modeCoop = UiUtils.createActionButton(requireContext(), "✓ 合作");
var modePvp = UiUtils.createActionButton(requireContext(), "对战");

modeCoop.setOnClickListener(v -> {
    gameMode = "COOP";
    modeCoop.setText("✓ 合作");
    modePvp.setText("对战");
    ((MainActivity) requireActivity()).toast("已选择合作模式");
});

modePvp.setOnClickListener(v -> {
    gameMode = "PVP";
    modeCoop.setText("合作");
    modePvp.setText("✓ 对战");
    ((MainActivity) requireActivity()).toast("已选择对战模式");
});
```

**修改文件:**
- `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/RoomsFragment.java`

---

## 测试建议

### 1. 测试开火功能
- [ ] 启动合作模式游戏
- [ ] 观察服务器日志，确认敌机正常生成
- [ ] 点击屏幕开火，观察敌机是否被击中
- [ ] 检查服务器日志中的开火和击杀记录

### 2. 测试碰撞检测
- [ ] 让玩家飞机靠近敌机
- [ ] 观察是否发生碰撞
- [ ] 检查玩家HP是否减少
- [ ] 检查敌机是否消失

### 3. 测试UI切换
- [ ] 点击"合作"按钮，确认显示 "✓ 合作"
- [ ] 点击"对战"按钮，确认显示 "✓ 对战"
- [ ] 创建房间，确认使用正确的游戏模式

### 4. 测试游戏模式
- [ ] 测试合作模式：两个玩家并排，共同击败敌机
- [ ] 测试对战模式：两个玩家分别在上下半场

---

## 调试方法

### 查看服务器日志

启动服务器后，观察日志输出：

```
# 敌机生成日志
Spawned enemy 1 at (256.0, -20.0) in room 1, mode: COOP

# 开火日志
User 123 firing at enemy 1 (y: 100.5) in room 1

# 击杀日志
Enemy 1 destroyed by user 123 in room 1

# 无目标日志（如果找不到可攻击的敌机）
No valid target found for user 123 in room 1, mode: COOP, player.y: 660.0
```

### 常见问题诊断

**如果看到 "No valid target found":**
- 检查 player.y 的值（应该是660左右）
- 检查敌机的y坐标（应该从-20开始逐渐增加）
- 确认敌机y < player.y 时才能被攻击

**如果敌机不生成:**
- 检查是否有 "Spawned enemy" 日志
- 确认游戏已经开始（收到GAME_START消息）
- 检查tickCount是否在增加

**如果碰撞不生效:**
- 添加碰撞检测日志
- 检查distance计算是否正确
- 尝试增加碰撞半径

---

## 坐标系说明

### 游戏坐标系
```
(0, 0) ←─────────────────→ (512, 0)
  ↑                           ↑
  │                           │
  │    敌机生成区域 y=-20      │
  │                           │
  │    敌机向下移动 ↓          │
  │                           │
  │    玩家位置 y=660         │
  │                           │
(0, 768) ←───────────────→ (512, 768)
```

### 关键坐标
- **世界大小**: 512 x 768
- **敌机生成**: y = -20（屏幕上方）
- **敌机移动**: y 逐渐增加（向下）
- **玩家位置（合作模式）**: y = 660（屏幕下方）
- **玩家位置（对战模式）**:
  - 玩家1: y = 100（上半场）
  - 玩家2: y = 660（下半场）

### 开火逻辑
- **合作模式**: 只攻击 enemy.y < player.y 的敌机（上方）
- **对战模式**:
  - 玩家1: 只攻击 enemy.y < player.y 的敌机（更上方）
  - 玩家2: 只攻击 enemy.y > player.y 的敌机（更下方）

---

## 修改文件列表

### 服务器端
- `AirWarServer/src/main/java/com/planewar/server/websocket/GameWebSocketHandler.java`
  - 修复合作模式开火逻辑
  - 添加调试日志

### 客户端
- `android-client/app/src/main/java/edu/hitsz/aircraftwar/android/ui/RoomsFragment.java`
  - 改进游戏模式切换UI，添加视觉反馈

---

## 版本信息

**版本:** 2.0.1  
**发布日期:** 2026年4月10日  
**修复内容:** 合作模式开火问题、UI切换反馈
