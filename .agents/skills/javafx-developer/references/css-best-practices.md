# JavaFX CSS 最佳实践指南

本指南涵盖 JavaFX CSS 语法特性、变量与主题架构、选择器优先级、常用控件样式、响应式布局、动画与过渡、`derive()` 函数、Scene Builder 集成以及运行时主题切换。

---

## 1. JavaFX CSS 与 Web CSS 的语法差异

JavaFX CSS 基于 CSS 语法，但有几个关键差异。理解这些差异是正确进行样式化的前提。

| 特性 | Web CSS | JavaFX CSS |
|----------------------|----------------------------------|-----------------------------------------------|
| 属性前缀 | 无前缀（例如 `color`） | 大多数属性使用 `-fx-` 前缀（例如 `-fx-text-fill`） |
| 颜色属性 | `color`（文本）、`background-color` | `-fx-text-fill`（文本）、`-fx-background-color` |
| 尺寸单位 | 支持 `px`、`em`、`rem`、`%` 等 | 支持 `px`、`em`、`pt`，但不支持 `rem` |
| 选择器类型 | 标签、类、ID、属性、伪类 | 类型选择器（例如 `Button`）、样式类（例如 `.button`）、ID、伪类 |
| 伪类 | `:hover`、`:focus` 等 | `:hover`、`:focused`、`:pressed`、`:armed` 等 |
| 变量（自定义属性） | `--var` + `var()` | 查找颜色（looked-up colors）；在 `.root` 上用 `-fx-` 前缀定义，子节点按名称直接引用，无需 `var()` |
| 布局属性 | `display`、`flex`、`grid` | JavaFX 布局由布局容器管理；CSS 只控制外观 |
| 函数 | `calc()`、`rgb()` 等 | `derive()`、`ladder()` 及其他 JavaFX 专用函数 |
| 继承 | 部分属性可继承 | `-fx-font-*` 等属性可继承 |

> 注意：JavaFX CSS **不支持** `var()` 函数（那是 Web CSS 的特性）。JavaFX 的"查找颜色（looked-up color）"机制让你可以在 `.root` 上用 `-fx-` 前缀定义自定义属性；子节点直接按名称引用它们，无需 `var()` 包装。

### 1.1 类型选择器与样式类选择器

在 Web CSS 中，`button` 选择标签名，而在 JavaFX CSS 中你必须区分两种选择器：

- **类型选择器**：不带点书写，直接使用控件的类名。在 JavaFX 中，`Node.getTypeSelector()` 默认返回简单类名（例如 `Button`、`Label`、`TextField`），因此类型选择器写成 `Button`。
- **样式类选择器**：带点书写（例如 `.button`），匹配其 `getStyleClass()` 中包含该名称的节点。标准 JavaFX 控件注册的样式类通常全部为小写（例如 `button`、`label`、`text-field`）。

```css
/* Type selector: no dot, matches all Button controls */
Button {
    -fx-background-color: #4a90d9;
    -fx-text-fill: white;
}

/* Style class selector: with dot, matches nodes whose style class contains "button" */
.button {
    -fx-background-color: #4a90d9;
    -fx-text-fill: white;
}
```

> 提示：标准的 `Button` 控件默认也具有 `button` 样式类，因此 `Button` 和 `.button` 都能匹配它；但两者的优先级不同，类型选择器的优先级低于样式类选择器。

### 1.2 常见 JavaFX CSS 属性映射

| 用途 | Web CSS 属性 | JavaFX CSS 属性 |
|----------------|----------------------|-----------------------------|
| 文本颜色 | `color` | `-fx-text-fill` |
| 背景颜色 | `background-color` | `-fx-background-color` |
| 字体大小 | `font-size` | `-fx-font-size` |
| 字体粗细 | `font-weight` | `-fx-font-weight` |
| 内边距 | `padding` | `-fx-padding` |
| 圆角 | `border-radius` | `-fx-background-radius` |
| 边框 | `border` | `-fx-border-color` / `-fx-border-width` |
| 光标 | `cursor` | `-fx-cursor` |
| 不透明度 | `opacity` | `-fx-opacity` / `opacity` |

---

## 2. CSS 变量（查找颜色）

