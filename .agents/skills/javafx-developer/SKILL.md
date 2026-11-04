---
name: javafx-developer
description: |
  JavaFX 桌面应用开发技能：覆盖项目脚手架、FXML UI 设计、MVC/MVVM 架构、
  数据绑定、CSS 主题化与跨平台打包。
  触发场景：构建 JavaFX 应用、创建 FXML 布局、设计桌面 UI、
  实现数据绑定、集成 ControlsFX/RichTextFX 或打包 JavaFX 应用时调用。
license: Apache-2.0
compatibility: Requires JDK 17+. Supports JavaFX 17/21/24/25/26.
metadata:
  author: DongZY0617
  version: "1.0"
triggers:
  - create
  - generate
  - build
  - scaffold
  - implement
  - JavaFX app
  - fix
  - apply refactoring
depends_on:
  - javafx-architect (optional)
  - javafx-designer (optional)
  - javafx-code-reviewer
  - javafx-refactorer (optional)
consumes_from:
  - javafx-architect (optional, architecture-handoff.json)
  - javafx-designer (optional, design-handoff.json)
  - javafx-code-reviewer (fix handoff report)
  - javafx-refactorer (optional, refactor-handoff.json)
produces_for:
  - javafx-code-reviewer
---

# JavaFX 开发

你是 JavaFX 桌面应用开发专家。本技能为构建 JavaFX 应用提供全面指导——从项目脚手架到跨平台打包。

## 适用时机（When to Apply）

在以下情况使用本技能：
- 用户想创建 JavaFX 桌面应用
- 用户提到 FXML、Scene Builder、JavaFX CSS 或桌面 UI 设计
- 用户询问 JavaFX 的 MVC/MVVM 架构
- 用户需要数据绑定、Properties、ObservableList 模式
- 用户想集成第三方库（ControlsFX、MaterialFX、RichTextFX、Ikonli）
- 用户需要打包或部署 JavaFX 应用（jpackage、jlink）
- 用户询问 JavaFX 版本选择或 JDK 兼容性
- 用户提到 JavaFX 控件、表格、表单、对话框或导航
- 用户提供来自 `javafx-code-reviewer` 的修复 handoff 报告（进入修复消费模式，见 Step 5.5 与下方 Fix Consumption Protocol）

### 与其他技能的触发消歧

开发技能的触发词（`create`、`generate`、`build`、`scaffold`、`implement`）设计得较为宽泛——开发技能是主要的代码生成技能。但与其他技能之间仍可能产生歧义：

| 用户意图 | 路由至 | 理由 |
|-------------|-----------|-----------|
| "创建一个 JavaFX 应用" | **developer** | 代码生成意图 |
| "为……生成代码" | **developer** | 明确的代码生成 |
| "构建一个 JavaFX 应用" | **developer** | 脚手架意图（非编译） |
| "设计 UI 并生成代码" | **designer → developer**（顺序） | 先设计，再从设计 handoff 生成 |
| "审查代码" | **code-reviewer** | 审查意图，非生成 |
| "重构这个类" | **refactorer** | 重构分析意图 |
| 歧义（"修复这个应用"） | **与用户确认** | 可能指代码审查、运行时修复或重构 |

**特殊情况——`build`**：当用户说"构建一个 JavaFX 应用"时，指**脚手架/生成** → 路由至 `javafx-developer`。"a/an" 冠词的出现用于区分生成意图。

## 技术栈（Technology Stack）

### JavaFX 版本矩阵（截至 2026）

| JavaFX 版本 | 最低 JDK | LTS | 推荐用途 |
|----------------|---------|-----|-----------------|
| 26.x | JDK 24 | 否 | 最新特性（Metal 渲染器、Headless 预览） |
| 25.x | JDK 23 | **是** | 生产环境首选 |
| 24.x | JDK 22 | 否 | 过渡版本（引入 `--enable-native-access` 要求） |
| 21.x | JDK 17 | **是** | 保守稳定选择 |
| 17.x | JDK 11 | 是（截至 2026.10） | 遗留维护（即将到期，不推荐新项目） |

> **注意**：本表中的"最低 JDK"指运行该 JavaFX 版本所需的最低 JDK；`project-setup.md` 版本表中的"对应 JDK"指编译该 JavaFX 时使用的 `--release` 级别。两者基准不同但数据一致。

**默认推荐**：新项目优先 JavaFX 25 LTS（JDK 23+），保守选择 JavaFX 21 LTS（JDK 17+），除非用户另有要求。

### 构建工具
- **Maven**（默认）：`javafx-maven-plugin` 0.0.8
- **Gradle**：`org.openjfx.javafxplugin` 0.1.0

### 核心模块
- `javafx.controls` — UI 控件（始终需要）
- `javafx.fxml` — FXML 支持（使用 FXML 时）
- `javafx.web` — WebView 组件
- `javafx.media` — 音频/视频播放
- `javafx.swing` — Swing 互操作
- `javafx.graphics` — 随 controls 自动引入

## 工作流（Workflow）

### 第 1 步：需求分析与澄清

1. **检查需求 handoff**：若存在 `requirements/requirements-handoff.json`（由 `javafx-requirements` 产出），将其作为主要需求来源而非从头推断需求：
   - 使用 `user_stories[]` 作为 `requirements.md` 中功能清单的基础——每个用户故事（US-xxx → FR-xxx）成为一条功能需求条目
   - 使用 `non_functional_requirements[]` 填充非功能需求章节——每个 NFR 已量化目标与验证方法
   - 使用 `traceability_matrix[]` 作为需求追溯矩阵（`requirements.md` 第 7 节）的种子——预填需求编号、来源干系人与验证方法
   - 使用 `developer_instructions.req_id_convention` 作为 `@req` 标注格式（FR-xxx、NFR-xxx）
   - 使用 `developer_instructions.test_naming_convention` 作为测试方法命名（例如 `testUserCreation_FR_001()`）
   - 使用 `developer_instructions.key_constraints[]` 确保代码生成时遵守需求约束
   - 若无 handoff，则从用户请求推断需求（既有行为——下方第 2-5 步）
2. **识别意图**：确定请求属于哪个能力模块
3. **提取关键信息**：项目名、包路径、功能、UI 类型、数据模型
4. **询问缺失信息**：若关键信息缺失，询问用户
5. **推断默认值**：为包名、类名、模块名使用合理默认值
6. **输出需求规格**：以 `templates/docs/requirements.md` 为模板生成 `requirements.md` 文件。记录功能需求（功能清单、UI 流程）、非功能需求（性能、安全、兼容性）与技术约束（JavaFX 版本、JDK、构建工具、架构模式）。这作为增量审查与需求追溯的基线——审查技能可在审查报告中引用需求编号。消费需求 handoff 时，规格将增强干系人意图、验收标准与预填充的 RTM

### 第 2 步：版本与工具链选择

1. **检测 JDK 版本**：询问用户或从项目上下文推断
2. **推荐 JavaFX 版本**：优先 LTS 版本（25 或 21）
3. **选择构建工具**：默认 Maven，若用户指定则遵循其偏好
4. **确定模块需求**：根据功能识别所需 JavaFX 模块

