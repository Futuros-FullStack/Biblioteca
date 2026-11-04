# 评估测试用例

本文件定义 JavaFX Developer skill 的验收测试用例，用于量化技能输出质量。每条用例描述输入场景、预期输出与验收标准。

---

## 测试用例 1：基础 CRUD 表格视图

**输入**："帮我创建一个用户管理界面，用 TableView 展示用户列表，支持新增、编辑、删除"

**预期输出**：
- 生成一个 `User` 模型类（含 `StringProperty`/`LongProperty` 等 JavaFX Properties）
- 生成实现 `Initializable` 的 `UserController`，包含 `TableView` 绑定逻辑
- 生成包含 `TableView` + `TableColumn` 的 FXML 布局文件
- 生成对应的 CSS 样式文件
- 提供 Maven/Gradle 依赖说明与运行命令

**验收标准**：
- [ ] `TableView` 的 `TableColumn` 通过 `cellValueFactory` 绑定到 Model 的 Properties
- [ ] FXML 的 `fx:id` 与 Controller 的 `@FXML` 字段一一对应
- [ ] `module-info.java` 包含 `opens model to javafx.controls`（以支持 PropertyValueFactory 反射）
- [ ] 代码可无语法错误编译
- [ ] CSS 样式不使用 `var()` 函数

---

## 测试用例 2：MVVM 架构应用

**输入**："用 MVVM 模式创建一个任务管理应用，ViewModel 需要暴露 Properties 供 View 绑定"

**预期输出**：
- 生成 `Task` 模型类和 `TaskViewModel` 类
- ViewModel 暴露 `StringProperty`/`BooleanProperty` 等可绑定属性
- View（FXML + Controller）通过双向绑定连接 ViewModel
- Controller 仅处理 UI 事件，业务逻辑委托给 ViewModel/Service

**验收标准**：
- [ ] ViewModel 不直接引用 UI 控件（无 `@FXML` 注入）
- [ ] 双向绑定使用 `bindBidirectional()`
- [ ] 计算属性使用 `Bindings.createXxxBinding()`（不使用不存在的 `select()` API）
- [ ] Service 层通过构造器注入到 ViewModel

---

## 测试用例 3：Spring Boot + JavaFX 集成

**输入**："使用 Spring Boot + JavaFX + MyBatis + SQLite 创建一个应用"

**预期输出**：
- 启动类拆分为 `MyApp`（不继承 Application）+ `JavaFXApp`（继承 Application）
- Controller 使用 `@Component` + `@Scope("prototype")` 注解
- `application.yml` 配置 `web-application-type: none`
- MyBatis Mapper 接口和 XML 映射文件
- `controllerFactory` 设置为 `springContext::getBean`

**验收标准**：
- [ ] 主类**不**继承 `Application`（以避免 "JavaFX runtime components are missing" 错误）
- [ ] Controller 使用 `@Scope("prototype")` 注解（避免单例状态污染）
- [ ] JavaFX Properties 的 setter 处理 null（避免 `SimpleLongProperty.set(null)` NPE）
- [ ] 在 `application.yml` 中设置 `spring.main.web-application-type` 为 `none`
- [ ] 不引入 `spring-boot-devtools`（或设置为 optional + 禁用 restart）

---

## 测试用例 4：对话框与表单校验

**输入**："创建一个用户输入对话框，包含姓名和邮箱字段，输入无效时保存按钮禁用"

**预期输出**：
- 对话框 FXML 布局（TextField + TextArea + Button）
- DialogController 继承 BaseController，处理 OK/Cancel 事件
- 表单校验使用 `BooleanBinding` 组合实现声明式校验
- 保存按钮的 `disableProperty` 绑定到校验 Binding

**验收标准**：
- [ ] 对话框 Controller 可通过 `getName()`/`getDescription()` 获取用户输入
- [ ] 校验逻辑使用 `Bindings.createBooleanBinding()` 或 `isEmpty().or()` 组合
- [ ] 使用 JavaFX 原生 `Alert` 或 `Dialog`（不使用已废弃的 ControlsFX `Dialogs.create()` API）
- [ ] 对话框关闭后资源被正确释放

