# 打砖块游戏（Breakout）数据结构设计文档

- 迭代版本：V1.1.0.20261011
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-18（见 `../1-requirement/requirement-list.md`）

## 1. 说明

本项目无数据库，仅有两类数据结构：内存实体模型（`core` 包）与本地持久化键值（SharedPreferences）。

## 2. 内存实体模型

### 2.1 实体关系

```
GameEngine ──1:1── GameState ──1:1── Paddle
                          ├──1:N── Ball（多球）
                          ├──1:N── Brick
                          ├──1:N── PowerUpDrop（掉落物）
                          └──1:N── Laser（激光）
GameState ──1:N── activePowerUps: Set<PowerUpType>
Brick ──1:1── BrickType（枚举）
PowerUpDrop ──1:1── PowerUpType（枚举）
```

### 2.2 数据结构定义

| 结构 | 字段 | 类型 | 说明 |
|------|------|------|------|
| Rect | left/top/right/bottom | Float | 轴对齐包围盒 |
| Paddle | centerX | Float | 挡板中心 X |
| | halfWidth | Float | 挡板半宽（随关卡递减） |
| | y / height | Float | 挡板纵坐标/厚度 |
| Ball | position | Vec2 | 球心位置 |
| | velocity | Vec2 | 速度向量 |
| | radius | Float | 球半径 |
| Brick | row / col | Int | 网格坐标 |
| | type | BrickType | 砖类型 |
| | hp | Int | 剩余耐久（由 `type.hits` 初始化） |
| GameState | levelIndex | Int | 关卡索引 |
| | lives | Int | 剩余生命（初始 `MAX_LIVES=3`） |
| | status | GameStatus | 状态机 |
| | paddle | Paddle | 挡板快照 |
| | balls | List<Ball> | 球列表（READY 时为 1 个吸附球；多球道具后可多个） |
| | bricks | List<Brick> | 砖块快照 |
| | drops | List<PowerUpDrop> | 掉落物列表 |
| | lasers | List<Laser> | 激光列表 |
| | activePowerUps | Set<PowerUpType> | 已激活道具集合（不同道具叠加，同种幂等） |
| PowerUpDrop | type | PowerUpType | 道具种类 |
| | position | Vec2 | 掉落物中心位置 |
| Laser | x | Float | 激光发射列 X（挡板两端） |
| | y | Float | 激光顶端 Y |
| LevelConfig | ballSpeed | Float | 球速（非递减） |
| | paddleHalfWidth | Float | 挡板半宽（非递增） |
| | rows | List<List<BrickType?>> | 网格布局（null = 空） |

### 2.3 砖块类型

| BrickType | hits | indestructible | 说明 |
|-----------|------|----------------|------|
| NORMAL | 1 | false | 普通砖，1 击摧毁 |
| DURABLE_2 | 2 | false | 2 击耐久砖 |
| DURABLE_3 | 3 | false | 3 击耐久砖（第 6 关起） |
| INDESTRUCTIBLE | Int.MAX_VALUE | true | 硬砖，永不摧毁（第 7 关起） |

`destroyed` 派生规则：`!type.indestructible && hp <= 0`。

### 2.4 道具类型与参数常量

| PowerUpType | 效果 | 参数 |
|-------------|------|------|
| WIDE_PADDLE | 挡板半宽乘 `PADDLE_WIDEN_FACTOR` | 加长挡板 |
| MULTI_BALL | 新增一球（差异化角度），受 `MAX_BALLS` 上限 | 多球 |
| SLOW_BALL | 所有球速度乘 `BALL_SLOW_FACTOR` | 慢速球 |
| LASER | 激活激光，RUNNING 点击两端各发射一束 | 激光 |

掉落相关常量（`Field.kt` 扩展）：

| 常量 | 建议值 | 说明 |
|------|--------|------|
| `DROP_CHANCE` | 0.2f | 击碎可破坏砖掉落概率 |
| `DROP_SPEED` | 120f | 掉落物下落速度（逻辑单位/秒） |
| `DROP_RADIUS` | 8f | 掉落物半径 |
| `PADDLE_WIDEN_FACTOR` | 1.5f | 加长挡板倍率 |
| `MAX_BALLS` | 4 | 场上球数量上限 |
| `BALL_SLOW_FACTOR` | 0.6f | 慢速球倍率 |
| `LASER_SPEED` | 600f | 激光向上速度 |
| `LASER_RADIUS` | 3f | 激光命中判定半径 |

## 3. 关卡网格数据

- 关卡用字符串网格描述，`rows(...)` 解析：`D`=普通砖、`2`=2 击耐久、`3`=3 击耐久、`I`=硬砖、其他字符=空。
- 每行取 `Field.COLS`（10）个字符；行数即砖阵排数。
- `brickRect(row, col)` 将网格坐标映射到逻辑像素矩形（砖宽 = `WIDTH / COLS`，砖高 = `BRICK_HEIGHT`，顶部偏移 = `TOP_OFFSET`）。

## 4. 持久化数据结构

| 存储 | Key | 类型 | 默认值 | 说明 |
|------|-----|------|--------|------|
| SharedPreferences（文件 `breakout_progress`） | `max_unlocked_index` | Int | 0 | 最大已解锁关卡索引（单调递增） |

- 仅存一个整数，无需 SQLite/Room。
- 读取失败/无值时回退为 0（仅第 1 关解锁）。

## 5. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化数据结构设计 | Alie |
| V1.1.0.20261011 | 2026-10-11 | 新增 PowerUpType/PowerUpDrop/Laser 实体与 activePowerUps 集合；GameState.ball 改为 balls 列表；补充道具参数常量表 | Alie |
