# 底部天气剖影插图 — 设计文档

> 分支:待建 · 日期:2026-06-26
> 目标:在首页底部加一条**随天气变化的纯代码矢量景观剖影**(层叠山丘地平线),提升整体样式,且不引入任何美术资源、包体零增长。

## 背景与现状

- 首页 `fragment_weather_page.xml` 结构:`pageRoot`(FrameLayout,背景 `bg_sky`)→ `WeatherFxView`(全屏天气粒子动画)→ `SwipeRefreshLayout` → `NestedScrollView` → `llContent`(卡片流)。
- 背景视觉现有两层:`SkyGradient`(按条件出渐变,作为 `pageRoot` 背景由 fragment 设置)+ `WeatherFxView`(雨/雪/星/雾粒子)。**没有地平线/景观元素**,底部偏空。
- 天气类型统一从 OWM 图标码 + 昼夜推导,规范为 **8 个场景**(SkyGradient / WidgetVisuals 一致):晴昼、晴夜、多云昼、多云夜、雨、雷暴、雪、雾/霾。
- 项目**无美术管线**(FX 动画走纯代码即为此),故本插图同样走纯代码矢量。

## 决策(已与用户确认)

| 维度 | 决策 |
|---|---|
| 素材来源 | **纯代码矢量插图**(Canvas + Path 绘制),零美术资源,包体不变 |
| 是否随天气 | **随 8 个场景变**,与背景/动画联动 |
| 形态/位置 | **屏底固定前景剖影带**(在天空+动画之上、卡片之下),类 iOS 天气地平线 |
| 母题 | **层叠山丘**(2 层景深 + 少量前景点缀);备选天际线/波浪暂不做 |
| 动效 | v1 **静态**,仅场景切换时 ~400ms 颜色交叉淡化;不做飘动 |
| 视觉分寸 | **克制**,低饱和、与卡片有对比,保证卡片文字可读 |

## 架构设计

### 1. 组件 —— `WeatherScenicView`

新增 `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherScenicView.kt`,继承 `View`,纯 Canvas 绘制:

- **分层山丘**:2 层 `Path`(远山、近山)构成起伏地平线,颜色深浅区分景深;近山压在远山之上。
- **前景点缀**:近山轮廓上点缀少量松树剪影(几个简单三角/圆锥 Path),数量固定、低调。
- **底部铺满**:View 锚定屏底、整宽,高约 30% 屏高;山体从底部向上起伏,顶部留出天空。
- **场景驱动**:持有当前 `ScenicPalette`;`setScene(icon: String?)` 切换时,启动一个 ~400ms 的 alpha 交叉淡化(旧调色板淡出、新调色板淡入),与 `WeatherFxView` 的淡化思路一致。无 TestEnv 下用 `postInvalidateOnAnimation` 驱动淡化帧;淡化结束即停,**静态时零重绘**。
- **TestEnv**:`TestEnv.active` 下直接渲染目标调色板单帧(不进淡化循环),保证 UI 测试可达 idle —— 与 `WeatherFxView` 一致。

绘制顺序(自底向上):远山 → 近山 → 前景树 → (雪场景)山顶雪盖 → (雾场景)底部白色雾纱渐变。

### 2. 层级(改 `fragment_weather_page.xml`)

在 `pageRoot` 内、`WeatherFxView` **之前**插入 `WeatherScenicView`:

```
pageRoot (背景 bg_sky / SkyGradient)
 ├─ WeatherScenicView   ← 新增:layout_height="match_parent" 或固定,layout_gravity="bottom",由 View 内部只在底部绘制
 ├─ WeatherFxView       ← 全屏粒子;雨/雪/星落在山丘之前
 └─ SwipeRefreshLayout → NestedScrollView → llContent(卡片,最上层)
```

效果:`SkyGradient` 渐变(底)→ 底部山丘剖影 → 天气粒子(在山丘前)→ 半透明卡片(最上)。卡片滚到顶时屏幕下半部透出剖影;上滚时卡片覆盖之 —— iOS 地平线观感。

> 说明:`WeatherScenicView` 用 `match_parent` 高度但内部只在底部 30% 区域绘制(便于按屏高比例定位),其余透明。

### 3. 场景调色板 —— `ScenicPalette`(纯函数,可单测)

新增 `app/src/main/java/com/nimboweather/forecast/ui/home/ScenicPalette.kt`:

