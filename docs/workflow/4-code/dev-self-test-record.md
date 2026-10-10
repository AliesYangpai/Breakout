# 打砖块游戏（Breakout）开发自测记录

- 迭代版本：V1.0.0.20261009
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `../1-requirement/requirement-list.md`）

## 1. 自测范围

| 模块 | 文件 | 自测方式 |
|------|------|---------|
| 核心逻辑（core） | CollisionTest / EntitiesTest / GameEngineTest / LevelsTest / LevelProgressTest | JVM 单元测试（JUnit4） |
| 持久化（data） | SharedPreferencesProgressStoreTest | Robolectric 单元测试 |
| UI（ui） | GameScreenTest / BreakoutAppNavigationTest / AutoPauseOnBackgroundTest | instrumented test（需设备/模拟器） |

## 2. 单元测试执行结果

执行命令：`./gradlew :app:testDebugUnitTest`

**结果：BUILD SUCCESSFUL**

| 测试类 | 用例数 | 失败 | 错误 | 跳过 |
|--------|-------|------|------|------|
| CollisionTest | 8 | 0 | 0 | 0 |
| EntitiesTest | 6 | 0 | 0 | 0 |
| GameEngineTest | 8 | 0 | 0 | 0 |
| LevelsTest | 5 | 0 | 0 | 0 |
| LevelProgressTest | 4 | 0 | 0 | 0 |
| SharedPreferencesProgressStoreTest | 4 | 0 | 0 | 0 |
| **合计** | **35** | **0** | **0** | **0** |

## 3. 自测结论

- ✅ 核心碰撞、实体、游戏引擎、关卡数据、进度逻辑 35 个 JVM 单测全部通过，覆盖需求验收标准 AC-01~AC-06、AC-08、AC-09、AC-12 的底层逻辑。
- ✅ 构建成功，无编译错误/警告。
- ⚠️ instrumented 测试（UI 导航/输入/自动暂停）需真机或模拟器执行，未在本机自测阶段运行，交由【测试回归阶段】执行。

## 4. 遗留项

| 编号 | 遗留项 | 说明 | 转交阶段 |
|------|-------|------|---------|
| TODO-01 | instrumented test 未执行 | `app/src/androidTest/` 下 3 个测试类需设备运行 | 05-test |
| TODO-02 | 视觉反馈仅人工验证 | 砖颜色变化/消失等 | 05-test |

## 5. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261009 | 2026-10-09 | 依据新工作流规范初始化开发自测记录 | Alie |
