# JavaFX 第三方库集成指南

本指南介绍 JavaFX 生态中常用第三方库的特性、Maven 坐标、使用示例和版本兼容性，帮助开发者快速选择和集成。

---

## 1. ControlsFX

ControlsFX 是 JavaFX 生态中最成熟的扩展控件库，提供了丰富的组件，如对话框、通知、自动补全、属性表等。

### 1.1 主要特性

| 特性模块     | 说明                                                        |
|--------------------|--------------------------------------------------------------------|
| Dialogs            | 增强型对话框（确认、输入、异常展示等），比原生 Alert 更灵活 |
| Notifications      | 桌面通知气泡，支持自定义位置、动画和关闭动作 |
| AutoComplete       | 文本框自动补全                                         |
| PropertySheet      | 属性表控件，用于可视化编辑对象属性      |
| CheckComboBox      | 多选下拉框                                              |
| CheckListView      | 多选列表                                                  |
| RangeSlider        | 双滑块范围选择器                                           |
| Rating             | 星级评分控件                                                |
| SegmentedButton    | 分段按钮组                                             |
| StatusBar          | 状态栏控件                                                 |
| MasterDetailPane   | 主从布局面板                                          |
| PlusMinusSlider    | 加减滑块                                                  |
| SpreadsheetView    | 电子表格视图                                                   |

### 1.2 Maven 坐标

```xml
<dependency>
    <groupId>org.controlsfx</groupId>
    <artifactId>controlsfx</artifactId>
    <version>11.2.1</version>
</dependency>
```

### 1.3 代码示例

**对话框：**

```java
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

// Recommended: JavaFX native Alert (confirmation dialog)
Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
alert.setTitle("Confirm Delete");
alert.setHeaderText("Are you sure you want to delete this item?");
alert.setContentText("This action cannot be undone.");
alert.showAndWait().ifPresent(response -> {
    if (response == ButtonType.OK) {
        // perform the deletion
    }
});

// Recommended: JavaFX native TextInputDialog (input dialog)
TextInputDialog input = new TextInputDialog();
input.setTitle("Enter Name");
input.setHeaderText("Please enter the item name");
Optional<String> result = input.showAndWait();
result.ifPresent(name -> {
    // use the entered name
});
```

> 遗留兼容：ControlsFX 8.x 曾提供流式 `Dialogs.create().title(...).showConfirm()` API，**仅用于兼容遗留代码；新项目不要使用**。ControlsFX 新版本推荐使用 JavaFX 原生 `Alert` 和 `TextInputDialog`。参考：
>
> ```java
> // Not recommended: ControlsFX 8.x legacy fluent API (use native Alert / TextInputDialog for new code)
> // boolean confirm = Dialogs.create()
> //     .title("Confirm Delete")
> //     .masthead("Are you sure you want to delete this item?")
> //     .message("This action cannot be undone.")
> //     .showConfirm() == ButtonType.OK;
> ```

**通知气泡：**

```java
import org.controlsfx.control.Notifications;

Notifications.create()
    .title("Operation Successful")
    .text("Data has been saved")
    .graphic(new ImageView(successIcon))
    .hideAfter(Duration.seconds(3))
    .position(Pos.BOTTOM_RIGHT)
    .showInformation();

// Error notification
Notifications.create()
    .title("Save Failed")
    .text("Network connection timed out")
    .showError();
```

**自动补全：**

```java
import org.controlsfx.control.textfield.TextFields;

TextField searchField = new TextField();
// Bind auto-completion suggestion list
TextFields.bindAutoCompletion(searchField,
    "Apple", "Banana", "Cherry", "Date", "Elderberry");
```

**属性表：**

```java
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanProperty;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;

PropertySheet propertySheet = new PropertySheet();
// Add JavaBean-based property items
propertySheet.getItems().addAll(
    new BeanProperty(person, "name"),
    new BeanProperty(person, "age"),
    new BeanProperty(person, "email")
);
```

**CheckComboBox（多选下拉框）：**

