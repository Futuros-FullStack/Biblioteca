# 静态分析工具集成参考

> 在 JavaFX + Spring Boot 项目中集成 SpotBugs、PMD 和 Checkstyle 的指南。涵盖 Maven 插件配置、针对 JavaFX 模式定制的规则集、报告解析，以及 `runtime verification` 如何执行工具、`javafx-code-reviewer` 如何消费发现。

## 概述

三个互补的静态分析工具提供确定性的代码质量覆盖：

| 工具 | 关注点 | 能捕获的问题 | 报告位置 |
|------|-------|-----------------|-----------------|
| **SpotBugs** | Bug 模式 | 空指针风险、资源泄漏、并发问题、错误类型转换 | `target/spotbugsXml.xml` |
| **PMD** | 代码质量 | 复制粘贴、复杂度、未使用代码、设计问题 | `target/pmd.xml` |
| **Checkstyle** | 代码风格 | 命名、导入、空白、方法长度、圈复杂度 | `target/checkstyle-result.xml` |

### 为什么需要全部三个？

每个工具覆盖不同的类别——它们互为补充，而非冗余：

- SpotBugs 使用字节码分析 → 捕获 PMD/Checkstyle 会漏掉的运行时风险（NPE、泄漏）
- PMD 使用源代码分析 → 捕获 SpotBugs 会漏掉的设计问题（复制粘贴、复杂度）
- Checkstyle 强制约定 → 捕获影响可维护性但不影响正确性的风格违规

## Maven 插件配置

`pom.xml` 模板（见 `templates/maven/pom.xml`）预配置了全部三个插件。关键配置点：

### 版本属性

```xml
<properties>
    <spotbugs.version>4.8.6.6</spotbugs.version>
    <spotbugs.maven.plugin.version>4.8.6.6</spotbugs.maven.plugin.version>
    <pmd.version>7.7.0</pmd.version>
    <checkstyle.version>10.18.1</checkstyle.version>
</properties>
```

### 执行命令

```bash
# Run all three tools (generates XML + HTML reports)
mvn spotbugs:check pmd:check checkstyle:check

# Run individually
mvn spotbugs:check       # → target/spotbugsXml.xml, target/spotbugs.html
mvn pmd:check            # → target/pmd.xml
mvn checkstyle:check     # → target/checkstyle-result.xml

# Run with build (compile first, then analyze)
mvn compile spotbugs:check pmd:check checkstyle:check
```

> **注意**：所有插件都配置为 `failOnError=false` / `failsOnError=false`——它们会生成报告但**不会**让 Maven 构建失败。这使得 `runtime verification` 能够解析报告并独立分类严重度。

## 规则集配置

### Checkstyle（`checkstyle.xml`）

位于项目根目录（`${project.basedir}/checkstyle.xml`）。针对 JavaFX 定制：

| 规则类别 | 关键规则 | JavaFX 考量 |
|---------------|-----------|---------------------|
| 命名 | `ConstantName`、`MemberName`、`MethodName` | 标准约定 |
| 导入 | `AvoidStarImport`、`UnusedImports`、`ImportOrder` | 分组：java、javax、jakarta、org、com |
| 代码结构 | `MethodLength(max=80)`、`ParameterNumber(max=7)` | — |
| 复杂度 | `CyclomaticComplexity(max=15)` | 与 NFR-MAINT-002 对齐 |
| 编码 | `EmptyBlock`、`EqualsHashCode`、`NestedIfDepth(max=3)` | — |
| 设计 | `FinalClass`、`HideUtilityClassConstructor` | — |

**JavaFX 特例**：FXML Controller 在包私有字段上使用 `@FXML`——Checkstyle 配置了 `SuppressionCommentFilter`，且 `VisibilityModifier` 允许 `packageAllowed=true` 和 `protectedAllowed=true`。

### PMD（`pmd-ruleset.xml`）

位于项目根目录（`${project.basedir}/pmd-ruleset.xml`）。使用 PMD 7 基于类别的规则：

| 类别 | 关键规则 | JavaFX 考量 |
|----------|-----------|---------------------|
| 最佳实践 | `UnusedPrivateField`（对 `@FXML` 排除）、`UnusedPrivateMethod`（对 FXML Controller 排除） | 对 `@FXML` 注解字段使用 XPath 抑制 |
| 代码风格 | `ClassNamingConventions`，后缀为 `Controller\|Service\|Repository\|View\|Model\|ViewModel\|Presenter\|Util\|Exception` | 强制命名约定 |
| 设计 | `CognitiveComplexity(reportLevel=20)`、`TooManyMethods(max=20)`、`TooManyFields(max=25)` | — |
| 易错 | `BeanMembersShouldSerialize`（排除——JavaFX Properties 不是标准 Bean） | — |
| 性能 | 完整类别 | 捕获低效的字符串操作、不必要的对象创建 |
| 安全 | 完整类别 | 捕获硬编码密码、不安全的随机数 |

