# JavaFX 架构模式指南

本指南详细介绍 JavaFX 应用中常用的架构模式，包括 MVC 和 MVVM 的完整实现示例、模式对比、常见反模式、服务层设计、依赖注入解决方案以及事件总线模式。

---

## 1. MVC 模式（Model-View-Controller，模型-视图-控制器）

MVC 是最经典的 UI 架构模式。在 JavaFX 中，Model（模型）使用 JavaFX Properties（属性）暴露数据，View（视图）用 FXML 描述，Controller（控制器）通过 `@FXML` 注解处理用户交互并更新 Model。

### 1.1 职责划分

| 组件 | 职责 |
|-------------|-----------------------------------------------------------------------|
| Model（模型） | 数据模型与业务状态，使用 JavaFX Property 实现可观察性。 |
| View（视图） | 纯 UI 声明（FXML），不包含业务逻辑，通过绑定展示 Model 数据。 |
| Controller（控制器） | 接收用户事件，调用 Service/Model 方法，协调 View 与 Model 之间的交互。 |

### 1.2 完整代码示例

**Model — `Task.java`**

```java
package com.example.mvc.model;

import javafx.beans.property.*;

public class Task {
    private final StringProperty title = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    public Task(String title) {
        setTitle(title);
    }

    public StringProperty titleProperty() { return title; }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }

    public BooleanProperty completedProperty() { return completed; }
    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean value) { completed.set(value); }
}
```

**View — `task.fxml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="10" xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.example.mvc.controller.TaskController">
    <HBox spacing="10">
        <TextField fx:id="titleField" promptText="Enter task title" HBox.hgrow="ALWAYS"/>
        <Button text="Add" onAction="#handleAddTask"/>
    </HBox>
    <ListView fx:id="taskListView" VBox.vgrow="ALWAYS"/>
    <Label fx:id="statusLabel" text="Total 0 tasks"/>
</VBox>
```

**Controller — `TaskController.java`**

```java
package com.example.mvc.controller;

import com.example.mvc.model.Task;
import com.example.mvc.service.TaskService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class TaskController implements Initializable {

    @FXML private TextField titleField;
    @FXML private ListView<Task> taskListView;
    @FXML private Label statusLabel;

    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final TaskService taskService = new TaskService(); // Service layer

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        taskListView.setItems(tasks);
        taskListView.setCellFactory(lv -> new TaskListCell());

        // Bind the status label to the task count
        statusLabel.textProperty().bind(
            Bindings.size(tasks).asString("Total %d tasks"));

        // Load initial data
        tasks.addAll(taskService.loadTasks());
    }

    @FXML
    private void handleAddTask() {
        String title = titleField.getText().trim();
        if (!title.isEmpty()) {
            Task task = new Task(title);
            taskService.saveTask(task);  // Call Service
            tasks.add(task);
            titleField.clear();
        }
    }
}
```

```java
// Add inside TaskController (requires import javafx.scene.control.ListCell;)
// Custom list cell
public static class TaskListCell extends ListCell<Task> {
    @Override
    protected void updateItem(Task task, boolean empty) {
        super.updateItem(task, empty);
        if (empty || task == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(task.getTitle());
            setStyle(task.isCompleted() ? "-fx-text-fill: gray;" : "-fx-text-fill: black;");
        }
    }
}
```

### 1.3 MVC 数据流

```
User interaction -> Controller.handleXxx() -> Service/Model update
                                        |
                                        v
View <- (binding/manual refresh) <- Model state change
```

---

## 2. MVVM 模式（Model-View-ViewModel，模型-视图-视图模型）

MVVM 使用 ViewModel（视图模型）作为 View 与 Model 之间的中介，充分利用 JavaFX 强大的数据绑定机制，使 View 与逻辑完全解耦。ViewModel 不持有对 View 的引用，只暴露可绑定的 Properties（属性）。

### 2.1 职责划分

| 组件 | 职责 |
|------------|-------------------------------------------------------------------------|
| Model（模型） | 纯数据模型与业务实体。 |
| ViewModel（视图模型） | 暴露 View 所需的 Properties 和 Commands（命令），封装业务逻辑，无 UI 依赖。 |
| View（视图） | FXML + Controller（薄 Controller），只负责将 UI 控件绑定到 ViewModel。 |