```java
import org.controlsfx.control.CheckComboBox;

CheckComboBox<String> checkCombo = new CheckComboBox<>(
    FXCollections.observableArrayList("Option A", "Option B", "Option C"));
checkCombo.getCheckModel().check(0);  // Check the first item
ObservableList<String> checked = checkCombo.getCheckModel().getCheckedItems();
```

---

## 2. MaterialFX

MaterialFX 提供 Google Material Design 风格的 JavaFX 控件，视觉现代、交互组件丰富。

### 2.1 主要控件

| 控件            | 说明                                |
|--------------------|--------------------------------------------|
| MFXButton          | Material 风格按钮                      |
| MFXTextField       | 带浮动标签的文本框             |
| MFXCheckbox        | Material 复选框                          |
| MFXComboBox        | 增强下拉框                          |
| MFXDatePicker      | 日期选择器                                |
| MFXTableView       | 增强表格（支持过滤、排序） |
| MFXDialog          | Material 风格对话框                      |
| MFXNotification    | 通知组件                     |
| MFXProgressBar     | 进度条                               |
| MFXSlider          | 滑块                                     |
| MFXStepper         | 步骤导航器                             |
| MFXFilterComboBox  | 可过滤下拉框                        |

### 2.2 Maven 坐标

```xml
<dependency>
    <groupId>io.github.palexdev</groupId>
    <artifactId>materialfx</artifactId>
    <version>11.17.0</version>
</dependency>
```

### 2.3 使用示例

```java
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXComboBox;

// Material-style button
MFXButton button = new MFXButton("Submit");
button.setStyle("-fx-background-color: #6200ee; -fx-text-fill: white;");

// Floating label text field
MFXTextField textField = new MFXTextField();
textField.setFloatingText("Username");
textField.setPromptText("Please enter username");

// Enhanced dropdown
MFXComboBox<String> combo = new MFXComboBox<>();
combo.getItems().addAll("Option 1", "Option 2", "Option 3");
combo.setPromptText("Please select");
combo.selectFirst();

// Use in FXML
```

```xml
<?import io.github.palexdev.materialfx.controls.*?>

<MFXButton text="Submit" styleClass="mfx-button"/>
<MFXTextField floatingText="Email" promptText="Please enter email"/>
```

---

## 3. RichTextFX

RichTextFX 提供富文本编辑区，支持样式文本、语法高亮、行号等。是构建代码编辑器或富文本编辑器的首选。

### 3.1 主要特性

| 特性              | 说明                                              |
|----------------------|---------------------------------------------------------|
| StyleClassedTextArea | 基于样式类的富文本区                        |
| CodeArea             | 专为代码编辑设计的文本区，支持行号 |
| InlineCssTextArea    | 基于 CSS 的富文本区                                |
| Syntax Highlighting  | 通过样式类高亮关键字、注释、字符串等 |
| Line Numbers         | 自动行号槽                            |
| Undo/Redo            | 内置撤销/重做支持                              |

### 3.2 Maven 坐标

```xml
<dependency>
    <groupId>org.fxmisc.richtext</groupId>
    <artifactId>richtextfx</artifactId>
    <version>0.11.5</version>
</dependency>
```

### 3.3 代码示例

**基础代码编辑器：**

```java
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

CodeArea codeArea = new CodeArea();
// Add line numbers
codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

// Set initial content
codeArea.replaceText(0, 0, "public class Hello {\n    \n}");

// Apply syntax highlighting (simplified example)
codeArea.textProperty().addListener((obs, oldText, newText) -> {
    // Clear old styles
    codeArea.setStyle(0, newText.length(), "-fx-fill: black;");

    // Highlight keywords
    String[] keywords = {"public", "class", "void", "static", "private"};
    for (String keyword : keywords) {
        int idx = 0;
        while ((idx = newText.indexOf(keyword, idx)) >= 0) {
            codeArea.setStyle(idx, idx + keyword.length(),
                "-fx-fill: #cc7832; -fx-font-weight: bold;");
            idx += keyword.length();
        }
    }
});
```