**版本选择逻辑**：
- JDK 24+ → JavaFX 26（若用户想要最新）或 25 LTS
- JDK 17-23 → JavaFX 21 LTS
- JDK 11-16 → JavaFX 17 LTS

**重要**：JavaFX 24+ 需要 `--enable-native-access=javafx.graphics` JVM 参数。

### 第 3 步：架构设计

1. **评估复杂度**：简单应用推荐 MVC，复杂应用推荐 MVVM，高可测性推荐 MVP
2. **设计分层**：定义 Model、View、Controller/ViewModel/Presenter 的职责
3. **规划文件结构**：组织类、FXML、CSS 文件
4. **选择 UI 模式**：从预设 UI 组件模式中选择

**MVC / MVVM / MVP 决策**：
- **MVC**：简单 CRUD 应用、管理面板、小工具 → Controller 直接操纵 UI
- **MVVM**：复杂业务逻辑、多视图应用 → ViewModel 暴露 Properties，View 绑定它们
- **MVP**：需要高可测性但不想用数据绑定 → Presenter 通过 View 接口显式控制 UI，Controller 实现接口并将逻辑委托给 Presenter

### 第 4 步：代码生成与模板填充

1. **检查架构 handoff**：加载模板前，检查项目根目录是否存在 `architecture/architecture-handoff.json`。若存在，说明 `javafx-architect` 已产出应指导代码生成的架构规格：
   - **包结构**：使用 `developer_instructions.package_structure` 模式（例如 `com.example.app.{module}`）替代默认包命名
   - **分层规则**：遵循 `developer_instructions.layering_rule`——将类放到正确的层（展示/应用/领域/基础设施），并强制无向上依赖
   - **命名规范**：应用 `developer_instructions.naming_convention` 模式（例如 `{Entity}Controller`、`{Entity}ViewModel`、`{Entity}Service`、`{Entity}Repository`）替代内置默认值
   - **技术栈**：生成 `pom.xml` 依赖时使用 `system_design.technology_stack` 中指定的版本与库（数据库、ORM、DI、日志）
   - **模块分解**：按 `system_design.modules[]` 定义的模块生成代码——每个模块获得自己的包与指定职责
   - **关键约束**：强制执行所有 `developer_instructions.key_constraints`（例如"所有数据库访问通过 Repository 接口"、"ViewModel 不得直接引用 JavaFX 控件"）
   - **UML 参考**：生成实体与服务类前阅读 `architecture/uml/class-diagram.puml` 以理解领域模型与类关系
   - **数据库 Schema**：消费 `database_schema` 生成 JPA 实体类、Repository 接口与 Flyway/Liquibase 迁移文件。实体字段名、类型与索引必须匹配 schema 表定义，迁移脚本必须反映 schema 的 DDL（见 `database-design.md` §6.2 handoff 协议）
   - **威胁模型**：消费 `threat_model` 在代码中实现安全缓解——例如对威胁模型中标记的字段做输入验证、对敏感操作做权限/授权检查、用参数化查询防止 SQL 注入、安全处理被识别为资产的密钥（见 `threat-modeling.md` handoff 协议）
   - 若 `architecture-handoff.json` 不存在，则使用内置默认值（默认行为）
2. **检查设计 handoff**：加载模板前，检查项目根目录是否存在 `design/design-handoff.json`。若存在，说明 `javafx-designer` 已产出应替代内置模板的设计工件：
   - **FXML 模板**：从 `design/fxml/*.fxml` 读取 FXML 原型，而非 `templates/fxml/*.fxml`。这些原型已具备正确的布局容器、fx:id 分配与 styleClass 值
   - **CSS 主题**：将 `design/css/light-theme.css` 与 `design/css/dark-theme.css` 复制到 `src/main/resources/css/`，而非使用内置 CSS 模板。设计主题包含完整的 CSS 变量系统与组件样式
   - **图标配置**：读取 `design/icons/icon-config.json`，向 `pom.xml` 添加 Ikonli Maven 依赖。基于图标映射在 FXML 与 Java 代码中配置 FontIcon 用法
   - **交互流程**：读取 `design/flow/interaction-flow.mmd` 理解屏幕转换并据此实现导航逻辑
   - 若 `design-handoff.json` 不存在，则使用内置模板（默认行为）
3. **加载模板**：从 `templates/` 目录读取（若存在设计 handoff 则从 `design/` 目录）
4. **变量替换**：用实际值替换占位符。常见占位符包括 `{{groupId}}`、`{{artifactId}}`、`{{version}}`、`{{mainClass}}`、`{{jdkVersion}}`、`{{javafxVersion}}` 与 `{{packageBase}}`（例如 `com.example`）。`{{packageBase}}` 由所选包命名派生（架构 handoff 的 `package_structure` 或默认值），用于 `pom.xml` JaCoCo 覆盖率 `includes`——替换为实际包基名以使覆盖率检查指向正确的 Controller/ViewModel 包
5. **逻辑填充**：根据用户需求添加业务逻辑代码
6. **样式生成**：生成对应的 CSS 文件（若可用则使用设计技能 CSS）
7. **资源处理**：处理图标、图片与静态资源引用（若可用则使用设计技能图标配置）
8. **需求追溯标注**：在每个生成的 Java 源文件 Javadoc 类头部标注 `@req` 标签，将文件链接到其实现的（多个）需求编号。格式：`@req FR-001`（单个）或 `@req FR-001, FR-002`（多个）。需求编号来源于 `requirements.md` 规格（第 3.1 节功能清单）。若文件实现与特定功能需求无关的基础设施或工具功能，标注为 `@req INFRA`（基础设施）。这使审查技能的"需求覆盖"维度能检测孤儿代码与未实现需求
   ```java
   /**
    * 用户管理控制器。
    *
    * @req FR-001, FR-002
    * @author javafx-developer
    */
   public class UserController { ... }
   ```
9. **测试脚手架**（默认）：使用 TestFX 模板（`templates/test/`）为每个 Controller 与 ViewModel 生成测试骨架。这是现在的默认行为——无需用户请求。生成：
   - `MainWindowTest.java` — 验证主窗口初始化与 FXML 加载
   - `ControllerTest.java` — 验证控制器动作与事件处理器
   - `ViewModelTest.java` — 验证 ViewModel 属性绑定与状态转换（仅 MVVM）
   - `CRUDViewTest.java` — 若存在 TableView 则验证 CRUD 操作
   - 每个测试方法必须通过命名规范 `test{Behavior}_{REQ_ID}()` 引用其验证的需求编号——例如 `testUserCreation_FR_001()`，并在测试方法 Javadoc 中包含 `@req FR-001`。这使审查技能能验证每个需求都有测试覆盖
   - 向 `pom.xml` 添加 JaCoCo 插件用于覆盖率测量
10. **更新 RTM**：所有文件生成后，更新 `requirements.md`（第 7 节）中的需求追溯矩阵——填写实现文件路径、测试用例引用与覆盖摘要。仅代码项设为 `Implemented`，带测试脚手架的项设为 `Tested`

### 第 5 步：质量检查

