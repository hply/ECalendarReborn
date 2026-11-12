# 中华万年历 · 重建版（ECalendarReborn）

对 `zhwnl.apk`（中华万年历 v10.0.10，`cn.etouch.ecalendar`，versionCode 100010）做**静态逆向取证**后，用 Kotlin + Jetpack Compose 独立重写的 Android 工程。

**硬约束：不含任何第三方埋点、统计、广告 SDK。** 原包占 84.7% 类体积的这些 SDK 全部未移植。

## 为什么要重建

原包 89.4 MB，8 个 dex（约 72 MB 字节码），34,976 个类，其中自研 `cn.etouch.*` 仅 5,341 个（15.3%）。真正的日历/黄历算法在 `cn.etouch.ecalendar.nongliManager.CnNongLiManager` / `CnJieQiManager` / `CnNongLiData` 里，其余体积被广告与统计 SDK 占据。

本工程只保留**能离线自洽运行**的自研功能，不联网也能用：

| 保留 | 不复刻 |
| --- | --- |
| 万年历月/周/日视图、农历与节气 | 账号登录与云同步 |
| 黄历（宜忌、建除、值神、冲煞、时辰吉凶、彭祖百忌、纳音、空亡、神煞） | 在线曲库 / 视频 / 播客 / 直播 |
| 择日查询 | 社区 UGC、AI 聊天、付费算命下单 |
| 日程 / 待办 / 记事 / 生日纪念日（AlarmManager 提醒） | 个推与厂商 push |
| 八字四柱、星座配对、生肖速查 | 广告（穿山甲 / 优量汇 / 快手 / 百度 / BeiZi / AnyThink / 美数） |
| 节日节气、桌面小组件、设置与换肤 | 统计（友盟 / AppLog / ti-monitor） |
| 天气（改接 Open-Meteo 公开接口） | 支付宝 / pingpp 支付、高德定位、自研 C++ 加固解码 |

## 技术栈

- Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.11.1 / JDK 17
- Jetpack Compose（BOM 2024.12.01）+ Material 3
- Room 2.6.1（KSP）+ DataStore 1.1.1
- AlarmManager + BroadcastReceiver 本地提醒，AppWidget `RemoteViews` 小组件
- `minSdk 26` / `targetSdk 35` / `compileSdk 35`

包名 `com.ecalendar.reborn`（debug 变体后缀 `.debug`），`versionCode 100010`，`versionName 10.0.10-reborn.1`。

## 目录

```
app/src/main/java/com/ecalendar/reborn/
├── core/lunar/          # 农历与黄历算法（纯 Kotlin，无 Android 依赖）
│   LunarDate / LunarCalendar / SolarTerm / Ganzhi / Festivals / Almanac / Zodiac / Bazi
├── data/                # Room 实体、DAO、数据库、设置、天气
├── reminder/            # 通知、闹钟调度、广播接收器
├── widget/              # 两个 AppWidget Provider
└── ui/                  # AppState、主题与换肤、组件、6 个页面、导航
```

## 构建

```bash
# 1. 本地 SDK 路径
 echo "sdk.dir=/path/to/Android/sdk" > local.properties

# 2. 构建 debug 包
./gradlew :app:assembleDebug

# 3. 装上设备
 adb install -r app/build/outputs/apk/debug/app-debug.apk
```

已在本机验证：`:app:compileDebugKotlin`、`:app:processDebugResources`、`:app:assembleDebug` 全部 BUILD SUCCESSFUL，产物 `app-debug.apk` 17 MB。

## 逆向证据

`docs/reverse-engineering-report.md` 是完整取证报告（包身份、组件全名单统计、类分布 census、核心算法反编译片段、逐 SDK 排除清单）。`docs/build-notes.md` 记录构建与修错过程。`docs/evidence/` 存放原始证据（`badging.txt`、`nongli-methods.txt` 等），大体积 JSON（`manifest.json` 714 KB、`classes-all.json` 2.6 MB）因体积未入库，需要时按报告中的命令重跑。

## 法律与伦理

- 本工程代码为独立重写，不含原包反编译源码、图片资源或签名文件。
- 算法实现参考通行的农历/干支/节气规则与对原包行为的静态观察，用于个人学习与兼容性研究。
- 原包名、商标、内容版权归原权利人所有。请勿用于商业分发或冒充原应用。