**富文本编辑：**

```java
import org.fxmisc.richtext.StyleClassedTextArea;

StyleClassedTextArea area = new StyleClassedTextArea();
area.appendText("Normal text ");
area.appendText("Red bold", List.of("red-bold"));
area.appendText(" Normal text");

// Define style classes in CSS
// .red-bold { -fx-fill: red; -fx-font-weight: bold; }
```

---

## 4. Ikonli

Ikonli 提供基于字体图标的统一解决方案，集成了数十种图标包，作为传统图片图标的替代方案。

### 4.1 支持的图标包（部分）

| 图标包          | 说明                        | Maven artifactId              |
|--------------------|------------------------------------|-------------------------------|
| MaterialDesign     | Google Material Design 图标       | `ikonli-materialdesign2-pack` |
| FontAwesome        | Font Awesome 图标                 | `ikonli-fontawesome-pack`     |
| Material Icons     | Material Icons                     | `ikonli-materialicons-pack`   |
| Ionicons           | Ionicons 图标                     | `ikonli-ionicons4-pack`       |
| Octicons           | GitHub Octicons                    | `ikonli-octicons-pack`        |
| Feather            | Feather Icons                      | `ikonli-feather-pack`         |
| Bootstrap Icons    | Bootstrap Icons                    | `ikonli-bootstrapicons-pack`  |
| BoxIcons           | Box Icons                          | `ikonli-boxicons-pack`        |
| AntDesign Icons    | Ant Design Icons                   | `ikonli-antdesignicons-pack`  |
| CoreUI             | CoreUI Icons                       | `ikonli-coreui-pack`          |

### 4.2 Maven 坐标

```xml
<!-- Core library -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-javafx</artifactId>
    <version>12.3.1</version>
</dependency>

<!-- Select icon packs as needed -->
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-materialdesign2-pack</artifactId>
    <version>12.3.1</version>
</dependency>
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-fontawesome-pack</artifactId>
    <version>12.3.1</version>
</dependency>
```

### 4.3 使用示例

```java
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.fontawesome.FontAwesome;

// Create icon
FontIcon saveIcon = new FontIcon(MaterialDesignS.CONTENT_SAVE);
saveIcon.setIconSize(24);
saveIcon.setIconColor(Color.BLUE);

// Add to button
Button saveButton = new Button("Save");
saveButton.setGraphic(saveIcon);

// FontAwesome icon
FontIcon userIcon = new FontIcon(FontAwesome.USER);
userIcon.setIconSize(20);

// Use in FXML
```

```xml
<?import org.kordamp.ikonli.javafx.FontIcon?>
<?import org.kordamp.ikonli.materialdesign2.MaterialDesignS?>

<FontIcon iconLiteral="md2al-content_save" iconSize="24" />
```

---

## 5. ValidatorFX

ValidatorFX 提供声明式表单校验框架，与 JavaFX 控件深度集成。

### 5.1 Maven 坐标

```xml
<dependency>
    <groupId>net.synedra</groupId>
    <artifactId>validatorfx</artifactId>
    <version>0.4.0</version>
</dependency>
```

### 5.2 使用示例

