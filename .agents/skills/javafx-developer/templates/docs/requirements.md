# 需求规格说明书 — {{PROJECT_NAME}}

> 本文档由 `javafx-developer` 第 1 步自动生成，作为增量审查与需求追溯的基线。

## 1. 项目概述

- **项目名称（Project Name）**：{{PROJECT_NAME}}
- **包名（Package）**：{{PACKAGE}}
- **项目描述（Description）**：{{PROJECT_DESCRIPTION}}

## 2. 技术约束

| 约束 | 决策 | 理由 |
|-----------|----------|-----------|
| JavaFX 版本 | {{JAVAFX_VERSION}} | {{JAVAFX_RATIONALE}} |
| JDK 版本 | {{JDK_VERSION}} | {{JDK_RATIONALE}} |
| 构建工具 | {{BUILD_TOOL}} | {{BUILD_TOOL_RATIONALE}} |
| 架构模式 | {{ARCHITECTURE_PATTERN}} | {{ARCHITECTURE_RATIONALE}} |
| UI 模式 | {{UI_PATTERN}} | {{UI_PATTERN_RATIONALE}} |
| 主题 | {{THEME}} | {{THEME_RATIONALE}} |

## 3. 功能需求

### 3.1 功能清单

| ID | 功能 | 优先级 | 描述 |
|----|---------|----------|-------------|
| FR-001 | {{FEATURE_NAME}} | {{PRIORITY}} | {{FEATURE_DESCRIPTION}} |
| FR-002 | {{FEATURE_NAME}} | {{PRIORITY}} | {{FEATURE_DESCRIPTION}} |

### 3.2 UI 流程

#### 流程 1：{{FLOW_NAME}}

1. 用户 {{ACTION}}
2. 系统 {{RESPONSE}}
3. 用户 {{NEXT_ACTION}}
4. 系统 {{FINAL_RESPONSE}}

### 3.3 数据模型

| 模型 | 属性 | 校验 |
|-------|-----------|------------|
| {{MODEL_NAME}} | {{PROPERTY_LIST}} | {{VALIDATION_RULES}} |

## 4. 非功能需求

### 4.1 性能

| 需求 | 目标 | 测量方法 |
|-------------|--------|-------------|
| 启动时间 | < 3 秒 | 从启动到首个窗口可见耗时 |
| UI 响应性 | 每帧 < 16ms | 动画 60 FPS |
| 内存占用 | < 256MB | 完整加载后的 JVM 堆 |

### 4.2 安全

| 需求 | 实现方式 |
|-------------|----------------|
| 输入校验 | 所有用户输入在处理前均经过校验 |
| SQL 注入防护 | 使用数据库时采用参数化查询 |
| 路径遍历防护 | 所有文件路径均经清洗与校验 |
| 凭据处理 | 不硬编码凭据；使用环境变量 |

### 4.3 兼容性

| 平台 | 状态 |
|----------|--------|
| Windows 10/11 | 目标支持 |
| macOS 12+ | 目标支持 |
| Ubuntu 22.04+ | 目标支持 |

## 5. UI 设计

### 5.1 屏幕清单

| 屏幕 | FXML 文件 | 控制器 | 描述 |
|--------|----------|------------|-------------|
| {{SCREEN_NAME}} | {{FXML_FILE}} | {{CONTROLLER_NAME}} | {{SCREEN_DESCRIPTION}} |

### 5.2 组件清单

| 组件 | 类型 | 位置 | 绑定 |
|-----------|------|----------|---------|
| {{COMPONENT_NAME}} | {{COMPONENT_TYPE}} | {{SCREEN_NAME}} | {{BINDING_PROPERTY}} |

## 6. 依赖

| 依赖 | 版本 | 作用域 | 用途 |
|-----------|---------|-------|---------|
| org.openjfx:javafx-controls | {{JAVAFX_VERSION}} | compile | UI 控件 |
| org.openjfx:javafx-fxml | {{JAVAFX_VERSION}} | compile | FXML 支持 |
| {{ADDITIONAL_DEPENDENCY}} | {{VERSION}} | {{SCOPE}} | {{PURPOSE}} |

## 7. 需求追溯矩阵（RTM）

> 本矩阵将需求映射到实现文件与测试用例。在代码生成（第 4 步）与修复消费（第 5.5 步）期间更新。`javafx-code-reviewer` 在"需求覆盖"维度验证此矩阵。

### 7.1 需求 → 实现 → 测试映射

| 需求 ID | 描述 | 实现文件 | 测试用例 | 状态 |
|---------------|-------------|------------------------|--------------|--------|
| FR-001 | {{FEATURE_DESCRIPTION}} | `{{FILE_PATH}}` | `{{TEST_CLASS}}#{{TEST_METHOD}}` | {{STATUS}} |
| FR-002 | {{FEATURE_DESCRIPTION}} | `{{FILE_PATH}}` | `{{TEST_CLASS}}#{{TEST_METHOD}}` | {{STATUS}} |
| NFR-PERF-001 | 启动时间 < 3s | `App.java` | `MainWindowTest#testColdStartupTime` | 已验证（Verified） |

### 7.2 覆盖摘要

| 需求类型 | 总数 | 已实现 | 已测试 | 覆盖率 % |
|-----------------|-------|-------------|--------|------------|
| 功能（FR） | {{FR_TOTAL}} | {{FR_IMPLEMENTED}} | {{FR_TESTED}} | {{FR_COVERAGE}}% |
| 非功能（NFR） | {{NFR_TOTAL}} | {{NFR_IMPLEMENTED}} | {{NFR_TESTED}} | {{NFR_COVERAGE}}% |
| **合计** | **{{TOTAL}}** | **{{IMPLEMENTED}}** | **{{TESTED}}** | **{{COVERAGE}}%** |

### 7.3 未追溯代码

> 无 `@req` 标注的文件。审查技能将其标记为潜在孤儿代码。

| 文件 | 类型 | 备注 |
|------|------|------|
| {{FILE_PATH}} | {{FILE_TYPE}} | {{NOTE}} |

### 7.4 标注约定

- **`@req`**：置于类 Javadoc 头部或方法 Javadoc。格式：`@req FR-001`（单个）或 `@req FR-001, FR-002`（多个）
- **测试方法命名**：`test{Behavior}_{REQ_ID}()` — 例如 `testUserCreation_FR_001()`
- **状态值**：`Planned`（尚未实现）、`Implemented`（代码已编写、未测试）、`Tested`（有通过的测试）、`Verified`（已通过运行时验证）

## 8. 变更历史

| 日期 | 变更 | 原因 |
|------|--------|--------|
| {{DATE}} | 初始创建 | 需求分析（第 1 步） |
