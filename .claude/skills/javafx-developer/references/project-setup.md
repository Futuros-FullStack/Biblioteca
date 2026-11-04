# JavaFX 项目搭建指南

本指南涵盖 JavaFX 项目的完整搭建流程，包括版本选择、构建工具配置（Maven / Gradle）、模块化配置、跨平台打包以及自定义运行时镜像（jlink）的创建。

---

## 1. JavaFX 版本路线图

JavaFX 自 OpenJFX 项目独立维护以来，采用六个月一版的发布节奏（与 JDK LTS 周期对齐）。下表列出主要版本的发布日期、LTS 状态和关键特性。

| JavaFX 版本 | 发布日期   | 对应 JDK | LTS 状态 | 关键特性 / 说明                                                |
|----------------|-----------------|--------------------|------------|---------------------------------------------------------------------|
| 17             | 2021-09-14      | JDK 17             | LTS（至 2026 年 10 月） | 长期支持版本，稳定成熟；第三方库兼容性最佳，适合遗留系统维护。 |
| 21             | 2023-09-19      | JDK 17             | LTS        | LTS 版本；新增基于订阅的监听器 API，性能与稳定性提升。 |
| 24             | 2025-03-18      | JDK 22             | 非 LTS    | 引入访问原生渲染层需 `--enable-native-access` 的要求；多显示器改进。 |
| 25             | 2025-09-16      | JDK 23             | LTS        | 最新 LTS；继续完善 24 的特性并增强稳定性。当前最新补丁为 25.0.3。 |
| 26             | 2026-03-17      | JDK 24             | 非 LTS    | 短期支持版本，紧跟最新 JDK 特性。当前最新补丁为 26.0.1。        |

### 版本选择建议

- **新项目（推荐）**：**JavaFX 25 LTS**（最新 LTS，JDK 23+）——享受最新特性与长期支持。
- **生产 / 保守选择**：**JavaFX 21 LTS**（成熟 LTS，JDK 17+）——生态支持最好。
- **遗留系统维护**：**JavaFX 17 LTS**（至 2026 年 10 月）——适合不便升级的项目。
- **尝鲜最新特性**：**JavaFX 26**（JDK 24+），但需注意 `--enable-native-access` 标志要求。
- **长期维护**：遵循 JDK LTS 节奏，即 JavaFX 17 -> 21 -> 25。

---

## 2. Maven 配置

### 2.1 javafx-maven-plugin 0.0.8

`javafx-maven-plugin` 由 Gluon 维护，简化了 JavaFX 应用的运行和调试。当前推荐版本为 **0.0.8**。

核心目标（goals）：

- `javafx:run`：运行 JavaFX 应用。
- `javafx:debug`：以调试模式运行，支持远程调试。
- `javafx:jlink`：通过 jlink 创建自定义运行时镜像并打包。

### 2.2 完整 pom.xml 示例