```java
import net.synedra.validatorfx.Validator;
import net.synedra.validatorfx.Decoration;
import net.synedra.validatorfx.ValidationMessage;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import java.util.IdentityHashMap;
import java.util.Map;

public class FormController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField ageField;
    @FXML private Button submitButton;

    private Validator validator = new Validator();
    // Save the error Tooltip reference for each control so the same instance can be passed when uninstalling
    private final Map<Node, Tooltip> errorTooltips = new IdentityHashMap<>();

    @FXML
    public void initialize() {
        // Username validation: non-empty and length >= 3
        validator.createCheck()
            .dependsOn("username", usernameField.textProperty())
            .withMethod(c -> {
                String username = c.get("username");
                if (username == null || username.trim().length() < 3) {
                    c.error("Username must be at least 3 characters");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(usernameField);

        // Email validation
        validator.createCheck()
            .dependsOn("email", emailField.textProperty())
            .withMethod(c -> {
                String email = c.get("email");
                if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
                    c.error("Invalid email format");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(emailField);

        // Age validation
        validator.createCheck()
            .dependsOn("age", ageField.textProperty())
            .withMethod(c -> {
                try {
                    int age = Integer.parseInt(c.get("age"));
                    if (age < 0 || age > 150) {
                        c.error("Age must be between 0 and 150");
                    }
                } catch (NumberFormatException e) {
                    c.error("Age must be a number");
                }
            })
            .decoratingWith(this::decorateError)
            .decorate(ageField);

        // Bind submit button to validation status
        submitButton.disableProperty().bind(validator.containsErrorsProperty());
    }

    /**
     * Error decorator: adds a red border and a tooltip to the control.
     * Key point: the Tooltip instance is saved in a member Map, and the same reference is passed
     * to uninstall() on remove; do not pass null (otherwise the installed Tooltip cannot be removed).
     */
    private Decoration decorateError(ValidationMessage message) {
        return new Decoration() {
            @Override
            public void add(Node target) {
                target.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                // create and save the Tooltip reference
                Tooltip tooltip = new Tooltip(message.getText());
                tooltip.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                Tooltip.install(target, tooltip);
                errorTooltips.put(target, tooltip);
            }

            @Override
            public void remove(Node target) {
                target.setStyle("");
                // when uninstalling, you must pass the same Tooltip instance used during install
                Tooltip tooltip = errorTooltips.remove(target);
                if (tooltip != null) {
                    Tooltip.uninstall(target, tooltip);
                }
            }
        };
    }

    @FXML
    private void handleSubmit() {
        if (!validator.containsErrors()) {
            // Submit logic
        }
    }
}
```

> 注意：`Tooltip.uninstall(Node, Tooltip)` 的第二个参数必须是 `Tooltip.install` 时使用的同一个 `Tooltip` 实例；传入 `null` 不会卸载任何 Tooltip，这是一个常见错误。

---

## 6. TestFX

TestFX 是 JavaFX 的 UI 自动化测试框架，支持模拟用户交互和断言 UI 状态。

### 6.1 Maven 坐标

```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-core</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
```

### 6.2 测试示例

```java
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

class MainAppTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        // Start the application
        new MainApp().start(stage);
    }

    @Test
    void shouldAddTaskWhenClickingAddButton() {
        // Enter task title
        clickOn("#titleField").write("Test Task");

        // Click the add button
        clickOn("#addButton");

        // Verify the list contains the new task
        verifyThat("#taskListView", node -> {
            ListView<?> list = (ListView<?>) node;
            return list.getItems().size() == 1;
        });

        // Verify the status label updates
        verifyThat("#statusLabel", hasText("Total 1 tasks"));
    }

    @Test
    void shouldShowErrorWhenTitleEmpty() {
        // Click add without entering anything
        clickOn("#addButton");

        // Verify the list is still empty
        verifyThat("#taskListView", node -> {
            ListView<?> list = (ListView<?>) node;
            return list.getItems().isEmpty();
        });
    }
}
```

### 6.3 常用 TestFX 操作

```java
// Mouse operations
clickOn("#buttonId");
rightClickOn("#nodeId");
doubleClickOn("#nodeId");
moveTo("#nodeId");

// Keyboard operations
clickOn("#textField").write("Hello World");
type(KeyCode.ENTER);
press(KeyCode.CONTROL).press(KeyCode.C).release(KeyCode.C).release(KeyCode.CONTROL);

// Find nodes
Button button = lookup("#submitButton").query();
Label label = lookup(".error-label").query();

// Assertions
verifyThat("#label", hasText("Success"));
verifyThat("#button", NodeMatchers.isVisible());
verifyThat("#button", NodeMatchers.isDisabled());
```

---

## 7. JMetro

JMetro 是基于微软现代 UI（Metro/Fluent Design）风格的 JavaFX 主题，提供现代的扁平外观。

