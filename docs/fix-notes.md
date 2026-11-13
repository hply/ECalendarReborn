# 启动崩溃修复记录（StackOverflowError）

## 现象

用户首次安装启动即闪退，日志中反复出现：

```
java.lang.StackOverflowError
  at com.ecalendar.reborn.core.lunar.LunarCalendar.solarToLunar(LunarCalendar.kt:138)
  at com.ecalendar.reborn.core.lunar.Festivals.isLunarNewYearEve(Festivals.kt:68)
  at com.ecalendar.reborn.core.lunar.Festivals.of(Festivals.kt:62)
  at com.ecalendar.reborn.core.lunar.LunarCalendar.solarToLunar(LunarCalendar.kt:138)
  ...
```

## 根因

`solarToLunar` 在构造结果时调用 `Festivals.of` 汇总节日；而 `Festivals.of` 里的「除夕」判断
`isLunarNewYearEve` 又反向调用 `solarToLunar` 计算今天与明天的农历。两者构成无终止条件的
相互递归，主日历页在 Compose 首帧渲染时（`MonthGrid` 遇到今天/腊月那天）即爆栈。

## 修复

1. `LunarCalendar` 新增 `internal fun raw(date): Raw`，只做公历→农历的纯算术（年/月/日/是否闰月/该月天数/闰月），
   **不做任何节日聚合**。
2. `solarToLunar` 改为 `raw(date)` + `Festivals.of(...)` 组装 `LunarDate`。
3. `Festivals.isLunarNewYearEve` 改用 `LunarCalendar.raw`，彻底断开反向回调；除夕判定仍保留
   「腊月非闰 + 明日为正月初一」语义。

## 验证

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL
- JVM 直测（避开 Android 层）：`2026-02-16 → 二〇二五年腊月廿九 | fest=除夕`、
  `2026-02-17 → 二〇二六年正月初一 | fest=春节`，无递归、结果与历法一致。
- 建议今后给 `core/lunar` 增加纯 JVM 单测（`solarToLunar` 跑 1900–2100 每日、与 `lunarToSolar` 互逆），
  这类递归/越界问题在 CI 里就能拦住。
