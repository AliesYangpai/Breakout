# 打砖块游戏（Breakout）发布检查清单

- 迭代版本：V1.0.0.20261010
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `../1-requirement/requirement-list.md`）

## 1. 状态说明

> **本清单为初始骨架，尚未执行发布检查。** 正式发布前逐项勾选。

## 2. 检查清单

| 类别 | 检查项 | 结果 | 备注 |
|------|--------|------|------|
| 代码 | 发布分支为 main 且已打 tag | ⬜ 未执行 | 见 development-rules.md 分支管理规则 |
| 代码 | 回归测试通过（见 5-test/regression-test-report.md） | ⬜ 未执行 | 当前回归报告为「待执行」 |
| 版本 | `versionName` / `versionCode` 正确 | ⬜ 未核对 | 当前 `versionName=1.0`、`versionCode=1` |
| 版本 | 版本号符合 `Va.b.c.d` 规范 | ✅ | V1.0.0.20261010 |
| 构建 | `./gradlew assembleRelease` 成功 | ⬜ 未执行 | 当前仅跑过 debug 单测 |
| 权限 | 无多余运行时权限声明 | ✅ | 仅 SharedPreferences，无权限声明 |
| 配置 | `minSdk/targetSdk/compileSdk` 符合设计 | ✅ | 24 / 35 / 35 |
| 配置 | 竖屏锁定、应用名正确 | ✅ | `screenOrientation=portrait`、label「打砖块」 |
| 资源 | 资源完整无缺失 | ⬜ 未核对 | — |
| 隐私 | 无隐私合规风险（不采集个人信息） | ✅ | 仅本地存档 |

## 3. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化发布检查清单 | Alie |
