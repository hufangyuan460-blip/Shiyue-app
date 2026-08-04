# 拾页：第一版开发文档

## 1. 技术目标

构建一个本地优先、可恢复、可备份的 Android 应用。核心阅读数据不依赖服务器；网络仅用于 ISBN 书籍资料查询和封面下载。

技术实现优先保证：

- 阅读计时在后台、锁屏和进程重建后仍可准确恢复。
- 数据模型能够支持后续云同步，但第一版不引入账号系统。
- UI、领域规则和数据存储分层，便于测试。
- 所有关键数据均能导出和恢复。

## 2. 推荐技术栈

| 领域 | 方案 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单 Activity、分层架构、单向数据流 |
| 依赖注入 | Hilt |
| 本地数据库 | Room |
| 简单设置 | DataStore |
| 异步与状态 | Kotlin Coroutines + Flow |
| 导航 | Navigation Compose |
| 网络 | Retrofit/OkHttp 或 Ktor Client，二选一 |
| JSON | Kotlin Serialization |
| 图片加载 | Coil |
| ISBN 扫描 | CameraX + ML Kit Barcode Scanning |
| 后台任务 | WorkManager |
| 通知 | Android Notification API |
| 测试 | JUnit、Room 测试、Compose UI 测试 |

依赖版本在项目初始化时统一放入 Gradle Version Catalog，并选用当时稳定版本，避免在文档中长期固化具体版本号。

## 3. 工程结构

第一版建议使用单应用模块加按功能分包。若规模明显扩大，再拆分 Gradle 模块。

```text
app/src/main/java/.../
├─ app/                  # Application、Activity、导航和全局主题
├─ core/
│  ├─ database/          # Room、Entity、DAO、迁移
│  ├─ datastore/         # 偏好和提醒配置
│  ├─ network/           # ISBN 查询与 DTO
│  ├─ model/             # 跨功能领域模型
│  ├─ backup/            # 备份、恢复、校验
│  ├─ notification/      # 通知渠道与调度
│  └─ ui/                # 颜色、字体、通用组件
├─ feature/
│  ├─ onboarding/
│  ├─ bookshelf/
│  ├─ bookedit/
│  ├─ scanner/
│  ├─ reading/
│  ├─ note/
│  ├─ review/
│  └─ settings/
└─ domain/               # 用例和核心业务规则
```

分层依赖方向：

```text
Compose UI → ViewModel → Use Case → Repository → Room / DataStore / Network
```

UI 不直接访问 DAO 或网络接口。Repository 接口位于领域层或核心模型层，实现位于数据层。

## 4. 数据模型

所有主键使用 UUID 字符串，时间统一保存为 UTC 毫秒时间戳；界面按设备时区显示。时长使用毫秒或秒的整数值，不保存格式化文本。

### 4.1 BookEntity

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | UUID 主键 |
| title | String | 书名，必填 |
| author | String? | 作者 |
| isbn10 | String? | ISBN-10 |
| isbn13 | String? | ISBN-13 |
| coverPath | String? | 本地封面文件路径 |
| coverSourceUrl | String? | 原始封面地址，仅用于溯源 |
| totalPages | Int? | 总页数 |
| currentPage | Int | 当前进度页，默认 0 |
| status | Enum | WISH、READING、FINISHED、PAUSED |
| publisher | String? | 出版社 |
| publishedDate | String? | 出版日期原始值 |
| startedAt | Long? | 开始阅读时间 |
| finishedAt | Long? | 完成时间 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

`progress` 不持久化，由 `currentPage / totalPages` 计算，避免冗余不一致。

### 4.2 ReadingSessionEntity

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | UUID 主键 |
| bookId | String | 外键，关联书籍 |
| state | Enum | ACTIVE、PAUSED、COMPLETED、DISCARDED |
| startedAt | Long | 开始时间 |
| endedAt | Long? | 结束时间 |
| startPage | Int? | 起始页 |
| endPage | Int? | 结束页 |
| activeDurationMs | Long | 已结算的有效阅读时长 |
| activeSegmentStartedAt | Long? | 当前活动计时段的开始时间 |
| updateBookProgress | Boolean | 是否更新全书进度 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

