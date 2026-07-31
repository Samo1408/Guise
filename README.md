# Guise Reborn

Guise Reborn 是 Guise 的社区维护续作。它是一个 LSPosed/Xposed 模块，可针对指定应用配置机型、系统属性、Android ID、区域、网络、Wi-Fi、定位及基站等运行环境信息。

本仓库保留了公开源码仓库的完整 Git 历史，并基于作者公开表示“感兴趣的可以接手继续开发”的版本继续维护：

- 原项目：[Houvven/Guise](https://github.com/Houvven/Guise)
- 本仓库采用的源码上游：[AlliotTech/Guise](https://github.com/AlliotTech/Guise)
- 上述源码仓库的 GitHub Fork 上游：[shenghuang147/Guise](https://github.com/shenghuang147/Guise)

## 当前状态

当前维护线以原版 `Guise 1.1.2` 的完整功能为起点，已经转为现代化主线。应用仍保留 `1.1.3-reborn.1` 版本号（正式发布前再统一调整），当前面向 Android 17 / API 37 构建。

本轮刷新包括：

- 使用 JDK 17、Gradle 9.6.1、Android Gradle Plugin 9.3.1、Kotlin 2.4.10、Android SDK 与 Build Tools 37。
- UI 迁移到当前稳定版 Jetpack Compose BOM `2026.06.01` 和 Material 3，启用 edge-to-edge、系统/浅色/深色主题及 Monet 动态取色。
- 英文、简体中文、日文和阿拉伯文资源完整对应；Android 13 及以上可通过系统的应用语言设置切换。
- 模块入口、Hook 拦截器、Remote Preferences 和作用域管理迁移到 Modern Xposed API 102；不再使用旧版 XposedBridge/XposedHelpers、旧模块元数据和直接修改 LSPosed 数据库的实现。
- 采用 MediaStore 与系统文件选择器导入导出，移除“所有文件访问”、旧外部存储权限和明文网络配置。
- 升级 Room、MMKV 2、KSP、协程、序列化和 AndroidX；移除 Accompanist、Ktor 1.x、旧 SQLite shell 及单独的通用 `lib` 模块。
- MMKV 2 官方仅提供 64 位 Android 原生库，因此当前 APK 仅构建 `arm64-v8a`。

现有功能包括：

- 已安装应用管理、搜索、过滤、排序及 LSPosed 作用域同步。
- 按应用配置设备型号、显示大小（DPI）、系统版本、网络、SIM、Wi-Fi、唯一标识、定位和基站信息。
- 电池电量、截图限制、窗口隐私及联系人/图片/视频/音频访问控制。
- 内置设备数据库、配置模板、预设、导入导出、运行日志和应用设置。

内置品牌与型号数据库来自 [KHwang9883/MobileModels](https://github.com/KHwang9883/MobileModels) 的[官方 CSV 导出](https://github.com/KHwang9883/MobileModels-csv)，数据库部分遵循 [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)；具体来源版本与转换说明见 `app/src/main/assets/devices.NOTICE.txt`。

全球运营商预设来自 MIT 许可的 [pbakondy/mcc-mnc-list](https://github.com/pbakondy/mcc-mnc-list)，编码依据 [ITU-T E.212](https://www.itu.int/rec/T-REC-E.212/en)；具体筛选规则与来源版本见 `app/src/main/assets/carriers.NOTICE.txt`。

Android 版本、API、DPI、网络和语言等内置预设集中维护在 `app/src/main/res/raw/presets.json`，避免把数据散落在 UI 代码中。

## 开放源代码许可

应用内可在“设置 → 开放源代码许可”查看以下项目、数据来源和许可证，并直接打开项目主页或许可证原文：

- [Guise Reborn](https://github.com/daxiaamu/Guise_Reborn)：本应用代码采用 [GNU GPL v3.0 or later](LICENSE)，并保留原始 Guise 的 Git 历史和作者署名。
- [AndroidX / Jetpack Compose / Material 3](https://github.com/androidx/androidx)：[Apache License 2.0](https://source.android.com/docs/setup/about/licenses)。
- [Kotlin / kotlinx.coroutines / kotlinx.serialization](https://github.com/JetBrains/kotlin)：[Apache License 2.0](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt)。
- [libxposed API / service](https://github.com/libxposed)：[Apache License 2.0](https://github.com/libxposed/api/blob/master/LICENSE)。
- [MaterialKolor](https://github.com/jordond/MaterialKolor)：主体采用 [MIT License](https://github.com/jordond/MaterialKolor/blob/main/LICENSE)，其 Material Color Utilities 模块采用 [Apache License 2.0](https://github.com/material-foundation/material-color-utilities/blob/main/LICENSE)。
- [MMKV](https://github.com/Tencent/MMKV)：[BSD 3-Clause License](https://github.com/Tencent/MMKV/blob/master/LICENSE.TXT)。
- [MobileModels / MobileModels-csv](https://github.com/KHwang9883/MobileModels)：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)。
- [mcc-mnc-list](https://github.com/pbakondy/mcc-mnc-list)：[MIT License](https://github.com/pbakondy/mcc-mnc-list/blob/master/LICENSE)。

目前主要模块：

- `app`：Jetpack Compose 管理界面、配置数据和完整 Xposed Hook 实现。
- `ktx-xposed`：基于 Modern Xposed API 的 Hook 适配层和模块日志基础设施。
- `guise-test`：独立的 API 37 测试应用，用真实 Android API 核验各项伪装结果，不依赖 Guise 的配置对象。

## Guise Test

测试应用包名为 `com.daxiaamu.guise.test`。它可检查应用版本、设备与系统字段、Android ID、IMEI、手机号、网络、Wi-Fi、SIM、基站、定位、电池、语言地区、联系人/媒体空白通行和截图限制，并完整支持英文、简体中文、日文和阿拉伯文。

使用时先安装测试 APK，在 Guise 中为“Guise Test”配置容易识别的值并同步作用域，然后强行停止并重新打开测试应用。测试应用直接读取系统 API；权限不足、设备无 SIM 或接口不受支持时会显示具体异常，不会把它们误判为 Hook 成功。版本值与编译值不同、或空白通行查询得到 `null` 游标时，界面会给出明确的 Hook 检测提示。

单独构建和检查测试应用：

```powershell
.\gradlew.bat :guise-test:assembleDebug :guise-test:lintDebug
```

产物位于 `guise-test/build/outputs/apk/debug/guise-test-debug.apk`，经过 R8 优化和资源收缩，并使用 Android 默认调试签名。

## 构建

需要：

- JDK 17
- Android SDK Platform 37
- Android SDK Build-Tools 37.0.0

在仓库根目录创建不纳入版本控制的 `local.properties`：

```properties
sdk.dir=D\:\\AndroidSDK
```

然后执行：

```bash
./gradlew assembleDebug lintDebug
```

Windows：

```powershell
.\gradlew.bat assembleDebug lintDebug
```

当前不配置正式发布签名，Debug APK 沿用 Android 默认调试签名。不要向仓库提交签名文件、口令或本机 SDK 路径。

## 开发原则

1. Android 与 Modern Xposed 的正式新 API 优先；旧实现妨碍安全性、可维护性或新系统支持时直接替换。
2. 保留现有用户配置和模板的数据迁移能力，但不为无效或危险的内部实现长期背负兼容层。
3. Hook 功能按机型、系统属性、标识符、网络和定位分别隔离，单个 Hook 失败不得拖垮目标进程。
4. 发布 APK 前必须在真实的、已安装支持 Modern Xposed API 102 框架的 Android 设备上回归。

Android 17 对面向 API 37 的应用收紧了 `static final` 字段修改。设备构建字段伪装会继续兼容较旧目标应用，但不能把反射篡改视为面向 API 37 应用的可靠能力；版本信息伪装已经改为修改系统 `PackageInfo` 返回值。

## 许可证

Guise Reborn 的应用代码采用 [GNU General Public License v3.0 or later](LICENSE) 发布。Copyright © 2026 大侠阿木及 Guise Reborn 贡献者。您可以在遵守该许可证、保留版权与许可证声明并公开相应源代码的前提下使用、修改和分发。

原始 Guise 的作者署名及 Git 历史继续保留。独立数据资产维持各自的上游许可证：设备数据库为 `CC BY-NC-SA 4.0`，运营商预设为 `MIT License`；详见应用内“开放源代码许可”和对应的 `NOTICE` 文件。

---

## 【以下为原作者说明】

原项目：[Houvven/Guise](https://github.com/Houvven/Guise)

本说明所在的源码仓库：[AlliotTech/Guise](https://github.com/AlliotTech/Guise)

这个软件从第一个版本发布到现在应该有一年多了，具体哪天发布的我已经忘记了，也懒得去看。

只记得在某天晚上下载了王者荣耀发现其并没有开发我当时使用的设备的120帧开关，于是我想要去改机型，当时应用变量和应用伪装最后一个免费版本在我的设备上已经无法使用。我去了解了源计划，但是那个时候我还是大学生，50块钱的永久会员对于那时候的我有点肉疼，于是我便趁着寒假自己开发了一个类似的软件，后面开学后就一直没有更新。至于为什么最近更新了，有点难评…… 我马上毕业了，我在学校主修的方向是Java后端开发，但是无奈一直找不到工作。其实这次更新是想加入一些付费内容，可以看到最新的alpha版本移除了内置的机型库，我其实是想从这方面收费的，基础功能免费且继续开源。这几天也想了很多，这个APP诞生的初衷就是为了免费，我并不想违背我的初衷，而且这个APP并没有什么技术含量，属于非常简单的，任何一个计算机专业的大一学生可以做出来一个类似的。

说了这么多也不知道说了说什么，还是说说重点吧，这个APP我已经决定停更,感兴趣的可以接手继续开发。感谢这段时间大家的支持。