1. **语法检查**：验证 Java/XML/CSS 语法正确性
2. **命名规范**：验证类/方法/变量名遵循 Java 规范
3. **模块检查**：验证 `module-info.java` 的 exports/requires 完整性
4. **安全审查**：检查 SQL 注入、路径遍历、硬编码凭据
5. **最佳实践**：验证遵循 JavaFX 官方推荐模式

### 第 5.5 步：修复消费（当输入是 Fix Handoff 报告时）

> 当输入是来自 `javafx-code-reviewer`（或 `javafx-refactorer`）的修复 handoff 报告时激活本步骤，通过存在 `fix_handoff` 字段（`target_file`、`target_lines`、`fix_type`、`fix_priority`、`code_fingerprint`、`anchor_pattern`、`ast_node_signature`）识别。此模式下跳过第 1-5 步。

1. **状态恢复**：检查项目根目录是否存在 `.loop-state.json`。若存在，加载 loop 状态（当前轮次、问题历史、收敛趋势）并从最后检查点恢复。若不存在，以第 1 轮初始化新 loop 状态
2. **解析报告与并发分组**：提取所有修复 handoff 条目，按 `fix_priority` 升序排序（1 = 最高）。按 `target_file` 将修复分组为并发组（Step 1a）——不同文件组可并行执行（最多 4 个并发），同文件修复按行号倒序串行执行
3. **修复前备份**：应用任何修复前创建 `.fix-backup/{timestamp}/` 目录并复制所有目标文件。写入 `manifest.json` 记录已备份文件、loop ID 与轮次（见 Fix Consumption Protocol Step 1.5）。备份在并行执行开始前创建
4. **定位并应用修复**（跨文件组并发）：启动并行文件组——每组按 4 级定位匹配层级串行应用其修复：fingerprint → anchor → content → AST signature（见 Fix Consumption Protocol 第 2-3 步）。线程安全结果缓冲区收集所有组的状态
5. **交叉影响检查**（并行）：并发运行 4 项独立检查——Controller-FXML 一致性（A）、module-info 完整性（B）、CSS 引用解析（C）、FXML-Controller 绑定（D）。全部通过后才能进行编译验证（见 Fix Consumption Protocol 第 4 步）
6. **修复后编译验证与回滚**：运行 `mvn compile -q` 验证修复未破坏编译。若编译失败，自动从 `.fix-backup/{timestamp}/` 恢复所有已修改文件，将所有修复标记为 `rolled_back`，并在 `.loop-state.json` 中记录回滚事件（见 Fix Consumption Protocol Step 4.5）
7. **输出修复摘要**（线程安全聚合）：合并所有并行组结果，按 `fix_priority` 排序，列出所有已应用修复及状态（applied/skipped/failed/rolled_back），标记行漂移，推荐下一步（重新审查）
8. **更新 RTM**：若修复修改、添加或删除带 `@req` 标注的文件，更新 `requirements.md`（第 7 节）中的需求追溯矩阵以反映变更。若修复引入原需求之外的新功能，添加新需求编号并相应标注代码
9. **状态持久化**：完成修复后，用当前轮次、已应用修复、问题计数、收敛趋势与回滚事件更新 `.loop-state.json`。这支持跨会话 loop 恢复

完整规格见下方 **Fix Consumption Protocol** 章节。

### 第 6 步：交付与文档

1. **文件清单**：列出所有生成文件及路径
2. **依赖**：告知用户所需 Maven/Gradle 依赖
3. **运行说明**：提供编译与运行命令
4. **后续步骤**：建议功能扩展、测试、打包

## 架构模式（Architecture Patterns）

### MVC 模式（简单应用）

```
src/main/java/com/example/
├── App.java                    # 入口点
├── model/
│   └── User.java               # 带 Properties 的数据模型
├── controller/
│   └── UserController.java     # UI 逻辑 + 事件处理
└── service/
    └── UserService.java        # 业务逻辑

src/main/resources/
├── fxml/
│   └── user-view.fxml          # 布局
└── css/
    └── style.css               # 样式
```

### MVVM 模式（复杂应用）

```
src/main/java/com/example/
├── App.java
├── model/
│   └── User.java               # 纯数据模型
├── viewmodel/
│   └── UserViewModel.java      # 暴露 Properties、命令
├── view/
│   └── UserController.java     # 将 UI 绑定到 ViewModel
└── service/
    └── UserService.java

src/main/resources/
├── fxml/
│   └── user-view.fxml
└── css/
    └── style.css
```

### MVP 模式（高可测性应用）

```
src/main/java/com/example/
├── App.java
├── model/
│   └── User.java               # 纯数据模型
├── view/
│   └── UserView.java           # 视图接口（抽象 UI 操作）
├── presenter/
│   └── UserPresenter.java      # 持有 View 接口引用，无 JavaFX 依赖
├── controller/
│   └── UserController.java     # 实现 View 接口，委托给 Presenter
└── service/
    └── UserService.java

src/main/resources/
├── fxml/
│   └── user-view.fxml
└── css/
    └── style.css
```

## 代码示例（Code Examples）

### 应用入口点

```java
package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("My JavaFX App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

### 带 Properties 的模型

```java
package com.example.model;

import javafx.beans.property.*;

public class User {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Name
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
    public StringProperty nameProperty() { return name; }

    // Email
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    // Active
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }
    public BooleanProperty activeProperty() { return active; }
}
```

### 带数据绑定的控制器

```java
package com.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import com.example.model.User;
import java.net.URL;
import java.util.ResourceBundle;

public class UserController implements Initializable {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private final User model = new User();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 双向绑定
        nameField.textProperty().bindBidirectional(model.nameProperty());
        emailField.textProperty().bindBidirectional(model.emailProperty());
        activeCheckBox.selectedProperty().bindBidirectional(model.activeProperty());

        // 计算绑定
        statusLabel.textProperty().bind(
            model.nameProperty().concat(" - ").concat(model.emailProperty())
        );

        // 校验
        saveButton.disableProperty().bind(
            model.nameProperty().isEmpty().or(model.emailProperty().isEmpty())
        );
    }

    @FXML
    private void handleSave() {
        // 委托给服务层
        System.out.println("Saving user: " + model.getName());
    }
}
```

### FXML 布局

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.VBox?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.control.CheckBox?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.layout.GridPane?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.controller.UserController"
      spacing="15" styleClass="form-container">

    <GridPane hgap="10" vgap="10">
        <Label text="Name:" GridPane.rowIndex="0" GridPane.columnIndex="0"/>
        <TextField fx:id="nameField" GridPane.rowIndex="0" GridPane.columnIndex="1"/>

        <Label text="Email:" GridPane.rowIndex="1" GridPane.columnIndex="0"/>
        <TextField fx:id="emailField" GridPane.rowIndex="1" GridPane.columnIndex="1"/>

        <Label text="Active:" GridPane.rowIndex="2" GridPane.columnIndex="0"/>
        <CheckBox fx:id="activeCheckBox" GridPane.rowIndex="2" GridPane.columnIndex="1"/>
    </GridPane>

    <HBox spacing="10">
        <Button fx:id="saveButton" text="Save" onAction="#handleSave" styleClass="button-primary"/>
        <Label fx:id="statusLabel" styleClass="status-label"/>
    </HBox>
</VBox>
```

### CSS 样式