JavaFX 通过"查找颜色（looked-up color）"机制支持自定义颜色属性：在 `.root` 上用 `-fx-` 前缀定义后，子节点可以直接按名称引用它们（**无需 `var()` 包装**；JavaFX CSS 不支持 `var()` 函数），从而集中管理主题颜色。

### 2.1 定义与使用

```css
.root {
    /* Define theme variables (looked-up colors) */
    -fx-primary-color: #2196f3;
    -fx-accent-color: #ff9800;
    -fx-bg-color: #ffffff;
    -fx-text-color: #333333;
    -fx-radius: 8;
}

/* Reference variables: colors are referenced directly by name */
.button-primary {
    -fx-background-color: -fx-primary-color;
    -fx-text-fill: white;
    /* Referencing a looked-up color for size properties is unreliable; use a literal value instead */
    -fx-background-radius: 8;
}
```

> 注意：查找颜色主要面向**颜色**值。对 `-fx-background-radius` 和 `-fx-border-radius` 等尺寸属性直接使用查找颜色在 JavaFX 中不可靠；应改用字面数值（例如 `8`、`4`）。

### 2.2 变量作用域

- 在 `.root` 上定义的变量具有全局作用域，所有节点都可访问。
- 在特定节点上重新定义变量会覆盖全局值，仅影响该节点及其子节点。

```css
.custom-pane {
    /* Only this pane and its children use a different primary color */
    -fx-primary-color: #e91e63;
}
```

---

## 3. 主题变量架构（浅色 / 深色）

通过查找颜色构建完整的主题系统，实现浅色与深色主题的统一管理。

### 3.1 浅色主题变量定义

```css
/* light-theme.css */
.root {
    /* Background colors */
    -fx-bg-primary: #ffffff;
    -fx-bg-secondary: #f5f5f5;
    -fx-bg-tertiary: #e0e0e0;

    /* Text colors */
    -fx-text-primary: #212121;
    -fx-text-secondary: #757575;
    -fx-text-disabled: #bdbdbd;

    /* Accent colors */
    -fx-accent: #2196f3;
    -fx-accent-hover: derive(-fx-accent, -15%);
    -fx-accent-pressed: derive(-fx-accent, -25%);

    /* Status colors */
    -fx-success: #4caf50;
    -fx-warning: #ff9800;
    -fx-danger: #f44336;

    /* Borders and dividers */
    -fx-border-color: #e0e0e0;
    -fx-divider-color: #eeeeee;

    /* Radius and spacing (reference constants only; use literals for size properties) */
    -fx-radius-sm: 4;
    -fx-radius-md: 8;
    -fx-radius-lg: 12;
}
```

### 3.2 深色主题变量定义

```css
/* dark-theme.css */
.root {
    -fx-bg-primary: #1e1e1e;
    -fx-bg-secondary: #252525;
    -fx-bg-tertiary: #333333;

    -fx-text-primary: #ffffff;
    -fx-text-secondary: #b0b0b0;
    -fx-text-disabled: #666666;

    -fx-accent: #64b5f6;
    -fx-accent-hover: derive(-fx-accent, 15%);
    -fx-accent-pressed: derive(-fx-accent, 25%);

    -fx-success: #66bb6a;
    -fx-warning: #ffa726;
    -fx-danger: #ef5350;

    -fx-border-color: #444444;
    -fx-divider-color: #383838;

    -fx-radius-sm: 4;
    -fx-radius-md: 8;
    -fx-radius-lg: 12;
}
```

### 3.3 引用主题变量的控件样式

```css
.button {
    -fx-background-color: -fx-bg-secondary;
    -fx-text-fill: -fx-text-primary;
    -fx-background-radius: 8;
    -fx-padding: 8 16 8 16;
}

.button:hover {
    -fx-background-color: -fx-bg-tertiary;
}

.button-primary {
    -fx-background-color: -fx-accent;
    -fx-text-fill: white;
}

.button-primary:hover {
    -fx-background-color: -fx-accent-hover;
}

.text-field {
    -fx-background-color: -fx-bg-primary;
    -fx-text-fill: -fx-text-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-width: 1;
    -fx-background-radius: 4;
}

.label {
    -fx-text-fill: -fx-text-primary;
}
```

---

## 4. 选择器优先级与特异性