### 2.2 完整代码示例

**Model — `User.java`**

```java
package com.example.mvvm.model;

import javafx.beans.property.*;

public class User {
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty age = new SimpleIntegerProperty();

    public User(String name, int age) {
        setName(name);
        setAge(age);
    }

    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    public IntegerProperty ageProperty() { return age; }
    public int getAge() { return age.get(); }
    public void setAge(int value) { age.set(value); }
}
```

**ViewModel — `UserViewModel.java`**

```java
package com.example.mvvm.viewmodel;

import com.example.mvvm.model.User;
import com.example.mvvm.service.UserService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UserViewModel {

    private final UserService userService = new UserService();

    // Form input bindings
    private final StringProperty inputName = new SimpleStringProperty();
    private final IntegerProperty inputAge = new SimpleIntegerProperty();

    // List data
    private final ObservableList<User> users = FXCollections.observableArrayList();

    // Selected item
    private final ObjectProperty<User> selectedUser = new SimpleObjectProperty<>();

    // Computed property: whether the form is valid
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);

    public UserViewModel() {
        // Form validation: name is non-empty and age is between 0-150
        formValid.bind(Bindings.createBooleanBinding(() -> {
            String name = getInputName();
            int age = getInputAge();
            return name != null && !name.trim().isEmpty() && age >= 0 && age <= 150;
        }, inputName, inputAge));

        // Load initial data
        users.addAll(userService.loadAllUsers());
    }

    /** Add user command */
    public void addUser() {
        if (!formValid.get()) return;
        User user = new User(getInputName().trim(), getInputAge());
        userService.save(user);
        users.add(user);
        inputName.set("");
        inputAge.set(0);
    }

    /** Remove selected user command */
    public void removeSelectedUser() {
        User selected = getSelectedUser();
        if (selected != null) {
            userService.delete(selected);
            users.remove(selected);
        }
    }

    // === Property accessors ===
    public StringProperty inputNameProperty() { return inputName; }
    public String getInputName() { return inputName.get(); }

    public IntegerProperty inputAgeProperty() { return inputAge; }
    public int getInputAge() { return inputAge.get(); }

    public ObservableList<User> getUsers() { return users; }

    public ObjectProperty<User> selectedUserProperty() { return selectedUser; }
    public User getSelectedUser() { return selectedUser.get(); }
    public void setSelectedUser(User value) { selectedUser.set(value); }

    public BooleanProperty formValidProperty() { return formValid; }
}
```

**View Controller（薄 Controller）— `UserViewController.java`**

```java
package com.example.mvvm.view;

import com.example.mvvm.viewmodel.UserViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.ResourceBundle;

public class UserViewController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField ageField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, Number> ageCol;
    @FXML private Button addButton;
    @FXML private Button removeButton;

    private final UserViewModel viewModel = new UserViewModel();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bidirectional binding: UI controls <-> ViewModel Property
        nameField.textProperty().bindBidirectional(viewModel.inputNameProperty());
        ageField.textProperty().bindBidirectional(
            viewModel.inputAgeProperty(), new NumberStringConverter());

        // Table data binding
        userTable.setItems(viewModel.getUsers());
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        ageCol.setCellValueFactory(c -> c.getValue().ageProperty());

        // Selected item binding
        viewModel.selectedUserProperty()
            .bind(userTable.getSelectionModel().selectedItemProperty());

        // Button disabled state bound to computed property
        addButton.disableProperty().bind(viewModel.formValidProperty().not());
        removeButton.disableProperty()
            .bind(viewModel.selectedUserProperty().isNull());

        // Button events delegated to ViewModel commands
        addButton.setOnAction(e -> viewModel.addUser());
        removeButton.setOnAction(e -> viewModel.removeSelectedUser());
    }
}
```

### 2.3 MVVM 数据流

```
View (FXML + thin Controller) <-bidirectional binding-> ViewModel (Property/Command)
                                            | calls
                                            v
                                        Service / Model
```

---

## 3. MVC 与 MVVM 对比