---

## 测试用例 5：跨平台打包

**输入**："把我的 JavaFX 应用打包成 Windows exe 安装程序"

**预期输出**：
- 提供包含 `--type exe`、`--win-menu`、`--win-shortcut` 的 `jpackage` 命令
- 包含 `--win-upgrade-uuid` 参数
- 包含 `--java-options "--enable-native-access=javafx.graphics"`
- 说明需要安装 Inno Setup（exe）或 WiX Toolset 4.x（msi）
- 提供图标格式要求（`.ico`，多尺寸内嵌）

**验收标准**：
- [ ] 命令包含 `--enable-native-access=javafx.graphics`
- [ ] WiX 版本记录为 4.x（通过 `dotnet tool install` 安装），而非 3.x
- [ ] 不使用 `gu install native-image`（GraalVM JDK 21+ 已内置）
- [ ] `--win-upgrade-uuid` 使用有效的 UUID 格式

---

## 测试用例 6：CSS 主题切换

**输入**："实现明/暗主题切换功能"

**预期输出**：
- 明暗两套 CSS 文件，主题变量定义在 `.root` 中
- ThemeManager 类管理主题切换与偏好持久化
- 颜色变量使用直接引用（`-fx-primary`），不使用 `var()`
- 边框圆角使用字面量数值，不使用通过 lookup 引用的尺寸变量

**验收标准**：
- [ ] CSS **不**使用 `var()` 函数（JavaFX CSS 不支持）
- [ ] 颜色变量定义在 `.root` 中，子节点直接按名称引用
- [ ] `-fx-border-radius`/`-fx-background-radius` 使用字面量数值
- [ ] 主题切换通过 `scene.getStylesheets().setAll()` 实现
- [ ] 用户偏好持久化到 `Preferences`

---

## 测试用例 7：数据绑定与内存管理

**输入**："实现一个主从视图，选中列表项时更新详情表单，注意防止内存泄漏"

**预期输出**：
- 使用 `FilteredList` + `SortedList` 处理列表过滤与排序
- 选择监听器更新详情视图
- 在自定义 `dispose()` 方法中移除监听器（不使用不存在的 `@FXML dispose()`）
- 通过 `stage.setOnCloseRequest()` 或视图切换回调触发清理

**验收标准**：
- [ ] 不使用不存在的 `person.select(p -> ...)` API
- [ ] 在自定义清理方法中通过 `removeListener()` 移除监听器
- [ ] 不声称存在 `@FXML dispose()` 生命周期方法
- [ ] 不再需要时释放 `Bindings.createXxxBinding()` 返回的 Bindings
- [ ] 后台任务使用 `Task` + `Platform.runLater()` 回到 UI 线程

---

## 测试用例 8：版本选择与兼容性

**输入**："我在用 JDK 17，应该选哪个 JavaFX 版本？"

**预期输出**：
- 推荐 JavaFX 21 LTS（JDK 17+，成熟稳定）
- 说明 JavaFX 25 LTS 是最新 LTS（JDK 23+），如需要最新特性可升级
- 说明 JavaFX 17 LTS 于 2026 年 10 月停止支持
- 提醒 JavaFX 24+ 需要添加 `--enable-native-access=javafx.graphics`

**验收标准**：
- [ ] 版本矩阵与官方 Gluon 路线图一致
- [ ] JavaFX 25 标记为已发布的 LTS（而非 "planned"/"expected"）
- [ ] JavaFX 26 标记为已发布（而非 "planned"）
- [ ] JavaFX 17 LTS 标记为支持至 2026 年 10 月
- [ ] 提及 `--enable-native-access` 要求适用于 JavaFX 24+

---

## 测试用例 9：修复消费 —— 指纹匹配（正例）

**输入**：来自 `javafx-code-reviewer` 的修复交接报告，包含 3 个修复。评审后 `target_lines` 处的代码未发生改变，因此 `code_fingerprint` 完全匹配。

