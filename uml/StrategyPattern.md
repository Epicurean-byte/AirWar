# 策略模式设计说明

## 类图

参见 `BulletStrategy.puml`（可在 IDE 的 PlantUML 插件中渲染）。

## 角色说明

- **BulletFactory（接口）**：策略抽象，定义统一的 `createBullets` 行为，使不同弹道实现可以互换。
- **AbstractPatternBulletFactory（抽象类）**：对公共属性（发射数量、方向、`BulletCreator`）进行封装，简化具体策略的实现。
- **StraightBulletFactory / ScatterBulletFactory / RingBulletFactory（具体策略）**：分别封装直射、散射、环射算法，实现 `createBullets` 生成对应弹幕。
- **BulletCreator（函数式接口）**：屏蔽子弹类型差异，允许同一策略复用到英雄弹和敌机弹。
- **HeroAircraft（上下文）**：维护当前 `BulletFactory` 策略并在 `shoot` 时委托执行，道具激活时通过 `setFireMode`/`cycleFireMode` 切换策略。
- **SuperEliteEnemy / BossEnemy（策略使用者）**：根据自身类型组合对应的射击策略，重用同一套弹道实现。
- **FireSupply / SuperFireSupply（策略切换触发者）**：道具激活后调用英雄机切换策略，分别切换为散射和环射。
- **BaseBullet 及其子类 HeroBullet、EnemyBullet（产品族）**：被策略实例化的具体子弹对象。