| 对比维度 | MVC | MVVM |
|---------------------------|--------------------------------------|--------------------------------------------|
| 复杂度 | 较低，结构简单直接 | 较高，需要额外的 ViewModel 层 |
| 可测试性 | 中等，Controller 依赖 JavaFX 控件 | 高，ViewModel 无 UI 依赖，可进行纯单元测试 |
| 耦合度 | Controller 直接操作 View 控件，耦合度较高 | View 与逻辑通过绑定解耦，耦合度较低 |
| 数据同步 | 手动刷新或单向绑定 | 双向绑定自动同步 |
| 代码量 | 较少 | 较多（需编写 ViewModel 和绑定代码） |
| 适用场景 | 简单表单、小工具、快速原型 | 中大型应用、复杂表单、对可测试性要求高 |
| 学习曲线 | 平缓 | 较陡峭，需理解绑定机制 |
| 团队协作 | View 与逻辑易混杂 | 前端/逻辑可并行开发 |

### 3.1 选择建议

- **小型应用 / 工具类软件**：使用 MVC，快速直接。
- **中大型应用 / 需要单元测试**：使用 MVVM，ViewModel 可独立测试。
- **混合使用**：简单页面用 MVC，复杂页面用 MVVM；两者可在同一项目中并存。

---

## 4. MVP 模式（Model-View-Presenter，模型-视图-主持人）

MVP（Model-View-Presenter）是一种介于 MVC 与 MVVM 之间的架构模式。它把所有 UI 逻辑从 View 中抽离到 Presenter，但与 MVVM 不同，它不依赖数据绑定，View 与 Presenter 通过接口显式交互。

**两种变体**

- **被动视图（Passive View）**：View 完全被动，所有 UI 状态更新都由 Presenter 通过调用 View 接口的方法来完成。View 不包含任何逻辑——即使是简单的格式化也交给 Presenter 处理。这样可测试性最高，但 Presenter 承载的代码更多。
- **监督控制器（Supervising Controller）**：Presenter 处理主要 UI 逻辑，但允许 View 通过简单绑定（如直接属性绑定）处理部分显示逻辑，Presenter 只介入复杂交互。这样代码量更少，也是实践中更常用的变体。

**何时选择 MVP**

- 希望在 MVC 与 MVVM 之间取得折中：实现 UI 逻辑的完全分离（提升可测试性），同时不引入 MVVM 的数据绑定机制（避免绑定链带来的复杂性与内存管理负担）。
- View 逻辑复杂但难以用声明式绑定表达（例如依赖大量条件分支或动画协调）。
- 团队更熟悉 Presenter 接口风格，或需要在不同 UI 框架间复用 Presenter。

**Presenter 示例**

```java
// View interface: abstracts the capabilities the View exposes to the Presenter
public interface TaskView {
    String getInputText();
    void setTaskList(List<Task> tasks);
    void showEmptyInputError();
}

// Presenter: holds a reference to the View interface, depends on no JavaFX controls,
// and can be independently unit tested
public class TaskPresenter {
    private final TaskView view;
    private final TaskService service;

    public TaskPresenter(TaskView view, TaskService service) {
        this.view = view;
        this.service = service;
    }

    public void onAddTask() {
        String title = view.getInputText();
        if (title == null || title.isBlank()) {
            view.showEmptyInputError();
            return;
        }
        Task task = new Task(title);
        service.saveTask(task);
        view.setTaskList(service.loadTasks());  // update the View through the interface
    }
}

// Controller implements the View interface, only does UI operations,
// and delegates all logic to the Presenter
public class TaskController implements TaskView {
    private final TaskPresenter presenter;

    public TaskController(TaskService service) {
        this.presenter = new TaskPresenter(this, service);
    }

    @FXML private void handleAddTask() { presenter.onAddTask(); }

    @Override public String getInputText() { return titleField.getText(); }
    @Override public void setTaskList(List<Task> tasks) {
        taskListView.setItems(FXCollections.observableArrayList(tasks));
    }
    @Override public void showEmptyInputError() { statusLabel.setText("Title cannot be empty"); }
}
```

---

## 5. 需要避免的反模式

### 5.1 臃肿的 Controller（Fat Controller）

**问题**：Controller 中堆积了大量业务逻辑、数据访问和校验代码，导致难以维护和测试。