**预期输出**：
- 进入修复消费模式（跳过 Steps 1–5，激活 Step 5.5）
- 在应用任何修复前创建 `.fix-backup/{timestamp}/` 目录及 `manifest.json`
- 对每个修复，计算 `target_lines` 处代码的 SHA-256（空白标准化）并与 `code_fingerprint` 比对
- 全部 3 个修复在 Level 1（指纹）匹配，直接应用到 `target_lines`
- 修复后 `mvn compile -q` 通过
- 修复摘要列出全部 3 个修复，状态为 `applied`

**验收标准**：
- [ ] `.fix-backup/{timestamp}/manifest.json` 存在，并记录所有备份文件、循环 ID 与轮次编号
- [ ] 全部 3 个修复状态为 `applied`（而非 `relocated` 或 `skipped`）
- [ ] 修复摘要中修复按 `fix_priority` 升序排列
- [ ] 应用修复后 `mvn compile -q` 退出码为 0
- [ ] `.loop-state.json` 更新为 `fixes_applied: 3`、`fixes_skipped: 0`、`fixes_rolled_back: 0`

---

## 测试用例 10：修复消费 —— 锚点匹配的行偏移恢复（边界）

**输入**：包含 2 个修复的修复交接报告。修复 #1（在第 30 行插入）已在先前批次中应用，使修复 #2 的 `target_lines`（原为 "45-60"）下移 5 行。修复 #2 的 `code_fingerprint` 不再匹配，但 `anchor_pattern`（前 2 行 + 后 2 行）仍在恰好一个位置匹配。

**预期输出**：
- 修复 #1 在 Level 1（指纹匹配）应用
- 修复 #2：检测到指纹不匹配（行偏移），回退到 Level 2（基于锚点匹配）
- 锚点模式在文件中恰好一个位置匹配
- 修复 #2 应用在重新定位的位置，状态记录为 `applied (relocated by anchor)`
- 修复摘要为修复 #2 标记 `line_drift: true`

**验收标准**：
- [ ] 修复 #1 状态为 `applied`，修复 #2 状态为 `applied (relocated by anchor)`
- [ ] 修复摘要为修复 #2 包含 `line_drift` 标记
- [ ] 同一文件组内的修复按 `target_lines` 起始行**降序**串行应用（最高行优先）以最小化偏移
- [ ] 修复后编译通过
- [ ] 重新定位的位置正确（修复应用在实际代码位置，而非过期行号）

---

## 测试用例 11：修复消费 —— AST 签名重定位（边界）

**输入**：一份修复交接报告，目标方法在评审后被移动到不同类并重命名。`code_fingerprint`、`anchor_pattern` 与基于内容的匹配全部失败。`ast_node_signature` 为 `com.example.controller.UserController#handleSave(ActionEvent)`，而方法现在位于 `com.example.controller.UserController#onSave(ActionEvent)`。

**预期输出**：
- Level 1–3（指纹、锚点、内容）全部失败
- 回退到 Level 4（AST 签名匹配）：按签名搜索方法
- 若通过部分签名匹配找到方法（同类、参数类型匹配的方法），在该处应用修复
- 状态记录为 `applied (relocated by AST)`
- 若完全找不到方法，状态为 `skipped (AST signature not found)`

**验收标准**：
- [ ] 按顺序尝试 4 级匹配层级：指纹 → 锚点 → 内容 → AST 签名
- [ ] 若 AST 匹配成功，修复应用在正确的方法位置
- [ ] 使用 `ast_node_signature` 字段进行签名搜索，而非行号
- [ ] 对非 Java 文件（FXML、CSS、`module-info.java`），`ast_node_signature` 为 `null`，跳过 Level 4
- [ ] 若 4 级全部失败，状态为 `skipped (line drift)`，并在修复摘要中给出清晰说明

---

## 测试用例 12：修复消费 —— 并行文件组（正例）

**输入**：一份修复交接报告，包含针对 4 个不同文件的 6 个修复：`UserController.java`（2 个修复）、`UserService.java`（1 个修复）、`main-view.fxml`（1 个修复）、`styles.css`（2 个修复）。没有两个文件组共享同一个文件。

