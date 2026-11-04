# JavaFX CI/CD 流水线指南

本指南涵盖 JavaFX 项目的持续集成与持续交付（CI/CD），包括使用 Monocle 进行无头 UI 测试、使用 jpackage 进行跨平台打包、环境搭建、Loop Orchestration Protocol 集成，以及适用于 GitHub Actions 和 GitLab CI 的生产级流水线配置。

---

## 1. 概述

### 1.1 与 JavaFX 技能集的 CI/CD 集成

JavaFX 技能集遵循由 **Loop Orchestration Protocol**（循环编排协议，见 `SKILL.md` → Loop Orchestration Protocol）定义的闭环质量模型。该循环依次经过：

```
generate → review → verify → fix → re-verify
```

CI/CD 流水线在规模上实现了 **verify**（验证）阶段。其中 `runtime verification`（运行时验证）执行本地验证（编译、运行、打包），而 CI 流水线则在不依赖人工值守的情况下，在跨操作系统、JDK 版本和打包目标的多台临时基础设施上执行相同的验证——且结果可复现。

| 技能角色 | 本地产物 | CI 对应物 |
|------------|---------------|---------------|
| `javafx-developer` | 生成的源代码 | 仓库提交 / PR 分支 |
| `javafx-code-reviewer` | 审查报告（静态） | 静态分析作业（SpotBugs、Error Prone） |
| `runtime verification` | 验证报告（编译 + 运行 + 打包） | CI 测试、构建和打包作业 |
| Fix Consumption | `.loop-state.json` 循环状态 | 修复提交后重新触发的 CI 运行 |

### 1.2 流水线目标

1. **编译验证**：在每次推送时捕获语法、模块路径和依赖错误。
2. **无头 UI 测试**：在没有物理显示器的 CI 环境中运行 TestFX 集成测试（Monocle）。
3. **跨平台打包**：通过 jpackage 矩阵构建生成原生安装包（exe/msi、dmg/pkg、deb/rpm）。
4. **产物管理**：将安装包和构建输出作为带版本号的产物上传。
5. **循环集成**：CI 作业对应 generate→review→verify→fix 循环中的 verify 阶段，从而在修复后实现自动重新验证。

---

## 2. 环境搭建要求

### 2.1 JDK

| 要求 | 详情 |
|-------------|---------|
| 最低版本 | JDK 17（JavaFX 21 LTS 基线） |
| 推荐 | JDK 21 LTS（Eclipse Temurin / Microsoft Build of OpenJDK） |
| 最新特性 | JDK 23+（JavaFX 25 LTS）、JDK 24+（JavaFX 26） |
| 安装操作 | `actions/setup-java@v4`（GitHub）、`java` 镜像标签（GitLab） |

JavaFX 24+ 需要 `--enable-native-access=javafx.graphics` JVM 参数。该参数必须在所有测试运行器、构建插件和 jpackage 的 `--java-options` 中传递。

### 2.2 Maven

| 要求 | 详情 |
|-------------|---------|
| 版本 | Maven 3.8+（GitHub Actions 运行器已内置） |
| 缓存键 | 基于 `pom.xml` 哈希的 `~/.m2/repository` |
| 构建命令 | `mvn -B clean package`（`-B` = 批处理模式，无交互提示） |

### 2.3 JavaFX SDK

对于 jpackage 打包，CI 环境需要 JavaFX SDK 或位于模块路径上的模块化 JavaFX JAR。当使用带平台分类器的 Maven 依赖时，编译/测试阶段可跳过 SDK 下载步骤，但建议在 jpackage 时下载，以确保原生库可用。

```bash
# Download JavaFX SDK for packaging
curl -L "https://download2.gluonhq.com/openjfx/21.0.11/openjfx-21.0.11_${PLATFORM}-x64_bin-sdk.zip" -o javafx-sdk.zip
unzip -q javafx-sdk.zip -d javafx-sdk
export JAVAFX_SDK="javafx-sdk/javafx-sdk-21.0.11/lib"
```

| 平台 | 分类器 | SDK URL 后缀 |
|----------|------------|----------------|
| Windows x64 | `win` | `windows-x64_bin-sdk.zip` |
| Linux x64 | `linux` | `linux-x64_bin-sdk.zip` |
| macOS x64 | `mac` | `osx-x64_bin-sdk.zip` |
| macOS aarch64 | `mac-aarch64` | `osx-aarch64_bin-sdk.zip` |