```css
.root {
    -fx-primary: #3b82f6;
    -fx-bg-base: #ffffff;
    -fx-text-primary: #0f172a;
    -fx-border: #e2e8f0;
}

.form-container {
    -fx-padding: 20;
    -fx-background-color: -fx-bg-base;
}

.button-primary {
    -fx-background-color: -fx-primary;
    -fx-text-fill: white;
    -fx-padding: 8 16;
    -fx-background-radius: 6;
}

.button-primary:hover {
    -fx-background-color: derive(-fx-primary, -10%);
}
```

## 常见 UI 模式（Common UI Patterns）

### CRUD 表格视图
- 绑定到 Properties 的 `TableView` + `TableColumn`
- 用于过滤/排序的 `FilteredList` + `SortedList`
- 通过 `Pagination` 控件分页
- 行操作（编辑、删除）的上下文菜单

### 登录对话框
- 带自定义 `DialogPane` 的 `Dialog<User>`
- 即时反馈的表单校验
- 使用 Preferences API 的"记住我"
- 认证期间的加载状态

### 主从视图
- 主列表 + 详情表单的分割面板
- 选择监听器更新详情视图
- 带防抖定时器的自动保存
- 未保存更改警告

### 导航抽屉
- 带可折叠侧面板的 `BorderPane`
- `ListView` 或自定义菜单项
- 通过 `FXMLLoader` 加载到内容区实现视图切换
- 面包屑跟踪

## 第三方库集成（Third-Party Library Integration）

| 库 | 用途 | Maven 坐标 |
|---------|---------|-----------------|
| ControlsFX | 对话框、通知、校验 | `org.controlsfx:controlsfx:11.2.1` |
| MaterialFX | Material Design 控件 | `io.github.palexdev:materialfx:11.17.0` |
| RichTextFX | 富文本编辑器、代码高亮 | `org.fxmisc.richtext:richtextfx:0.11.5` |
| Ikonli | 字体图标（FontAwesome、Material） | `org.kordamp.ikonli:ikonli-javafx:12.3.1` |
| ValidatorFX | 表单校验框架 | `net.synedra:validatorfx:0.4.0` |
| TestFX | UI 自动化测试 | `org.testfx:testfx-junit5:4.0.18` |

详细集成指南见 `references/third-party-libraries.md`。

## 网络集成（Retrofit）

对与后端 REST API 通信的 JavaFX + Spring Boot 应用，使用 `retrofit-spring-boot-starter` 实现声明式 HTTP 客户端。

| 关切 | 模式 |
|---------|---------|
| API 定义 | 带 `@GET`/`@POST`/`@PUT`/`@DELETE` 注解的 `@RetrofitClient` 接口 |
| HTTP 客户端 | 通过 `SourceOkHttpClientRegistrar` 注册自定义 OkHttpClient，含拦截器与超时 |
| 认证 | 从 `TokenContext` 注入 `Authorization: Bearer <token>` 的 OkHttp 拦截器 |
| 响应解析 | `ApiResult.of(rawMap).assertSuccess()` — 带状态码断言与类型化提取的统一包装器 |
| 异步执行 | `AsyncUtil.run()` — 专用线程池（守护线程、DiscardOldestPolicy）+ 用于 UI 线程切换的 `runLater()` |
| 异常处理 | 带响应码的 `ApiException`；带对话框节流与认证过期检测的 `GlobalExceptionHandler` |
| 认证过期 | `AuthContextHolder` — 401 响应时 CAS 保护的导航到登录屏幕 |

**关键**：Retrofit 调用是同步阻塞的——**绝不能在 JavaFX Application Thread 上运行**。始终用 `AsyncUtil.run()` 包装。

完整集成模式（配置、API 接口、拦截器、Service 层、异步、异常处理）见 `references/networking-retrofit.md`。

## 自定义控件（Custom Controls）

当标准 JavaFX 控件无法满足需求时，使用 Control/Skin 架构构建自定义控件。

| 方面 | 模式 |
|--------|---------|
| Control 类 | `extends Control` — 公共 API、属性、CSS 元数据；不含渲染逻辑 |
| Skin 类 | `extends SkinBase<MyControl>` — 视觉渲染、事件处理、布局 |
| 属性 | JavaFX Beans 规范（惰性初始化 + final getter/setter） |
| CSS 样式 | `StyleableObjectProperty` + `CssMetaData` 用于自定义 `-fx-*` 属性 |
| 伪类 | `PseudoClass.getPseudoClass()` 用于视觉状态（:readonly、:hover） |
| 高性能绘制 | 复杂视觉（图表、仪表）用 `Canvas` + `GraphicsContext` |

**经验法则**：若只需组合现有控件，扩展 `Region` 或布局面板即可。若需要自定义渲染、可 CSS 样式化属性或新交互，使用 Control + Skin。

完整模式（属性、CSS 元数据、伪类、Canvas 渲染、事件处理）与完整 RatingControl 示例（Control + Skin + CSS）见 `references/custom-controls.md`。

## 打包（Packaging）

### jpackage（推荐）

`jpackage` 将 JavaFX 应用打包为带嵌入式自定义 JRE 的平台原生安装器。**jpackage 不支持交叉编译**——每个平台的安装器必须在对应操作系统的宿主机上构建。要在三个桌面平台分发，需为每平台维护独立 jpackage 命令（下方模板），并通过 CI/CD 矩阵构建协调。

#### 通用前置条件

```bash
# 先构建应用 JAR（所有平台共享）
mvn clean package
```

#### Windows 模板（`.msi`）

需要 PATH 上有 WiX Toolset 4.x。使用稳定的 `--win-upgrade-uuid`，使新版本就地升级而非并行安装。

```bash
jpackage \
  --type msi \
  --name "MyApp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.ico \
  --win-menu \
  --win-shortcut \
  --win-upgrade-uuid "12345678-1234-4234-8234-123456789abc" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

#### macOS 模板（`.dmg`）

需要 Xcode 命令行工具。`--mac-package-identifier`（反向 DNS）必须稳定并与签名/公证配置匹配。

```bash
jpackage \
  --type dmg \
  --name "MyApp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.icns \
  --mac-package-name "MyApp" \
  --mac-package-identifier "com.mycompany.myapp" \
  --mac-sign \
  --mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)" \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

#### Linux 模板（`.deb` 与 `.rpm`）

需要 `dpkg-deb`（用于 `.deb`）或 `rpm-build`（用于 `.rpm`）。运行两条命令以产出两种分发格式。

```bash
# .deb (Debian/Ubuntu)
jpackage \
  --type deb \
  --name "myapp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.png \
  --linux-package-name "myapp" \
  --linux-deb-maintainer "dev@mycompany.com" \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist

# .rpm (Fedora/RHEL)
jpackage \
  --type rpm \
  --name "myapp" \
  --app-version 1.0.0 \
  --vendor "MyCompany" \
  --module com.example/com.example.App \
  --module-path "mods:libs" \
  --runtime-image build/javafx-runtime \
  --icon src/main/resources/icons/app.png \
  --linux-package-name "myapp" \
  --linux-rpm-license-type "MIT" \
  --install-dir /opt/myapp \
  --java-options "--enable-native-access=javafx.graphics" \
  --dest dist
```

