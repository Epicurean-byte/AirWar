# 数据访问对象模式设计说明

## 类图

参见 `ScoreDao.puml`（使用 PlantUML 插件渲染截屏）。

## 角色说明

- **ScoreRecord（实体类）**：封装单条得分信息（玩家名、得分、时间），为 DAO 读写的数据对象。
- **ScoreDao（接口）**：数据访问对象抽象，定义 `loadRecords()`、`saveRecords()` 等持久化操作，隔离业务层与数据源。
- **FileScoreDao（DAO 实现）**：负责与文件系统交互，实现 ScoreDao 接口，包含路径字段和文件创建、读取、写入逻辑。
- **ScoreBoard（业务类/服务层）**：组合 ScoreDao，处理排行榜排序、截取前 N 名、打印输出等业务规则，不关心数据存储细节。
- **Game（客户端/调用者）**：游戏主类，在游戏结束时调用 ScoreBoard 记录并展示排行榜。

通过 DAO 模式，分离了排行榜的业务规则与持久化细节，方便将来替换数据源（如数据库）而无需修改 Game 或 ScoreBoard 的业务逻辑。