### 2.4 Monocle（无头测试）

Monocle 是 JavaFX glass 窗口工具包的无头实现。它允许 TestFX 集成测试在没有物理显示器或 Xvnc 的 CI 服务器上运行。

| 要求 | 详情 |
|-------------|---------|
| 依赖 | `org.testfx:openjfx-monocle:jdk-21+27`（test 作用域） |
| 必需的系统属性 | 见下表 |
| 仅限 Linux | Monocle 主要用于 Linux CI 运行器 |

**Monocle 系统属性**（作为 Surefire `argLine` 传递）：

| 属性 | 值 | 用途 |
|----------|-------|---------|
| `-Dtestfx.robot=awt` | AWT robot | 使用基于 AWT 的输入注入 |
| `-Dtestfx.headless=true` | true | 启用无头模式 |
| `-Dprism.order=sw` | Software | 使用软件渲染管线 |
| `-Dprism.text=t2k` | T2K | 使用 T2K 文本渲染器（无头模式下更稳定） |
| `--enable-native-access=javafx.graphics` | — | JavaFX 24+ 必需 |

**Maven Surefire 配置**（见 `templates/test/pom-test-dependencies.xml`）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <argLine>
            --enable-native-access=javafx.graphics
            -Dtestfx.robot=awt
            -Dtestfx.headless=true
            -Dprism.order=sw
            -Dprism.text=t2k
        </argLine>
    </configuration>
</plugin>
```

### 2.5 打包所需的平台工具链

jpackage 无法交叉编译。每个平台的原生安装包必须在对应操作系统的运行器上构建。此外还需要以下平台专用工具：

| 平台 | 必需工具 | 安装方式 |
|----------|---------------|--------------|
| Windows | WiX Toolset 4.x（msi）、Inno Setup（exe） | `dotnet tool install --global wix` |
| macOS | Xcode 命令行工具 | `xcode-select --install`（GitHub `macos-latest` 已预装） |
| Linux | `dpkg-deb`（deb）、`rpmbuild`（rpm） | `sudo apt-get install -y dpkg rpm` |

在 CI 上进行 Linux 打包时，还需安装字体包，以避免安装程序 UI 中出现豆腐块（缺失字形）：

```bash
sudo apt-get install -y dpkg rpm libgl1-mesa-glx libxslt1.1
```

---

## 3. 运行时验证到 CI 阶段的映射

`runtime verification`（运行时验证）在本地执行三个验证维度。每个维度都直接对应一个 CI 作业/阶段：

| 运行时验证维度 | 本地验证 | CI 阶段 | CI 作业 |
|------------------------|--------------------|----------|--------|
| 编译验证 | `mvn compile` | `build` | `compile` |
| 运行时验证 | `mvn test`（TestFX + Monocle） | `test` | `test` |
| 打包验证 | `jpackage` 冒烟构建 | `package` | `jpackage` |

### 3.1 CI 质量门映射

Loop Orchestration Protocol 定义了一个组合质量门（审查者 AND 运行者都必须通过）。在 CI 中，这映射为作业依赖：

```
compile (gate: must pass)
   ↓
test (gate: must pass, headless Monocle)
   ↓