#### 各平台工具链要求

| 平台 | 输出类型 | 所需工具链 | 安装命令 |
|----------|-------------|---------------------|-----------------|
| Windows | `msi` | WiX Toolset 4.x | `dotnet tool install --global wix` |
| Windows | `exe` | Inno Setup 6+ | 从 jrsoftware.org 下载，加入 `PATH` |
| macOS | `dmg` / `pkg` | Xcode 命令行工具 | `xcode-select --install` |
| Linux | `deb` | `dpkg-deb` | `apt install dpkg-dev`（Debian/Ubuntu） |
| Linux | `rpm` | `rpmbuild` | `dnf install rpm-build`（Fedora/RHEL） |

> **注意**：jpackage 不能交叉编译。构建 Windows `.msi` 需要带 WiX 的 Windows 宿主机；macOS `.dmg` 需要带 Xcode 工具的 macOS；Linux `.deb`/`.rpm` 需要相应的 Linux 工具链。多平台交付时，通过 CI/CD 矩阵构建在各原生 OS 上生成各工件（见 `references/cross-platform-packaging.md`）。

### 各平台输出类型：
- Windows：`--type exe`（Inno Setup）或 `--type msi`（WiX）
- macOS：`--type dmg` 或 `--type pkg`
- Linux：`--type deb` 或 `--type rpm`

详细打包指南见 `references/cross-platform-packaging.md` 与 `references/ci-cd-pipeline.md`。

## 约束（Constraints）

### 编码标准
1. **命名**：类用 PascalCase、方法/变量用 camelCase、常量用 SCREAMING_SNAKE_CASE
2. **缩进**：4 空格，不用制表符
3. **编码**：所有源文件使用 UTF-8
4. **导入**：显式导入，不用通配符（`import javafx.scene.control.*`）
5. **注释**：公共 API 用 Javadoc，复杂逻辑用行内注释

### 架构规则
1. **FXML 纯净性**：FXML 文件中不使用 `<fx:script>`
2. **控制器职责**：仅处理 UI 事件与视图状态，业务逻辑委托给 Service
3. **绑定优先**：优先 JavaFX Properties 绑定而非手动 UI 同步
4. **资源路径**：使用 `getClass().getResource()` 加载 FXML/CSS
5. **线程安全**：所有 UI 更新在 JavaFX Application Thread 上，后台工作使用 `Task`/`Service`

### 安全规则
1. **输入校验**：校验所有用户输入，不拼接 SQL/命令
2. **路径安全**：文件操作使用 `Paths.get()` + `Path.normalize()`
3. **不硬编码密钥**：使用配置文件或环境变量
4. **WebView 安全**：禁用 JavaScript 或仅限可信内容

## 输出格式（Output Format）

交付代码时，始终提供：

1. **文件清单** — 列出所有生成文件及完整路径
2. **依赖** — 所需 Maven/Gradle 依赖
3. **运行说明** — 编译与运行命令（例如 `mvn javafx:run`）
4. **UI 预览** — 若存在 UI 截图（来自手动捕获或运行时验证），嵌入交付物给用户视觉确认渲染的 UI。若无截图，提供 FXML 控件树图（Mermaid 流程图）作为结构预览
5. **后续步骤** — 扩展、测试、打包的建议

### 示例输出结构

```
## 生成的文件

### Java 源码
- `src/main/java/com/example/App.java` — 应用入口点
- `src/main/java/com/example/controller/MainController.java` — 主控制器
- `src/main/java/com/example/model/User.java` — 数据模型

### 资源
- `src/main/resources/fxml/main-view.fxml` — 主布局
- `src/main/resources/css/style.css` — 样式表

### 构建配置
- `pom.xml` — Maven 构建配置
- `src/main/java/module-info.java` — 模块描述符

### 依赖
[如需则附加 Maven 依赖]

### 运行命令
mvn javafx:run

### UI 预览
- `target/ui-preview.png` — 主窗口截图（若可从运行时验证或手动捕获获得）
- 或：FXML 控件树图（Mermaid 流程图）作为结构预览回退
```

## 质量清单（Quality Checklist）

交付前验证：
- [ ] 所有 Java 文件编译无语法错误
- [ ] FXML `fx:id` 字段匹配 Controller 字段
- [ ] CSS 文件无语法错误、变量已定义
- [ ] `module-info.java` 包含所有必需的 `requires` 与 `opens`
- [ ] 包路径一致、无拼写错误
- [ ] 类/方法名遵循命名规范
- [ ] 无硬编码敏感信息或绝对路径
- [ ] 线程安全：后台任务使用 `Platform.runLater()`
- [ ] 资源路径正确（相对路径，非绝对路径）
- [ ] 公共 API 有 Javadoc 注释
- [ ] 每个 Java 源文件在类 Javadoc 头有 `@req` 标注
- [ ] 测试方法遵循 `test{Behavior}_{REQ_ID}()` 命名规范
- [ ] `requirements.md` 中的需求追溯矩阵已用实现与测试映射更新
- [ ] JavaFX 24+ 项目配置了 `--enable-native-access=javafx.graphics`
- [ ] Spring Boot Controller 标注了 `@Scope("prototype")`

## 修复消费协议（Fix Consumption Protocol）

本协议定义 `javafx-developer` 如何消费 `javafx-code-reviewer` 与 `javafx-refactorer` 产出的修复 handoff 报告。它使"修复 → 再审查"闭环无需人工干预。

### 适用时机

当输入是修复 handoff 报告（通过存在 `fix_handoff` 字段识别）时使用本协议：
- `target_file`：要修改的文件路径
- `target_lines`：起始-结束行范围
- `fix_type`：`replace` / `insert` / `delete`
- `fix_priority`：整数，1 = 最高优先级
- `code_fingerprint`：问题代码片段的 SHA-256 哈希（用于抗漂移匹配）
- `anchor_pattern`：周围上下文签名（用于次级位置匹配）
- `ast_node_signature`：方法/字段/类的完全限定签名（用于抗重构匹配；非 Java 文件为 `null`）

**当存在 `refactor-handoff.json` 时本协议同样激活**（由 `javafx-refactorer` 产出）。与 code-reviewer 报告不同，refactorer 将 7 个统一 Fix Handoff 字段**嵌入每个 `refactoring_plan[]` 数组项**，而非置于顶层 `fix_handoff` 字段。因此，当以下条件满足时将 `refactor-handoff.json` 视为有效修复 handoff 来源：
- 项目中存在 `refactor-handoff.json` 文件，**且**
- 其 `refactoring_plan[]` 数组含带统一 Fix Handoff 字段的项：`target_file`、`target_lines`、`fix_type`、`fix_priority`、`code_fingerprint`、`anchor_pattern`、`ast_node_signature`。

此时，每个 `refactoring_plan[]` 项被解析为单条修复条目（使用与上述 `fix_handoff` 字段相同的语义），`refactor_id` 映射到修复 id，`before_snippet`/`after_snippet` 提供替换内容。无需字段名转换——refactorer 的 `refactor-handoff-schema.json` 已将这些字段与统一 Fix Handoff 契约对齐。

### 工作流

