# 本地数据库使用指南

## 概述

本项目使用 SQLite 数据库存储游戏记录和用户统计数据。数据库设计参考了 lab5 的 SQLiteDemo，采用单例模式管理数据库连接。

## 数据库结构

### 1. 游戏记录表 (game_records)

存储每一局游戏的详细记录。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | INTEGER | 主键，自增 |
| user_id | INTEGER | 用户ID |
| game_mode | VARCHAR | 游戏模式（SINGLE/PVP/COOP） |
| score | INTEGER | 得分 |
| coins | INTEGER | 获得金币 |
| duration | INTEGER | 游戏时长（秒） |
| enemies_killed | INTEGER | 击杀敌机数 |
| max_combo | INTEGER | 最大连击数 |
| play_time | DATETIME | 游戏时间 |
| is_win | INTEGER | 是否胜利（0/1，联机模式） |
| room_id | INTEGER | 房间ID（联机模式） |

### 2. 用户统计表 (user_stats)

存储用户的累计统计数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| user_id | INTEGER | 主键，用户ID |
| total_games | INTEGER | 总游戏场次 |
| total_score | INTEGER | 总得分 |
| total_coins | INTEGER | 总金币 |
| total_playtime | INTEGER | 总游戏时长（秒） |
| best_score | INTEGER | 最高分 |
| pvp_wins | INTEGER | PVP 胜利次数 |
| pvp_losses | INTEGER | PVP 失败次数 |
| coop_games | INTEGER | COOP 游戏次数 |
| last_update | DATETIME | 最后更新时间 |

## 使用方法

### 1. 获取数据库实例

```java
// 在 Activity 或 Fragment 中
GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(context);
```

### 2. 保存游戏记录

#### 单机游戏结束时
```java
// 创建游戏记录
GameRecord record = new GameRecord();
record.userId = currentUserId;
record.gameMode = "SINGLE";
record.score = finalScore;
record.coins = earnedCoins;
record.duration = gameDurationInSeconds;
record.enemiesKilled = totalEnemiesKilled;
record.maxCombo = maxComboCount;
record.playTime = new Date();

// 保存到数据库
GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(this);
long recordId = dbHelper.insertGameRecord(record);

Log.d("Game", "Game record saved with id: " + recordId);
```

#### 联机游戏结束时
```java
GameRecord record = new GameRecord();
record.userId = currentUserId;
record.gameMode = "PVP";  // 或 "COOP"
record.score = finalScore;
record.coins = earnedCoins;
record.duration = gameDurationInSeconds;
record.enemiesKilled = totalEnemiesKilled;
record.maxCombo = maxComboCount;
record.playTime = new Date();
record.isWin = (winnerUserId == currentUserId);  // 是否胜利
record.roomId = currentRoomId;

long recordId = dbHelper.insertGameRecord(record);
```

### 3. 查询游戏记录

#### 查询最近的游戏记录
```java
GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(this);
List<GameRecord> recentGames = dbHelper.queryGameRecords(userId, 10);

for (GameRecord record : recentGames) {
    Log.d("Game", "Score: " + record.score + ", Mode: " + record.gameMode);
}
```

#### 查询指定模式的记录
```java
// 查询最近10场单机游戏
List<GameRecord> singleGames = dbHelper.queryGameRecordsByMode(userId, "SINGLE", 10);

// 查询最近10场PVP游戏
List<GameRecord> pvpGames = dbHelper.queryGameRecordsByMode(userId, "PVP", 10);

// 查询最近10场COOP游戏
List<GameRecord> coopGames = dbHelper.queryGameRecordsByMode(userId, "COOP", 10);
```

#### 查询最高分记录
```java
GameRecord bestRecord = dbHelper.getBestScoreRecord(userId);
if (bestRecord != null) {
    Log.d("Game", "Best score: " + bestRecord.score);
}
```

### 4. 查询用户统计

```java
GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(this);
UserStats stats = dbHelper.getUserStats(userId);

// 显示统计信息
Log.d("Stats", "Total games: " + stats.totalGames);
Log.d("Stats", "Total score: " + stats.totalScore);
Log.d("Stats", "Best score: " + stats.bestScore);
Log.d("Stats", "PVP win rate: " + stats.getPvpWinRate() + "%");
Log.d("Stats", "Average score: " + stats.getAverageScore());
```

### 5. 在 UI 中显示统计信息