JavaFX CSS 遵循与 Web CSS 类似的优先级规则。优先级从高到低：

1. **内联样式**（通过 `setStyle()` 设置）——最高优先级
2. **ID 选择器**（`#myId`）
3. **样式类 + 伪类选择器**（`.button:hover`）
4. **样式类选择器**（`.button`）
5. **类型选择器**（`Button` 匹配所有 Button 控件）

### 4.1 优先级示例

```css
/* Type selector: no dot, lowest priority */
Button {
    -fx-background-color: gray;
}

/* Style class selector: with dot, higher priority than the type selector */
.danger-button {
    -fx-background-color: red;
}

/* ID selector: highest priority */
#saveButton {
    -fx-background-color: green;
}
```

```java
// Inline style: highest priority, overrides all CSS rules
saveButton.setStyle("-fx-background-color: blue;");
```

### 4.2 !important

JavaFX CSS 支持 `!important`，可用于提高样式规则的优先级。在日常开发中，优先通过更高优先级的选择器或内联样式来管理覆盖；仅在万不得已时将 `!important` 作为最后手段使用。

```css
.button {
    -fx-background-color: -fx-accent !important;
}
```

### 4.3 多样式类叠加

一个节点可以拥有多个样式类；后添加且在 CSS 中较晚定义的类优先：

```java
button.getStyleClass().addAll("button", "primary", "large");
```

---

## 5. 常用控件样式

### 5.1 Button

```css
.button {
    -fx-background-color: -fx-accent;
    -fx-text-fill: white;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-padding: 8 20 8 20;
    -fx-background-radius: 6;
    -fx-border-width: 0;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: -fx-accent-hover;
}

.button:pressed {
    -fx-background-color: -fx-accent-pressed;
}

.button:disabled {
    -fx-opacity: 0.5;
    -fx-cursor: default;
}

/* Outlined secondary button */
.button-outline {
    -fx-background-color: transparent;
    -fx-border-color: -fx-accent;
    -fx-border-width: 1.5;
    -fx-border-radius: 6;
    -fx-text-fill: -fx-accent;
}
```

### 5.2 TextField / PasswordField

```css
.text-field {
    -fx-background-color: -fx-bg-primary;
    -fx-text-fill: -fx-text-primary;
    -fx-prompt-text-fill: -fx-text-disabled;
    -fx-border-color: -fx-border-color;
    -fx-border-width: 1;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
    -fx-padding: 8 10 8 10;
    -fx-font-size: 14px;
}

.text-field:focused {
    -fx-border-color: -fx-accent;
    -fx-border-width: 2;
}

.text-field.error {
    -fx-border-color: -fx-danger;
}
```

### 5.3 TableView

```css
.table-view {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-width: 1;
}

/* Table header */
.table-view .column-header {
    -fx-background-color: -fx-bg-secondary;
    -fx-border-color: -fx-divider-color;
}

.table-view .column-header .label {
    -fx-text-fill: -fx-text-primary;
    -fx-font-weight: bold;
}

/* Rows */
.table-row-cell {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-divider-color;
}

.table-row-cell:odd {
    -fx-background-color: -fx-bg-secondary;
}

.table-row-cell:selected {
    -fx-background-color: -fx-accent;
}

.table-row-cell:selected .text {
    -fx-fill: white;
}

/* Empty table placeholder */
.table-view .placeholder .label {
    -fx-text-fill: -fx-text-secondary;
}
```

### 5.4 ListView

```css
.list-view {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-width: 1;
    -fx-background-radius: 4;
}

.list-cell {
    -fx-background-color: transparent;
    -fx-text-fill: -fx-text-primary;
    -fx-padding: 8 12 8 12;
}

.list-cell:filled:hover {
    -fx-background-color: -fx-bg-tertiary;
}

.list-cell:filled:selected {
    -fx-background-color: -fx-accent;
    -fx-text-fill: white;
}
```

### 5.5 ComboBox

```css
.combo-box {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}

.combo-box .arrow-button {
    -fx-background-color: -fx-bg-secondary;
}

.combo-box .list-cell {
    -fx-text-fill: -fx-text-primary;
}
```

### 5.6 ScrollBar