计时不能只依赖内存倒计时。活动时长计算如下：

```text
显示时长 = activeDurationMs
         + (当前为 ACTIVE ? now - activeSegmentStartedAt : 0)
```

暂停、结束或恢复时在事务内结算当前活动段。这样进程被杀死后可根据数据库时间戳恢复。

### 4.3 NoteEntity

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | UUID 主键 |
| bookId | String | 所属书籍 |
| sessionId | String? | 所属阅读场次，可空 |
| pageNumber | Int? | 关联页码 |
| content | String | 随记正文 |
| imagePath | String? | 本地附件路径 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

第一版不区分书摘、感悟和普通笔记类型，以降低记录成本；未来可通过数据库迁移增加类型。

### 4.4 ReadingGoalEntity

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | String | 主键 |
| type | Enum | DAILY_MINUTES、WEEKLY_DAYS |
| targetValue | Int | 分钟数或天数 |
| enabled | Boolean | 是否启用 |
| effectiveFrom | Long | 生效日期 |
| createdAt | Long | 创建时间 |
| updatedAt | Long | 更新时间 |

目标完成度从阅读场次聚合，不单独保存每日完成结果。

### 4.5 ReminderSettings

适合存入 DataStore：

- 是否启用
- 提醒本地时间
- 生效星期集合
- 通知文案偏好
- 上次计划任务信息

### 4.6 数据关系与索引

```text
Book 1 ─── N ReadingSession
Book 1 ─── N Note
ReadingSession 1 ─── N Note（可选关联）
```

建议索引：

- `Book.status`
- `Book.isbn13` 和 `Book.isbn10`
- `ReadingSession.bookId, startedAt`
- `ReadingSession.state`
- `Note.bookId, pageNumber`
- `Note.createdAt`

数据库约束应保证同一时间最多一个 `ACTIVE` 或 `PAUSED` 场次。Room/SQLite 难以直接表达跨状态唯一约束时，由事务型 Repository 操作与启动时一致性修复共同保证。

## 5. 核心领域逻辑

### 5.1 开始阅读

1. 查询是否存在未结束场次。
2. 若存在，返回现有场次而不是创建新场次。
3. 校验起始页范围。
4. 在事务中创建 `ACTIVE` 场次并记录活动段开始时间。
5. 若书籍为想读或搁置，可提示切换为在读。

### 5.2 暂停与继续

暂停时：

```text
activeDurationMs += now - activeSegmentStartedAt
activeSegmentStartedAt = null
state = PAUSED
```

继续时：

```text
activeSegmentStartedAt = now
state = ACTIVE
```

所有操作必须幂等，避免快速重复点击造成双重累计。

### 5.3 结束阅读

1. 若为活动状态，先结算当前活动段。
2. 校验结束页在合法范围内。
3. 写入结束时间、结束页和完成状态。
4. 若用户选择更新进度，更新书籍 `currentPage`。
5. 结束页小于现有进度时，默认不回退。
6. 达到总页数且用户确认完成时，将书籍设为已读并记录完成时间。
7. 上述写入放在一个数据库事务中。

### 5.4 阅读统计

- 阅读时长只统计 `COMPLETED` 场次的 `activeDurationMs`。
- 阅读天数按用户设备时区将场次映射到自然日。
- 跨午夜场次第一版可归入结束日；如需精确日历分摊，可在后续拆分活动片段。
- 连续天数由至少存在有效阅读时长的自然日计算。
- 统计查询通过 Room 聚合 SQL 或 Repository 聚合完成。

## 6. ISBN 扫描与资料查询

### 6.1 扫描

- 使用 CameraX 获取预览帧。
- 使用条码识别库识别 EAN-13/EAN-8 等格式。
- 校验并标准化 ISBN-10/ISBN-13。
- 扫描成功后立即停止重复识别并进入查询页。
- 相机权限被拒绝时提供手动输入入口。

