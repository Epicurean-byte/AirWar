# 观察者模式设计说明（炸弹道具）

## 类图

参见 `BombObserver.puml`。

## 角色说明

- **BombObserver（接口）**：观察者抽象，暴露 `onBombActivated()` 与 `isActive()`，所有受炸弹影响的对象均实现此接口。
- **BombEventPublisher（发布者）**：单例，维护观察者列表并在炸弹触发时逐一通知，汇总英雄可获得的分数。
- **AbstractEnemy / MobEnemy / EliteEnemy / SuperEliteEnemy / BossEnemy**：作为具体观察者，响应炸弹事件。普通、精英敌机会直接消失，超级精英扣血，Boss 无影响。
- **EnemyBullet**：观察者，实现后在炸弹触发时立即消失。
- **Game**：持有发布者引用，负责在敌机、敌机子弹生成或消失时进行注册/注销；英雄机拾取炸弹时调用 `notifyObservers()`。
- **BombSupply**：主题事件触发者，道具被拾取后调用发布者通知所有观察者并将返回的分数加给英雄。