**JavaFX 特例抑制**：PMD 的 `UnusedPrivateField` 和 `UnusedPrivateMethod` 配置了对 `@FXML` 注解字段的 XPath 抑制——这些字段/方法是由 `FXMLLoader` 通过反射注入/调用的，而非直接代码。

### SpotBugs（`spotbugs-exclude.xml`）

位于项目根目录（`${project.basedir}/spotbugs-exclude.xml`）。排除常见的 JavaFX 误报：

| 排除的模式 | 原因 |
|-----------------|--------|
| `@FXML` 类上的 `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` | FXML 注入的字段不在构造函数中设置 |
| `*Property` 类型上的 `UWF_NULL_FIELD` | JavaFX Properties 使用延迟初始化模式（首次访问前为 null） |
| `@FXML` 方法上的 `UPM_UNCALLED_PUBLIC_METHOD` | FXML 事件处理器由 FXMLLoader 通过反射调用 |
| `*Application` 类上的 `UPM_UNCALLED_PUBLIC_METHOD` | JavaFX 生命周期方法（`start()`、`init()`、`stop()`）可能看起来未被使用 |
| `*Test` 类上的 `EI_EXPOSE_REP` | 测试类可能为断言暴露内部状态 |
| 对 `generated`/`auto` 包的所有检查 | 生成代码不是手动维护的 |

## 报告解析

`runtime verification` 解析每个工具生成的 XML 报告。统一的解析方式：

### SpotBugs 报告（`target/spotbugsXml.xml`）

```xml
<SpotBugs>
  <BugInstance type="NP_NULL_ON_SOME_PATH" priority="2" category="CORRECTNESS">
    <Class classname="com.example.UserService"/>
    <Method classname="com.example.UserService" name="findById" signature="(J)Lcom/example/User;"/>
    <SourceLine classname="com.example.UserService" start="45" end="45"/>
  </BugInstance>
</SpotBugs>
```

**解析字段**：
- `type` → 规则 ID（例如 `NP_NULL_ON_SOME_PATH`）
- `priority` → 1=高，2=中，3=低
- `category` → CORRECTNESS、PERFORMANCE、SECURITY 等
- `SourceLine.start` → 行号
- `Class.classname` + `Method.name` → AST 签名 `{package}.{Class}#{method}({params})`

### PMD 报告（`target/pmd.xml`）

```xml
<pmd>
  <file name="src/main/java/com/example/UserService.java">
    <violation beginline="45" endline="45" rule="AvoidInstantiatingObjectsInLoops"
               ruleset="Performance" priority="3">
      Avoid instantiating objects in loops
    </violation>
  </file>
</pmd>
```

**解析字段**：
- `rule` → 规则 ID
- `ruleset` → 类别（Performance、Design 等）
- `priority` → 1=高，2=中高，3=中，4=中低，5=低
- `beginline` → 行号
- `file.name` → 源文件路径

### Checkstyle 报告（`target/checkstyle-result.xml`）

```xml
<checkstyle>
  <file name="src/main/java/com/example/UserService.java">
    <error line="45" column="12" severity="warning"
           message="Method length is 85 lines (max allowed is 80)."
           source="com.puppycrawl.tools.checkstyle.checks.sizes.MethodLengthCheck"/>
  </file>
</checkstyle>
```

**解析字段**：
- `source` → 规则 ID（Checkstyle 检查类名）
- `severity` → `error` 或 `warning`
- `line`、`column` → 位置
- `file.name` → 源文件路径

### 统一问题映射

所有三个工具的发现都映射到统一的问题结构，供 `javafx-code-reviewer` 消费：

```json
{
  "tool": "spotbugs | pmd | checkstyle",
  "rule_id": "NP_NULL_ON_SOME_PATH | AvoidInstantiatingObjectsInLoops | MethodLengthCheck",
  "severity": "Critical | Major | Minor | Info",
  "category": "CORRECTNESS | PERFORMANCE | STYLE",
  "source_file": "src/main/java/com/example/UserService.java",
  "line_number": 45,
  "ast_node_signature": "com.example.UserService#findById(Long)",
  "message": "Possible null pointer dereference of user",
  "fix_suggestion": "Add null check before accessing user fields"
}
```

**优先级到严重度的映射**：

