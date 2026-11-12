# 构建与验证记录

## 工具链（本次实际使用）

| 工具 | 版本 / 路径 |
| --- | --- |
| JDK | 17（`/home/user/tools/jdk17`） |
| Android SDK | `/home/user/tools/android-sdk`（build-tools 35.0.0、platforms;android-35、platform-tools） |
| Gradle | 8.11.1（wrapper 自带 `gradle-wrapper.jar`） |
| AGP / Kotlin / KSP | 8.7.3 / 2.0.21 / 2.0.21-1.0.28 |
| Compose BOM | 2024.12.01（Material3 1.3.1） |
| Room / DataStore | 2.6.1 / 1.1.1 |
| REA | 6.8.0 + jadx-headless-mcp 0.7.1（分析阶段） |

## 命令

```bash
export JAVA_HOME=/home/user/tools/jdk17
export PATH=/home/user/tools/jdk17/bin:$PATH
export ANDROID_HOME=/home/user/tools/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:compileDebugKotlin :app:processDebugResources
./gradlew :app:assembleDebug
```

## 验证结果

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- `:app:processDebugResources` — BUILD SUCCESSFUL
- `:app:assembleDebug` — BUILD SUCCESSFUL，产物 `app/build/outputs/apk/debug/app-debug.apk`（约 17 MB）
- `aapt2 dump badging` 复核：`com.ecalendar.reborn.debug`，versionCode 100010，versionName `10.0.10-reborn.1`，
  minSdk 26 / targetSdk 35 / compileSdk 35，权限 8 条（+ 依赖合并的 WAKE_LOCK）
- Room KSP 已生成 schema 与实现类（`kspDebugKotlin` 通过）

## 过程中修掉的编译错误（记录以备复现）

1. `Zodiac.constellationOf` 用 `Pair` 直接比较 → `Pair` 非 `Comparable`，改为 `month*100+day` 整数比较。
2. `SectionTitle` 定义在 `ui/screens/CalendarScreen.kt`，被其他 screen 以 `ui.components` 导入 → 移到 `ui/components/Common.kt`。
3. `ToolsScreen` 的 `festivalItems(year)` 在 `LazyListScope`（非 Composable 上下文）里调用 `remember` →
   改为在 Composable 层 `remember(year)` 计算后传入 `List`。
4. `WeatherScreen` 误写的自定义 `Spacer` 扩展 → 删除，补 `width` 导入。
5. `MainActivity` / `SettingsScreen` 残留裸表达式与无用 `LocalContext` 导入 → 清理。

## 已知待办（后续可做）

- 未在真机/模拟器上跑 UI 冒烟测试；建议在 Android 12–15 设备上验证通知、精确闹钟与两个桌面小组件。
- release 构建未配置签名（需自备 keystore）与混淆规则（`proguard-rules.pro` 已预留 Room/Compose 规则）。
- 农历表为公开资料等价实现，如需与原包逐位一致，可再对 `CnNongLiData.lunarInfo` 常量数组做一次导出比对。