### 6.2 资料提供方抽象

定义统一接口，避免绑定单一数据源：

```kotlin
interface BookMetadataProvider {
    suspend fun findByIsbn(isbn: String): BookMetadataResult
}
```

Repository 可按优先级尝试多个提供方，并统一映射字段。实现前需要根据目标发布地区确认书籍数据源的许可、覆盖率、配额、隐私条款和封面使用规则。

### 6.3 离线行为

- 已查询的书籍资料保存到本地。
- 下载封面复制到 App 私有存储，不依赖远程 URL 长期展示。
- 无网络、超时、无结果时进入可编辑表单。
- ISBN 查询失败不能阻塞添加书籍。

## 7. 图片与文件管理

- 书籍封面和随记图片保存到 App 私有目录。
- 数据库只保存相对文件标识或受控路径，不保存图片二进制。
- 导入图片时复制到私有目录，避免依赖临时 URI 权限。
- 生成适合列表展示的缩略图或由图片库缓存。
- 删除或替换图片时清理孤立文件；清理操作应可重试。
- 备份时包含原图和清单校验值。

推荐目录：

```text
files/
├─ covers/<book-id>.<ext>
├─ notes/<note-id>.<ext>
└─ backups/
```

## 8. 提醒与通知

- 创建低打扰级别的“阅读提醒”通知渠道。
- 使用 WorkManager 安排可延迟的周期检查，而不是依赖秒级精确闹钟。
- Worker 在发送前检查提醒开关、生效日和目标完成情况。
- 用户更改目标、时间、时区或系统重启后重新安排任务。
- Android 新版本通知权限应在用户主动开启提醒时申请，而不是首次启动即申请。

## 9. 备份与恢复

### 9.1 备份格式

使用一个带版本号的 ZIP 包：

```text
backup.zip
├─ manifest.json
├─ data.json
├─ covers/
└─ notes/
```

`manifest.json` 至少包含：

- 格式版本
- App 版本
- 导出时间
- 数据记录数量
- 文件清单及校验值

`data.json` 使用稳定的导出 DTO，不直接复制 Room 数据库文件，从而允许数据库结构演进。

### 9.2 恢复流程

1. 使用系统文件选择器选择备份包。
2. 解压到临时目录并校验路径、格式版本和校验值。
3. 展示备份摘要。
4. 用户选择替换现有数据或取消；第一版可暂不支持智能合并。
5. 在事务中导入结构化数据。
6. 原子移动媒体文件。
7. 成功后重建提醒并清理临时文件。

恢复前自动创建当前数据的安全备份，降低误操作风险。

### 9.3 随记导出

支持按全部或单本书导出 Markdown：

```markdown
# 书名

## 第 128 页

随记内容……

记录于 2026-08-04 21:32
```

## 10. 状态恢复与异常处理

应用启动时执行活动场次检查：

- `ACTIVE`：根据时间戳继续显示计时，并询问异常超长场次是否继续。
- `PAUSED`：恢复暂停界面。
- 多个未结束场次：选择最近一个为有效场次，其余标记为待修复并提示用户。
- 设备时间被手动调整：检测负时长或异常跨度，要求用户确认时间。

异常长阅读阈值应可配置，第一版可使用合理默认值，例如连续数小时后发送本地提示，但不自动结束阅读。

## 11. 权限与隐私

| 权限/能力 | 使用时机 |
| --- | --- |
| 相机 | 用户进入 ISBN 扫描时 |
| 通知 | 用户开启阅读提醒时 |
| 图片选择 | 使用系统 Photo Picker，尽量不申请完整媒体库权限 |
| 网络 | ISBN 查询和封面下载 |

隐私说明应明确：

- 阅读记录和随记默认仅保存在设备本地。
- 不建立用户账号，不上传个人阅读数据。
- ISBN 查询时只向资料提供方发送 ISBN 和必要请求信息。
- 用户可随时导出或删除全部数据。

