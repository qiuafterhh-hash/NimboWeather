# 天气模拟动画优化 — 设计文档

> 分支:`feat/weather-fx-polish` · 日期:2026-06-25
> 目标:在不引入美术资源的前提下,把首页背景天气动画(`WeatherFxView`)从"固定 7 种粗糙粒子"提升为"真实数据驱动、克制精致、可平滑切换"的程序化动画。

## 背景与现状

- 首页每个城市页背后有一层 `WeatherFxView`(157 行,单文件包揽所有逻辑),由 `WeatherPageFragment:44` 通过 `setCondition(icon)` 驱动,只看 OWM 的 icon code + 昼夜后缀。
- 底色由 `SkyGradient`(按条件出渐变)负责,FX 层叠在卡片(半透明)与底色之间。
- 现状局限:
  - 仅 7 种 `Fx`(RAIN/SNOW/CLOUDS/STORM/STARS/FOG/NONE),无强度概念 —— 小雨和暴雨画面一样。
  - 不感知风(无倾斜、无风向)。
  - 切换是 `seed()` **硬重置**,视觉突兀。
  - 单层粒子,无视差;云是单椭圆,显假。

### 竞品参考结论(Local Weather Alerts 1.7.2,已逆向 `_xapk_extract/jadx_out`)

竞品"丰富感"的来源是 **①状态细分**(`条件 × 昼夜 × 强度 × 遷移`,71 张 `bg_*art*.webp`)+ **②预渲染美术资源**(`TextureVideoView` 动画背景),**不是**代码粒子。
本项目作为独立开发,无美术管线,故 **不抄资源路线**,只借鉴其"状态细分"的思路,用纯代码实现强度/昼夜/平滑过渡。

## 决策(已与用户确认)

| 维度 | 决策 |
|---|---|
| 技术路线 | **纯代码增强** `WeatherFxView`,不引入 webp/视频/Lottie 资源,包体零增长 |
| 覆盖场景 | 全部:雨/雷暴、雪、晴(昼/夜)、云/阴、雾/霾 |
| 强度/风向 | **用真实天气数据驱动**(OWM 风速/风向 + Open-Meteo 分钟降水) |
| 视觉分寸 | **克制精致**,以氛围为主,保证半透明卡片上的文字可读 |

## 架构设计

### 1. 数据接入 —— `FxSpec`

用一个不可变参数对象替代当前只传 `icon` 的方式:

```kotlin
data class FxSpec(
    val scene: FxScene,     // 解析后的场景枚举
    val windDeg: Int?,      // 风向(度);决定雨雪倾斜方向、云/雾漂移方向
    val windSpeed: Float,   // m/s;决定倾斜角度与云/雾速度
    val intensity: Float    // 0..1 归一化降水/浓度强度
)

enum class FxScene { CLEAR_DAY, CLEAR_NIGHT, CLOUDS, OVERCAST, RAIN, STORM, SNOW, FOG, NONE }
```

**强度 `intensity` 的来源(按优先级回退):**
1. `HomeCard.Nowcast.series`(15 分钟 mm 序列)存在时,取峰值 mm 归一化(雨:0→1 映射约 0–8mm/15min;雪同理用更低上限)。
2. 无临近数据时,回退到 `HomeCard.Current.rainProb`(降水概率)。
3. 再无则按 icon code 粗分(如 `09` 阵雨 / `10` 雨 给固定中等值)。

**风:**
- `windDeg` 直接取 `HomeCard.Current.windDeg`(已存在)。
- `windSpeed` 需新增:在 `WeatherCardsBuilder` 给 `HomeCard.Current` 补一个数值字段 `windSpeed: Float`(来自已有 `Wind.speed`),供 FX 使用(现有的 `windText` 是给 UI 显示的字符串,不复用解析)。

**纯函数 `FxMapper`(framework-free,可单测):**
```kotlin
object FxMapper {
    fun sceneFrom(icon: String?): FxScene
    fun intensityFrom(nowcastSeries: List<Double>?, rainProb: Int?, icon: String?): Float
    fun tilt(windDeg: Int?, windSpeed: Float): Float   // 返回弧度或归一化倾斜量
}
```
与 `AirQualityIndex` / `MoonPhase` 同等定位 —— 放 `data/` 或 `ui/home/`,无 Android 依赖,JUnit 覆盖。

### 2. 结构重构 —— 图层化

当前单文件什么都干。拆为小图层接口,每层职责单一、可独立理解:

```kotlin
interface FxLayer {
    fun resize(w: Int, h: Int)
    fun update(dtSec: Float, spec: FxSpec)
    fun draw(canvas: Canvas, alpha: Float)   // alpha 由场景交叉淡化驱动
}
```