### 7.1 Maven 坐标

```xml
<dependency>
    <groupId>org.jfxtras</groupId>
    <artifactId>jmetro</artifactId>
    <version>11.6.16</version>
</dependency>
```

### 7.2 使用示例

```java
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(url);
        Scene scene = new Scene(loader.load());

        // Apply the JMetro theme (light or dark)
        JMetro jMetro = new JMetro(Style.LIGHT);
        jMetro.setScene(scene);

        stage.setScene(scene);
        stage.show();
    }
}
```

JMetro 会自动将 Modern UI 风格应用到所有标准 JavaFX 控件，无需手动编写 CSS。

---

## 8. BootstrapFX

BootstrapFX 将 Twitter Bootstrap 的 CSS 风格带入 JavaFX，提供熟悉的 Bootstrap 风格控件外观。

### 8.1 Maven 坐标

```xml
<dependency>
    <groupId>org.kordamp.bootstrapfx</groupId>
    <artifactId>bootstrapfx-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

### 8.2 使用示例

```java
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Scene scene = new Scene(root, 800, 600);

        // Load the BootstrapFX stylesheet
        scene.getStylesheets().add(
            "org/kordamp/bootstrapfx/bootstrapfx.css");

        stage.setScene(scene);
        stage.show();
    }
}
```

```xml
<!-- Use Bootstrap style classes in FXML -->
<Button text="Primary" styleClass="btn,btn-primary"/>
<Button text="Danger" styleClass="btn,btn-danger"/>
<Button text="Success" styleClass="btn,btn-success"/>
<Label text="Warning" styleClass="alert,alert-warning"/>
<Label text="Info" styleClass="badge,badge-info"/>
```

---

## 9. AtlantaFX

AtlantaFX 是一个现代 Flat 风格的主题库，内置多个高质量主题（如 Casper、Primer 等），为标准 JavaFX 控件提供统一、精致的扁平外观，开箱即用。

### 9.1 主要特性

| 特性               | 说明                                                        |
|-----------------------|--------------------------------------------------------------------|
| Flat 风格主题     | 内置多个主题：Casper、Primer Light/Dark、Atlanta Dark 等 |
| 零 CSS              | 直接替换 user-agent 样式表；无需手写 CSS |
| 高保真控件 | 覆盖几乎所有标准 JavaFX 控件，包括 hover/focus/disabled 状态 |
| 浅色 / 深色          | 通过 `Style` 枚举或不同主题类一键切换 |
| 兼容 ControlsFX | 为部分扩展控件提供主题适配        |

### 9.2 Maven 坐标

```xml
<dependency>
    <groupId>io.github.mkpaz</groupId>
    <artifactId>atlantafx-base</artifactId>
    <version>2.0.1</version>
</dependency>
```

### 9.3 使用示例

```java
import atlantafx.base.theme.Casper;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Set the user-agent stylesheet before creating any UI
        Application.setUserAgentStylesheet(new Casper().getUserAgentStylesheet());

        Scene scene = new Scene(FXMLLoader.load(url), 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}
```

切换到深色主题：

```java
// Switch to Primer Dark at runtime
Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

// Or use Primer Light
Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
```

> 提示：AtlantaFX 通过 `setUserAgentStylesheet` 全局生效；所有标准控件自动获得 Flat 外观，无需在 Scene 上额外加载 CSS。

---

## 10. FXGL

FXGL 是基于 JavaFX 的 2D 游戏开发引擎，提供完整的游戏循环、物理引擎、动画、AI、粒子系统等功能。

### 10.1 Maven 坐标

```xml
<dependency>
    <groupId>com.github.almasb</groupId>
    <artifactId>fxgl</artifactId>
    <version>17.3</version>
</dependency>
```

### 10.2 使用示例

```java
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Rectangle;

