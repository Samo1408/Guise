# Guise Reborn

Guise Reborn 是 Guise 的社区维护续作。它是一个 LSPosed/Xposed 模块，可针对指定应用配置机型、系统属性、Android ID、区域、网络、Wi-Fi、定位及基站等运行环境信息。

本仓库保留了公开源码仓库的完整 Git 历史，并基于作者公开表示“感兴趣的可以接手继续开发”的版本继续维护：

- 原项目：[Houvven/Guise](https://github.com/Houvven/Guise)
- 本仓库采用的源码上游：[AlliotTech/Guise](https://github.com/AlliotTech/Guise)
- 上述源码仓库的 GitHub Fork 上游：[shenghuang147/Guise](https://github.com/shenghuang147/Guise)

## 当前状态

当前维护线已恢复到与原版 `Guise 1.1.2` APK 精确对应的完整源码，并升级为可使用 JDK 17、Gradle 8.7、AGP 8.6.1 和 Android SDK 35 构建的 `1.1.3-reborn.1`。此前不完整的 `2.x alpha` 重写不再作为主维护线。

现有功能包括：

- 已安装应用管理、搜索、过滤、排序及 LSPosed 作用域同步。
- 按应用配置设备型号、系统版本、网络、SIM、Wi-Fi、唯一标识、定位和基站信息。
- 电池电量、截图限制、窗口隐私及联系人/图片/视频/音频访问控制。
- 内置设备数据库、配置模板、预设、导入导出、运行日志和应用设置。

目前主要模块：

- `app`：Jetpack Compose 管理界面、配置数据和完整 Xposed Hook 实现。
- `ktx-xposed`：Xposed Hook、共享配置和模块日志基础设施。
- `lib`：命令执行及 SQLite 辅助功能。

## 构建

需要：

- JDK 17
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0 或兼容版本

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

Debug 构建不需要发布签名。当前 release 构建默认不配置作者签名；正式发布前需由维护者在本机或 CI 中安全配置新的签名，且不要提交签名文件、口令或本机 SDK 路径。

## 开发原则

1. 以完整的 `1.1.2` 功能和配置格式为兼容基线，优先修复构建、崩溃和 Android/LSPosed 兼容问题。
2. Hook 功能按机型、系统属性、标识符、网络和定位分别验证，避免单个 Hook 失败拖垮目标进程。
3. 架构迁移必须在不丢失现有界面、模板、日志和 Hook 功能的前提下进行。
4. 发布 APK 前必须在真实的、已安装 LSPosed 的测试设备上回归。

## 许可证

上游仓库当前没有提供明确的许可证文件。本仓库暂不擅自为历史代码指定许可证；在许可证和历史代码授权范围得到进一步确认前，请谨慎分发衍生 APK 或复用代码。

---

## 【以下为原作者说明】

原项目：[Houvven/Guise](https://github.com/Houvven/Guise)

本说明所在的源码仓库：[AlliotTech/Guise](https://github.com/AlliotTech/Guise)

这个软件从第一个版本发布到现在应该有一年多了，具体哪天发布的我已经忘记了，也懒得去看。

只记得在某天晚上下载了王者荣耀发现其并没有开发我当时使用的设备的120帧开关，于是我想要去改机型，当时应用变量和应用伪装最后一个免费版本在我的设备上已经无法使用。我去了解了源计划，但是那个时候我还是大学生，50块钱的永久会员对于那时候的我有点肉疼，于是我便趁着寒假自己开发了一个类似的软件，后面开学后就一直没有更新。至于为什么最近更新了，有点难评…… 我马上毕业了，我在学校主修的方向是Java后端开发，但是无奈一直找不到工作。其实这次更新是想加入一些付费内容，可以看到最新的alpha版本移除了内置的机型库，我其实是想从这方面收费的，基础功能免费且继续开源。这几天也想了很多，这个APP诞生的初衷就是为了免费，我并不想违背我的初衷，而且这个APP并没有什么技术含量，属于非常简单的，任何一个计算机专业的大一学生可以做出来一个类似的。

说了这么多也不知道说了说什么，还是说说重点吧，这个APP我已经决定停更,感兴趣的可以接手继续开发。感谢这段时间大家的支持。