```css
.scroll-bar {
    -fx-background-color: transparent;
}

.scroll-bar .thumb {
    -fx-background-color: -fx-text-disabled;
    -fx-background-radius: 4;
}

.scroll-bar .thumb:hover {
    -fx-background-color: -fx-text-secondary;
}

.scroll-bar .increment-button,
.scroll-bar .decrement-button {
    -fx-background-color: transparent;
    -fx-padding: 0;
}
```

### 5.7 MenuBar / MenuItem

```css
.menu-bar {
    -fx-background-color: -fx-bg-secondary;
    -fx-border-color: -fx-divider-color;
    -fx-border-width: 0 0 1 0;
}

.menu-bar .label {
    -fx-text-fill: -fx-text-primary;
}

.menu-item .label {
    -fx-text-fill: -fx-text-primary;
}

.menu-item:focused {
    -fx-background-color: -fx-accent;
}

.menu-item:focused .label {
    -fx-text-fill: white;
}
```

### 5.8 CheckBox / RadioButton / ToggleButton

```css
/* Check box */
.check-box {
    -fx-text-fill: -fx-text-primary;
    -fx-font-size: 14px;
}
.check-box .box {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
}
.check-box:selected .box {
    -fx-background-color: -fx-accent;
    -fx-border-color: -fx-accent;
}

/* Radio button */
.radio-button {
    -fx-text-fill: -fx-text-primary;
    -fx-font-size: 14px;
}
.radio-button .radio {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-radius: 1em;
}
.radio-button:selected .radio {
    -fx-border-color: -fx-accent;
}
.radio-button:selected .dot {
    -fx-background-color: -fx-accent;
}

/* Toggle button */
.toggle-button {
    -fx-background-color: -fx-bg-secondary;
    -fx-text-fill: -fx-text-primary;
    -fx-background-radius: 4;
    -fx-cursor: hand;
}
.toggle-button:selected {
    -fx-background-color: -fx-accent;
    -fx-text-fill: white;
}
.toggle-button:disabled {
    -fx-opacity: 0.5;
}
```

### 5.9 ProgressBar / ProgressIndicator

```css
.progress-bar {
    -fx-background-color: -fx-bg-tertiary;
    -fx-background-radius: 4;
    -fx-pref-height: 8;
}
.progress-bar .track {
    -fx-background-color: transparent;
}
.progress-bar .bar {
    -fx-background-color: -fx-accent;
    -fx-background-radius: 4;
}
.progress-bar:disabled .bar {
    -fx-background-color: -fx-text-disabled;
}

/* Circular progress indicator */
.progress-indicator {
    -fx-progress-color: -fx-accent;
}
```

### 5.10 TabPane

```css
.tab-pane .tab-header-area {
    -fx-background-color: -fx-bg-secondary;
}
.tab-pane .tab {
    -fx-background-color: transparent;
    -fx-padding: 6 14 6 14;
    -fx-background-radius: 4 4 0 0;
}
.tab-pane .tab:selected {
    -fx-background-color: -fx-bg-primary;
}
.tab-pane .tab-label {
    -fx-text-fill: -fx-text-secondary;
}
.tab-pane .tab:selected .tab-label {
    -fx-text-fill: -fx-text-primary;
    -fx-font-weight: bold;
}
.tab-pane .tab-close-button {
    -fx-background-color: -fx-text-secondary;
}
```

### 5.11 Spinner

```css
.spinner {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}
.spinner .text-field {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
}
.spinner .increment-arrow-button,
.spinner .decrement-arrow-button {
    -fx-background-color: -fx-bg-secondary;
    -fx-background-radius: 0;
}
.spinner .increment-arrow-button:hover,
.spinner .decrement-arrow-button:hover {
    -fx-background-color: -fx-bg-tertiary;
}
.spinner .increment-arrow,
.spinner .decrement-arrow {
    -fx-background-color: -fx-text-secondary;
}
```

### 5.12 Tooltip

```css
.tooltip {
    -fx-background-color: #333333;
    -fx-text-fill: white;
    -fx-background-radius: 4;
    -fx-border-color: transparent;
    -fx-font-size: 12px;
    -fx-padding: 6 10 6 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);
}
```

### 5.13 TreeView