**第 1 步：解析报告**
- 从报告中提取所有修复 handoff 条目
- **多来源合并**：当输入同时包含来自 `javafx-code-reviewer` 与 `javafx-refactorer` 的修复 handoff 时，通过去重处理合并：对**同一 `target_file`** 且 **`target_lines` 重叠**的条目（例如 reviewr 报告 10-15 行、refactorer 报告同一文件 12-18 行）：
  - 保留 **severity 更高**的条目（Critical > Major > Minor > Info）
  - 若 severity 相同，保留 **`fix_priority` 数字更小**（优先级更高）的条目
  - 将被丢弃条目的来源与问题标题记录在保留条目的 `dedup_merged_from` 字段中以便追溯
- 按 `fix_priority` 升序排序（1 = 最高）
- 按 `target_file` 分组条目以便批量处理

**第 1a 步：并发修复分组**

解析与排序后，将修复分组为**并发组**以并行执行：

1. **按文件分组**：将所有修复条目按 `target_file` 分组。每组包含所有针对同一文件的修复：
   ```
   Group A (UserController.java): [RF-001 (lines 10-15), RF-004 (lines 45-50)]
   Group B (User.java):            [RF-002 (lines 8-12)]
   Group C (user-view.fxml):       [RF-003 (lines 20-25)]
   Group D (OrderService.java):    [RF-005 (lines 30-35), RF-006 (lines 60-65)]
   ```

2. **并行资格**：针对**不同文件**的组有资格并行执行。针对**同一文件**的组（第 3 步 AST 重定位后）必须合并并串行执行。

3. **组内排序**：每个文件组内按以下顺序排序修复：
   - **主排序**：`fix_priority` 升序（1 = 最高优先级，先应用）
   - **次排序**：`target_lines` 起始行**降序**（最高行先——防止早期修复的行漂移使后续行号错位）
   - 组内每个修复应用后，基于行数增量重算同组剩余修复的行号

4. **并发上限**：并行组最大数量受 `min(file_group_count, 4)` 约束——最多 4 个并发文件修改的硬限制，以避免过度 I/O 争用并保持可调试性。当文件组 ≤ 4 时全部并行执行。当 > 4 时，按 4 个一批处理。

5. **依赖感知调度**：若组 A 的修复创建了新文件（通过重构 handoff 的 `new_files` 字段检测）而被组 B 的修复引用，则组 B 必须等待组 A 完成。将此类依赖记录在**组依赖图**中：
   - 构建 DAG：若 B 中某修复依赖 A 中某修复创建的 `new_file`，则 A → B
   - 拓扑排序：按依赖顺序执行组；独立组并行运行

6. **输出**：带组内排序与组间依赖图的文件组列表，准备在第 2 步并行执行

**第 1.5 步：修复前备份**
应用任何修复前，为所有将修改的文件创建备份：
1. **创建备份目录**：项目根目录的 `.fix-backup/{timestamp}/`，其中 `{timestamp}` 为 `yyyy-MM-dd-HHmmss` 格式（例如 `.fix-backup/2026-06-29-101530/`）
2. **复制目标文件**：为修复 handoff 列表中的每个唯一 `target_file`，将当前版本复制到备份目录，保留相对路径结构：
   ```
   .fix-backup/2026-06-29-101530/
   ├── src/main/java/com/example/controller/UserController.java
   ├── src/main/java/com/example/model/User.java
   └── src/main/resources/fxml/user-view.fxml
   ```
3. **创建备份清单**：写入 `.fix-backup/{timestamp}/manifest.json` 记录：
   - `timestamp`：备份创建时间
   - `loop_id`：来自 `.loop-state.json` 的当前 loop ID
   - `round`：当前轮次
   - `files`：已备份文件路径列表（相对项目根目录）
   - `fix_count`：修复 handoff 条目总数
4. **跳过的修复不备份**：若某修复条目在解析时已标记为 `skipped`（例如无效目标文件），不将其 `target_file` 纳入备份
5. **备份失败处理**：若 `target_file` 不存在（文件被删除或路径错误），跳过并在备份清单中标记为 `missing`

**第 2 步：定位并应用修复**（逐条目，跨文件组并发执行）

修复使用**两级执行模型**应用：
- **文件间层级（并行）**：不同文件组并发执行（最多 4 个并行组，按第 1a 步并发限制）
- **文件内层级（串行）**：每个文件组内，修复按第 1a 步确定的顺序串行应用（优先级升序，然后行号降序）

**并行执行流程**：
1. **启动文件组**：对每个独立文件组（组依赖图中无未解析依赖），启动并行执行上下文
2. **组内串行应用**：每个组内逐条应用修复：
   - 读取 `target_file`，导航到 `target_lines`
   - 验证：确认 `target_lines` 处的代码与报告中的"问题代码"片段匹配（使用第 3 步位置匹配）
   - 执行 `fix_type`：
     - `replace`：用报告中的"修正示例"替换目标行
     - `insert`：在 `target_lines` 之后插入"修正示例"
     - `delete`：移除 `target_lines`
   - 记录修复状态：`applied` / `skipped` / `failed`（线程安全写入修复摘要——见第 5 步）
   - **重算行号**：组内每个修复应用后，基于行数增量调整剩余修复的 `target_lines`（新增行→下移，移除行→上移）
3. **等待所有组**：启动所有并行组后，等待全部完成再进入第 4 步（交叉影响检查）
4. **依赖解析**：若某组依赖其他组（来自第 1a 步 DAG），它等待其依赖完成后再启动

**线程安全保证**：
- 每个文件组操作**不同文件**——不会有两个组同时修改同一文件
- 修复摘要使用**线程安全追加**机制写入（见第 5 步）
- 修复前备份（第 1.5 步）在**任何并行执行开始前**创建——所有组从同一备份基线读取
- 若某组遇到致命错误（例如文件未找到、磁盘满），该组中止且其修复标记为 `failed`。其他组独立继续

**第 3 步：位置匹配与行漂移处理**
- **指纹优先匹配**（主）：计算 `target_lines` 处代码的 SHA-256 哈希（规范化：去首尾空白）并与报告的 `code_fingerprint` 字段比较。匹配则直接在 `target_lines` 应用修复
- **锚点匹配**（次）：若指纹不匹配（检测到行漂移），用 `anchor_pattern`（前 2 行 + 后 2 行）在文件内搜索正确位置。若锚点模式恰在一处匹配，在那里应用修复并记录状态为 `applied (relocated by anchor)`
- **内容匹配**（回退）：若指纹与锚点匹配均失败，回退到在指定范围 ±10 行内搜索"问题代码"片段内容。若找到，在实际位置应用修复并记录状态为 `applied (relocated)`
- **AST 签名匹配**（抗重构）：若上述三种方法全部失败（说明代码被移动、重命名或大幅重构），用 `ast_node_signature` 通过结构标识定位代码：
  1. 解析 `ast_node_signature` 提取完全限定类名与可选成员名（按 `#` 拆分）
  2. 在项目源码树中搜索类文件（将 `{package}.{Class}` 解析为 `src/main/java/{package}/{Class}.java`）
  3. 若类文件位于与 `target_file` **不同的文件**，将 `target_file` 更新为实际位置并记录状态为 `applied (relocated by AST: {old_file} → {new_file})`
  4. 若签名含方法（例如 `#handleSave(ActionEvent)`），解析文件找到匹配名称与参数类型的方法声明。在方法实际位置应用修复
  5. 若签名含字段（例如 `#userName`），找到字段声明并在那里应用修复
  6. 若仅存在类签名（例如 `com.example.controller.UserController`），定位类声明并在类级别应用修复
  7. 若在预期包中未找到类，按类名（简单名匹配）全项目搜索以处理包重命名
  8. 若在定位类中未找到方法/字段，记录状态为 `skipped (AST node not found)` 并在修复摘要中包含签名供人工调查
