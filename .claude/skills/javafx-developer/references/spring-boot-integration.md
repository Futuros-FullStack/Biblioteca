# Spring Boot + JavaFX 集成指南

本指南详细介绍 Spring Boot 与 JavaFX 的集成，涵盖启动类设计、依赖注入、配置管理、持久化层集成以及常见陷阱。这是实际开发中最常见的 JavaFX 集成场景之一。

---

## 1. 为什么使用 Spring Boot

在纯 JavaFX 应用中，Controller 通常通过 `new` 创建，依赖手动管理，处理复杂业务逻辑比较困难。通过引入 Spring Boot，你可以获得：

- **依赖注入**：Controller、Service、Repository 之间的依赖由 Spring 容器管理
- **事务管理**：通过 `@Transactional` 进行声明式数据库事务管理
- **自动配置**：数据源、MyBatis/JPA 等组件开箱即用
- **生态兼容**：可直接使用 Spring Data、Spring Security 等生态组件

---

## 2. 三种集成方式对比

| 方式 | 原理 | 优点 | 缺点 | 推荐场景 |
|----------|-----------|------|------|----------------------|
| **手动集成** | 主类先启动 Spring 再调用 `Application.launch()` | 零额外依赖，灵活可控，对新手友好 | 需要手动管理 ControllerFactory | 通用场景（推荐） |
| **javafx-weaver** | net.rgielen:javafx-weaver 自动注入 Controller | 自动化程度高，社区活跃 | 额外依赖，有学习成本 | 中大型项目 |
| **spring-boot-javafx-support** | org.bsc.javafx:javafx-spring-boot-starter | 配置简单 | 维护不活跃，版本滞后 | 快速原型 |

> **本技能默认推荐手动集成方式**：无需额外依赖，代码直观，初学者易上手。

---

## 3. 启动类拆分原则（最关键知识点）

### 3.1 问题：主类直接继承 Application

**错误做法**（会导致启动失败）：

```java
@SpringBootApplication
public class MyApp extends Application {  // Main class directly extends Application

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = SpringApplication.run(MyApp.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // ...
    }

    public static void main(String[] args) {
        launch(args);  // Directly call launch()
    }
}
```

**错误信息**：
```
Error: JavaFX runtime components are missing, and are required to run this application
```

### 3.2 原因分析

当 JVM 检测到主类（包含 `public static void main` 的类）是 `Application` 的子类时，会使用 JavaFX 专用启动器（`com.sun.javafx.application.LauncherImpl`）运行。该启动器要求 `javafx.graphics` 以**命名模块**的形式存在于模块路径上。

在 classpath 模式（无 `module-info.java`，通过 `mvn spring-boot:run` 或 `java -jar` 运行）下，JavaFX JAR 以自动模块形式放在 classpath 上，JavaFX 启动器无法找到它们，从而报错。

> 注意：纯 JavaFX 项目通过 `javafx-maven-plugin` 用 `mvn javafx:run` 运行不会触发此错误，因为插件把 JavaFX JAR 放在了模块路径上。但集成 Spring Boot 时通常使用 `mvn spring-boot:run`，走的是 classpath 模式。

### 3.3 解决方案：拆分为两个类

**正确做法**：

```java
// 1. Spring Boot startup class: does not extend Application, uses normal JVM startup flow
@SpringBootApplication
public class MyApp {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Step 1: Start the Spring container
        springContext = SpringApplication.run(MyApp.class, args);
        // Step 2: Start JavaFX (delegated to the JavaFX entry class)
        Application.launch(JavaFXApp.class, args);
    }
}
```

```java
// 2. JavaFX entry class: extends Application, responsible for UI loading
public class JavaFXApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        // Obtain Controller Bean from the Spring container via controllerFactory
        loader.setControllerFactory(MyApp.springContext::getBean);

        Scene scene = new Scene(loader.load(), 1024, 768);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("My Application");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Release the Spring container after JavaFX shuts down
        MyApp.springContext.close();
    }
}
```