| 工具 | 优先级 | 映射的严重度 |
|------|----------|-----------------|
| SpotBugs | 1（高） | 重要 |
| SpotBugs | 2（中） | 次要 |
| SpotBugs | 3（低） | 提示 |
| PMD | 1-2（高） | 重要 |
| PMD | 3（中） | 次要 |
| PMD | 4-5（低） | 提示 |
| Checkstyle | error | 次要 |
| Checkstyle | warning | 提示 |

> **注意**：工具发现永远不会映射到"严重"——"严重"保留给运行者动态验证发现的编译错误和运行时崩溃。工具发现始终是确定性的代码质量问题。

## 运行者集成

`runtime verification` 作为其验证工作流的一部分执行静态分析。执行协议见第 2.5 步（静态分析验证）。

### 执行顺序

```
Step 2: Compile Verification (mvn compile)
    ↓ (if compile passes)
Step 2.5: Static Analysis Verification (mvn spotbugs:check pmd:check checkstyle:check)
    ↓
Step 3: Runtime Verification (mvn javafx:run)
```

静态分析**在编译之后**运行（SpotBugs 需要编译后的字节码），**在运行之前**运行（在启动应用前捕获问题）。

### 短路

- 如果编译验证失败 → 跳过静态分析（没有字节码可分析）
- 静态分析发现**不会**短路运行时验证——它们被记录为问题，但不阻止运行时测试

### 报告集成

静态分析发现作为单独维度（"静态分析验证"）包含在运行者的验证报告中，与其他维度使用相同的 Fix Handoff 格式。

## 审查者集成

`javafx-code-reviewer` 将统一的静态分析发现作为第 10 维度（静态分析工具发现）消费。审查者：

1. 读取运行者生成的 `static-analysis-findings.json`（如果存在）
2. 对每个工具发现，检查审查者自己的 LLM 审查是否已经识别出相同问题（去重）
3. LLM 审查未发现的工具发现，以补充问题形式添加，`source: "spotbugs" | "pmd" | "checkstyle"`
4. 已被 LLM 审查发现的工具发现，标注为"已由静态分析工具确认"以获得更高置信度
5. 审查者还可以使用工具发现来验证自己的结论（例如，如果 SpotBugs 报告 `NP_NULL_ON_SOME_PATH`，则审查者的 NPE 风险发现得到佐证）

### 去重逻辑

| 场景 | 操作 |
|----------|--------|
| LLM 发现问题 + 工具发现相同问题 | 保留 LLM 发现，标注"已由 {tool} 确认" |
| LLM 发现问题 + 工具未发现 | 保留 LLM 发现（LLM 能发现工具漏掉的语义问题） |
| LLM 未发现问题 + 工具发现问题 | 将工具发现作为补充问题添加，`source: {tool}` |

## Gradle 配置

对于 Gradle 项目，等价插件：

```groovy
plugins {
    id 'com.github.spotbugs' version '6.0.22'
    id 'pmd'
    id 'checkstyle'
}

spotbugs {
    effort = 'max'
    reportLevel = 'low'
    excludeFilter = file('spotbugs-exclude.xml')
}

pmd {
    toolVersion = '7.7.0'
    rulesFiles = files('pmd-ruleset.xml')
    consoleOutput = false
}

checkstyle {
    toolVersion = '10.18.1'
    configFile = file('checkstyle.xml')
}

// Generate XML reports
spotbugsMain {
    reports {
        xml.required = true
        html.required = true
    }
}
```

## 常见陷阱

1. **工具导致构建失败**：所有插件都配置为 `failOnError=false`——如果工具发现问题，构建仍会成功，使运行者能够独立解析报告。如果你启用 `failOnError=true`，任何发现都会导致构建失败，从而阻塞运行者执行
2. **@FXML 字段的误报**：如果没有 PMD 中的 XPath 抑制和 SpotBugs 中的排除过滤器，`@FXML` 注入的字段会被标记为"未使用"——始终包含 JavaFX 特例排除
3. **缺少配置文件**：如果 `checkstyle.xml`、`pmd-ruleset.xml` 或 `spotbugs-exclude.xml` 缺失，插件会使用默认规则（可能过严或过宽）。开发者模板包含全部三个配置文件
4. **SpotBugs 需要已编译的类**：SpotBugs 分析字节码而非源码——必须在 `mvn compile` 之后运行。如果未先编译就运行 `mvn spotbugs:check`，会得到空报告
5. **PMD 版本不匹配**：PMD 7.x 使用基于类别的规则集（`category/java/bestpractices.xml`），而 PMD 6.x 使用规则集文件。确保 `pmd-ruleset.xml` 与插件中的 PMD 版本匹配
6. **Checkstyle 抑制文件可选**：`checkstyle.xml` 引用 `checkstyle-suppressions.xml`，`optional=true`——如果文件不存在，Checkstyle 在无文件级抑制的情况下运行