**预期输出**：
- 修复按 `target_file` 分组为 4 个并行文件组
- 每个组内按 `target_lines` 起始行降序串行应用其修复
- 各组并发执行（并行文件组）
- 线程安全的结果缓冲收集所有组的状态
- 跨影响检查（Controller-FXML、module-info、CSS 引用、FXML-Controller 绑定）在所有组完成后并行运行
- 全部 6 个修复应用完成，修复后编译通过

**验收标准**：
- [ ] 同一文件中的修复在同一组中串行应用（不对同一文件并发写入）
- [ ] 不同文件中的修复在不同组中，可并发执行
- [ ] 每个组内修复按 `target_lines` 起始行降序排序（最高优先）
- [ ] 编译验证前所有 4 项跨影响检查均通过
- [ ] 修复摘要合并所有并行组的结果，按 `fix_priority` 跨组排序

---

## 测试用例 13：修复消费 —— 编译失败与自动回滚（负例）

**输入**：一份包含 3 个修复的修复交接报告。修复 #2 引入语法错误（`corrected_example` 不正确）。修复 #1 和 #3 有效。应用全部 3 个修复后，`mvn compile -q` 失败。

**预期输出**：
- 全部 3 个修复应用到工作树
- `mvn compile -q` 失败（编译错误）
- 触发自动回滚：从 `.fix-backup/{timestamp}/` 恢复所有被修改的文件
- 全部 3 个修复标记为 `rolled_back`（不仅是修复 #2 —— 整个批次回滚）
- 向 `.loop-state.json` 追加 `rollback_event`，含时间戳、备份目录、恢复的文件、编译错误计数、`rollback_verification: "passed"`
- 回滚验证：重新运行 `mvn compile -q` 确认项目再次编译
- 修复摘要建议人工干预

**验收标准**：
- [ ] 全部 3 个修复状态为 `rolled_back`（批次级回滚，而非单个）
- [ ] `.fix-backup/{timestamp}/` 中的文件恢复到原始位置
- [ ] `.loop-state.json` 中的 `rollback_event` 包含：`round`、`timestamp`、`backup_dir`、`files_restored`、`compile_error_count`、`rollback_verification`
- [ ] 回滚后 `mvn compile -q` 通过（项目恢复到修复前状态）
- [ ] 修复摘要建议"需要人工干预 —— 修复导致编译失败"
- [ ] `.fix-backup/` **不**清理（回滚时保留以供人工检查）

---

## 测试用例 14：修复消费 —— 多来源交接合并，编排模式（正例）

**输入**：来自编排器的一份合并后的修复交接批次，包含来自 `javafx-code-reviewer`（4 个修复）和 `runtime verification`（2 个修复）的修复。两个修复针对同一文件且行范围重叠：评审者修复 A（45-60 行，Critical）与执行者修复 B（50-55 行，Major）。编排器已对两者去重（保留严重级别更高的修复 A），并在修复 A 上记录 `dedup_merged_from: "runner"`。

**预期输出**：
- 开发者接收到已合并并去重的批次（共 5 个修复，而非 6 个）
- 修复 A 具有 `dedup_merged_from: "runner"` 字段以便追溯
- 使用标准 4 级匹配层级应用全部 5 个修复
- 修复摘要列出全部 5 个修复，修复 A 注明其取代了一个执行者修复

**验收标准**：
- [ ] 开发者在条目上检测到 `dedup_merged_from` 字段，并跳过重复去重（编排模式）——按原样消费预先合并的批次
- [ ] 修复 A 的 `dedup_merged_from` 字段保留在修复摘要中以便追溯
- [ ] 两个来源的修复在整个合并批次内按 `fix_priority` 排序
- [ ] 每个修复的 `source` 字段（`reviewer` 或 `runner`）保留在修复摘要中
- [ ] 应用修复总数 = 5（而非 6），确认去重已被遵守

---

## 测试用例 14.1：修复消费 —— 多来源交接合并，独立模式（正例）