```css
.tree-view {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-width: 1;
    -fx-background-radius: 4;
}
.tree-cell {
    -fx-background-color: transparent;
    -fx-text-fill: -fx-text-primary;
    -fx-padding: 4 8 4 8;
}
.tree-cell:filled:hover {
    -fx-background-color: -fx-bg-tertiary;
}
.tree-cell:filled:selected {
    -fx-background-color: -fx-accent;
    -fx-text-fill: white;
}
.tree-cell:expanded {
    -fx-font-weight: bold;
}
```

### 5.14 DatePicker

```css
.date-picker {
    -fx-background-color: -fx-bg-primary;
    -fx-border-color: -fx-border-color;
    -fx-border-radius: 4;
    -fx-background-radius: 4;
}
.date-picker .arrow-button {
    -fx-background-color: -fx-bg-secondary;
}
.date-picker:focused {
    -fx-border-color: -fx-accent;
}

/* Popup calendar */
.date-picker-popup .day-cell:selected {
    -fx-background-color: -fx-accent;
    -fx-text-fill: white;
}
.date-picker-popup .today {
    -fx-border-color: -fx-accent;
    -fx-border-width: 1;
}
.date-picker-popup .previous-month,
.date-picker-popup .next-month {
    -fx-text-fill: -fx-text-disabled;
}
```

---

## 6. 响应式布局技巧

JavaFX 没有像 Web 那样的媒体查询，但可以通过以下方法实现响应式效果。

### 6.1 使用布局容器实现自动缩放

```java
// HBox / VBox hgrow / vgrow properties enable flexible layout
HBox.setHgrow(textField, Priority.ALWAYS);
VBox.setVgrow(tableView, Priority.ALWAYS);
```

### 6.2 监听窗口大小以动态切换样式

```java
scene.widthProperty().addListener((obs, oldVal, newVal) -> {
    double width = newVal.doubleValue();
    ObservableList<String> classes = root.getStyleClass();
    classes.removeAll("layout-small", "layout-medium", "layout-large");
    if (width < 600) {
        classes.add("layout-small");
    } else if (width < 1000) {
        classes.add("layout-medium");
    } else {
        classes.add("layout-large");
    }
});
```

```css
/* Small screen layout: single column, compact */
.layout-small .sidebar {
    -fx-pref-width: 0;
    -fx-max-width: 0;
}

.layout-small .content {
    -fx-font-size: 12px;
}

/* Large screen layout: wide sidebar */
.layout-large .sidebar {
    -fx-pref-width: 250;
}

.layout-large .content {
    -fx-font-size: 14px;
}
```

### 6.3 使用 Stage 最大化/全屏监听器

```java
stage.maximizedProperty().addListener((obs, old, isMax) -> {
    root.setStyle(isMax ? "-fx-font-size: 16px;" : "-fx-font-size: 14px;");
});
```

---

## 7. 动画与过渡

JavaFX CSS 通过 `Transition` API 支持动画，也可以在 CSS 中使用 `-fx-transition` 风格的过渡（需与代码结合）。

### 7.1 无法直接在 CSS 中定义过渡

与 Web CSS 的 `transition` 不同，JavaFX CSS 不支持在样式表中直接定义过渡动画。动画必须通过 Java 代码中的 `Transition` 类实现。

### 7.2 常见动画代码示例

```java
// Fade in/out
FadeTransition fade = new FadeTransition(Duration.millis(300), button);
fade.setFromValue(0.0);
fade.setToValue(1.0);
fade.play();

// Scale transition (hover effect)
ScaleTransition scale = new ScaleTransition(Duration.millis(200), button);
scale.setToX(1.05);
scale.setToY(1.05);
scale.setAutoReverse(true);
scale.setCycleCount(2);

button.setOnMouseEntered(e -> {
    scale.setFromX(1.0); scale.setFromY(1.0);
    scale.setToX(1.05); scale.setToY(1.05);
    scale.playFromStart();
});

button.setOnMouseExited(e -> {
    scale.setFromX(1.05); scale.setFromY(1.05);
    scale.setToX(1.0); scale.setToY(1.0);
    scale.playFromStart();
});

// Translate
TranslateTransition move = new TranslateTransition(Duration.millis(500), node);
move.setToX(100);
move.play();

// Rotate
RotateTransition rotate = new RotateTransition(Duration.millis(1000), node);
rotate.setByAngle(360);
rotate.setCycleCount(Animation.INDEFINITE);
rotate.play();
```