以下是一份基于 JavaFX 21 的完整 Maven 项目配置，支持跨平台运行：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>javafx-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <javafx.version>21.0.11</javafx.version>
        <main.class>com.example.javafxapp.MainApp</main.class>
    </properties>

    <dependencies>
        <!-- JavaFX base modules (include as needed) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
            <version>${javafx.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-media</artifactId>
            <version>${javafx.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- JavaFX Maven Plugin -->
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                    <!-- JavaFX 24+ requires native access to be enabled -->
                    <options>
                        <option>--enable-native-access=javafx.graphics</option>
                    </options>
                </configuration>
            </plugin>

            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <release>21</release>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.3 运行命令

```bash
# Run the application
mvn javafx:run

# Debug mode
mvn javafx:debug

# Create a jlink runtime image
mvn javafx:jlink
```

---

## 3. Gradle 配置

### 3.1 org.openjfx.javafxplugin 0.1.0

Gradle 用户推荐使用官方 `org.openjfx.javafxplugin` 插件，当前版本为 **0.1.0**。该插件自动处理跨平台依赖分类器，无需手动指定平台后缀。

### 3.2 完整 build.gradle 示例

```groovy
plugins {
    id 'java'
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'com.example'
version = '1.0.0'

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.11"
    modules = ['javafx.controls', 'javafx.fxml', 'javafx.graphics', 'javafx.web', 'javafx.media']
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = 'com.example.javafxapp.MainApp'
    // JavaFX 24+ requires native access to be enabled
    applicationDefaultJvmArgs = ['--enable-native-access=javafx.graphics']
}

tasks.named('run') {
    // Ensure JVM args are passed at runtime
    jvmArgs += ['--enable-native-access=javafx.graphics']
}
```

### 3.3 Kotlin DSL（build.gradle.kts）示例

```kotlin
plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.11"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.example.javafxapp.MainApp"
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}
```

### 3.4 运行命令

```bash
# Run the application
gradle run

# Build executable JAR
gradle build

# Create a jlink runtime image
gradle jlink
```

---

## 4. module-info.java 配置

JavaFX 11+ 推荐使用 Java 模块系统（JPMS）。在 `src/main/java/module-info.java` 中声明模块依赖和导出。

### 4.1 基本结构

```java
module com.example.javafxapp {
    // Declare required JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    // Export packages containing public classes
    exports com.example.javafxapp;
    exports com.example.javafxapp.controller;
    exports com.example.javafxapp.model;

    // Open packages for javafx.fxml to load Controllers via reflection
    opens com.example.javafxapp.controller to javafx.fxml;
    opens com.example.javafxapp.view to javafx.fxml;

    // If using a dependency injection framework (e.g., Guice), open the relevant packages
    opens com.example.javafxapp to com.google.inject;
}
```

### 4.2 requires / exports / opens 关键字参考

| 关键字              | 用途                                                                 |
|----------------------|-------------------------------------------------------------------------|
| `requires`           | 声明本模块依赖的其他模块。加上 `transitive` 表示传递依赖。 |
| `requires transitive` | 依赖会传递给依赖本模块的模块。        |
| `exports`            | 将指定包的公共 API 对外暴露（编译期与运行期均可见）。 |
| `opens`              | 在运行期开放指定包的反射访问（深度反射），但编译期不暴露。 |
| `opens ... to`       | 仅向指定模块开放反射访问。                |
| `uses` / `provides`  | 服务加载机制（ServiceLoader）相关声明。          |

### 4.3 关键说明

1. **FXML 加载需要 `opens`**：`FXMLLoader` 通过反射实例化 Controller 类，因此必须使用 `opens` 而非 `exports` 来开放包含 Controller 的包。
2. **不要将包含 FXML Controller 的包 `exports` 给 `javafx.fxml`**；改用 `opens ... to javafx.fxml`。
3. **资源文件**：`module-info.java` 必须位于 `src/main/java` 根目录，资源文件放在 `src/main/resources`。

---

## 5. JDK 24+ 的 --enable-native-access 要求

从 JavaFX 24 开始，JavaFX 的图形渲染层（`javafx.graphics` 模块）通过 JNI 访问原生代码。在 JDK 24+ 严格的模块封装机制下，必须显式授予原生访问权限，否则应用将启动失败并抛出 `IllegalCallerException`。

### 5.1 如何添加

**命令行：**

```bash
java --enable-native-access=javafx.graphics -m com.example.javafxapp/com.example.javafxapp.MainApp
```

**Maven（javafx-maven-plugin）：**

```xml
<configuration>
    <options>
        <option>--enable-native-access=javafx.graphics</option>
    </options>
</configuration>
```

**Gradle：**

```groovy
application {
    applicationDefaultJvmArgs = ['--enable-native-access=javafx.graphics']
}
```

### 5.2 多模块场景

如果同时使用其他需要原生访问的库（例如 JNI 库），可以使用 `ALL-UNNAMED` 或列出多个模块名：

```bash
java --enable-native-access=javafx.graphics,com.example.nativeLib -m ...
```

> 注意：使用 `ALL-UNNAMED` 会降低安全性，仅建议在开发和调试阶段使用。

---

## 6. 跨平台分类器

JavaFX 的 `javafx.graphics` 模块包含平台相关的原生二进制库，因此 Maven 依赖需要通过分类器区分平台。

### 6.1 平台分类器参考

| 平台             | 分类器          | 说明                                                        |
|----------------------|---------------------|--------------------------------------------------------------|
| Windows x64          | `win`               | 自动选择，对应 `javafx.graphics` 的 Windows 原生库 |
| Linux x64            | `linux`             | Linux 平台原生库                                |
| macOS x64（Intel）    | `mac`               | macOS Intel 平台                                         |
| macOS aarch64（Apple Silicon） | `mac-aarch64` | macOS M1/M2/M3 芯片                                         |
| Linux aarch64        | `linux-aarch64`     | Linux ARM64 平台                                         |
| Linux arm32          | `linux-arm32-monocle` | 嵌入式 ARM 平台（Monocle）                            |

### 6.2 在 Maven 中手动指定分类器

默认情况下，`javafx-maven-plugin` 和 Maven 会根据当前构建机器自动选择分类器。如果需要为特定平台打包，可以手动指定：

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
    <version>21.0.11</version>
    <classifier>linux</classifier>
</dependency>
```

### 6.3 Maven 跨平台 Profile 配置

```xml
<profiles>
    <profile>
        <id>windows</id>
        <activation>
            <os><family>windows</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-graphics</artifactId>
                <version>${javafx.version}</version>
                <classifier>win</classifier>
            </dependency>
        </dependencies>
    </profile>
    <profile>
        <id>linux</id>
        <activation>
            <os><family>linux</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-graphics</artifactId>
                <version>${javafx.version}</version>
                <classifier>linux</classifier>
            </dependency>
        </dependencies>
    </profile>
    <profile>
        <id>mac</id>
        <activation>
            <os><family>mac</family></os>
        </activation>
        <dependencies>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-graphics</artifactId>
                <version>${javafx.version}</version>
                <classifier>mac</classifier>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

> 提示：使用 Gradle `org.openjfx.javafxplugin` 插件时，分类器由插件自动处理，无需手动配置。

---

## 7. jlink 自定义运行时镜像

`jlink` 是 JDK 内置工具，可将模块化 Java 应用及其依赖打包成精简的自定义 JRE 镜像，目标机器无需预装 JDK。

### 7.1 前提条件

- 项目必须使用 Java 模块系统（即包含 `module-info.java`）。
- 所有依赖必须是模块化 JAR（或提供 `Automatic-Module-Name`）。
- JavaFX 的模块化 JAR 满足此要求。

### 7.2 使用 jlink 命令

```bash
jlink \
  --module-path "mods:libs" \
  --add-modules com.example.javafxapp \
  --output build/javafx-runtime \
  --strip-debug \
  --compress zip-6 \
  --no-header-files \
  --no-man-pages \
  --bind-services
```

参数说明：

| 参数          | 说明                                                        |
|--------------------|--------------------------------------------------------------------|
| `--module-path`    | 模块搜索路径，包含应用模块和 JavaFX 模块 JAR。 |
| `--add-modules`    | 要包含进镜像的根模块。                              |
| `--output`         | 输出目录。                                                  |
| `--strip-debug`    | 去除调试信息以减小镜像体积。                     |
| `--compress`       | 压缩级别（JDK 21+：`zip-0` 到 `zip-9`；旧版本：`0`-`2`）。 |
| `--no-header-files`| 排除头文件。                                             |
| `--no-man-pages`   | 排除手册页。                                                |
| `--bind-services`  | 链接服务提供者（如需使用 ServiceLoader）。              |

### 7.3 使用 javafx-maven-plugin 创建 jlink 镜像

```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>${main.class}</mainClass>
        <options>
            <option>--enable-native-access=javafx.graphics</option>
        </options>
    </configuration>
    <executions>
        <execution>
            <id>default-cli</id>
            <goals>
                <goal>jlink</goal>
            </goals>
            <configuration>
                <stripDebug>true</stripDebug>
                <compress>zip-6</compress>
                <noHeaderFiles>true</noHeaderFiles>
                <noManPages>true</noManPages>
                <launcher>app=${main.class}</launcher>
                <jlinkImageName>javafx-runtime</jlinkImageName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

执行：

```bash
mvn javafx:jlink
```

生成的镜像位于 `target/javafx-runtime/`，其中包含启动脚本 `bin/app`（或 `bin/app.bat`）。

### 7.4 使用 Gradle 创建 jlink 镜像

Gradle `org.openjfx.javafxplugin` 插件提供 `jlink` 任务：

```bash
gradle jlink
```

可在 `build.gradle` 中定制：

```groovy
jlink {
    options = ['--strip-debug', '--compress', 'zip-6', '--no-header-files', '--no-man-pages']
    launcher {
        name = 'app'
        jvmArgs = ['--enable-native-access=javafx.graphics']
    }
    jlinkBasePath = 'build'
    imageName = 'javafx-runtime'
}
```

### 7.5 运行 jlink 镜像

```bash
# Linux / macOS
./build/javafx-runtime/bin/app

# Windows
build\javafx-runtime\bin\app.bat
```

### 7.6 jlink 镜像体积优化技巧

1. **只包含必要模块**：在 `--add-modules` 中仅列出实际需要的模块。
2. **启用压缩**：使用 `--compress zip-6` 或更高。
3. **去除调试信息**：`--strip-debug`。
4. **排除不必要文件**：`--no-header-files --no-man-pages`。
5. 典型的 JavaFX 桌面应用 jlink 镜像约为 40-80 MB（取决于包含的模块）。

---

## 8. 完整项目目录结构参考

```
javafx-app/
├── pom.xml                      # Maven configuration (or build.gradle)
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java # Module declaration
│       │   └── com/
│       │       └── example/
│       │           └── javafxapp/
│       │               ├── MainApp.java          # Application entry point
│       │               ├── controller/
│       │               │   └── MainController.java
│       │               ├── model/
│       │               │   └── User.java
│       │               └── service/
│       │                   └── UserService.java
│       └── resources/
│           ├── fxml/
│           │   └── main.fxml
│           ├── css/
│           │   └── style.css
│           └── images/
│               └── icon.png
└── target/                      # Build output
```

---

## 9. MainApp 入口类示例

```java
package com.example.javafxapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            MainApp.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        scene.getStylesheets().add(
            getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("JavaFX Application");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 10. 常见问题排查

### Q1：运行时报 "module not found" 错误

确保 `module-info.java` 对使用的所有 JavaFX 模块都有 `requires`，且 `pom.xml` / `build.gradle` 中依赖声明正确。

### Q2：FXML 加载报 "Cannot load FXMLLoader" 或反射错误

检查 `module-info.java` 是否对 Controller 包有 `opens ... to javafx.fxml`。

### Q3：JavaFX 24 启动报 IllegalCallerException

添加 `--enable-native-access=javafx.graphics` JVM 参数。

### Q4：跨平台构建缺少原生库

确认 `javafx-graphics` 依赖使用了正确的平台分类器，或使用 Gradle 插件自动处理。

### Q5：jlink 报 "Module not found" 或自动模块错误

jlink 要求所有依赖是显式模块或自动模块。如果某个依赖是无名模块（没有 `Automatic-Module-Name`），必须将其包装为自动模块，或改用 `jpackage`。

### Q6：运行时报 "JavaFX runtime components are missing" 错误

**场景**：Spring Boot + JavaFX 集成项目，通过 `mvn spring-boot:run` 或 `java -jar` 运行时出错。

**原因**：主类（包含 `main` 方法的类）直接 `extends Application`，因此 JVM 使用 JavaFX 专用启动器运行。该启动器要求 `javafx.graphics` 以命名模块的形式存在于模块路径上。在 classpath 模式（无 `module-info.java`）下，JavaFX JAR 放在 classpath 上，启动器无法找到它们。

**解决方案**：将启动类拆分为两个类——一个 Spring Boot 启动类（不继承 Application）+ 一个 JavaFX 入口类（继承 Application）。详见 `references/spring-boot-integration.md` 第 3 节。

```java
// Startup class: does not extend Application
@SpringBootApplication
public class MyApp {
    static ConfigurableApplicationContext springContext;
    public static void main(String[] args) {
        springContext = SpringApplication.run(MyApp.class, args);
        Application.launch(JavaFXApp.class, args);
    }
}

// JavaFX entry class: extends Application
public class JavaFXApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(MyApp.springContext::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }
}
```

> 注意：纯 JavaFX 项目用 `mvn javafx:run` 运行不会触发此错误，因为 `javafx-maven-plugin` 将 JavaFX JAR 放在模块路径上。此问题仅出现在 classpath 模式（Spring Boot 集成、`java -jar` 执行）下。

### Q7：IDE 编译报 "ExceptionInInitializerError" + "TypeTag :: UNKNOWN"

**场景**：在 IntelliJ IDEA 中编译或运行项目时出现以下错误：
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```
而命令行 `mvn compile` 编译正常。

**原因**：IDE 选择的 Project JDK 与项目实际需要的 JDK 版本不匹配。Lombok 通过反射访问 JDK 编译器的内部 API（`com.sun.tools.javac.code.TypeTag`）来生成代码。当 IDE 使用的 JDK 版本与 Lombok 插件不兼容时，就会触发此错误。

**解决方案**：
1. **检查 IDE 的 Project JDK**：File -> Project Structure -> Project SDK，确保与 `pom.xml` 中配置的 `java.version` 一致。
2. **更新 Lombok 插件**：Settings -> Plugins -> 搜索 Lombok -> 更新到最新版本。
3. **检查 Maven 导入器 JDK**：Settings -> Build -> Build Tools -> Maven -> Importing -> JDK for importer，确保与项目一致。

> **提示**：这是 Lombok 与 JDK 版本不匹配导致的典型错误。Lombok 1.18.30+ 支持 JDK 21，但 IDE 的 Lombok 插件可能未相应更新。确保 IDE 的 JDK 选择、Lombok 插件版本和 `pom.xml` 中的 `java.version` 三者一致。

### Q8：HiDPI / 4K 屏幕下 UI 模糊

**场景**：在高分辨率屏幕（4K 显示器、Retina 显示屏）上，JavaFX 应用 UI 模糊或字体不清晰。

**原因**：JavaFX 默认支持 HiDPI 缩放，但某些情况下 HiDPI 被显式禁用，或显示器未正确识别。

**解决方案**：
1. 确保**不要**设置 `-Dprism.allowhidpi=false`（默认为 true；显式设为 false 会禁用 HiDPI）。
2. 在 Windows 上检查缩放设置：右键应用 -> 属性 -> 兼容性 -> 更改高 DPI 设置 -> 勾选"替代高 DPI 缩放行为"-> 选择"应用程序"。
3. 如果使用 `.properties` 配置，可添加 `-Dprism.allowhidpi=true` 确保启用。

### Q9：Linux 上中文显示为方块（豆腐块）

**场景**：在 Linux 系统上运行 JavaFX 应用，中文显示为方块或乱码。

**原因**：系统未安装中文字体，或 JavaFX 字体渲染引擎（Pango/Freetype）找不到中文字体。

**解决方案**：
1. 安装中文字体：`sudo apt install fonts-noto-cjk` 或 `sudo apt install fonts-wqy-microhei`。
2. 确认 fontconfig 配置正确：`fc-list :lang=zh` 应列出中文字体。
3. 如果使用容器（Docker），在镜像中安装字体。

### Q10：非模块化项目用 `java -jar` 报 "JavaFX runtime components missing"

**场景**：非模块化项目（无 `module-info.java`），构建为 fat JAR 后用 `java -jar app.jar` 运行报错。

**原因**：非模块化项目的普通 JAR 不包含 JavaFX 模块，且 `java -jar` 不会自动将 JavaFX 添加到模块路径。

**解决方案**：用 `maven-shade-plugin` 构建 fat JAR，并在启动时通过 `--module-path` 指定 JavaFX SDK 路径：
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar app.jar
```
或使用 `maven-shade-plugin` 将所有依赖（包括 JavaFX）打包进 fat JAR，但注意 JavaFX 的平台原生库仍需额外配置。