**要点**：
1. `MyApp` 不继承 `Application`，因此 JVM 使用正常启动流程，不触发 JavaFX 启动器检查
2. `springContext` 声明为 `static` 以便 `JavaFXApp` 访问
3. `Application.launch(JavaFXApp.class, args)` 指定 JavaFX 入口类
4. 在 `stop()` 中关闭 Spring 容器以确保资源释放

---

## 4. Controller 注入机制

### 4.1 controllerFactory 原理

`FXMLLoader` 默认通过 `Class.newInstance()` 创建 Controller（无参构造器）。设置 `controllerFactory` 后，`FXMLLoader` 将 Controller 类名传给工厂方法，由工厂决定如何实例化。

```java
loader.setControllerFactory(springContext::getBean);
```

这行代码等价于：

```java
loader.setControllerFactory(controllerClass -> springContext.getBean(controllerClass));
```

当 `FXMLLoader` 解析 `fx:controller="com.example.controller.UserController"` 时，会调用 `springContext.getBean(UserController.class)`，从 Spring 容器中获取一个注入好依赖的 Controller 实例。

### 4.2 Controller 必须注册为 Spring Bean

```java
@Component  // Must be annotated with @Component (or @Controller, etc., a Spring stereotype annotation)
public class UserController implements Initializable {

    private final UserService userService;  // Injected via constructor

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialization logic
    }
}
```

> **注意**：Spring 4.3+ 支持单构造器类的自动注入；无需 `@Autowired` 注解。

### 4.3 对话框中的 Controller 注入

打开对话框时也需要设置 controllerFactory：

```java
private boolean showUserDialog(User user) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user-dialog.fxml"));
    loader.setControllerFactory(springContext::getBean);  // Also needs to be set
    Parent root = loader.load();

    UserDialogController controller = loader.getController();
    controller.setUser(user);

    Stage dialogStage = new Stage();
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    dialogStage.setScene(new Scene(root));
    controller.setDialogStage(dialogStage);
    dialogStage.showAndWait();

    return controller.isSaved();
}
```

> **陷阱**：如果对话框的 FXMLLoader 未设置 controllerFactory，会抛出 `NoSuchBeanException`，或 Controller 中注入的 Service 为 null。

---

## 5. 完整 pom.xml 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>myapp</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <javafx.version>21.0.11</javafx.version>
    </properties>

    <dependencies>
        <!-- Spring Boot core (without web server) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- JavaFX Controls -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX FXML -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- JavaFX Graphics (includes platform native libraries) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <version>${javafx.version}</version>
        </dependency>

        <!-- Lombok (optional) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 关键说明

1. **使用 `spring-boot-starter` 而非 `spring-boot-starter-web`**：桌面应用不需要 Web 服务器。
2. **不要使用 `javafx-maven-plugin`**：运行命令是 `mvn spring-boot:run` 而非 `mvn javafx:run`。
3. **不要创建 `module-info.java`**：以 classpath 模式运行，避免模块化的复杂性。
4. **Lombok 注解处理器**：Spring Boot 父 POM 已管理 Lombok 依赖；无需在 `maven-compiler-plugin` 中单独配置 `annotationProcessorPaths`。

---

## 6. application.yml 配置

```yaml
spring:
  # Do not start a web server (desktop application)
  main:
    web-application-type: none
    banner-mode: off
  # Data source configuration (using SQLite as an example)
  datasource:
    url: jdbc:sqlite:data/app.db
    driver-class-name: org.sqlite.JDBC
    username: sa
    password: ""
  # Automatically execute table creation scripts on startup
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

# MyBatis configuration (if used)
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.model
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.example: DEBUG
```

### 关键配置说明

| 配置项 | 用途 | 说明 |
|---------------------|---------|-------|
| `spring.main.web-application-type=none` | 关闭 Web 服务器 | 桌面应用不需要 Tomcat/Netty |
| `spring.main.banner-mode=off` | 关闭 Spring 横幅 | 可选，减少控制台噪音 |
| `spring.sql.init.mode=always` | 启动时执行 SQL 脚本 | 用于自动建表和初始化数据 |

