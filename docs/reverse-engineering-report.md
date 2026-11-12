# 中华万年历 v10.0.10 逆向分析报告

> 目标 APK：`zhwnl.apk`（89,399,034 字节），附件 URL `https://aka.doubaocdn.com/s/B2Udm5Yy2j`
> 工具：REA 6.8.0（`reverse-engineer-anything`）+ JADX headless MCP 0.7.1 + aapt2（build-tools 35.0.0）
> 环境：JDK 17、Gradle 8.11.1、Android SDK 35
> 分析性质：**纯静态**（APK 从未被安装或执行，无模拟器运行）

## 1. 包级事实（观测）

| 项 | 值 | 来源 |
| --- | --- | --- |
| 包名 | `cn.etouch.ecalendar` | aapt2 badging / REA `inspect_android_package` |
| 应用名 | 中华万年历 | badging |
| versionCode / versionName | 100010 / 10.0.10 | badging |
| minSdk / targetSdk / compileSdk | 24 / 30 / 34 | badging |
| launchable-activity | `cn.etouch.ecalendar.ECalendar` | badging |
| native ABI | `arm64-v8a`、`armeabi-v7a` | badging |
| dex 数量 | 8（classes.dex ~9.2 MB … classes8.dex ~9.1 MB，合计约 72 MB） | APK 成员清单 |
| 原生库 | 36 个 arm64 `.so`（`libEcalendarLib.so`、`libdexvmp.so`、`libpanglearmor.so`、`libumeng-spy.so`、`libcrashsdk.so`、`libmp3.so`、`libgdx.so` 等） | APK 成员清单 |
| 类总数 | **34,976** | REA `inspect_android_package`（`class_count`） |
| 资源条目 | 8,901 | REA（`resource_count`） |
| 权限 | 75 条 | badging + manifest xmltree |
| 组件（REA） | 512 items（status `complete`，1 条 limitation） | REA components |
| 组件（xmltree 解析，含 alias） | 653：activity 523 / receiver 42 / service 41 / provider 38 / activity-alias 9 | `components.json` |
| 签名校验 | `not_performed` | REA limitations（未做 v1/v2/v3 摘要一致性验证） |
| 引擎 | `jadx-headless-mcp 0.7.1`，heap 4096 MiB | REA engine 字段 |

关键 limitations：REA 的组件清单为静态清单声明，**不代表运行时实际可达**；反射、动态加载与未解析调用未知；
`inspect_android_package` 未执行签名校验，故本报告不主张任何签名身份结论。

## 2. 组件与代码构成（观测 + 归并统计）

- 653 个组件中：`cn.etouch` 自研组件 346 个（activity 310），三方 307 个。
- 自研 activity 覆盖面：日历/黄历、天气、记事、待办、生日、算命测算、健康、视频、播客、AI 聊天、社区、会员支付。
- 类级 census（全量 34,976 类按包前缀归并）：

| 包前缀 | 类数 | 占比 | 判定 |
| --- | --- | --- | --- |
| `cn.etouch` | 5,341 | 15.3% | 自研 |
| `com.byazt` | 4,392 | 12.6% | 三方（广告聚合） |
| `com.smartdigimkt` | 2,085 | 6.0% | 三方（Mediabrix/广告） |
| `com.cqyh` | 1,980 | 5.7% | 三方（广告） |
| `com.huawei` | 1,822 | 5.2% | 三方（HMS/push） |
| `com.hihonor` | 1,590 | 4.5% | 三方（荣耀 push） |
| `com.beizi` | 1,278 | 3.7% | 三方（BeiZi 广告） |
| `com.anythink` | 1,221 | 3.5% | 三方（TopOn 广告聚合） |
| `androidx.media3` | 958 | 2.7% | 框架 |
| `com.google` | 929 | 2.7% | 三方（GMS/Firebase 组件） |
| `com.meishu` | 799 | 2.3% | 三方（美数广告） |
| `com.xiaomi` | 491 | 1.4% | 三方（小米 push） |
| `com.igexin` | 348 | 1.0% | 三方（个推） |
| `com.umeng` | 330 | 0.9% | 三方（友盟统计） |
| `com.bytedance` | 300 | 0.9% | 三方（穿山甲/字节 SDK） |
| `com.baidu` | 254 | 0.7% | 三方（百度广告/统计） |
| `com.getui` | 235 | 0.7% | 三方（个推 GETUI） |
| `com.kwad` | 235 | 0.7% | 三方（快手广告） |
| `com.vivo` | 235 | 0.7% | 三方（vivo push） |
| `com.amap` | 229 | 0.7% | 三方（高德定位） |
| `com.alipay` | 202 | 0.6% | 三方（支付宝支付） |
| `com.bykv` | 184 | 0.5% | 三方（穿山甲依赖） |
| `com.efs` | 154 | 0.4% | 三方（AppLog 统计） |

