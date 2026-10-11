# 打砖块游戏（Breakout）开发自测记录

- 迭代版本：V1.1.0.20261011
- 文档创建人：Alie
- 创建时间：2026-10-11
- 关联需求：REQ-01 ~ REQ-18（见 `../1-requirement/requirement-list.md`）

## 1. 自测范围

| 模块 | 文件 | 自测方式 |
|------|------|---------|
| 核心逻辑（core） | CollisionTest / EntitiesTest / GameEngineTest / LevelsTest / LevelProgressTest | JVM 单元测试（JUnit4） |
| 持久化（data） | SharedPreferencesProgressStoreTest | Robolectric 单元测试 |
| UI（ui） | GameScreenTest / BreakoutAppNavigationTest / AutoPauseOnBackgroundTest | instrumented test（需设备/模拟器） |

## 2. 本次变更内容（V1.1.0）

| 变更点 | 文件 | 说明 |
|--------|------|------|
| 新增道具/激光实体 | `core/Entities.kt` | 新增 `PowerUpType`、`PowerUpDrop`、`Laser` |
| 新增道具参数常量 | `core/Field.kt` | 掉落概率/速度、倍率、上限、激光速度 |
| 游戏引擎重构 | `core/GameEngine.kt` | 单球→多球列表；抽出 `applyPowerUpTo`/`stepBalls`/`stepDrops`/`stepLasers`/`resolveLifeAndOutcome` 纯函数；新增 `tap` 分流、可注入随机源 |
| 渲染与输入 | `ui/GameScreen.kt` | 渲染多球/掉落物/激光/激光高亮；单击改用 `engine.tap` 分流 |

## 3. 单元测试执行结果

执行命令：`./gradlew :app:testDebugUnitTest`

**结果：BUILD SUCCESSFUL**

| 测试类 | 用例数 | 失败 | 错误 | 跳过 |
|--------|-------|------|------|------|
| CollisionTest | 8 | 0 | 0 | 0 |
| EntitiesTest | 6 | 0 | 0 | 0 |
| GameEngineTest | 22 | 0 | 0 | 0 |
| LevelsTest | 5 | 0 | 0 | 0 |
| LevelProgressTest | 4 | 0 | 0 | 0 |
| SharedPreferencesProgressStoreTest | 4 | 0 | 0 | 0 |
| **合计** | **49** | **0** | **0** | **0** |

> GameEngineTest 新增 14 例：底部空白区移动（2）、激光双束/无激光/READY 分流/伤害/硬砖无伤（5）、道具效果与叠加幂等（5）、多球全掉才扣命（1）、reset 清效果（1）。

## 4. 编译验证

执行命令：`./gradlew :app:assembleDebug`

**结果：BUILD SUCCESSFUL**，无编译错误/警告。

## 5. 自测结论

- ✅ 核心逻辑 49 个 JVM 单测全部通过，覆盖本次新增 AC-13~AC-18 的底层逻辑。
- ✅ debug 包编译成功，UI 层多球/掉落物/激光渲染与 `tap` 分流编译无误。
- ✅ 道具效果通过纯函数 `applyPowerUpTo` 实现幂等与叠加，`Set<PowerUpType>` 天然去重。
- ⚠️ instrumented 测试（UI 导航/输入/自动暂停）需真机或模拟器执行，交由【测试回归阶段】执行。

## 6. 遗留项

| 编号 | 遗留项 | 说明 | 转交阶段 |
|------|-------|------|---------|
| TODO-01 | instrumented test 未执行 | `app/src/androidTest/` 下 3 个测试类需设备运行 | 05-test |
| TODO-02 | 视觉反馈仅人工验证 | 掉落物/激光渲染、加长挡板视觉、激光高亮 | 05-test |
| TODO-03 | 随机掉落概率仅逻辑验证 | 掉落概率 0.2 的实际手感需人工/统计验证 | 05-test |

## 7. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化开发自测记录 | Alie |
| V1.1.0.20261011 | 2026-10-11 | 记录道具系统/激光/多球/底部移动编码自测结果；用例数 35→49 | Alie |