日志不得记录随记正文、图片内容或完整备份路径等敏感信息。

## 12. 无障碍与国际化基础

- 所有点击区域至少满足 Android 推荐触控尺寸。
- 封面图片提供内容描述，纯装饰元素设为空描述。
- 计时状态不能只通过颜色表达。
- 支持系统字体缩放，关键按钮不能被截断。
- 动效尊重“移除动画/减少动态效果”设置。
- 文案放入字符串资源，不在 Compose 中硬编码，为后续国际化留出空间。

## 13. 测试策略

### 13.1 单元测试

- 页码范围和进度计算
- 回读时的进度更新规则
- 暂停、继续和结束时长结算
- 重复点击的幂等性
- 每日/每周目标完成度
- 连续阅读天数
- ISBN 标准化和校验
- 备份 DTO 版本兼容

### 13.2 数据库与集成测试

- DAO 查询和级联删除
- 阅读结束事务的原子性
- Room Schema 导出和迁移测试
- 多个活动场次的一致性修复
- 备份后清空并恢复的数据一致性

### 13.3 UI 测试

- 添加书籍完整流程
- 从书架开始阅读
- 暂停、进后台、恢复和结束
- 阅读中创建随记
- 权限拒绝与手动降级路径
- 深色模式与大字体布局

### 13.4 人工验收设备场景

- 锁屏 10 分钟后恢复
- 强制停止/系统回收后恢复场次
- 午夜前后跨日阅读
- 飞行模式添加书籍和记录阅读
- 系统时间或时区变化
- 无剩余网络、通知或相机权限
- 备份、卸载、重装和恢复

## 14. 开发阶段划分

### 阶段 0：项目基础

- 初始化 Kotlin/Compose 工程
- 建立主题、导航、依赖注入和代码规范
- 配置 Room Schema 导出和基础 CI

> 初始化范围记录（2026-08-04）：当前工程骨架已完成 Kotlin/Compose、Material 3 主题、单 Activity、四入口导航和基础测试。根据本次初始化要求，Hilt 在首个需要依赖注入的功能阶段接入，Room 与 Schema 导出在阶段 1 建立数据层时接入，基础 CI 另行配置；当前不提前引入这些尚未使用的依赖。

### 阶段 1：书架闭环

- 数据库和 Book Repository
- 书架、手动添加、编辑和详情
- 封面导入与本地保存

### 阶段 2：阅读闭环

- ReadingSession 数据与领域逻辑
- 开始、暂停、继续、结束和恢复
- 页码进度与场次时间线

### 阶段 3：随记

- 阅读中快速随记
- 图片附件
- 全局随记和书籍随记列表

### 阶段 4：ISBN

- CameraX 扫描
- ISBN 校验
- 资料提供方接入、失败降级和封面缓存

### 阶段 5：目标与回顾

- 目标设置
- 通知提醒
- 日历、趋势和月度回顾

### 阶段 6：数据安全与发布准备

- 备份、恢复和 Markdown 导出
- 数据库迁移测试
- 无障碍、性能、隐私和异常场景验收

## 15. 完成定义

一个功能只有满足以下条件才算完成：

- 业务规则已有自动化测试。
- 离线与权限拒绝路径可用。
- 进程重建后状态正确。
- 错误信息可理解且允许重试或降级。
- 深色模式、大字体和基础无障碍通过检查。
- 不记录敏感日志。
- 数据结构变更包含 Room Migration 和 Schema 更新。
- 相关产品和开发文档同步更新。

## 16. 开发前待确认事项

以下事项不阻塞当前架构，但应在编码前确定：

1. 正式产品名、包名和最低 Android 版本。
2. 字体、品牌色和图标方案。
3. 面向中国大陆还是全球发行，以选择 ISBN 元数据提供方。
4. 备份仅支持用户手动导出，还是允许写入用户选择的云盘目录。
5. 是否允许手动补录历史阅读场次。
6. 第三方 SDK、数据源和封面图片的许可与隐私合规要求。