### 7.3 复合动画

```java
// Parallel animation: execute multiple animations simultaneously
ParallelTransition parallel = new ParallelTransition(
    new FadeTransition(Duration.millis(400), node),
    new ScaleTransition(Duration.millis(400), node)
);
parallel.play();

// Sequential animation: execute one after another
SequentialTransition sequential = new SequentialTransition(
    new PauseTransition(Duration.millis(500)),
    new FadeTransition(Duration.millis(300), node)
);
sequential.play();
```

### 7.4 通过 CSS 伪类触发动画状态

```css
.card {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
}

.card:hover {
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);
}
```

与代码中的 `Transition` 结合以实现平滑过渡。

---

## 8. derive() 函数

`derive()` 是 JavaFX CSS 专用的颜色函数，用于基于基础颜色生成更浅或更深的变体，非常适合构建配色方案。

### 8.1 语法

```css
derive(<color>, <percentage>)
```

- 正百分比：颜色变浅（向白色混合）。
- 负百分比：颜色变深（向黑色混合）。
- 范围：`-100%`（纯黑）到 `100%`（纯白）。

### 8.2 使用示例

```css
.root {
    -fx-base-color: #2196f3;
}

.button {
    -fx-background-color: derive(-fx-base-color, 0%);    /* Original color */
}

.button:hover {
    -fx-background-color: derive(-fx-base-color, 15%);   /* Lighten by 15% */
}

.button:pressed {
    -fx-background-color: derive(-fx-base-color, -20%);  /* Darken by 20% */
}

.button:disabled {
    -fx-background-color: derive(-fx-base-color, 60%);   /* Significantly lightened, pale color */
}
```

### 8.3 ladder() 函数

`ladder()` 根据背景亮度自动选择前景色，实现自适应的文本颜色：

```css
.label {
    /* Based on background brightness, choose text color between white and black */
    -fx-text-fill: ladder(-fx-background-color,
        white 49%,
        black 50%);
}
```

含义：当背景亮度低于 49% 时使用白色文本；高于 50% 时使用黑色文本。

---

## 9. Scene Builder 集成

Scene Builder 是官方可视化 FXML 设计工具，支持 CSS 样式的实时预览。

### 9.1 加载 CSS 文件

1. 在 Scene Builder 中打开 FXML 文件。
2. 在左侧 **Documents** 面板中，选择 **Controller** 或选择根节点。
3. 在右侧 **Properties** 面板中，找到 **Stylesheets** 属性。
4. 点击 `+` 添加 CSS 文件（CSS 文件必须首先放入资源目录）。

### 9.2 实时预览

加载 CSS 后，Scene Builder 会实时渲染样式效果，方便进行可视化调整。

### 9.3 使用样式类

1. 选择一个控件。
2. 在 **Properties** 面板中，找到 **Style Class** 字段。
3. 输入样式类名（例如 `button-primary`）；多个类之间用空格分隔。

### 9.4 内联样式调试

在 **Properties** 面板的 **Style** 字段中输入内联 CSS 以快速测试：

```
-fx-background-color: #ff0000; -fx-text-fill: white;
```

### 9.5 推荐工作流

1. 在 Scene Builder 中构建 FXML 布局并分配样式类名。
2. 在外部编辑器（IDE）中编写 CSS 文件。
3. 返回 Scene Builder 加载 CSS 并预览效果。
4. 迭代调整 CSS 与布局。

---

## 10. 运行时主题切换

实现浅色 / 深色主题之间的动态切换，无需重启应用。

### 10.1 准备主题 CSS 文件

```
resources/
|-- css/
|   |-- light-theme.css   # Light theme variables
|   |-- dark-theme.css    # Dark theme variables
|   |-- controls.css      # Control styles (reference variables, theme-agnostic)
```

### 10.2 主题切换代码