**输入**：开发者在独立模式下被调用（无编排器），直接接收到两份独立的修复交接报告：一份来自 `javafx-code-reviewer`（4 个修复），一份来自 `runtime verification`（2 个修复）。任何条目都不携带 `dedup_merged_from` 字段。两个修复针对同一文件且行范围重叠：评审者修复 A（45-60 行，Critical）与执行者修复 B（50-55 行，Major）。

**预期输出**：
- 开发者检测到缺少 `dedup_merged_from`，激活独立去重
- 修复 A 与修复 B 被去重：保留修复 A（Critical），丢弃修复 B（Major）
- 开发者将修复 A 的 `dedup_merged_from` 字段设置为 `"runner"`（镜像编排器的字段名）
- 最终批次有 5 个修复（而非 6 个），按 `fix_priority` 升序排序

**验收标准**：
- [ ] 开发者检测到任何条目上**无** `dedup_merged_from` 字段，并激活独立去重
- [ ] 同一文件上的重叠条目被去重（相同 `target_file` + 重叠的 `target_lines`）
- [ ] 保留严重级别更高的条目（修复 A，Critical）；丢弃严重级别更低的条目（修复 B，Major）
- [ ] 被保留条目的 `dedup_merged_from` 字段填充为被丢弃条目的来源（`"runner"`）以便追溯
- [ ] 应用修复总数 = 5（而非 6），确认已执行独立去重
- [ ] 去重后的批次在全部来源范围内按 `fix_priority` 升序重新排序

---

## 测试用例 15：修复消费 —— 架构交接消费（正例）

**输入**：用户请求"为一个用户管理应用设计架构并生成代码"。编排器先运行 `javafx-architect`，生成 `architecture-handoff.json`，其中包含 `system_design.architecture_pattern: "MVVM + Service Layer"`、含 3 张表的 `database_schema`，以及指定 `@req` 注解格式的 `developer_instructions`。随后触发 `javafx-developer`。

**预期输出**：
- 开发者 Step 4 检查 `architecture-handoff.json` 并消费它
- 生成的项目遵循交接中的 MVVM + Service Layer 模式
- 数据库集成遵循 `database_schema`（表名、列类型、索引一致）
- 生成代码中的 `@req` 注解遵循交接中的 `req_id_convention`
- `docs/` 中的 `requirements.md` 反映架构交接的模块结构

**验收标准**：
- [ ] 生成的代码使用 MVVM 模式（ViewModel 无 `@FXML` 引用，Controller 委托给 ViewModel）
- [ ] 数据库实体类与 `database_schema` 表定义一致（字段名、类型）
- [ ] `@req FR-xxx` 注解与交接中的 `req_id_convention` 一致
- [ ] 若缺少 `database_schema`（项目 `database: "none"`），则不生成数据库代码
- [ ] `developer_instructions`（注解格式、测试命名约定）被遵守

---

## 测试用例 16：修复消费 —— 约束违规（负例）

**输入**：一份修复交接报告，其中一个修复为 `fix_type: "replace"` 但未提供 `corrected_example` 字段。另一个修复针对的项目中不存在的文件（`target_file: "src/main/java/com/example/NonExistent.java"`）。

**预期输出**：
- 缺少 `corrected_example` 的修复（针对 `fix_type: "replace"`）：状态 `skipped (missing corrected_example)`，作为警告包含在修复摘要中
- 针对不存在文件的修复：状态 `skipped (target file not found)`，包含在修复摘要中
- 其他有效修复正常应用
- 修复摘要清晰区分 `applied`、`skipped` 与 `failed` 状态
- 修复后编译通过（仅应用了有效修复）

**验收标准**：
- [ ] `fix_type: "replace"` 且缺少 `corrected_example` 的修复被跳过，而非以空内容应用
- [ ] `fix_type: "delete"` 且缺少 `corrected_example` 的修复有效（删除无需替换）并应被应用
- [ ] 针对不存在文件的修复状态为 `skipped (target file not found)`
- [ ] 同一批次中的有效修复仍被应用（一个坏修复不阻塞整个批次）
- [ ] 修复摘要包含 `warnings` 小节，列出所有被跳过的修复及原因
- [ ] `.loop-state.json` 准确记录 `fixes_applied`、`fixes_skipped` 计数

---