package (gate: conditional, required for release)
```

**短路规则**：如果编译失败，则跳过 test 和 package 阶段（与 Loop Orchestration Protocol 中运行者的"编译失败即短路"规则一致）。

### 3.2 CI 结果到循环状态的映射

| CI 结果 | 循环状态等价物 | 下一步操作 |
|-----------|-----------------------|------------|
| 所有作业通过 | `runner_result.conclusion: Pass` | 组合门检查；若审查者也通过 → 交付 |
| 测试作业失败 | `runner_result.conclusion: Fail`（运行时） | 为运行时问题生成修复交接 |
| 打包作业失败 | `runner_result.conclusion: Conditional`（打包） | 为打包问题生成修复交接 |
| 编译失败 | `runner_result.conclusion: Fail`（编译） | 短路：跳过运行时 + 打包验证 |

---

## 4. GitHub Actions 配置

### 4.1 工作流结构

JavaFX 项目的 GitHub Actions 工作流使用 **矩阵策略** 在多个操作系统上运行作业。关键组件如下：

1. **矩阵**：`os: [windows-latest, macos-latest, ubuntu-latest]`
2. **JDK 设置**：`actions/setup-java@v4`，使用 Temurin 发行版
3. **Maven 缓存**：`actions/cache@v4`，基于 `~/.m2/repository`
4. **JavaFX SDK**：为 jpackage 下载（仅打包阶段）
5. **Monocle**：通过 Maven Surefire `argLine` 配置（除 Linux 字体外无需额外的 CI 特定设置）

### 4.2 Linux 上的 Monocle 无头测试

在 Linux CI 运行器上，TestFX 测试通过 Monocle 以无头方式运行。无需 `xvfb` 或显示服务器——Monocle 提供基于软件实现的窗口工具包。

关键配置：
- `-Dtestfx.headless=true` 系统属性启用 Monocle
- `-Dprism.order=sw` 强制软件渲染（CI 上无 GPU）
- `-Dprism.text=t2k` 使用 T2K 文本引擎以获得稳定的无头渲染
- `openjfx-monocle` 依赖必须位于测试 classpath 上

在 Windows 和 macOS CI 运行器上，默认存在显示服务器，因此 Monocle 并非严格必需。不过，为了保持一致性，可在所有平台启用无头模式。

### 4.3 jpackage 跨平台打包矩阵

每个平台会生成不同类型的安装包。矩阵使用 `include` 将操作系统映射到平台专用配置：

| OS | 平台 | 安装包类型 | 图标格式 | 附加标志 |
|----|----------|---------------|-------------|-------------|
| `windows-latest` | `windows` | `msi` | `.ico` | `--win-menu`、`--win-shortcut`、`--win-upgrade-uuid` |
| `macos-latest` | `mac` | `dmg` | `.icns` | `--mac-package-identifier` |
| `ubuntu-latest` | `linux` | `deb` | `.png` | `--linux-deb-maintainer` |

> **交叉编译限制**：jpackage 无法在 Linux 运行器上生成 Windows 安装包。每个平台必须在其对应的操作系统运行器上构建。这正是矩阵策略至关重要的原因。

### 4.4 完整工作流模板

见 `templates/ci/github-actions.yml`，其中包含完整的生产级 GitHub Actions 工作流，包括：
- 多平台矩阵（windows、macos、ubuntu）
- 带 Maven 缓存的 JDK 21 设置
- 编译验证步骤
- 使用 Monocle 无头模式的测试验证
- 使用平台专用安装包类型的 jpackage 打包
- 产物上传

---

## 5. GitLab CI 配置

### 5.1 流水线结构

GitLab CI 使用 `stages` 数组定义作业执行顺序。对于 JavaFX 项目：

```yaml
stages:
  - compile
  - test
  - package
```

### 5.2 运行器标签

GitLab CI 使用 **带标签的运行器** 将作业派发到特定的操作系统环境：

| 阶段 | 运行器标签 | 镜像 / Shell |
|-------|-----------|---------------|
| compile | `linux` | `maven:3.9-eclipse-temurin-21` |
| test | `linux` | `maven:3.9-eclipse-temurin-21` |
| package（windows） | `windows` | Windows shell 运行器 |
| package（mac） | `macos` | macOS shell 运行器 |
| package（linux） | `linux` | `maven:3.9-eclipse-temurin-21` |

### 5.3 完整配置模板

见 `templates/ci/gitlab-ci.yml`，其中包含完整的 GitLab CI 模板，包括：
- 编译阶段（Maven）
- 测试阶段（TestFX + Monocle 无头）
- 带平台专用作业的打包阶段（Windows、macOS、Linux）
- 产物发布

---

## 6. Loop Orchestration Protocol 集成

### 6.1 将 CI 作为验证阶段

Loop Orchestration Protocol 定义了循环：**generate → review → verify → fix → re-verify**。CI 流水线充当自动化的 **verify**（验证）阶段。

```
[Developer generates code] → [Reviewer reviews] → [CI pipeline verifies]
                                                        ↓ Pass
                                                    [Deliver]
                                                        ↓ Fail
                                                    [Developer fixes]
                                                        ↓
                                                    [CI re-verifies on new push]
