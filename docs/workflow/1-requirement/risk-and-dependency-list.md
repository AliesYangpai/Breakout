# 打砖块游戏（Breakout）风险与依赖清单

- 迭代版本：V1.0.0.20261009
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `requirement-list.md`）

## 1. 风险清单

| 风险 ID | 风险描述 | 影响程度 | 应对方案 | 负责人 |
|---------|---------|---------|---------|--------|
| RISK-01 | 球无限水平飞行导致打不到砖、卡关 | 中 | 反弹时强制最小垂直速度分量（`Collision.kt` `reflectOffPaddle` 中 `minVertical = speed * 0.25f`） | Alie |
| RISK-02 | 球在砖/墙之间高频抖动或卡边界 | 中 | 碰撞后加微小位置偏移（`normal * (radius + 0.5f)`）推出重叠区 | Alie |
| RISK-03 | 低帧率下球穿透砖/挡板（变步长缺陷） | 高 | `dt` 钳制上限 1/30s，防止单步位移过大 | Alie |
| RISK-04 | 切关后生命周期观察器持有旧 engine 闭包，导致切后台自动暂停作用于错误关卡 | 高 | `DisposableEffect` 以 `engine` 实例为 key（commit 39dcd66 已修复并回归） | Alie |
| RISK-05 | 过关面板闪现：切关瞬间旧 `gameState` 残留 WON 状态 | 中 | `gameState`/`engine` 以 `levelIndex` 为 key 重建（commit 9663dc0 已修复） | Alie |
| RISK-06 | 触控输入像素坐标与逻辑坐标混用导致挡板错位 | 高 | 输入前统一 `toLogicalX` 换算（同 scale），渲染与输入共用同一变换 | Alie |
| RISK-07 | 状态竞态：过关/失败瞬间同时触发暂停或切屏 | 中 | 状态机一次仅一状态；`launchBall`/`pause`/`resume` 均做状态守卫，非法跳转忽略 | Alie |
| RISK-08 | 存档读写失败导致崩溃 | 低 | `SharedPreferencesProgressStore` 失败静默降级为「仅当前关可玩」，下次重试写入 | Alie |
| RISK-09 | UI/交互层自动化覆盖不足（曾出过 Critical bug） | 中 | 补 Compose UI Test 与 instrumented test，见测试用例文档 §4 缺口 | Alie |
| RISK-10 | Android 版本兼容性（24~35） | 低 | 使用稳定版 Compose BOM 与 androidx 组件，minSdk 24 覆盖主流机型 | Alie |

## 2. 依赖清单

| 依赖 ID | 依赖项 | 类型 | 说明 |
|---------|-------|------|------|
| DEP-01 | Jetpack Compose（BOM 2024.10.01） | 技术 | UI 声明式渲染 |
| DEP-02 | Material3 | 技术 | 菜单/弹窗/卡片组件 |
| DEP-03 | SharedPreferences | 技术 | 进度持久化（仅存一个整数） |
| DEP-04 | androidx.lifecycle | 技术 | 生命周期观察（切后台自动暂停） |
| DEP-05 | JUnit 4 + Robolectric | 测试 | core 逻辑 JVM 单测 + 依赖 Context 的存档单测 |
| DEP-06 | Compose UI Test（androidTest） | 测试 | 导航/输入/自动暂停仪器化测试 |

## 3. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261009 | 2026-10-09 | 依据新工作流规范初始化风险与依赖清单 | Alie |
