# 打砖块游戏（Breakout）数据结构设计文档

- 迭代版本：V1.0.0.20261009
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `../1-requirement/requirement-list.md`）

## 1. 说明

本项目无数据库，仅有两类数据结构：内存实体模型（`core` 包）与本地持久化键值（SharedPreferences）。

## 2. 内存实体模型

### 2.1 实体关系

```
GameEngine ──1:1── GameState ──1:1── Paddle / Ball
                          └──1:N── Brick
Brick ──1:1── BrickType（枚举）
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
| | paddle / ball / bricks | — | 实体快照 |
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
| V1.0.0.20261009 | 2026-10-09 | 依据新工作流规范初始化数据结构设计 | Alie |