```java
// 在 Fragment 中显示用户统计
public class StatsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        
        MainActivity activity = (MainActivity) requireActivity();
        long userId = activity.getCurrentUser().getUserId();
        
        GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(requireContext());
        UserStats stats = dbHelper.getUserStats(userId);
        
        // 显示统计信息
        TextView totalGamesText = view.findViewById(R.id.total_games);
        totalGamesText.setText("总场次: " + stats.totalGames);
        
        TextView bestScoreText = view.findViewById(R.id.best_score);
        bestScoreText.setText("最高分: " + stats.bestScore);
        
        TextView winRateText = view.findViewById(R.id.win_rate);
        winRateText.setText(String.format("胜率: %.1f%%", stats.getPvpWinRate()));
        
        return view;
    }
}
```

### 6. 清理旧数据

```java
// 只保留最近100条记录
GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(this);
dbHelper.deleteOldGameRecords(userId, 100);
```

## 集成到现有代码

### 1. 在单机游戏结束时保存记录

修改 `AircraftWarSurfaceView.java` 或游戏结束处理代码：

```java
private void onGameOver() {
    // 现有的游戏结束逻辑...
    
    // 保存游戏记录到本地数据库
    MainActivity activity = (MainActivity) getContext();
    if (activity != null && activity.getCurrentUser() != null) {
        GameRecord record = new GameRecord();
        record.userId = activity.getCurrentUser().getUserId();
        record.gameMode = "SINGLE";
        record.score = this.score;
        record.coins = this.coins;
        record.duration = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);
        record.enemiesKilled = this.enemiesKilled;
        record.maxCombo = this.maxCombo;
        
        GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(getContext());
        dbHelper.insertGameRecord(record);
    }
}
```

### 2. 在联机游戏结束时保存记录

修改 `PvpGameFragment.java` 的 `onGameOver` 方法：

```java
private void onGameOver(JSONObject payload) {
    long winnerUserId = payload.optLong("winnerUserId", 0L);
    MainActivity activity = (MainActivity) requireActivity();
    
    // 现有的游戏结束逻辑...
    
    // 保存游戏记录到本地数据库
    if (activity.getCurrentUser() != null) {
        GameRecord record = new GameRecord();
        record.userId = myUserId;
        record.gameMode = gameMode;  // "PVP" 或 "COOP"
        record.score = (int) myScore;
        record.coins = (int) myCoins;
        record.duration = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);
        record.enemiesKilled = enemiesKilledCount;
        record.maxCombo = maxComboCount;
        record.isWin = (winnerUserId == myUserId);
        record.roomId = roomId;
        
        GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(requireContext());
        dbHelper.insertGameRecord(record);
    }
    
    settlePvp();
    activity.showMainMenu();
}
```

### 3. 添加统计界面

创建新的 `StatsFragment.java`：

```java
public class StatsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);
        
        root.addView(UiUtils.createTitle(requireContext(), "游戏统计"));
        
        MainActivity activity = (MainActivity) requireActivity();
        long userId = activity.getCurrentUser().getUserId();
        
        GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(requireContext());
        UserStats stats = dbHelper.getUserStats(userId);
        
        // 总体统计
        var statsPanel = UiUtils.createPanel(requireContext(), 12);
        statsPanel.setOrientation(LinearLayout.VERTICAL);
        statsPanel.addView(UiUtils.createSectionTitle(requireContext(), "总体统计"));
        statsPanel.addView(UiUtils.createBody(requireContext(), "总场次: " + stats.totalGames));
        statsPanel.addView(UiUtils.createBody(requireContext(), "总得分: " + stats.totalScore));
        statsPanel.addView(UiUtils.createBody(requireContext(), "总金币: " + stats.totalCoins));
        statsPanel.addView(UiUtils.createBody(requireContext(), "最高分: " + stats.bestScore));
        statsPanel.addView(UiUtils.createBody(requireContext(), 
            String.format("平均分: %d", stats.getAverageScore())));
        root.addView(statsPanel);
        
        // PVP 统计
        var pvpPanel = UiUtils.createPanel(requireContext(), 12);
        pvpPanel.setOrientation(LinearLayout.VERTICAL);
        pvpPanel.addView(UiUtils.createSectionTitle(requireContext(), "对战统计"));
        pvpPanel.addView(UiUtils.createBody(requireContext(), "胜利: " + stats.pvpWins));
        pvpPanel.addView(UiUtils.createBody(requireContext(), "失败: " + stats.pvpLosses));
        pvpPanel.addView(UiUtils.createBody(requireContext(), 
            String.format("胜率: %.1f%%", stats.getPvpWinRate())));
        root.addView(pvpPanel);
        
        // COOP 统计
        var coopPanel = UiUtils.createPanel(requireContext(), 12);
        coopPanel.setOrientation(LinearLayout.VERTICAL);
        coopPanel.addView(UiUtils.createSectionTitle(requireContext(), "合作统计"));
        coopPanel.addView(UiUtils.createBody(requireContext(), "合作场次: " + stats.coopGames));
        root.addView(coopPanel);
        
        // 最近游戏记录
        var historyPanel = UiUtils.createPanel(requireContext(), 12);
        historyPanel.setOrientation(LinearLayout.VERTICAL);
        historyPanel.addView(UiUtils.createSectionTitle(requireContext(), "最近游戏"));
        
        List<GameRecord> recentGames = dbHelper.queryGameRecords(userId, 5);
        for (GameRecord record : recentGames) {
            String recordText = String.format("%s - 分数: %d, 金币: %d", 
                record.gameMode, record.score, record.coins);
            historyPanel.addView(UiUtils.createCaption(requireContext(), recordText));
        }
        root.addView(historyPanel);
        
        // 返回按钮
        var backButton = UiUtils.createActionButton(requireContext(), "返回");
        backButton.setOnClickListener(v -> activity.showMainMenu());
        root.addView(backButton);
        
        return scrollView;
    }
}
```