图层实现(每个一小段,边界清晰):
- `RainLayer`、`SnowLayer`、`CloudLayer`、`StarLayer`、`FogLayer`、`LightningLayer`。
- 近/远视差通过同类型图层两个实例(不同 depth 参数)实现,不新增类。

`WeatherFxView` 退化为协调者:持有当前激活图层列表、驱动帧循环(`postInvalidateOnAnimation`)、在场景切换时管理交叉淡化。新增 `setSpec(spec: FxSpec)`;为兼容/测试保留薄的 `setCondition(icon)`(内部构造默认 `FxSpec`)。

### 3. 各场景增强细节(克制、数据驱动)

| 场景 | 增强点 |
|---|---|
| **雨 / 雷暴** | 雨丝数量&下落速度随 `intensity` 缩放;**倾斜角 = `FxMapper.tilt(windDeg, windSpeed)`**(风向定左右、风速定角度);**近/远双层视差**(近景粗而快、远景细而慢、alpha 更低);雷暴:闪电柔和泛白 → 渐暗恢复,触发时序随机(沿用现有 `nextFlash` 思路但软化) |
| **雪** | 雪花大小/密度分层;随风横向摇摆(`windSpeed` 驱动 `vx`);近/远视差 |
| **晴(昼)** | 极淡光晕/光斑缓慢漂移 + 稀疏浮尘(很轻,alpha 低);**点缀,可裁剪** |
| **晴(夜)** | 现有星空闪烁 + 偶发流星划过 + 柔和月晕;**流星/月晕为点缀,可裁剪** |
| **云 / 阴** | 多层云视差漂移,速度随 `windSpeed`、方向随 `windDeg`;云块用**多个叠加圆**做蓬松边缘(替代当前单 `drawOval`);`OVERCAST` 比 `CLOUDS` 更密更暗 |
| **雾 / 霾** | 多层横向雾气漂移,浓度分级(icon `50` 雾最浓) |

### 4. 平滑切换 + 昼夜过渡

- 场景切换不再 `seed()` 硬重置:维护一个全局 `sceneAlpha` 补间(~500ms),旧图层淡出、新图层淡入,通过 `FxLayer.draw(canvas, alpha)` 的 alpha 参数实现。
- 切换期间短暂同时持有"旧场景图层 + 新场景图层"两组,补间结束后释放旧组。
- 昼夜:底色由 `SkyGradient` 负责;FX 仅在 `StarLayer` ↔ 晴日光晕层之间交叉淡化,不做额外昼夜色处理。

### 5. 性能与可读性

- **沿用现有机制**:粒子数封顶、`dt` 钳制(`coerceIn(0f, 0.05f)`)、`isShown` 时才续帧、`onDetachedFromWindow` 停循环、`TestEnv.active` 下渲染单帧不进循环。
- 整体 alpha 压低(克制路线),保证半透明卡片上的文字清晰;高强度时密度有上限,不无限堆粒子。
- 全部在现有单 `View.onDraw` 内绘制,**不引入额外线程、不加载任何资源**。

### 6. 测试策略

- `FxMapper` 三个纯函数做 JUnit 单测(场景解析边界、强度归一与回退优先级、风向倾斜符号与范围)。
- 图层动画本身(粒子运动)不做强测;依赖现有 `TestEnv` 单帧路径保证 UI 测试可达 idle。

## 影响面 / 改动清单

- 新增:`ui/home/FxMapper.kt`、`ui/home/FxSpec.kt`(或同文件)、`FxLayer` 接口 + 各图层(可同文件或拆分)。
- 修改:`ui/home/WeatherFxView.kt`(重构为协调者 + 图层)、`ui/home/WeatherPageFragment.kt`(组装 `FxSpec`、改调 `setSpec`)、`ui/home/WeatherCardsBuilder.kt` + `HomeCard.Current`(补 `windSpeed`)。
- 新增测试:`app/src/test/.../FxMapperTest.kt`。
- 不改:`SkyGradient`、广告/数据/widget 等其余模块;包体不变。

## 非目标(YAGNI)

- 不引入任何美术资源(webp/视频/Lottie)。
- 不做竞品式 71 张状态细分背景。
- 不引入渲染线程 / `SurfaceView` / `TextureView`(现有 `View.onDraw` 足够,克制路线下负载低)。
- 不改造 `SkyGradient` 的昼夜底色逻辑。

## 验收标准

1. 小雨与暴雨画面明显不同(密度/速度随真实 mm 变化)。
2. 有风时雨雪可见倾斜,方向与 `windDeg` 一致。
3. 雨/雪/云有近远视差层次。
4. 切换城市/条件时为淡入淡出,无硬跳。
5. 卡片文字在所有场景下仍清晰可读。
6. `FxMapperTest` 全绿;`./gradlew :app:testDebugUnitTest` 通过;`assembleDebug` 通过。