public class SimpleGame extends GameApplication {

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Simple Game");
        settings.setWidth(800);
        settings.setHeight(600);
    }

    @Override
    protected void initGame() {
        // Create player entity
        player = FXGL.entityBuilder()
            .at(400, 300)
            .view(new Rectangle(40, 40, javafx.scene.paint.Color.BLUE))
            .buildAndAttach();
    }

    @Override
    protected void initInput() {
        // Keyboard control
        FXGL.onKey(KeyCode.W, () -> player.translateY(-5));
        FXGL.onKey(KeyCode.S, () -> player.translateY(5));
        FXGL.onKey(KeyCode.A, () -> player.translateX(-5));
        FXGL.onKey(KeyCode.D, () -> player.translateX(5));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## 11. 兼容性矩阵

下表展示各第三方库与 JavaFX 版本的兼容性（根据公开信息整理；实际使用前请查阅最新文档）。

| 库      | Maven 版本 | JavaFX 17 | JavaFX 21 | JavaFX 24+        | 说明                            |
|--------------|---------------|-----------|-----------|-------------------|----------------------------------|
| ControlsFX   | 11.2.1        | 兼容 | 兼容 | 基本兼容 | 成熟稳定，使用广泛 |
| MaterialFX   | 11.17.0       | 兼容 | 兼容 | 已知兼容 | 需要 JavaFX 17+；控件 API 差异较大 |
| RichTextFX   | 0.11.5        | 兼容 | 兼容 | 已知兼容 | 依赖 ReactFX / Flowless    |
| Ikonli       | 12.3.1        | 兼容 | 兼容 | 兼容   | 纯 Java，兼容性好    |
| ValidatorFX  | 0.4.0         | 兼容 | 兼容 | 已知兼容 | 轻量级，纯 Java          |
| TestFX       | 4.0.18        | 兼容 | 兼容 | 需要 --add-opens | 测试框架；反射访问 JavaFX 模块 |
| JMetro       | 11.6.16       | 兼容 | 兼容 | 已知兼容 | 主题库                    |
| BootstrapFX  | 0.4.0         | 兼容 | 兼容 | 兼容   | 纯 CSS，兼容性好     |
| AtlantaFX    | 2.0.1         | 兼容 | 兼容 | 已知兼容 | 需要 JavaFX 17+；Flat 主题库 |
| FXGL         | 17.3          | 兼容 | 兼容 | 已知兼容 | 游戏引擎，需要 JavaFX 17+ |

### 兼容性说明

1. **JavaFX 24+ `--enable-native-access`**：部分依赖原生代码的库可能需要额外的 JVM 参数配置。
2. **`--add-opens` 参数**：TestFX 等库反射访问 JavaFX 内部成员，在更高 JDK 版本上需要类似 `--add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED` 的参数，否则会抛出 `InaccessibleObjectException`。
3. **模块化冲突**：非模块化库在 jlink 打包时可能报错；可通过 `--add-modules` 处理，或将该库转换为自动模块。
4. **版本锁定**：建议在 `pom.xml` 中统一锁定 JavaFX 版本和库版本，避免传递依赖冲突。
5. **测试验证**：升级 JavaFX 版本后，务必对所有第三方库功能进行回归测试。

---

## 12. 库选型建议

| 需求场景                 | 推荐库                 |
|--------------------------------------|-------------------------------------|
| 对话框 / 通知 / 高级控件 | ControlsFX（对话框优先用原生 Alert） |
| Material Design 风格 UI             | MaterialFX                          |
| 代码编辑器 / 富文本              | RichTextFX                          |
| 字体图标                           | Ikonli                              |
| 表单校验                      | ValidatorFX 或原生 BooleanBinding |
| UI 自动化测试                | TestFX                              |
| 现代主题（Metro 风格）           | JMetro                              |
| 现代 Flat 风格主题              | AtlantaFX                           |
| Bootstrap 风格                      | BootstrapFX                         |
| 2D 游戏开发                  | FXGL                                |
| 多选下拉框                | ControlsFX（CheckComboBox）          |
| 范围滑块                         | ControlsFX（RangeSlider）            |
| 属性编辑面板               | ControlsFX（PropertySheet）          |