---

## 7. 完整项目结构

```
myapp/
├── pom.xml
├── data/                           # SQLite database file directory
│   └── app.db
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/
│       │       ├── MyApp.java              # Spring Boot startup class (does not extend Application)
│       │       ├── JavaFXApp.java          # JavaFX entry class (extends Application)
│       │       ├── controller/
│       │       │   ├── MainController.java # Main view controller (@Component)
│       │       │   └── DialogController.java
│       │       ├── service/
│       │       │   ├── UserService.java
│       │       │   └── impl/
│       │       │       └── UserServiceImpl.java
│       │       ├── mapper/
│       │       │   └── UserMapper.java     # MyBatis Mapper (@Mapper)
│       │       └── model/
│       │           └── User.java           # Entity class (JavaFX Properties)
│       └── resources/
│           ├── application.yml             # Spring Boot configuration
│           ├── schema.sql                  # Table creation script
│           ├── fxml/
│           │   ├── main-view.fxml
│           │   └── user-dialog.fxml
│           ├── css/
│           │   ├── light-theme.css
│           │   └── dark-theme.css
│           └── mapper/
│               └── UserMapper.xml          # MyBatis XML mapping
└── target/                         # Build output
```

> **注意**：不要创建 `module-info.java`；以 classpath 模式运行。

---

## 8. 与持久层框架的集成

### 8.1 MyBatis 集成

**依赖**：
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.47.0.0</version>
</dependency>
```

**Mapper 接口**：
```java
@Mapper
public interface UserMapper {
    List<User> findPage(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Long id);
}
```

**Service 层事务**：
```java
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional
    public User save(User user) {
        if (user.getId() == 0) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        return user;
    }
}
```

### 8.2 JavaFX Properties 与 MyBatis 的兼容性

JavaFX Properties 的 getter/setter 可被 MyBatis 正确映射，但需要处理基本类型 Properties 的 null 值：

```java
public class User {
    private final LongProperty id = new SimpleLongProperty();

    public long getId() { return id.get(); }

    // MyBatis may pass null during injection (insert scenario); safe handling is needed
    public void setId(Long id) {
        this.id.set(id == null ? 0L : id);
    }

    public LongProperty idProperty() { return id; }
}
```

> **陷阱**：`SimpleLongProperty.set(long)` 接受基本类型 `long`；传入 `null` 会抛出 `NullPointerException`。`IntegerProperty` 和 `BooleanProperty` 同理。必须在 setter 中处理 null。

---

## 9. 运行与打包

### 9.1 开发运行

```bash
# Run using the Spring Boot Maven plugin (recommended)
mvn spring-boot:run

# Or compile first, then run
mvn clean compile
mvn spring-boot:run
```

> **不要使用** `mvn javafx:run`，因为项目没有配置 `javafx-maven-plugin`，需要通过 `spring-boot:run` 启动 Spring Boot 容器。

### 9.2 打包为可执行 JAR

```bash
mvn clean package
java -jar target/myapp-1.0.0.jar
```

### 9.3 打包为原生安装程序

使用 jpackage 创建平台原生安装程序：

```bash
# First build the JAR
mvn clean package

# Create a Windows installer
jpackage \
  --type exe \
  --name "MyApp" \
  --app-version 1.0.0 \
  --input target \
  --main-jar myapp-1.0.0.jar \
  --main-class com.example.MyApp \
  --win-menu \
  --win-shortcut