```

### 6.2 通过 CI 触发循环

CI 流水线通过两种方式触发循环：

1. **PR 验证（generate → verify）**：当开发者推送代码（即"generate"阶段的产物）时，CI 流水线运行验证。如果测试失败，循环进入"fix"阶段——开发者消费修复交接报告、应用修复并再次推送，从而触发新的 CI 运行（re-verify）。

2. **夜间完整验证（re-verify）**：定时运行的夜间流水线执行完整的验证套件（编译 + 所有测试 + 打包），以捕获可能漏过 PR 级检查的回归问题。

### 6.3 CI 到循环状态的桥接

CI 结果可映射到 Loop Orchestration Protocol 中定义的 `.loop-state.json` 结构：

```json
{
  "loop_id": "uuid-v4",
  "project": "javafx-app",
  "current_round": 2,
  "rounds": [
    {
      "round": 1,
      "phase": "verify",
      "runner_result": {
        "conclusion": "Fail",
        "compile": "pass",
        "runtime": "fail",
        "packaging": "skipped"
      },
      "ci_run_id": "github-actions-run-12345",
      "ci_run_url": "https://github.com/owner/repo/actions/runs/12345"
    }
  ],
  "convergence_trend": [3, 1],
  "next_action": "fix"
}
```

### 6.4 CI 中的收敛检测

Loop Orchestration Protocol 定义了收敛检测："将当前轮次的问题数量与上一轮次比较。"在 CI 中，这转化为：

- **第 N 轮问题数** = CI 运行 N 中失败的测试数 / 打包错误数
- **收敛中（Converging）** = 第 N 轮问题数 < 第 N-1 轮问题数
- **未收敛（Non-converging）** = 连续 2 轮问题数未减少 → 暂停循环，向用户报告

CI 流水线通过生成可跨运行比较的结构化测试报告（JUnit XML）来支持这一点：

```bash
# Generate JUnit XML report (Maven Surefire does this by default)
mvn -B test --no-transfer-progress
# Reports are in target/surefire-reports/*.xml
```

### 6.5 最大轮次强制

协议将 fix→verify 循环限制为 3 轮。在 CI 驱动的循环中，可通过以下方式强制执行：

1. 在 `.loop-state.json` 中跟踪轮次数（提交或作为 CI 产物存储）
2. 使用 CI 作业条件：`if: ${{ round < 3 }}`（GitHub）或 `rules: if: $ROUND < 3`（GitLab）
3. 若达到最大轮次仍未通过 → CI 报告失败，循环暂停以等待用户干预

---

## 7. 示例工作流

### 7.1 PR 验证工作流

**目的**：对每个拉取请求提供快速反馈。仅在 Linux 上运行编译 + 测试（无头）。不打包（对 PR 迭代来说太慢）。

**触发**：针对 `main` 或 `develop` 分支的 `pull_request` 事件。

**作业**：
1. `compile` — 在 Ubuntu 上、JDK 21 下执行 `mvn -B compile`
2. `test` — 使用 Monocle 无头模式执行 `mvn -B test`（依赖 compile）

**典型耗时**：3-5 分钟（含 Maven 缓存）。

```yaml
# Simplified PR validation (see full template for details)
name: PR Validation
on:
  pull_request:
    branches: [main, develop]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B test
```

**循环角色**：这是初始"generate"之后的"verify"步骤。如果失败，开发者进入"fix"模式并再次推送。

### 7.2 发布打包工作流

**目的**：为发布构建各平台原生安装包。由 Git 标签（`v*`）触发。

**触发**：推送匹配 `v*` 的标签，或 `workflow_dispatch`（手动）。

**作业**：
1. `package` — 跨 `windows-latest`、`macos-latest`、`ubuntu-latest` 的矩阵构建
2. 每个作业：编译 → 创建 jlink 运行时 → jpackage → 上传产物
3. 可选：创建带上传安装包的 GitHub Release

**典型耗时**：每平台 10-20 分钟（并行）。

```yaml
# Simplified release packaging (see full template for details)
name: Release
on:
  push:
    tags: ['v*']