```kotlin
/** Color/flag set for one scene's scenic band. */
data class ScenicPalette(
    val farHill: Int,       // 远山色 (ARGB)
    val nearHill: Int,      // 近山色
    val accent: Int,        // 前景点缀(树)色
    val snowCaps: Boolean,  // 山顶雪盖
    val fogVeil: Boolean    // 底部雾纱
)

/** Pure mapping icon -> palette (framework-free, unit-tested). */
object ScenicPalettes {
    fun from(icon: String?): ScenicPalette
}
```

颜色用 `Int`(ARGB),以 `0xAARRGGBB.toInt()` 整数常量定义,保持纯 JVM 可测、无 Android `View`/`Color` 依赖。

复用 `FxMapper.sceneFrom(icon)` 得到 `FxScene`,再映射到 8 场景调色板。FX 把"少云夜"并入晴夜、多云细分 CLOUDS/OVERCAST —— 剖影统一按 SkyGradient 的 8 场景口径处理(即 02n 仍按"多云夜"上色),映射在 `ScenicPalette.from` 内自行按 icon code 判定,**不强行复用 FxScene 的细分差异**。

| 场景(icon) | 远山 | 近山 | 点缀 | snowCaps | fogVeil |
|---|---|---|---|---|---|
| 晴昼 01d | 暖中蓝 | 暖深蓝 | 深绿 | 否 | 否 |
| 晴夜 01n | 深蓝 | 更深蓝 | 暗青 | 否 | 否 |
| 多云昼 02/03/04 d | 灰蓝 | 深灰蓝 | 墨绿 | 否 | 否 |
| 多云夜 02/03/04 n | 暗蓝灰 | 更暗蓝灰 | 暗墨绿 | 否 | 否 |
| 雨 09/10 | 去饱和青灰 | 暗青灰 | 暗绿 | 否 | 否 |
| 雷暴 11 | 暗灰 | 最暗灰 | 近黑绿 | 否 | 否 |
| 雪 13 | 冷灰 | 冷深灰 | 暗绿 | **是** | 否 |
| 雾/霾 50 | 灰 | 深灰 | 灰绿 | 否 | **是** |

颜色与 `SkyGradient` 同色系(从其常量取色或邻近取色),保证整体协调、低饱和。

### 4. 驱动(改 `WeatherPageFragment`)

`UiState.Data` 分支已有 `icon`,加一行:

```kotlin
view.findViewById<WeatherScenicView>(R.id.weatherScenic).setScene(icon)
```

与现有 `setSpec(...)`(驱动 FX)、`SkyGradient.drawable(icon)`(驱动底色)并列,三者同源、同步切换。

### 5. 性能与可读性

- 静态绘制:仅场景变化时重绘(+ ~400ms 淡化帧),平时零开销;无独立线程、无资源加载,包体不变。
- 低饱和 + 锚底,只占屏幕下方,**不干扰卡片文字**。
- 不可见暂停/`isShown` 守卫沿用 View 惯例;`TestEnv` 单帧路径保证测试可达 idle。

### 6. 测试

- `ScenicPaletteTest`(JUnit):验证 8 场景的映射(各 icon code → 正确的 snowCaps/fogVeil 标志与场景归类边界,如 02n=多云夜、04d=多云昼、50=雾)。
- View 绘制本身不强测(同 `WeatherFxView` 惯例)。

## 影响面 / 改动清单

- 新增:`ui/home/WeatherScenicView.kt`、`ui/home/ScenicPalette.kt`。
- 修改:`res/layout/fragment_weather_page.xml`(插入 scenic view + 加 `@id/weatherScenic`)、`ui/home/WeatherPageFragment.kt`(调 `setScene`)。
- 新增测试:`test/.../ui/home/ScenicPaletteTest.kt`。
- 不改:`SkyGradient`、`WeatherFxView`、`FxMapper`、卡片/数据/广告等其余模块;包体不变。

## 非目标(YAGNI)

- 不引入任何位图/webp/Lottie/视频资源。
- v1 不做飘动/视差动画(山丘静态;只有切换淡化)。
- 不做城市天际线/抽象波浪母题(仅层叠山丘)。
- 不改 `SkyGradient` 与 `WeatherFxView` 的现有逻辑。

## 验收标准

1. 首页底部出现层叠山丘剖影,位于天空+粒子之上、卡片之下。
2. 剖影随当前天气切换(8 场景各不同;雪有山顶雪盖、雾有底部雾纱)。
3. 切换城市/天气时为颜色淡化,无硬跳。
4. 雨/雪粒子落在山丘前方;卡片文字在所有场景下清晰可读。
5. `ScenicPaletteTest` 全绿;`./gradlew :app:testDebugUnitTest` 与 `assembleDebug` 通过;APK 体积不增长。