```java
// Anti-pattern: Controller directly operates the database
@FXML
private void handleLogin() {
    String username = usernameField.getText();
    String password = passwordField.getText();
    // Business logic, validation, database access all stuffed in the Controller
    if (username.length() < 3) { /* ... */ }
    try (Connection conn = DriverManager.getConnection("jdbc:...")) {
        PreparedStatement ps = conn.prepareStatement("SELECT ...");
        // ...
    }
}
```

```java
// Correct approach: Controller only delegates to Service
@FXML
private void handleLogin() {
    boolean success = authService.login(
        usernameField.getText(), passwordField.getText());
    if (success) { showMainView(); }
    else { showError("Login failed"); }
}
```

### 5.2 在 FXML 中嵌入业务逻辑

**问题**：通过 FXML 的 `onAction` 用脚本（如 JavaScript）编写逻辑，或在 FXML Controller 中通过复杂的行内表达式处理业务。

```xml
<!-- Anti-pattern: Embedding script logic in FXML -->
<Button text="Calculate" onAction="#calculate">
    <fx:script>
        var result = parseInt(a) + parseInt(b);
        label.setText(result);
    </fx:script>
</Button>
```

FXML 应保持纯声明式，所有逻辑都应放入 Controller 或 ViewModel。

### 5.3 紧耦合

**问题**：Controller 直接 `new` 具体依赖类，导致这些依赖无法替换和测试。

```java
// Anti-pattern: Hardcoded dependencies
public class OrderController {
    private MySQLDatabase db = new MySQLDatabase();  // Tight coupling
    private EmailService email = new SmtpEmailService();
}
```

```java
// Correct approach: Interface + constructor injection
public class OrderController {
    private final Database db;
    private final EmailService email;

    public OrderController(Database db, EmailService email) {
        this.db = db;
        this.email = email;
    }
}
```

### 5.4 其他常见反模式

| 反模式 | 描述与改进 |
|---------------------------------------|----------------------------------------------------------------------|
| 上帝控制器（God Controller） | 单个 Controller 管理所有功能；应拆分为多个 Controller。 |
| 在 UI 线程上执行耗时操作 | 阻塞 JavaFX Application Thread 导致界面卡顿；应使用 `Task` + 后台线程。 |
| 直接暴露内部集合 | 应返回只读视图或用 Property 包装。 |
| 忽略资源释放 | 不移除监听器会导致内存泄漏；参见数据绑定指南。 |

---

## 6. 服务层设计

服务层封装业务逻辑与数据访问，作为 Controller/ViewModel 与数据层之间的中间层，使 UI 层保持整洁且可测试。

### 6.1 服务层职责

- 封装业务规则与事务边界。
- 协调多个 Repository / DAO。
- 提供与 UI 无关的 API，返回领域对象。
- 处理异常并将其转换为业务语义。

### 6.2 服务层实现示例

```java
package com.example.service;

import com.example.model.Task;
import com.example.repository.TaskRepository;

import java.util.List;

public class TaskService {

    private final TaskRepository repository;

    // Constructor injection of Repository (interface)
    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /** Load all tasks */
    public List<Task> loadTasks() {
        return repository.findAll();
    }

    /** Save task (with business validation) */
    public void saveTask(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        repository.save(task);
    }

    /** Toggle task completion status */
    public void toggleComplete(Task task) {
        task.setCompleted(!task.isCompleted());
        repository.update(task);
    }

    /** Delete task */
    public void deleteTask(Task task) {
        repository.delete(task);
    }
}
```

### 6.3 异步服务调用

耗时的服务调用应在后台线程执行，以避免阻塞 UI 线程：

```java
@FXML
private void handleLoadTasks() {
    Task<List<Task>> loadTask = new Task<>() {
        @Override
        protected List<Task> call() {
            return taskService.loadTasks();  // Execute in background
        }
    };
    loadTask.setOnSucceeded(e -> {
        tasks.setAll(loadTask.getValue());  // Return to UI thread to update
        statusLabel.setText("Loading complete");
    });
    loadTask.setOnFailed(e -> {
        statusLabel.setText("Loading failed: " + loadTask.getException().getMessage());
    });
    new Thread(loadTask).start();
}
```

---

## 7. 依赖注入

依赖注入用于解耦组件之间的依赖关系，使测试和实现替换更加容易。在 JavaFX 中有三种常见做法。

### 7.1 手动依赖注入

适用于小型项目，通过工厂或构造函数手动传递依赖。