## 性能优化建议

### 1. 批量操作
如果需要插入多条记录，使用事务：

```java
SQLiteDatabase db = dbHelper.openWriteLink();
db.beginTransaction();
try {
    for (GameRecord record : records) {
        dbHelper.insertGameRecord(record);
    }
    db.setTransactionSuccessful();
} finally {
    db.endTransaction();
}
```

### 2. 异步操作
在后台线程执行数据库操作：

```java
new Thread(() -> {
    GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(context);
    List<GameRecord> records = dbHelper.queryGameRecords(userId, 100);
    
    // 在主线程更新 UI
    runOnUiThread(() -> {
        updateUI(records);
    });
}).start();
```

### 3. 定期清理
定期清理旧数据以保持数据库性能：

```java
// 每次启动应用时清理
GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(this);
dbHelper.deleteOldGameRecords(userId, 100);  // 只保留最近100条
```

## 数据迁移

如果未来需要升级数据库结构，在 `GameDatabaseHelper.onUpgrade()` 中处理：

```java
@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
        // 版本1到版本2的升级
        db.execSQL("ALTER TABLE game_records ADD COLUMN new_field INTEGER DEFAULT 0");
    }
    if (oldVersion < 3) {
        // 版本2到版本3的升级
        db.execSQL("CREATE TABLE new_table (...)");
    }
}
```

## 注意事项

1. **单例模式**: 使用 `getInstance()` 获取数据库实例，不要直接 new
2. **上下文**: 传入 ApplicationContext 避免内存泄漏
3. **线程安全**: 数据库操作是线程安全的，但建议在后台线程执行
4. **关闭连接**: 通常不需要手动关闭，系统会自动管理
5. **数据备份**: 重要数据应该同步到服务器

## 调试

查看数据库内容：

```bash
# 使用 adb 导出数据库
adb pull /data/data/edu.hitsz.aircraftwar.android/databases/aircraft_war.db

# 使用 SQLite 工具查看
sqlite3 aircraft_war.db
.tables
SELECT * FROM game_records;
SELECT * FROM user_stats;
```

## 示例：完整的游戏流程

```java
// 1. 游戏开始
long gameStartTime = System.currentTimeMillis();
int enemiesKilled = 0;
int maxCombo = 0;

// 2. 游戏进行中
// ... 游戏逻辑 ...
enemiesKilled++;
maxCombo = Math.max(maxCombo, currentCombo);

// 3. 游戏结束
int duration = (int) ((System.currentTimeMillis() - gameStartTime) / 1000);

GameRecord record = new GameRecord();
record.userId = currentUserId;
record.gameMode = "SINGLE";
record.score = finalScore;
record.coins = earnedCoins;
record.duration = duration;
record.enemiesKilled = enemiesKilled;
record.maxCombo = maxCombo;

GameDatabaseHelper dbHelper = GameDatabaseHelper.getInstance(context);
dbHelper.insertGameRecord(record);

// 4. 显示统计
UserStats stats = dbHelper.getUserStats(currentUserId);
showStatsDialog(stats);
```
