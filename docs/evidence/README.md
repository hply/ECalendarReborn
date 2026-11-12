# docs/evidence

原始取证输出体积较大（合计约 4.5 MB），未全部提交到仓库，与归档包 `ECalendarRebuild-Archive.zip`
内的 `zhwnl-analysis/` 目录一并提供。其中较为关键的两份可直接在归档包中查看：

| 文件 | 说明 |
| --- | --- |
| `badging.txt` | aapt2 badging 原始输出（包身份、权限、ABI） |
| `nongli-methods.txt` | `calChongSha` / `calDayTimeCyclical` 反编译源码（逐字复原依据） |
| `components.json` | 653 条组件解析结果 |
| `manifest-tree.txt` | aapt2 xmltree 原始输出（471 KB） |
| `manifest.json` | REA `inspect-android-package` 完整信封（714 KB） |
| `classes-own.json` | 自研包类检索结果（5,093 类 / 632 KB） |
| `classes-all.json` | 全量类清单（34,976 类 / 2.6 MB） |

复现方式见 `docs/reverse-engineering-report.md` 第 7 节。