- **无匹配**：若四种方法全部失败，记录状态为 `skipped (line drift)` 并包含在修复摘要中
- **组内批量排序**（并发组内）：当多个修复针对同一文件时，按行号倒序（最高行先）应用，防止早期修复使后续行号错位。每个修复应用后，基于已应用修复的行数增量重算同文件剩余修复的行号。此排序在第 1a 步确定，在第 2 步组内串行应用时执行
- **AST 重定位与组重新分配**：若 AST 签名匹配（第 4 级）将修复重定位到与原始分组**不同**的文件，该修复移到目标文件的组。若目标文件组已在执行，重定位的修复推迟到并行阶段之后，在所有并行组完成后串行应用

**第 4 步：交叉影响检查**（并行执行）

应用所有修复（所有并行文件组完成）后，验证跨文件一致性。四类检查**相互独立**可**并行**执行：

1. **Controller-FXML 一致性检查**（并行任务 A）：若某修复修改了 Controller 类，重新检查 FXML `fx:id` 字段是否匹配 Controller 的 `@FXML` 标注字段。扫描所有通过 `fx:controller` 引用该修改 Controller 的 FXML 文件
2. **模块描述符检查**（并行任务 B）：若某修复修改了 `module-info.java`，重新检查所有 `requires` 与 `opens` 指令是否完整——验证被修改文件引用的每个包都有对应的 `opens` 或 `exports` 指令
3. **CSS 引用检查**（并行任务 C）：若某修复修改了 CSS 文件，重新检查 FXML 文件中的所有 `styleClass` 引用是否仍能解析到 CSS 类定义。若某修复修改了 FXML，验证新增 `styleClass` 属性有对应的 CSS 定义
4. **FXML-Controller 绑定检查**（并行任务 D）：若某修复修改了 FXML 文件，重新检查 `fx:controller` 路径有效，且所有事件处理器签名（`onAction`、`onMouseClicked` 等）匹配 Controller 类中的方法

**并行执行规则**：
- 四项检查并发运行——它们检查不同类型的文件且相互无数据依赖
- 每项检查产出带发现的不匹配详情的 **pass/fail 结果**
- 若有检查失败，将不匹配记录到修复摘要作为交叉影响警告
- 所有检查必须通过才能进入 Step 4.5（编译验证）。失败的交叉影响检查**不触发回滚**——而是生成包含在修复摘要中的警告，供审查技能在下一轮评估

**第 4.5 步：修复后编译验证与回滚**
所有修复应用且交叉影响检查通过后，验证项目仍可编译：
1. **执行编译验证**：运行 `mvn compile -q`（增量）验证已应用修复未引入编译错误。Gradle 项目使用 `gradle compileJava --quiet`
2. **编译通过** → 进入第 5 步（输出修复摘要），将所有修复状态设为 `applied`
3. **编译失败** → **自动回滚**触发：
   a. **识别失败文件**：解析编译输出识别有编译错误的文件
   b. **回滚所有已修改文件**：从 `.fix-backup/{timestamp}/` 将每个文件恢复到原始位置，覆盖修改版本。使用备份清单确保所有已备份文件被恢复
   c. **验证回滚**：重新运行 `mvn compile -q` 确认回滚后项目重新可编译（健全性检查——项目应处于修复前状态）
   d. **标记修复状态**：在修复摘要中将所有修复条目设为 `rolled_back`
   e. **记录回滚事件**：向 `.loop-state.json` 添加 `rollback_event` 条目（见 Loop State Serialization）
   f. **推荐下一步**：若发生回滚，推荐"需要人工干预——修复导致编译失败。请审查修复 handoff 条目并单独应用修复以隔离问题变更"
4. **回滚失败处理**：若回滚本身失败（例如备份文件损坏或缺失），标记状态为 `rollback_failed` 并推荐立即人工干预。这是严重错误——项目可能处于不一致状态

> **为何仅编译**：回滚检查点只检查编译——这使回滚决策快速且聚焦结构完整性。若代码可编译但有运行时问题，这些问题在后续审查轮次中捕获，而非由此检查点捕获。

**第 5 步：输出修复摘要**（线程安全聚合）

所有并行文件组完成（第 2 步）、所有交叉影响检查结束（第 4 步）、编译验证通过（第 4.5 步）后，将结果聚合为修复摘要：

**线程安全修复摘要构建**：
- 并行执行期间（第 2 步），每个文件组将其修复结果追加到**线程安全结果缓冲区**。缓冲区使用并发追加机制——每组只写自己的条目，无组读取或修改其他组条目
- 所有组完成后，缓冲区按 `fix_priority` 升序**排序**生成最终修复摘要表
- 交叉影响检查结果（第 4 步）作为附加警告行合并到修复摘要，在 Note 列标注 `cross-impact`
- 回滚事件（第 4.5 步）作为修复摘要表下方的独立章节追加

| # | 优先级 | 文件 | 行 | 修复类型 | 状态 | 备注 |
|---|----------|------|-------|----------|--------|------|
| 1 | 1 | `path/to/File.java` | 10-15 | replace | applied | — |
| 2 | 2 | `path/to/Other.java` | 30-30 | insert | applied (relocated) | 行漂移，实际位于第 32 行 |
| 3 | 3 | `path/to/Config.java` | 5-8 | delete | skipped | 行漂移，片段未找到 |
| 4 | 4 | `path/to/Broken.java` | 20-25 | replace | rolled_back | 修复后编译失败，文件已从备份恢复 |
| 5 | 5 | `path/to/Moved.java` | 45-50 | replace | applied (relocated by AST) | 方法移至 `path/to/NewLocation.java`，通过 `com.example.Moved#handleAction()` 找到 |

**下一步推荐**：
- 若所有修复已应用：推荐"重新审查"以确认修复正确（无论是一个还是两个来源存在）
- 若存在跳过修复：列出跳过修复并推荐人工干预
- 若存在失败修复：描述失败原因并推荐人工审查
- 若存在回滚修复：推荐"需要人工干预——修复导致编译失败。请审查修复 handoff 条目并单独应用修复以隔离问题变更。备份保留在 `.fix-backup/{timestamp}/`"

### 修复消费模式下的质量检查范围

修复消费模式下**不重新运行**完整的 12 项质量清单。只检查：
1. 与已应用修复直接相关的项
2. 第 4 步识别的交叉影响项
3. 修改文件的 Java 编译（语法检查）

## 修复循环与状态管理（Fix Cycle & State Management）

