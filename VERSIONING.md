# 版本管理 (Versioning & Release)

## 1. 语义化版本 SemVer

版本号 `主.次.补丁`（MAJOR.MINOR.PATCH）：

| 位 | 何时 +1 | 例 |
|---|---|---|
| MAJOR | 不兼容的重大变更 | 1.0.0 → 2.0.0 |
| MINOR | 新功能、向后兼容 | 0.1.0 → 0.2.0 |
| PATCH | bug 修复、向后兼容 | 0.1.0 → 0.1.1 |

发布前的产品按 `0.y.z`（当前 `0.1.0`）。

## 2. 版本号自动从 git tag 推导

`app/build.gradle.kts` 在构建时读取最近的 git tag：

- tag `vX.Y.Z` → `versionName = "X.Y.Z"`
- `versionCode = X*1_000_000 + Y*1_000 + Z`（单调递增，满足 Play 要求）
- 无 tag 时（如 CI 的浅克隆 debug 构建）回退到 `FALLBACK_VERSION_NAME = 0.1.0`

**所以不要手改 build.gradle 里的版本号——打 tag 即定版本。**

## 3. Conventional Commits（提交规范）

```
<类型>(<可选范围>): <简述>
```
类型：`feat`（新功能，对应 MINOR）、`fix`（修复，对应 PATCH）、`docs`、`test`、`ci`、`chore`、`refactor`、`perf`。
破坏性变更在脚注写 `BREAKING CHANGE:`（对应 MAJOR）。规范的提交让 Release 的自动发布说明更清晰。

## 4. 发布流程

1. 确认要发布的代码已在 `main`（已过必过检查）。
2. 更新 `CHANGELOG.md`：把 `[Unreleased]` 的内容归到新版本号下（走一个小 PR 合并）。
3. 在 main 上打 tag 并推送：
   ```bash
   git checkout main && git pull
   git tag -a v0.2.0 -m "v0.2.0"
   git push origin v0.2.0
   ```
4. 推 tag 自动触发 `.github/workflows/release.yml`：跑单测 → 打包 APK → 创建 GitHub Release（附 `NimboWeather-0.2.0.apk` + 自动生成的发布说明）。

> 当前 APK 为 debug 签名；正式签名（keystore + `signingConfig`）属 M5 上架阶段，届时 release 工作流改用 `bundleRelease`/`assembleRelease` 出签名包。

## 5. 分支策略（多人协作 + 多版本维护）

日常用 **GitHub Flow**（见下），需要同时维护多个已发布版本时叠加 **release 分支**：

```
main                    ← 开发主线，始终可发布、受保护、只经 PR 合入
 ├─ feat/* fix/* chore/*    ← 短生命周期特性/修复分支 → PR → squash 合并 → 删除
 └─ release/0.1            ← (按需) 0.1.x 稳定线，从 main 在某 tag 处拉出
      └─ 打 v0.1.1 / v0.1.2 等补丁 tag
```

**何时拉 release 分支**：当 main 已经在做下一个 MINOR（如 0.2），而线上 0.1 还需要打补丁时，从 `v0.1.0` 拉 `release/0.1`，在其上修 bug、打 `v0.1.1`。

**hotfix 流程**：
```bash
git checkout -b release/0.1 v0.1.0          # 首次从已发布 tag 拉稳定线
# 在 release/0.1 上修复 → 提交
git tag -a v0.1.1 -m "v0.1.1 hotfix" && git push origin release/0.1 v0.1.1
# 把修复合回 main，避免回归：
git checkout main && git cherry-pick <fix-commit>   # 或开 PR
```

现阶段（0.1.x、尚未上架）通常**只用 main + 特性分支**即可，等有正式发布版本需要并行维护时再启用 release 分支。

## 6. 常用命令速查

```bash
# 看当前会打成什么版本（最近 tag）
git describe --tags --abbrev=0
# 列所有版本
git tag -l --sort=-v:refname
# 发新版本
git tag -a v0.2.0 -m "v0.2.0" && git push origin v0.2.0
# 手动触发一次 release 工作流（不打 tag，用于调试）：Actions → Release → Run workflow
```
