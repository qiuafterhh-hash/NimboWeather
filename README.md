# Nimbo Weather

原生 Android (Kotlin) 天气 App。动态天空渐变 + 罗盘表盘 hero + 玻璃卡片流；可插拔广告中介（AdMob，预留 TopOn/MAX）；WorkManager/通知/Widget 留存。

- 包名：`com.nimboweather.forecast`　·　minSdk 24 / target 35
- 数据：OpenWeatherMap　·　广告：Google Mobile Ads（当前用官方测试广告位）

---

## 1. 环境要求
- **JDK 17+**（推荐 21）
- **Android Studio**（最新版）或 Android SDK + `platform-35` + `build-tools;35.0.0`
- Git

## 2. 拉代码
```bash
git clone <你的远程仓库地址> NimboWeather
```
> ⚠️ **建议 clone 到纯英文路径**（如 `C:\dev\NimboWeather`）。中文/特殊字符路径会触发 AGP 报错；本仓库已用 `android.overridePathCheck=true` 兜底（无 NDK 安全），但纯英文路径最省心。

## 3. 配置本地密钥（不进版本库，需各自准备）
这些文件已在 `.gitignore`，**不会提交**，每位成员要自行放置：

| 文件 | 来源 | 说明 |
|---|---|---|
| `local.properties` | 复制 `local.properties.example` 改 | 填 `sdk.dir` + `OPENWEATHER_API_KEY` |
| `app/google-services.json` | Firebase 控制台下载 | RemoteConfig + FCM 用（接入后才需要）；package 必须 `com.nimboweather.forecast` |
| 发布 keystore（`*.jks`） | 团队统一保管 | 仅 release 签名用，**绝不进库**，走密钥库/安全渠道分发 |

> OpenWeatherMap key：https://home.openweathermap.org/api_keys （免费档够用，新 key 约 1-2h 激活）

## 4. 编译运行
```bash
./gradlew assembleDebug          # 产出 app/build/outputs/apk/debug/app-debug.apk
# 或在 Android Studio 点 ▶ Run
```
> 本机模拟器若启动即终止（WHPX/虚拟化冲突），改用 **Android Studio 远程设备(Device Streaming)** 或 **USB 真机**。

## 5. 模块结构（`app/src/main/java/com/nimboweather/forecast/`）
- `data/` — OpenWeatherMap 接入、模型、缓存
- `location/` `prefs/` — 定位、单位/城市持久化
- `ui/` — `MainActivity`/`MainViewModel`、`home/`（卡片渲染器 + 罗盘 `CompassDialView`）、`findcity/`、`detail/`、`ads/`（全屏 Native 容器）
- `ads/` — `AdMediator`（统一编排/频控）+ `AdNetworkAdapter` 接口 + `adapters/AdmobAdapter`（预留 TopOn/MAX）+ `AppOpenAdManager`
- `config/` — `AdStrategy` / `StrategyProvider`（本地默认；接 Firebase 后换 RemoteConfig）/ `CardLayoutConfig`
- `consent/` — UMP 同意框
- `work/` `notify/` `widget/` — WorkManager 定时刷新、通知、桌面 Widget

## 6. 注意
- 广告位现为 **AdMob 官方测试 ID**，上线前换正式 ID（在 `config/StrategyProvider` + Manifest 的 `APPLICATION_ID`）。
- 合规：广告全屏延迟关闭等行为由 `AdStrategy` 配置（后续 RemoteConfig 云端可调）；上架需隐私政策/Data Safety。
