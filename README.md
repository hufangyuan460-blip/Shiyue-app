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
- Hilt 2.60.1
- Room 2.8.4、KSP 2.3.8
- Kotlin Coroutines 和 Flow 1.11.0
- Lifecycle 2.9.4、AndroidX Hilt Navigation Compose 1.3.0
- 单 Activity、按功能分包的工程结构
- 最低 Android API 26，目标 API 36，编译 SDK 36.1

当前已建立书籍领域模型、离线数据基础层，以及“手动添加、查看详情、编辑基本信息、手动更新阅读页码并同步书架”的本地闭环。网络、ISBN、封面选择、阅读计时和随记业务尚未实现。

发布包标识统一配置在 `gradle.properties` 的 `shiyue.applicationId`，当前值为 `com.shiyue.reader`。

## 工程结构

```text
app/src/main/java/com/shiyue/reader/
├─ app/                  # MainActivity、应用容器和导航
├─ core/
│  ├─ data/              # 离线 Repository 实现
│  ├─ database/          # Room Database、Entity、DAO 和映射
│  ├─ di/                # Hilt 数据库和 Repository 模块
│  ├─ model/             # 书籍领域模型
│  └─ ui/                # 通用 Compose UI 与浅色/深色主题
├─ domain/               # Repository 接口和业务用例
└─ feature/
   ├─ bookshelf/         # 真实书架列表与状态管理
   ├─ bookdetail/        # 书籍详情与实时单书 Flow
   ├─ bookedit/          # 手动添加和编辑书籍表单
   ├─ bookprogress/      # 手动更新阅读页码与确认流程
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

已完成阶段 0 工程骨架，以及阶段 1 的本地书籍管理闭环：Hilt 依赖注入、Room v1 数据库、Book Repository、Schema 导出、真实书架与单书 Flow、手动添加、书籍详情、基本信息编辑和手动更新阅读页码。编辑或更新成功后，详情与书架会通过 Flow 立即同步；数据关闭并重新打开 App 后仍保存在 Room 中。阅读计时、ISBN、本地封面导入和随记仍待后续实现。

## 手动验收步骤

1. 在空书架点击“添加第一本书”，填写书名、作者和总页数后保存。
2. 点击书架卡片进入详情，确认状态、页码、进度和本地时间格式正确。
3. 点击“编辑书籍”，修改基本信息并保存；确认详情和返回后的书架卡片立即更新。
4. 编辑后不保存直接返回，确认出现“放弃修改”提示且继续编辑时输入仍保留。
5. 点击“更新阅读进度”，输入较大的合法页码并保存；确认详情和书架进度同步。
6. 输入小于当前页的页码，确认回退提示；取消时数据不变，确认后允许回退。
7. 输入最后一页，分别验证“标记为已读”“仅更新页码”和取消三种选择。
8. 关闭并重新打开 App，确认书籍信息和页码仍然存在；同时检查深色模式、200% 字体和窄屏布局。