**结论（观测 + 归并）：三方代码约占 84.7%。** 用户明确要求"不要三方埋点统计广告 SDK"，
因此本次复刻只覆盖 `cn.etouch` 自研且可离线自洽的功能。

自研内部按 5 层包名 Top：`bean.net` 365、`module.fortune` 227、`module.mine` 183、
`module.calendar` 148、`module.ai` 146、`module.calculate` 142、`module.main` 140、
`module.pgc` 125、`tools.life` 109、`module.advert` 108、`module.voiceroom` 95、
`module.health` 90、`tools.almanac` 83、`module.podcast` 73、`common.component` 66、`module.weather` 61。

## 3. 核心算法复原（自研类，静态反编译）

定位到的核心类：

- `cn.etouch.ecalendar.nongliManager.CnNongLiManager`
  - 字段：`API`、`Animals`、`FangWei`、`Gan`、`Zhi`、`jieqi`、`lunarDate`、`lunarMonth`、`version`、`weekDays`
  - 方法：`calGongliToNongli`、`calNongliByMonth`、`calNongliJiangeDays`、`cyclical`、`cyclicalm`、
    `getGongliOneMonthDays`、`getGongliYearDays`、`getJieqi`、`isGongliYearIsLeapYear`、
    `lYearDays`、`leapDays`、`leapMonth`、`monthDays`、`nongliToGongli`、`AnimalsYear`、
    `calChongSha`、`calDayTimeCyclical`
- `CnJieQiManager`（字段 `chufu`/`dongzhi`/`mofu`/`shujiu`/`shuzi`，方法 `getJieQi`、`getShuJiuOrShufu`、`isHaveShuJiuOrShufu`）
- `CnNongLiData`（`jieqi`、`lunarInfo`、`maxYear`、`minYear`）

### 3.1 冲煞公式（逐字复原，证据 `nongli-methods.txt`）

```java
public String calChongSha(int i10) {
    StringBuilder sb2 = new StringBuilder();
    sb2.append("冲");
    int i11 = (i10 + 6) % 12;
    sb2.append(this.Animals[i11]);
    sb2.append("(");
    sb2.append(this.Gan[(i10 + 4) % 10]);
    sb2.append(this.Zhi[i11]);
    sb2.append(")煞");
    sb2.append(this.FangWei[(i10 + 3) % 4]);
    return sb2.toString();
}
```

其中 `i10` 为日支索引（0=子）。本工程 `core/lunar/Almanac.kt` 采用同一公式，并把
`FangWei` 定为 `["东","北","西","南"]`，使「子日煞南、丑日煞东、寅日煞北、卯日煞西」与通书一致
（原包 `FangWei` 字符串常量在反编译输出中被字段化，具体字面值未完整取出，此为**推断补全**，
已在代码注释中标注推断来源）。

### 3.2 时辰干支（`calDayTimeCyclical`）

原实现先取 `calGongliToNongli(y,m,d)[5] % 60` 得日序，再按日干分组决定 12 时辰的干支起始：
`i14 % 10 ∈ {0,5}` → 0；`{1,6}` → +12；`{2,7}` → +24；`{3,8}` → +36；`{4,9}` → +48，
即五鼠遁。随后按 `i14 % 12` 查 12 组吉凶数组。

本工程在 `core/lunar/Ganzhi.kt` 用 `start = intArrayOf(0,2,4,6,8)[dayGanIdx % 5]` 实现同一规则，
`core/lunar/Almanac.kt` 的十二时辰吉凶用值神起法 `hourGodStart = [8,10,0,2,4,6,8,10,0,2,4,6]`
（子午临申、丑未戌上、寅申加子、卯酉居寅、辰戌龙位、巳亥午上）。

### 3.3 农历数据表

`CnNongLiData` 采用经典的 1900–2100 年 `lunarInfo` 压缩整数表方案（每项编码闰月与各月大小）。
本工程 `core/lunar/LunarCalendar.kt` 使用等价公开表实现 `leapMonth/leapDays/monthDays/yearDays`，
并以 `BASE_DATE = 1900-01-31` 为锚点求年，`for (1..12)` 逐月累加、闰月单独处理。
节气使用 `SolarTerm.kt` 的分钟偏移表 + 回归年 `31556925974.7 ms`，基准 `1900-01-06 02:05 Asia/Shanghai`。

> 说明：`calChongSha` / `calDayTimeCyclical` 为**逐字复原**；`lunarInfo` 数值表与节气偏移表为
> **实现等价复刻**（公开历法资料 + 原包方法名/结构对照），未逐位比对原包常量数组。

## 4. 被剔除的三方 SDK（对应包前缀）