```java
public class AppFactory {

    public static MainController createMainController() {
        TaskRepository repo = new InMemoryTaskRepository();
        TaskService service = new TaskService(repo);
        return new MainController(service);
    }
}

// In Application.start()
FXMLLoader loader = new FXMLLoader(url);
loader.setControllerFactory(c -> AppFactory.createMainController());
Parent root = loader.load();
```

### 7.2 Guice 依赖注入

Google Guice 是一个轻量级 DI 框架，适用于中小型 JavaFX 项目。

**Maven 依赖：**

```xml
<dependency>
    <groupId>com.google.inject</groupId>
    <artifactId>guice</artifactId>
    <version>7.0.0</version>
</dependency>
```

**配置与使用：**

```java
public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TaskRepository.class).to(InMemoryTaskRepository.class);
        bind(TaskService.class).to(TaskServiceImpl.class);
    }
}

// Custom ControllerFactory to let FXMLLoader use Guice to create Controllers
public class GuiceControllerFactory implements Callback<Class<?>, Object> {
    private final Injector injector;

    public GuiceControllerFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Object call(Class<?> type) {
        return injector.getInstance(type);
    }
}

// Application launch
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Injector injector = Guice.createInjector(new AppModule());
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(new GuiceControllerFactory(injector));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.show();
    }
}
```

**在 Controller 中使用注入：**

```java
public class MainController implements Initializable {
    private final TaskService taskService;

    @Inject  // Guice constructor injection
    public MainController(TaskService taskService) {
        this.taskService = taskService;
    }
    // ...
}
```

> 注意：使用 Guice 时，`module-info.java` 需要将 Controller 包 `opens` 给 Guice 模块，以支持反射。

### 7.3 Spring 框架依赖注入

Spring Boot 可以与 JavaFX 集成，适用于已在 Spring 生态中的项目。

> **严重陷阱：主类不能直接继承 Application**
>
> 当主类（包含 `main` 方法的类）直接 `extends Application` 时，JVM 会尝试使用 JavaFX 专用启动器运行。在 classpath 模式下（无 `module-info.java`，通过 `mvn spring-boot:run` 或 `java -jar` 运行），JavaFX 启动器找不到 `javafx.graphics` 模块并抛出错误：
> ```
> Error: JavaFX runtime components are missing, and are required to run this application
> ```
> **解决方案**：将启动类拆分为两个类——Spring Boot 启动类（不继承 Application）+ JavaFX 入口类（继承 Application）。详见 `references/spring-boot-integration.md`。

**Maven 依赖：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>3.5.0</version>
</dependency>
```

**Spring Boot + JavaFX 集成（正确做法：拆分为两个类）：**

```java
// 1. Spring Boot launch class: does not extend Application, runs through normal JVM launch flow
@SpringBootApplication
public class MyApp {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Start the Spring container first
        springContext = SpringApplication.run(MyApp.class, args);
        // Then start JavaFX (delegate to JavaFX entry class)
        Application.launch(JavaFXApp.class, args);
    }
}
```

```java
// 2. JavaFX entry class: extends Application, responsible for UI loading
public class JavaFXApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        // Get Controller Bean from Spring container via controllerFactory
        loader.setControllerFactory(MyApp.springContext::getBean);
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        MyApp.springContext.close();
    }
}
```

**由 Spring 管理的 Controller：**

```java
@Component
public class MainController implements Initializable {
    private final TaskService taskService;

    // Spring constructor injection (no @Autowired annotation needed, Spring 4.3+ auto-injects single constructor)
    public MainController(TaskService taskService) {
        this.taskService = taskService;
    }
    // ...
}
```

### 7.4 三种 DI 方案对比

| 方案 | 复杂度 | 适用场景 | 优点 | 缺点 |
|----------|------------|---------------------------------|-------------------------|--------------------------|
| 手动 | 低 | 小型项目、原型 | 无额外依赖，简单直接 | 依赖需手动维护，易出错 |
| Guice | 中 | 中型项目、需要轻量 DI | 轻量、API 简洁、启动快 | 生态不如 Spring 丰富 |
| Spring | 高 | 大型企业应用、已有 Spring 生态 | 功能全面、生态强大 | 较重、启动较慢 |

---

## 8. 事件总线模式

事件总线用于实现组件之间的松散耦合通信，尤其适用于多个不相关模块需要对同一事件做出响应的场景（例如用户登录后刷新多个视图）。

### 8.1 简单事件总线实现

```java
package com.example.eventbus;