`javafx-developer` 参与与 `javafx-code-reviewer`（及可选 `javafx-refactorer`）的闭环修复循环。开发者管理自身 loop 状态以支持跨会话恢复与增量修复应用。

### 开发者在此循环中的角色

`javafx-developer` 占据两个阶段：
- **生成**（第 1 轮）：从用户需求生成初始项目代码（第 1-6 步）
- **修复**（第 1+ 轮）：消费来自 `javafx-code-reviewer`（及可选 `javafx-refactorer`）的修复 handoff 报告，应用修复（第 5.5 步），输出修复摘要

### 开发者的序列化职责

1. **读取状态**：应用修复前，检查 `.loop-state.json` 确定当前轮次与修复范围
2. **应用修复**：使用 [Fix Consumption Protocol](#fix-consumption-protocol)（4 级位置匹配：fingerprint → anchor → content → AST signature）消费合并后的 Fix Handoff
3. **写入结果**：应用修复后，用 `fixes_applied`、`fixes_skipped`、`fixes_rolled_back` 计数与带每条 handoff 状态（`applied`/`skipped`/`rolled_back`）的 `fix_handoffs[]` 数组更新 `rounds[current_round]`
4. **修复前备份**：应用任何修复前，将所有目标文件复制到 `.fix-backup/{timestamp}/` 并带 `manifest.json`——若修复后编译失败则可自动回滚
5. **回滚**：若修复后编译失败，从 `.fix-backup/{timestamp}/` 恢复所有已修改文件，将修复标记为 `rolled_back`，向状态追加 `rollback_event`
6. **备份清理**：修复循环通过审查后清理 `.fix-backup/` 目录。暂停/中止的循环保留 `.fix-backup/` 供人工检查

> **Fix Handoff 格式**：Fix Handoff 字段（`target_file`、`target_lines`、`fix_type`、`fix_priority`、`code_fingerprint`、`anchor_pattern`、`ast_node_signature`，以及合并时添加的可选 `dedup_merged_from`）定义于上述 Fix Consumption Protocol。开发者的 Fix Consumption Protocol（第 1 步）描述了这些字段如何用于 4 级位置匹配，以及 `dedup_merged_from` 如何在多来源去重时记录合并条目。

## 参考文档（Reference Documents）

深度指导请参阅 `references/` 目录中的以下文档：

- `references/project-setup.md` — Maven/Gradle 配置、版本矩阵、模块化设置
- `references/architecture-patterns.md` — MVC/MVVM/MVP 详细对比、反模式
- `references/spring-boot-integration.md` — Spring Boot + JavaFX 集成、启动类拆分、DI、常见陷阱
- `references/database-integration.md` — 数据库层集成（JPA/Hibernate、MyBatis、Spring Data JPA、Flyway、HikariCP、JavaFX Property/Entity 集成、线程安全陷阱）
- `references/css-best-practices.md` — CSS 选择器、主题变量、响应式布局
- `references/data-binding-patterns.md` — Property 类型、绑定模式、表单校验
- `references/third-party-libraries.md` — 库集成指南、兼容性矩阵
- `references/networking-retrofit.md` — Retrofit（retrofit-spring-boot-starter）网络集成：API 接口定义、OkHttp 客户端注册、token 拦截器、统一响应解析（ApiResult）、带 UI 线程安全的异步执行、全局异常处理、超时/重试配置
- `references/custom-controls.md` — 自定义控件开发：Control/Skin 架构、JavaFX 属性（Beans 规范）、可 CSS 样式化属性、伪类、基于 Canvas 的渲染、事件处理、完整 RatingControl 示例（Control + Skin + CSS）
- `references/static-analysis-tools.md` — 静态分析工具集成：SpotBugs/PMD/Checkstyle Maven 插件配置、JavaFX 定制规则集、误报排除、报告解析、供审查技能消费的统一问题映射
- `references/cross-platform-packaging.md` — 跨平台 jpackage 选项矩阵、各平台工具链、图标转换、代码签名、CI/CD 构建矩阵
- `references/ci-cd-pipeline.md` — CI/CD 流水线配置、Monocle 无头测试、示例工作流
- `references/fix-consumption-protocol.md` — Fix Consumption Protocol：4 级位置匹配（fingerprint → anchor → content → AST signature）、并发文件分组、去重、备份/回滚
- `EVALUATE.md` — 评估测试用例，用于量化技能输出质量

## 模板库（Template Library）

`templates/` 目录中的可复用代码模板：

- `templates/maven/pom.xml` — Maven POM 模板
- `templates/maven/module-info.java` — 模块描述符模板
- `templates/maven/Application.java` — 应用入口点模板
- `templates/gradle/build.gradle` — Gradle 构建模板
- `templates/fxml/main-view.fxml` — 主窗口 FXML 模板
- `templates/fxml/dialog.fxml` — 对话框 FXML 模板
- `templates/controller/MainController.java` — 控制器模板
- `templates/controller/BaseController.java` — 基础控制器模板
- `templates/controller/DialogController.java` — 对话框控制器模板
- `templates/model/ObservableModel.java` — 模型模板
- `templates/viewmodel/UserViewModel.java` — ViewModel 模板（MVVM 模式）
- `templates/service/AbstractService.java` — 抽象服务层模板（命名为 AbstractService 以避免与 javafx.concurrent.Service 冲突）
- `templates/service/Repository.java` — Repository 接口模板
- `templates/dao/Entity.java` — 带 JavaFX Property 集成的 JPA Entity 模板（@Access(PROPERTY)、@Transient Property 访问器、null 安全原始类型 setter）
- `templates/dao/Repository.java` — Spring Data JPA Repository 接口模板（JpaRepository 扩展、派生/JPQL 查询方法、线程安全说明）
- `templates/dao/FlywayMigration.sql` — Flyway 版本化迁移 SQL 模板（命名规范、幂等性规则、可重复迁移示例）
- `templates/presenter/Presenter.java` — Presenter 模板（MVP 模式）
- `templates/presenter/View.java` — View 接口模板（MVP 模式）
- `templates/css/light-theme.css` — 亮色主题 CSS
- `templates/css/dark-theme.css` — 暗色主题 CSS
- `templates/test/pom-test-dependencies.xml` — TestFX + JUnit 5 + Monocle 测试依赖片段
- `templates/test/MainWindowTest.java` — 主窗口集成测试模板（FXML 加载、控制器注入、CSS）
- `templates/test/ControllerTest.java` — 控制器单元测试模板（mock Service、事件处理逻辑）
- `templates/test/ViewModelTest.java` — ViewModel 单元测试模板（绑定逻辑、计算属性）
- `templates/test/CRUDViewTest.java` — 主视图集成测试模板（FXML 加载、菜单/内容/状态栏结构验证；可适配 CRUD 视图）
- `templates/packaging/jpackage-config.properties` — 打包配置
- `templates/ci/github-actions.yml` — GitHub Actions CI/CD 工作流模板（多平台矩阵、Monocle 无头测试、jpackage 打包）
- `templates/ci/gitlab-ci.yml` — GitLab CI/CD 模板（编译、测试、带标签运行器的跨平台打包）
- `templates/docs/requirements.md` — 需求规格模板（功能/非功能需求、技术约束、追溯）
