# 拾页（暂定名）

一款用于记录实体书阅读经历的 Android 应用。它围绕“拿起一本书、专注阅读、记录页码与随记、回顾阅读轨迹”设计，强调精致、克制和有仪式感的使用体验。

## 项目文档

- [产品设计文档](docs/product-design.md)
- [开发文档](docs/development.md)

## 第一版目标

- 通过 ISBN 扫描或手动录入添加实体书
- 记录每次阅读的起止时间、起止页码和累计时长
- 按页码计算阅读进度
- 创建与书籍、页码关联的随记
- 设置阅读目标和本地提醒
- 在完全本地保存个人数据，并支持备份与恢复

> “完全离线”在本项目中指无账号、无云同步、核心功能离线可用。ISBN 书籍资料查询需要临时联网，查询结果保存到本地；断网时可手动录入。

## 技术栈

当前工程骨架使用：

- Kotlin 2.3.10（由 Android Gradle Plugin 内置 Kotlin 支持提供）
- Android Gradle Plugin 9.2.1、Gradle Wrapper 9.4.1 和 Gradle Kotlin DSL
- Jetpack Compose、Material 3 和 Compose BOM 2026.06.00
- Navigation Compose 2.9.8
- 单 Activity、按功能分包的工程结构
- 最低 Android API 26，目标 API 36，编译 SDK 36.1

后续业务阶段将按照[开发文档](docs/development.md)逐步引入 Hilt、Room、Coroutines、Flow 等依赖。当前骨架不包含数据库、网络、ISBN 或阅读计时业务。

发布包标识统一配置在 `gradle.properties` 的 `shiyue.applicationId`，当前值为 `com.shiyue.reader`。

## 工程结构

```text
app/src/main/java/com/shiyue/reader/
├─ app/                  # MainActivity、应用容器和导航
├─ core/ui/              # 通用 Compose UI 与浅色/深色主题
└─ feature/
   ├─ bookshelf/         # 书架
   ├─ reading/           # 阅读
   ├─ note/              # 随记
   └─ review/            # 回顾
```

依赖版本集中在 `gradle/libs.versions.toml`。用户可见文案集中在 Android 字符串资源中。

## 本地构建

项目要求 JDK 17 或更高版本，并需要已安装 Android SDK 36.1。推荐使用 Android Studio 自带 JDK。

macOS / Linux：

```bash
./gradlew assembleDebug
```

Windows：

```powershell
.\gradlew.bat assembleDebug
```

## 测试与检查

macOS / Linux：

```bash
./gradlew testDebugUnitTest lintDebug
```

Windows：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug
```

连接 Android 设备或启动模拟器后，可运行 Compose UI 测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## 当前完成阶段

已完成阶段 0 的基础工程骨架：Gradle 构建配置、单 Activity、Compose Material 3 浅色/深色主题、四个底部导航入口、占位页面、基础单元测试和 Compose UI 导航测试。尚未实现任何业务数据或业务规则。