import javafx.application.Platform;
import java.util.*;
import java.util.concurrent.*;

/**
 * Lightweight event bus, supports registering/unregistering listeners and publishing events.
 * Event dispatch is executed on the JavaFX Application Thread.
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();
    public static EventBus getInstance() { return INSTANCE; }

    private final Map<Class<?>, List<EventListener<?>>> listeners =
        new ConcurrentHashMap<>();

    /** Register a listener */
    public <T> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
    }

    /** Unregister a listener */
    public <T> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /** Publish an event (notifies on the JavaFX Application Thread) */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        List<EventListener<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (EventListener<?> listener : list) {
                Platform.runLater(() -> ((EventListener<T>) listener).onEvent(event));
            }
        }
    }

    @FunctionalInterface
    public interface EventListener<T> {
        void onEvent(T event);
    }
}
```

### 8.2 定义事件

```java
package com.example.event;

// User logged in event
public record UserLoggedInEvent(String username, long timestamp) {}

// Task completed event
public record TaskCompletedEvent(Long taskId) {}

// Data refresh event
public record DataRefreshEvent(String source) {}
```

### 8.3 使用示例

```java
public class HeaderController implements Initializable {
    private final EventBus eventBus = EventBus.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Subscribe to event
        eventBus.subscribe(UserLoggedInEvent.class, this::onUserLoggedIn);
    }

    private void onUserLoggedIn(UserLoggedInEvent event) {
        welcomeLabel.setText("Welcome, " + event.username());
    }

    @FXML
    private void handleLogin() {
        // ... login logic ...
        eventBus.publish(new UserLoggedInEvent(username, System.currentTimeMillis()));
    }
}

public class SidebarController implements Initializable {
    private final EventBus eventBus = EventBus.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Another controller independently subscribes to the same event
        eventBus.subscribe(UserLoggedInEvent.class, e -> {
            loadUserMenus(e.username());
        });
    }
}
```

### 8.4 事件总线使用注意事项

1. **及时注销监听器**：Controller 销毁时调用 `unsubscribe`，防止内存泄漏。
2. **线程安全**：上述实现使用 `CopyOnWriteArrayList` 保证并发安全；如果事件处理涉及 UI 更新，使用 `Platform.runLater()` 切换到 UI 线程。
3. **避免循环事件**：在事件处理中发布新事件可能造成死循环；需谨慎设计。
4. **成熟方案**：生产环境可考虑使用 Google Guava 的 `EventBus` 或 Apache DeltaSpike 的事件机制。

### 8.5 Guava EventBus 集成示例

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.2.1-jre</version>
</dependency>
```

```java
// Create an event bus (can be configured as asynchronous)
EventBus eventBus = new EventBus("javafx-app");

// Subscribe: use the @Subscribe annotation
public class DashboardController {
    @Subscribe
    public void onUserLoggedIn(UserLoggedInEvent event) {
        Platform.runLater(() -> updateDashboard(event.username()));
    }
}

// Register and unregister
eventBus.register(dashboardController);
// eventBus.unregister(dashboardController); // Unregister on destroy

// Publish event
eventBus.post(new UserLoggedInEvent("admin", System.currentTimeMillis()));
```

---

## 9. 架构模式选择决策树

```
Application scale?
|-- Small (< 10 pages, simple logic)
|   |-- MVC + manual dependency injection
|-- Medium (10-50 pages, medium complexity)
|   |-- MVVM + Guice + event bus
|-- Large (> 50 pages, complex business)
    |-- MVVM + Spring + event bus + service layer
```

### 通用架构原则

1. **单一职责**：每个类只做一件事（Controller 管理交互、Service 管理业务、Repository 管理数据）。
2. **依赖倒置**：依赖接口而非具体实现，通过 DI 注入。
3. **UI 线程纯净性**：耗时操作放后台线程，UI 更新回到 Application Thread。
4. **可测试性**：将业务逻辑与 UI 解耦，使 ViewModel/Service 可独立单元测试。
5. **渐进式架构**：先从简单模式开始，随着复杂度增长逐步引入更高级的模式。
