# NimboWeather — 测试系统

四层测试，**全部跑在 GitHub Actions 云端模拟器**（绕开本机 WHPX 0-CPU 起不来 + 中文路径坑 gradle test-worker 两个问题）。

| 层 | 工具 | 跑在哪 | 验证什么 |
|---|---|---|---|
| 单元测试 | JUnit4 | `unit-test` job（无模拟器，ubuntu） | AQI 浓度档/分类、月相 illumination/相位/盈亏等纯逻辑 |
| UI 插桩 | Espresso + AndroidX Test | `instrumented-test` job 模拟器 | 首页骨架、抽屉控件、单位开关、语言弹窗（精确断言，含源码 R.id） |
| 功能黑盒 | Maestro（YAML） | 同上 | 真·端到端流程：onboarding→首页→抽屉→单位/语言→滑动切城→详情页 |
| 稳定性 | adb monkey | 同上 | 1500 次随机事件无崩溃/ANR |

## 触发方式

- `push` / `pull_request` 到 `main` 自动跑；也可在 Actions 页面手动 `workflow_dispatch`。
- 工作流文件：`.github/workflows/android-test.yml`（测试）、`android-ci.yml`（构建 + APK 产物）。

## 必需的仓库 Secret

| Secret | 用途 | 不设的后果 |
|---|---|---|
| `OPENWEATHER_API_KEY` | 注入 BuildConfig，让 App 真拉天气 | 天气数据加载失败，但 UI 骨架类断言仍通过 |

设置：仓库 **Settings → Secrets and variables → Actions → New repository secret**。

## 产物（Artifacts）

每次运行在 Actions 页面可下载：
- `unit-test-report` — 单测 HTML 报告
- `instrumented-reports-api34` — Espresso 报告 + `maestro-report.xml`（JUnit 格式）+ `monkey.log` + `logcat.txt`

## 本地跑（接真机/模拟器时）

```powershell
# 1) 先出 APK
.\build-debug.ps1
# 2) 插上手机(开 USB 调试)或起一个模拟器，确认 adb devices 有设备
adb devices
# 3) 一键装包 + Maestro 流程 + monkey
.\scripts\test-device.ps1
```
Maestro 安装：见 https://maestro.mobile.dev （`curl -Ls "https://get.maestro.mobile.dev" | bash`，Windows 用 WSL 或 PowerShell 安装脚本）。

> ⚠️ 本机 `./gradlew testDebugUnitTest` / `connectedAndroidTest` 因中文路径 `塔酷` 会让 test-worker 类加载失败——本地验证单测请用「拷到 ASCII 路径跑」的办法，或直接看 CI。详见构建说明。