```java
package com.example.theme;

import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;

public class ThemeManager {

    private static final String LIGHT_THEME = "/css/light-theme.css";
    private static final String DARK_THEME = "/css/dark-theme.css";
    private static final String CONTROLS = "/css/controls.css";

    private final Scene scene;
    private boolean darkMode = false;

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // Initially load light theme + control styles
        applyTheme(false);
    }

    /** Switch theme */
    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
    }

    private void applyTheme(boolean dark) {
        scene.getStylesheets().clear();
        // Load theme variable file first, then control styles
        scene.getStylesheets().add(
            getClass().getResource(dark ? DARK_THEME : LIGHT_THEME).toExternalForm());
        scene.getStylesheets().add(
            getClass().getResource(CONTROLS).toExternalForm());
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
```

### 10.3 在 Controller 中绑定切换按钮

```java
public class MainController implements Initializable {

    @FXML private ToggleButton themeToggle;
    private ThemeManager themeManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        themeManager = new ThemeManager(themeToggle.getScene());
        themeToggle.setText("Switch to dark theme");
        themeToggle.setOnAction(e -> {
            themeManager.toggleTheme();
            themeToggle.setText(themeManager.isDarkMode()
                ? "Switch to light theme" : "Switch to dark theme");
        });
    }
}
```

### 10.4 持久化用户主题偏好

```java
public class ThemeManager {
    private static final String PREF_KEY = "app.theme.dark";

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // Read last selection from Preferences
        darkMode = Preferences.userNodeForPackage(ThemeManager.class)
            .getBoolean(PREF_KEY, false);
        applyTheme(darkMode);
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme(darkMode);
        // Persist save
        Preferences.userNodeForPackage(ThemeManager.class)
            .putBoolean(PREF_KEY, darkMode);
    }
}
```

### 10.5 平滑过渡（可选）

在切换主题时给根节点添加淡入淡出效果：

```java
private void applyThemeWithTransition(boolean dark) {
    FadeTransition fadeOut = new FadeTransition(Duration.millis(150), scene.getRoot());
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.3);
    fadeOut.setOnFinished(e -> {
        applyTheme(dark);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), scene.getRoot());
        fadeIn.setFromValue(0.3);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    });
    fadeOut.play();
}
```

---

## 11. @import 与拆分大型 CSS 文件

JavaFX CSS 自 **8u20** 起支持 `@import` 规则，可用于将大型 CSS 文件拆分为多个模块，便于维护和复用。

```css
/* app.css — main stylesheet, imports modules in order */
@import "variables.css";     /* looked-up colors / theme variables */
@import "buttons.css";       /* button-related styles */
@import "tables.css";        /* table-related styles */
@import "forms.css";         /* form control styles */

.root {
    /* only rules specific to the main stylesheet go here */
}
```

注意事项：

- `@import` 必须位于 CSS 文件的**顶部**（在任何其他规则之前），否则会被忽略。
- 导入路径相对于当前 CSS 文件的位置解析；通过 `getResourceAsStream` 加载时此规则同样适用。
- 只需在 `scene.getStylesheets()` 中注册主样式表；通过 `@import` 引入的文件会自动加载。
- 使用 `@import` 可以将主题变量与控件样式在物理上分离，与第 10 节的运行时主题切换配合使用会更加灵活。

---

## 12. CSS 最佳实践总结

| 实践 | 描述 |
|---------------------------------------|----------------------------------------------------------------------|
| 使用查找颜色管理主题颜色 | 在 `.root` 中集中定义颜色；子节点按名称引用，便于修改和主题切换。 |
| 避免内联样式 | 内联样式优先级最高且难以维护；仅用于动态调试。 |
| 语义化的样式类名 | 使用 `.button-primary` 而非 `.blue-button`，与视觉效果解耦。 |
| 将主题变量与控件样式分离 | 主题文件只定义变量；控件文件引用变量，使主题可插拔。 |
| 使用 derive() 构建配色方案 | 从基础颜色派生 hover/pressed 状态颜色，保持颜色一致性。 |
| 避免过度嵌套的选择器 | 深层选择器层级会降低性能且难以覆盖；保持在 2-3 层以内。 |
| 为所有交互状态提供样式 | 包括 `:hover`、`:pressed`、`:focused`、`:disabled`。 |
| 使用 Scene Builder 进行可视化调试 | 实时预览可提高样式开发效率。 |
| 使用注释分区管理 CSS | 用注释划分代码块（按钮、表格、表单等）以便维护。 |
| 使用 @import 拆分 CSS | 自 8u20 起支持；按模块拆分大型样式表，便于维护和复用。 |