## 测试用例 17：设计交接消费

**输入**：用户请求"为一个用户管理应用设计 UI 并生成代码"。编排器先运行 `javafx-designer`，生成 `design/design-handoff.json`，其中包含 `design/fxml/*.fxml` 中的 FXML 原型、`design/css/light-theme.css` + `dark-theme.css` 中的 CSS 主题、`design/icons/icon-config.json` 中的图标配置，以及 `design/flow/interaction-flow.mmd` 中的交互流程。随后触发 `javafx-developer`。

**预期输出**：
- 开发者 Step 4 检测到 `design/design-handoff.json` 并消费设计交接，而非使用内置模板
- 使用 `design/fxml/*.fxml` 中的 FXML 原型作为基础布局（而非内置的 `templates/fxml/*.fxml`）
- CSS 主题文件（`light-theme.css`、`dark-theme.css`）复制到 `src/main/resources/css/`
- 若存在 `icon-config.json`，将 Ikonli Maven 依赖添加到 `pom.xml`，并在 FXML 与 Java 代码中配置 FontIcon 用法
- 设计器 FXML 中声明的 `fx:id` 名称与生成 Controller 中的 `@FXML` 字段完全一致

**验收标准**：
- [ ] 解析 `design-handoff.json` —— 开发者读取并消费交接产物
- [ ] 使用 FXML 原型作为基础（而非内置模板）—— 生成的 FXML 源自 `design/fxml/*.fxml`，保留布局容器、`fx:id` 赋值与 `styleClass` 值
- [ ] CSS 主题文件复制到 `src/main/resources/css/` —— 设计器的 `light-theme.css` 与 `dark-theme.css` 替换内置 CSS 模板
- [ ] 若存在 `icon-config.json`，将 Ikonli 依赖添加到 `pom.xml`（例如 `org.kordamp.ikonli:ikonli-javafx`）
- [ ] FXML 中的 `fx:id` 名称与生成 Controller 中的 `@FXML` 字段完全一致
- [ ] 强制执行 `developer_instructions.fx_id_consistency` —— 遵守交接中的任何 fx:id 一致性规则

---

## 测试用例 18：需求交接消费

**输入**：用户请求"基于这些需求创建一个 JavaFX 应用"。编排器先运行 `javafx-requirements`，生成 `requirements/requirements-handoff.json`，其中包含 `user_stories[]`、`non_functional_requirements[]`、`traceability_matrix[]` 种子数据，以及包含 `req_id_convention` 和 `test_naming_convention` 的 `developer_instructions`。随后触发 `javafx-developer`。

**预期输出**：
- 开发者 Step 1 检测到 `requirements/requirements-handoff.json` 并将其作为主要需求来源（而非从用户请求推断需求）
- 每条用户故事（US-xxx）映射到 `requirements.md` 中的一个功能需求（FR-xxx）
- NFR 作为生成约束应用（例如，性能 NFR 强制数据库 I/O 在后台线程执行）
- `requirements.md` 第 7 节的需求追溯矩阵从交接的 `traceability_matrix[]` 种子数据预填充
- 按照 `req_id_convention`，将 `@req` 注解添加到生成公共方法的 Javadoc 中

**验收标准**：
- [ ] 解析 `requirements-handoff.json` —— 读取 `user_stories[]`、`non_functional_requirements[]`、`traceability_matrix[]` 与 `developer_instructions`
- [ ] 用户故事映射到功能需求 —— 每个 US-xxx 成为 `requirements.md` 中的 FR-xxx 条目
- [ ] NFR 作为生成约束应用（例如，性能 NFR → 数据库访问包裹在 `Task`/后台线程中；安全 NFR → 参数化查询）
- [ ] 从交接的 `traceability_matrix[]` 预填充 `requirements.md`（第 7 节）中的 RTM 种子数据
- [ ] `@req` 注解添加到生成代码的公共方法中（格式遵循 `req_id_convention`，例如 `@req FR-001`）
- [ ] 测试方法遵循 `test{Behavior}_{REQ_ID}()` 命名约定（例如 `testUserCreation_FR_001()`）