| 类别 | 包前缀 / 组件证据 |
| --- | --- |
| 广告 | `com.bytedance`、`com.bykv`、`com.byazt`（穿山甲聚合）、`com.kwad`（快手）、`com.baidu`、`com.beizi`、`com.anythink`（TopOn）、`com.meishu`（美数）、`com.smartdigimkt`、`com.cqyh`，以及 `libpanglearmor.so` |
| 统计/埋点 | `com.umeng`（友盟，含 `libumeng-spy.so`）、`com.efs`（AppLog）、`com.bytedance` 内 ti-monitor、`libcrashsdk.so` |
| 推送 | `com.igexin`/`com.getui`（个推）、`com.huawei`（HMS push）、`com.hihonor`、`com.xiaomi`、`com.vivo`、`com.qq`（厂商通道） |
| 支付 | `com.alipay`、pingpp |
| 定位 | `com.amap` |
| 加固/解码 | `libdexvmp.so`、`libEcalendarLib.so`、`libgdx.so`（自研 C++ 层，未复刻） |

因此复刻工程权限从原包 75 条收敛到 8 条（INTERNET、ACCESS_NETWORK_STATE、POST_NOTIFICATIONS、
RECEIVE_BOOT_COMPLETED、SCHEDULE_EXACT_ALARM、USE_EXACT_ALARM、VIBRATE、SET_WALLPAPER），
组件从 653 个收敛到 6 个（MainActivity、2 receiver、2 widget provider）。

> 注：`aapt2` 构建时合并入 `WAKE_LOCK`（来自 AndroidX 依赖），非本工程主动声明。

## 5. 复刻工程与证据的对应关系

| 复刻文件 | 依据 |
| --- | --- |
| `core/lunar/LunarCalendar.kt` | `CnNongLiManager` / `CnNongLiData` 方法签名与 lunarInfo 方案 |
| `core/lunar/SolarTerm.kt` | `CnJieQiManager.getJieQi` / `getShuJiuOrShufu` |
| `core/lunar/Ganzhi.kt` | `calDayTimeCyclical`、`cyclical`、`cyclicalm` |
| `core/lunar/Almanac.kt` | `calChongSha` 公式、黄历宜忌/建除/值神/彭祖百忌 |
| `core/lunar/Bazi.kt`、`Zodiac.kt` | `tools.mc`、`tools.astro`、`module.calculate` 包结构 |
| `data/*` | 原包 `bean.net` + Room 数据库（本工程重设计 6 张表） |
| `reminder/*` | `remind` 系列 Activity + AlarmManager |
| `widget/*` | `settings.ConfigureCalendarActivity4x2`、`Widget_Note_Dialog` |

## 6. 未解与边界

1. 原包 `FangWei` 字面值、`Animals`/`Gan`/`Zhi` 数组字面值未逐项取出，本工程按通书规则补全（推断）。
2. `lunarInfo`、节气偏移表未与原包常量逐位 diff（实现等价而非字节等价）。
3. 原包 8 个 dex 中 84.7% 为三方 SDK，未做逐类语义分析（超出复刻范围）。
4. 未执行签名校验，未对 `libdexvmp.so` 做脱壳/解密（属加固层，超出范围）。
5. 原包 310 个自研 Activity 中约 60% 依赖服务端接口（曲库、社区、AI、付费测算），无法离线复刻。

## 7. 证据文件

原始证据与归档包一起提供（`ECalendarRebuild-Archive.zip` 内的 `zhwnl-analysis/` 目录）：

- `badging.txt`（aapt2 badging 原始输出）
- `manifest-tree.txt`（aapt2 `dump xmltree --file AndroidManifest.xml`，471 KB）
- `components.json`（653 条组件解析结果）
- `manifest.json`（REA `inspect_android_package` 完整信封，714 KB，含 `evidence_id`）
- `classes-own.json`（`cn.etouch.ecalendar` 类检索，5,093 类；REA evidence_id
  `ev_a07e2264aed9a2888e103e4b5651c285bd0614b0d42b88ec3ed3218f14a66eaf`）
- `classes-all.json`（全量 34,976 类）
- `nongli-methods.txt`（`calChongSha` / `calDayTimeCyclical` 反编译源码）

复现命令（需自行准备 JDK 17 与 `jadx-headless-mcp-0.7.1-all.jar`）：

```bash
export JAVA_HOME=/path/to/jdk17; export PATH=$JAVA_HOME/bin:$PATH
export REA_JADX_MCP_JAR=/path/to/jadx-headless-mcp-0.7.1-all.jar
export REA_JADX_HEAP_MIB=4096 REA_JADX_OPERATION_TIMEOUT_MS=600000
rea inspect-android-package /path/to/zhwnl.apk --json > manifest.json
rea search-android-classes /path/to/zhwnl.apk --query cn.etouch.ecalendar --json > classes-own.json
```
