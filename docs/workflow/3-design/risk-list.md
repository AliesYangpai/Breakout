# 打砖块游戏（Breakout）技术风险清单与应对方案

- 迭代版本：V1.0.0.20261010
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `../1-requirement/requirement-list.md`）

## 1. 风险清单

| 风险 ID | 风险描述 | 影响程度 | 应对方案 | 负责人 | 状态 |
|---------|---------|---------|---------|--------|------|
| RISK-01 | 球无限水平飞行打不到砖、卡关 | 中 | `reflectOffPaddle` 强制最小垂直分量（`speed * 0.25f`） | Alie | 已应对 |
| RISK-02 | 球在砖/墙间高频抖动或卡边界 | 中 | 碰撞后加位置偏移（`radius + 0.5f`）推出重叠区 | Alie | 已应对 |
| RISK-03 | 低帧率下球穿透砖/挡板 | 高 | `dt` 钳制上限 1/30s | Alie | 已应对 |
| RISK-04 | 切关后生命周期观察器持有旧 engine 闭包 | 高 | `DisposableEffect` 以 `engine` 实例为 key（commit 39dcd66 已修复） | Alie | 已修复 |
| RISK-05 | 过关面板闪现（旧 gameState 残留 WON） | 中 | `gameState`/`engine` 以 `levelIndex` 为 key 重建（commit 9663dc0 已修复） | Alie | 已修复 |
| RISK-06 | 触控像素坐标与逻辑坐标混用 | 高 | 输入统一 `toLogicalX` 换算，渲染/输入共用同一 scale | Alie | 已应对 |
| RISK-07 | 状态竞态（过关/失败瞬间并发操作） | 中 | 状态机单状态 + 状态守卫，非法跳转忽略 | Alie | 已应对 |
| RISK-08 | 存档读写失败崩溃 | 低 | SharedPreferences 失败静默降级 | Alie | 已应对 |
| RISK-09 | UI/交互层自动化覆盖不足 | 中 | 补 instrumented test（navigation/输入/自动暂停） | Alie | 部分应对 |
| RISK-10 | Android 版本兼容性（24~35） | 低 | 稳定版 Compose BOM + androidx 组件 | Alie | 已应对 |

## 2. 性能风险

| 项 | 评估 | 应对 |
|----|------|------|
| 砖块数量（≤ 10×8=80） | 每帧遍历碰撞，量级极小 | 无需优化，O(n) 足够 |
| 游戏循环开销 | 单球 + 单挡板 + 80 砖 | `withFrameNanos` 足够，无需 SurfaceView |
| 内存 | 全不可变 `state.copy` 更新 | 数据量小，GC 压力可忽略 |

## 3. 兼容性风险

| 项 | 评估 | 应对 |
|----|------|------|
| 屏幕比例差异 | 竖屏锁定 + 等比缩放黑边 | `scale = min(w/W, h/H)` 居中适配 |
| 系统返回键 | 不同版本 BackHandler 行为 | `BackHandler(enabled)` 按屏幕层级逐级返回 |
| 切后台生命周期 | ON_STOP 触发自动暂停 | `LifecycleEventObserver` 监听 |

## 4. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化技术风险清单 | Alie |