```

> **注意**：打包时 `--main-class` 指向 Spring Boot 启动类（`MyApp`），而非 JavaFX 入口类。

---

## 10. 常见陷阱检查清单

### 陷阱 1：主类直接继承 Application（最常见）

**症状**：`Error: JavaFX runtime components are missing`

**原因**：主类继承 Application，JVM 使用 JavaFX 启动器，在 classpath 模式下无法找到 JavaFX 模块。

**解决方案**：拆分为启动类 + JavaFX 入口类（见第 3 节）。

### 陷阱 2：对话框 Controller 注入失败

**症状**：打开对话框时，Controller 中的 Service 为 null，或抛出 `NoSuchBeanException`。

**原因**：对话框的 FXMLLoader 未设置 `controllerFactory`。

**解决方案**：所有 FXMLLoader 实例都需要设置 `loader.setControllerFactory(springContext::getBean)`。

### 陷阱 3：Controller 未标注 @Component

**症状**：`NoSuchBeanDefinitionException: No qualifying bean of type 'UserController'`

**原因**：Controller 类未标注 Spring 原型注解，Spring 容器中不存在该 Bean。

**解决方案**：给 Controller 类添加 `@Component` 注解。

### 陷阱 4：JavaFX Properties 的 Null 处理

**症状**：插入新记录时抛出 `NullPointerException`；MyBatis 将 null 值注入基本类型 Property。

**原因**：`SimpleLongProperty.set(long)` 等方法接受基本类型，不接受 null。

**解决方案**：在 setter 方法中将 null 转换为默认值（见第 8.2 节）。

### 陷阱 5：忘记关闭 Spring 容器

**症状**：应用窗口关闭后进程不退出，或数据库连接未释放。

**原因**：JavaFX 关闭后 Spring 容器仍在运行。

**解决方案**：在 `JavaFXApp.stop()` 中调用 `springContext.close()`。

### 陷阱 6：在 UI 线程执行长时间数据库操作

**症状**：UI 卡顿或无响应。

**原因**：MyBatis 查询在 JavaFX Application Thread 上同步执行。

**解决方案**：使用 `Task` + 后台线程执行耗时查询，并通过 `Platform.runLater()` 将结果返回到 UI 线程：

```java
Task<List<User>> loadTask = new Task<>() {
    @Override
    protected List<User> call() {
        return userService.findPage(keyword, page, pageSize);  // Executed in background
    }
};
loadTask.setOnSucceeded(e -> {
    userTable.getItems().setAll(loadTask.getValue());  // Update on UI thread
});
new Thread(loadTask).start();
```

### 陷阱 7：Lombok 注解处理器配置冲突

**症状**：Maven 编译报 `No processor claimed any of these annotations` 或 Lombok 注解不生效。

**原因**：在 `maven-compiler-plugin` 中手动配置了 `annotationProcessorPaths`，但未指定 Lombok 版本。

**解决方案**：Spring Boot 父 POM 已管理 Lombok 的注解处理器；无需在 `maven-compiler-plugin` 中单独配置 `annotationProcessorPaths`。删除该配置。

### 陷阱 8：IDE 中 Lombok 与 JDK 版本不匹配

**症状**：在 IntelliJ IDEA 中编译或运行时出现以下错误，而命令行 `mvn compile` 正常：
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

**原因**：IDE 选择的 Project JDK 与 `pom.xml` 中配置的 `java.version` 不匹配。Lombok 通过反射访问 JDK 编译器的内部 API（`TypeTag`），当 IDE 使用的 JDK 版本与 Lombok 插件不兼容时触发此错误。

**解决方案**：
1. 确保 IDE 的 Project SDK（File -> Project Structure -> Project SDK）与 `pom.xml` 中的 `java.version` 一致
2. 将 IDE 的 Lombok 插件更新到最新版本（Settings -> Plugins -> Lombok）
3. 检查 Maven Importing 使用的 JDK（Settings -> Build -> Maven -> Importing -> JDK for importer）

> **提示**：Lombok 1.18.30+ 支持 JDK 21，但 IDE 的 Lombok 插件可能未相应更新。确保 IDE 的 JDK 选择、Lombok 插件版本和 `pom.xml` 中的 `java.version` 三者一致。

### 陷阱 9：Controller 单例导致状态污染

**症状**：打开多个同类型窗口或对话框时，前一个窗口的输入数据"泄漏"到下一个，或 Controller 中的 UI 控件状态混乱。

**原因**：Spring 默认将所有 `@Component` Bean 注册为**单例**（singleton 作用域）。但在 JavaFX 中，每次 `FXMLLoader.load()` 都期望一个**全新的** Controller 实例。当 Spring 返回同一个单例 Bean 时，`@FXML` 字段会被新 FXML 的控件覆盖，旧控件引用丢失，导致状态污染和内存泄漏。

**解决方案**：给 Controller 类添加 `@Scope("prototype")`，确保每次从 Spring 容器获取时都创建新实例：

```java
@Component
@Scope("prototype")  // Create a new instance on each injection
public class UserDialogController implements Initializable {
    // ...
}
```

> **注意**：所有可能通过 `FXMLLoader` 多次加载的 Controller 都应使用 `prototype` 作用域。主窗口 Controller 如果只有一个实例可以保持单例，但仍需注意不要在 Controller 中缓存 UI 状态。

### 陷阱 10：spring-boot-devtools 导致 JavaFX 异常重启

**症状**：开发过程中修改代码后，JavaFX 窗口突然关闭或出现重复窗口、`Stage already showing` 等异常。

**原因**：`spring-boot-devtools` 的自动重启机制会监听 classpath 变化，文件变化时触发 Spring 容器重启。但 JavaFX 的 `Application` 生命周期与 Spring 容器不同步；容器重启不会重新调用 `Application.launch()`，导致状态不一致。

**解决方案**：
1. **排除 devtools 依赖**（推荐）：在 `pom.xml` 中将 devtools 设为 `optional` 或直接移除。
2. **禁用自动重启**：在 `application.yml` 中配置 `spring.devtools.restart.enabled: false`。
3. **仅排除 JavaFX 类触发**：在 `application.yml` 中配置 `spring.devtools.restart.exclude: static/**,public/**`（排除资源目录）。

```xml
<!-- If devtools must be used, set it to optional -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 陷阱 11：升级到 JavaFX 24+ 后启动失败

**症状**：将 JavaFX 版本从 21 升级到 24+ 后，`mvn spring-boot:run` 启动时报 `IllegalCallerException`。

**原因**：JavaFX 24+ 的图形渲染层通过 JNI 访问原生代码，在 JDK 24+ 严格的模块封装下需要 `--enable-native-access=javafx.graphics`。但 Spring Boot 的 `spring-boot-maven-plugin` 默认不传递该参数。

**解决方案**：在 `spring-boot-maven-plugin` 配置中添加 JVM 参数：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>--enable-native-access=javafx.graphics</jvmArguments>
    </configuration>
</plugin>
```

---

## 11. javafx-weaver 备选方案

如果项目规模较大，需要更自动化的 Controller 注入，可以使用 [javafx-weaver](https://github.com/rgielen/javafx-weaver)。

**依赖**：
```xml
<dependency>
    <groupId>net.rgielen</groupId>
    <artifactId>javafx-weaver-spring-boot-starter</artifactId>
    <version>2.0.1</version>
</dependency>
```

**用法**：
```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }
}

@Component
public class MainController implements FxController {
    private final FxWeaver fxWeaver;

    public MainController(FxWeaver fxWeaver) {
        this.fxWeaver = fxWeaver;
    }

    public void showView() {
        fxWeaver.loadView(MainController.class);  // Automatically loads FXML and injects the Controller
    }
}
```

> 注意：javafx-weaver 仍然需要拆分启动类；主类不能直接继承 Application。

---

## 12. 集成方式选择决策树

```
Project scale and requirements?
├── Small projects / quick prototyping / beginners
│   └── Manual integration (recommended approach in this guide)
├── Medium projects / need automated Controller injection
│   └── javafx-weaver
└── Existing spring-boot-javafx-support legacy projects
    └── Keep as-is or migrate to manual integration
```