jobs:
  package:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B clean package -DskipTests
      # ... jpackage steps per platform ...
      - uses: actions/upload-artifact@v4
        with:
          name: installer-${{ matrix.os }}
          path: dist/*
```

**循环角色**：这在循环通过（审查者 AND 运行者均 Pass）之后运行。它属于交付阶段。

### 7.3 夜间完整验证工作流

**目的**：捕获回归的全面验证。运行完整套件：编译 + 所有测试 + 打包冒烟测试，涵盖所有平台和 JDK 版本。

**触发**：带 cron 表达式的 `schedule`（例如 `0 2 * * *` = 每天 UTC 02:00）。

**作业**：
1. `compile-matrix` — 在全部 3 个 OS × 2 个 JDK 版本（21、23）上编译
2. `test-matrix` — 在所有平台上使用 Monocle 运行完整测试套件
3. `package-smoke` — 在所有平台上执行 jpackage 构建（非发布，仅验证打包是否正常）
4. `report` — 汇总结果，若失败则发布到 Slack/邮件

**典型耗时**：20-40 分钟（并行矩阵）。

```yaml
# Simplified nightly verification (see full template for details)
name: Nightly
on:
  schedule:
    - cron: '0 2 * * *'
  workflow_dispatch:
jobs:
  verify:
    strategy:
      matrix:
        os: [windows-latest, macos-latest, ubuntu-latest]
        jdk: ['21', '23']
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.jdk }}
          cache: maven
      - run: mvn -B clean verify
      # ... jpackage smoke test ...
```

**循环角色**：这是夜间循环的"re-verify"步骤。如果检测到回归（例如某个之前通过的测试现在失败），则提示循环重新进入"fix"阶段。

---

## 8. JavaFX 的 CI/CD 最佳实践

### 8.1 缓存

| 缓存目标 | 键 | 路径 | 用途 |
|-------------|-----|------|---------|
| Maven 仓库 | `{{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}` | `~/.m2/repository` | 避免重复下载依赖 |
| Gradle | `{{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}` | `~/.gradle/caches` | Gradle 项目同理 |
| JavaFX SDK | `javafx-sdk-21.0.11-${{ matrix.platform }}` | `javafx-sdk/` | 避免为打包重复下载 SDK |

### 8.2 版本一致性

- 从 Git 标签（`${{ github.ref_name }}`）提取应用版本，并传给 `jpackage --app-version`
- 确保 `pom.xml` 的 `<version>`、`module-info.java` 和 `jpackage --app-version` 使用相同的版本字符串
- 对于 SNAPSHOT 构建，附加 Git 短 SHA：`1.0.0-${{ github.sha }}`

### 8.3 平台相关注意事项

| 平台 | CI 坑点 | 解决方案 |
|----------|-----------|----------|
| Windows | WiX 不在 PATH 中 | 将 `$HOME/.dotnet/tools` 添加到 `GITHUB_PATH` |
| macOS | 签名需要证书 | 存储为 GitHub Secrets / GitLab CI Variables |
| Linux | 缺少字体（豆腐块） | `apt-get install fonts-dejavu fonts-noto-cjk` |
| Linux | 缺少 OpenGL 库 | `apt-get install libgl1-mesa-glx` |
| 所有 | JavaFX 24+ 原生访问 | 处处传递 `--enable-native-access=javafx.graphics` |

### 8.4 产物命名

使用包含平台和版本的一致产物名称：

```
MyApp-1.0.0-windows.msi
MyApp-1.0.0-macos.dmg
MyApp-1.0.0-linux.deb
```

### 8.5 流水线安全

1. **密钥管理**：将签名证书、API 令牌和密码存储在 GitHub Secrets 或 GitLab CI/CD Variables 中
2. **依赖扫描**：添加 `dependency-check` 或 Dependabot 进行漏洞扫描
3. **代码签名**：使用 `signtool` 签名 Windows 安装包，使用 `codesign` + 公证签名 macOS 安装包
4. **最小权限**：在 GitHub Actions 中默认使用 `permissions: contents: read`

---

## 9. 模板文件参考

| 模板 | 路径 | 描述 |
|----------|------|-------------|
| GitHub Actions 工作流 | `templates/ci/github-actions.yml` | JavaFX 的多平台 CI/CD 流水线 |
| GitLab CI 配置 | `templates/ci/gitlab-ci.yml` | 带标签运行器的 GitLab 等价物 |

### 9.1 使用模板

1. **复制**：将模板文件复制到项目的 `.github/workflows/`（GitHub）或仓库根目录（GitLab）
2. **定制**：将占位符值（`MY_APP`、`com.example.myapp`、版本字符串）替换为项目的实际值
3. **配置密钥**：将签名证书和 API 令牌添加为仓库密钥
4. **测试**：在启用自动触发之前，手动触发工作流（`workflow_dispatch`）

### 9.2 占位符参考

模板使用了以下必须替换的占位符：

| 占位符 | 含义 | 示例值 |
|-------------|---------|---------------|
| `MY_APP` | 应用显示名称 | `MyApp` |
| `myapp` | 应用小写名称（Linux 包名） | `myapp` |
| `com.example.myapp` | Java 模块名 | `com.example.myapp` |
| `com.example.myapp.MainApp` | 主类全限定名 | `com.example.myapp.MainApp` |
| `1.0.0` | 应用版本 | `1.0.0` |
| `com.mycompany.myapp` | macOS 包标识符 | `com.mycompany.myapp` |
| `dev@mycompany.com` | Linux deb 维护者邮箱 | `dev@mycompany.com` |
| `12345678-1234-1234-1234-123456789abc` | Windows 升级 UUID | 生成一个新的 UUID |

---

## 10. CI 问题排查

### 问题 1：TestFX 测试在 Linux CI 上因 "HeadlessException" 失败

**原因**：Monocle 不在测试 classpath 上，或未设置无头属性。

**解决方案**：确保存在 `org.testfx:openjfx-monocle` 依赖，且 Surefire `argLine` 包含 `-Dtestfx.headless=true -Dprism.order=sw`。

### 问题 2：jpackage 因 "jlink not found" 或 "no runtime image" 失败

**原因**：jpackage 需要运行时镜像（通过 jlink）或带 JavaFX SDK 的模块路径。

**解决方案**：先运行 `jlink` 并传入 `--runtime-image`，或通过带 JavaFX SDK lib 目录的 `--module-path` 提供。

### 问题 3：jpackage 在 Windows 上因 "wix not found" 失败

**原因**：WiX Toolset 4.x 未安装或不在 PATH 中。

**解决方案**：通过 `dotnet tool install --global wix` 安装，并将 `$HOME/.dotnet/tools` 添加到 PATH。WiX 3.x 与现代 jpackage 不兼容。

### 问题 4：macOS 构建因 "code signing failed" 失败

**原因**：CI 运行器上未配置签名证书或钥匙串。

**解决方案**：从密钥中将签名证书导入 macOS 钥匙串，或跳过夜间构建的签名、仅对发布版本签名。

### 问题 5：本地测试通过但 CI 上失败（时序问题）

**原因**：TestFX 测试可能对时序敏感；CI 运行器通常较慢。

**解决方案**：在测试中使用 `Thread.sleep()` 或 `Awaitility` 添加显式等待。通过 `-Dtestfx.timeout=10000`（10 秒）增加 TestFX 超时。

### 问题 6：JavaFX 24+ 测试因 "IllegalCallerException" 失败

**原因**：测试 JVM 参数中缺少 `--enable-native-access=javafx.graphics` 标志。

**解决方案**：在 `pom.xml` 的 Surefire `argLine` 中添加该标志。完整配置见 `templates/test/pom-test-dependencies.xml`。

### 问题 7：GitHub Actions 中 Maven 缓存不生效

**原因**：缓存键不匹配，或缓存已被清除。

**解决方案**：使用带 `cache: maven` 参数的 `actions/setup-java`（比手动 `actions/cache` 更简单）。确保在缓存步骤运行前已提交 `pom.xml`。

### 问题 8：GitLab CI 打包作业卡在 "pending" 状态

**原因**：没有注册带所需标签（`windows`、`macos`）的运行器。

**解决方案**：为 Windows 和 macOS 注册自托管运行器，或使用带相应标签的 GitLab SaaS 运行器。Linux 打包可使用共享 Docker 运行器。
