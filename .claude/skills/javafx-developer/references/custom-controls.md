# 自定义控件开发参考

> 使用 Control/Skin 架构构建可复用自定义 JavaFX 控件的模式。涵盖符合 JavaFX Beans 约定的属性定义、CSS 可样式化属性、基于 Canvas 的渲染、事件处理，以及一个完整的 RatingControl 示例（Control + Skin + CSS）。

## 何时构建自定义控件

当标准 JavaFX UI 控件（`Button`、`TableView`、`ComboBox` 等）无法满足你的需求时，可以构建自定义控件：

| 场景 | 推荐方案 |
|----------|---------------------|
| 将现有控件组合成可复用的布局 | **自定义 Region/Pane**（继承 `StackPane`、`HBox` 等）——更简单，无需 Skin |
| 新的交互行为 + 自定义视觉渲染 | **Control + Skin**——充分发挥 CSS 样式化和行为分离的能力 |
| 高性能自定义绘制（图表、仪表） | **基于 Canvas 的 Control + Skin**——GPU 加速的像素渲染 |
| 替换现有控件的视觉外观 | **Skin 覆盖**——为现有 Control 实现自定义 Skin |

**经验法则**：如果只需要组合现有控件，扩展 `Region` 或布局面板即可。如果需要自定义渲染、CSS 可样式化属性或新颖的交互行为，则使用完整的 Control/Skin 模式。

## Control / Skin 架构

### 关注点分离

JavaFX 遵循严格的类似 MVC 的分离：

```
┌─────────────────────────────────────────────────┐
│  Control (Model + Controller)                   │
│  ├── Public API (properties, methods)           │
│  ├── CSS-stylable properties                    │
│  └── No rendering logic                         │
├─────────────────────────────────────────────────┤
│  Skin (View)                                    │
│  ├── Visual rendering (nodes or canvas)         │
│  ├── Event handling (mouse, keyboard)           │
│  ├── Layout logic                               │
│  └── Bound to Control's properties              │
├─────────────────────────────────────────────────┤
│  CSS (Style)                                    │
│  ├── Property style declarations                │
│  ├── Pseudo-class states (:hover, :pressed)     │
│  └── Loaded via getUserAgentStylesheet()        │
└─────────────────────────────────────────────────┘
```

- **Control**（`extends Control`）：定义公共 API——属性、方法、CSS 元数据。不包含任何渲染逻辑。可将其视为控件用户交互的"接口"。
- **Skin**（`extends SkinBase<MyControl>`）：实现视觉渲染和交互行为。监听 Control 的属性并在其变化时更新视图。可将其视为视觉层的"实现"。
- **CSS**：样式化控件的属性和伪类，允许无需修改代码即可进行主题定制。

### 为何不直接继承 Region？

| 特性 | Region/Pane | Control + Skin |
|---------|-------------|----------------|
| CSS 可样式化自定义属性 | 有限（仅标准属性） | 完整支持（可定义自己的 `-fx-my-color`） |
| 伪类状态（:hover、:disabled） | 仅标准 | 自定义伪类 |
| 焦点遍历 | 手动 | 内置 |
| 换肤/主题切换 | 不可能 | 可以——运行时交换 Skin |
| 工具提示、上下文菜单 | 手动 | 内置 API |
| 复杂度 | 低 | 中 |

## JavaFX 属性（Beans 约定）

自定义控件通过 JavaFX 属性暴露状态。每个公共属性都遵循标准的三个方法模式：

```java
// Private field with lazy initialization
private DoubleProperty rating;

// Getter (returns the property, never null)
public final DoubleProperty ratingProperty() {
    if (rating == null) {
        rating = new SimpleDoubleProperty(this, "rating", 0.0);
    }
    return rating;
}

// Setter (delegates to property)
public final void setRating(double value) {
    ratingProperty().set(value);
}

// Value getter
public final double getRating() {
    return rating == null ? 0.0 : rating.get();
}
```

### 规则

1. **`final` 的 getter/setter**：属性 getter/setter 方法必须是 `final`，防止子类化破坏属性契约。
2. **延迟初始化**：属性字段在首次访问前为 `null`——为未使用的属性节省内存。
3. **属性 getter 永不为 null**：`ratingProperty()` 必须始终返回非 null 的 `Property` 对象（在首次访问时初始化）。
4. **构造函数名称**：将 bean 名称（字符串）作为第二个参数传给 `SimpleXxxProperty`——CSS 和工具会用到它。

### 只读属性

对于不应由外部设置的、计算得到或内部状态：

```java
private ReadOnlyDoubleWrapper percentage;  // wrapper, not the read-only version

public final ReadOnlyDoubleProperty percentageProperty() {
    if (percentage == null) {
        percentage = new ReadOnlyDoubleWrapper(this, "percentage", 0.0);
    }
    return percentage.getReadOnlyProperty();  // expose read-only view
}

public final double getPercentage() {
    return percentage == null ? 0.0 : percentage.get();
}
// No setter — external code cannot change this
```

## CSS 可样式化属性

自定义控件可以定义自己的 CSS 属性（例如 `-fx-star-color`），设计者可以通过 CSS 进行样式化，而无需改动 Java 代码。

### 第 1 步：定义 StyleableProperty 字段

```java
// StyleableProperty fields — CSS-stylable versions of regular properties
private StyleableObjectProperty<Paint> starColor;
private StyleableObjectProperty<Paint> starBorderColor;
private StyleableDoubleProperty starSize;
```

### 第 2 步：定义 CssMetaData

每个 CSS 属性都需要 `CssMetaData`，它告诉 JavaFX 如何将 CSS 值应用到属性：

```java
// CSS metadata for -fx-star-color
private static final CssMetaData<RatingControl, Paint> STAR_COLOR =
        new CssMetaData<RatingControl, Paint>("-fx-star-color",
                StyleConverter.getPaintConverter(), Color.GOLD) {

            @Override
            public boolean isSettable(RatingControl control) {
                return control.starColor == null || !control.starColor.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(RatingControl control) {
                return control.starColorProperty();
            }
        };
```

- **`isSettable`**：如果 CSS 可以设置该属性（即未绑定），则返回 `true`。如果开发者以编程方式绑定了属性，CSS 将不会覆盖它。
- **`getStyleableProperty`**：从控件实例返回实际的 `StyleableProperty` 字段。
- **默认值**（`Color.GOLD`）：当没有 CSS 规则指定该属性时使用。

### 第 3 步：聚合 CssMetaData

将所有 CSS 元数据收集到一个列表中，包括父类的元数据：

```java
private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

static {
    List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(
            Control.getClassCssMetaData());  // inherit parent's CSS properties
    list.add(STAR_COLOR);
    list.add(STAR_BORDER_COLOR);
    list.add(STAR_SIZE);
    STYLEABLES = Collections.unmodifiableList(list);
}

@Override
public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
    return STYLEABLES;
}
```

### 第 4 步：在 CSS 中使用

```css
.rating-control {
    -fx-star-color: gold;
    -fx-star-border-color: #999;
    -fx-star-size: 24;
}

.rating-control:readonly {
    -fx-star-color: #ccc;
}
```

## 伪类

伪类表示视觉状态（`:hover`、`:pressed`、`:disabled`、`:readonly`）。为控件的状态定义自定义伪类：

```java
// Define a pseudo-class "readonly"
private static final PseudoClass READONLY_PSEUDO_CLASS =
        PseudoClass.getPseudoClass("readonly");

// Update pseudo-class state when the property changes
private void updatePseudoClassState() {
    pseudoClassStateChanged(READONLY_PSEUDO_CLASS, isReadOnly());
}

// In the property setter:
public final void setReadOnly(boolean value) {
    readOnlyProperty().set(value);
    updatePseudoClassState();
}
```

CSS 用法：
```css
.rating-control:readonly .star {
    -fx-fill: #ccc;
}
```

## 基于 Canvas 的渲染

对于高性能的自定义绘制（图表、仪表、自定义形状），请使用 `Canvas` 而不是堆叠多个 `Shape` 节点。`Canvas` 提供用于立即模式绘制的 `GraphicsContext`——对复杂视觉来说更快。

### Canvas 与 Shape 节点对比

| 方面 | Shape 节点（Path、Circle 等） | Canvas |
|--------|----------------------------------|--------|
| 性能（10+ 个形状） | 较慢——每个形状都是一个场景图节点 | 更快——单个节点，GPU 加速 |
| CSS 样式化 | 单个形状可独立样式化 | 手动——样式变化时需重绘 |
| 事件处理 | 逐形状命中测试 | 手动命中测试 |
| 最适合 | 简单、少数形状、CSS 样式化 | 复杂、众多形状、高性能 |

### Canvas 绘制模式

```java
public class GaugeSkin extends SkinBase<GaugeControl> {

    private final Canvas canvas;
    private final GraphicsContext gc;

    public GaugeSkin(GaugeControl control) {
        super(control);
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        // Redraw when value changes
        control.valueProperty().addListener((obs, old, val) -> draw());

        // Redraw on resize
        canvas.widthProperty().addListener((obs, old, val) -> draw());
        canvas.heightProperty().addListener((obs, old, val) -> draw());
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        // Read CSS-stylable properties for colors
        GaugeControl control = getSkinnable();
        Paint fillColor = control.getArcColor();
        Paint bgColor = control.getBackgroundColor();

        // Draw arc
        gc.setStroke(bgColor);
        gc.setLineWidth(control.getStrokeWidth());
        gc.strokeArc(10, 10, w - 20, h - 20, 90, 360, ArcType.OPEN);

        gc.setStroke(fillColor);
        double angle = 360 * (control.getValue() / control.getMaxValue());
        gc.strokeArc(10, 10, w - 20, h - 20, 90, -angle, ArcType.OPEN);
    }

    @Override
    protected double computeMinWidth(double height) {
        return 80;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 80;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 200;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 200;
    }

    @Override
    protected void layoutChildren(double contentX, double contentY,
                                   double contentWidth, double contentHeight) {
        // Size the canvas to the control's content area
        canvas.resizeRelocate(contentX, contentY, contentWidth, contentHeight);
        draw();  // redraw after layout
    }
}
```

### Canvas 缩放策略

Canvas 不会随其父级自动缩放。你必须：
1. 在 Skin 中重写 `layoutChildren()` 以调用 `canvas.resizeRelocate()`
2. 为 `canvas.widthProperty()` 和 `canvas.heightProperty()` 添加监听器以触发重绘
3. 在布局完成后调用 `draw()`

## Skin 中的事件处理

Skin 处理鼠标和键盘事件。在构造函数中注册事件处理器：

```java
// Mouse press — start interaction
getSkinnable().setOnMousePressed(event -> {
    if (getSkinnable().isReadOnly()) return;
    double x = event.getX();
    updateValueFromMouse(x);
});

// Mouse drag — continue interaction
getSkinnable().setOnMouseDragged(event -> {
    if (getSkinnable().isReadOnly()) return;
    updateValueFromMouse(event.getX());
});

private void updateValueFromMouse(double mouseX) {
    double width = getWidth();
    double ratio = Math.max(0, Math.min(1, mouseX / width));
    double newValue = ratio * getSkinnable().getMaxValue();
    getSkinnable().setValue(newValue);
}
```

## 焦点处理

接受输入的自定义控件应支持焦点遍历：

```java
public RatingControl() {
    setFocusTraversable(true);
    // Keyboard support
    setOnKeyPressed(event -> {
        if (isReadOnly()) return;
        switch (event.getCode()) {
            case LEFT -> setRating(Math.max(0, getRating() - getIncrement()));
            case RIGHT -> setRating(Math.min(getMaxRating(), getRating() + getIncrement()));
            case SPACE -> setRating(getRating());  // confirm
        }
        event.consume();
    });
}
```

## 完整示例：RatingControl

一个具有 CSS 可样式化颜色、鼠标/键盘交互、只读模式和基于 SVG 星形渲染的星级评分控件。

### RatingControl.java（Control）

```java
package com.example.app.controls;

import javafx.beans.property.*;
import javafx.css.*;
import javafx.scene.control.Control;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RatingControl extends Control {

    // === Regular Properties ===

    // rating (0 to maxRating)
    private DoubleProperty rating;
    public final DoubleProperty ratingProperty() {
        if (rating == null) {
            rating = new SimpleDoubleProperty(this, "rating", 0.0) {
                @Override
                public void set(double newValue) {
                    double clamped = Math.max(0, Math.min(getMaxRating(), newValue));
                    super.set(clamped);
                }
            };
        }
        return rating;
    }
    public final void setRating(double value) { ratingProperty().set(value); }
    public final double getRating() { return rating == null ? 0.0 : rating.get(); }

    // maxRating (default 5)
    private DoubleProperty maxRating;
    public final DoubleProperty maxRatingProperty() {
        if (maxRating == null) {
            maxRating = new SimpleDoubleProperty(this, "maxRating", 5.0);
        }
        return maxRating;
    }
    public final void setMaxRating(double value) { maxRatingProperty().set(value); }
    public final double getMaxRating() { return maxRating == null ? 5.0 : maxRating.get(); }

    // readOnly
    private BooleanProperty readOnly;
    public final BooleanProperty readOnlyProperty() {
        if (readOnly == null) {
            readOnly = new SimpleBooleanProperty(this, "readOnly", false);
            readOnly.addListener((obs, old, val) -> {
                pseudoClassStateChanged(READONLY_PSEUDO_CLASS, val);
                setFocusTraversable(!val);
            });
        }
        return readOnly;
    }
    public final void setReadOnly(boolean value) { readOnlyProperty().set(value); }
    public final boolean isReadOnly() { return readOnly == null ? false : readOnly.get(); }

    // increment (keyboard step)
    private DoubleProperty increment;
    public final DoubleProperty incrementProperty() {
        if (increment == null) {
            increment = new SimpleDoubleProperty(this, "increment", 0.5);
        }
        return increment;
    }
    public final void setIncrement(double value) { incrementProperty().set(value); }
    public final double getIncrement() { return increment == null ? 0.5 : increment.get(); }

    // === CSS-Stylable Properties ===

    // -fx-star-color (filled star color)
    private StyleableObjectProperty<Paint> starColor;
    public final StyleableObjectProperty<Paint> starColorProperty() {
        if (starColor == null) {
            starColor = new SimpleStyleableObjectProperty<>(STAR_COLOR, this, "starColor", Color.GOLD);
        }
        return starColor;
    }
    public final void setStarColor(Paint value) { starColorProperty().set(value); }
    public final Paint getStarColor() { return starColor == null ? Color.GOLD : starColor.get(); }

    // -fx-star-border-color (unfilled star border)
    private StyleableObjectProperty<Paint> starBorderColor;
    public final StyleableObjectProperty<Paint> starBorderColorProperty() {
        if (starBorderColor == null) {
            starBorderColor = new SimpleStyleableObjectProperty<>(STAR_BORDER_COLOR, this,
                    "starBorderColor", Color.web("#cccccc"));
        }
        return starBorderColor;
    }
    public final void setStarBorderColor(Paint value) { starBorderColorProperty().set(value); }
    public final Paint getStarBorderColor() {
        return starBorderColor == null ? Color.web("#cccccc") : starBorderColor.get();
    }

    // -fx-star-size (star size in pixels)
    private StyleableDoubleProperty starSize;
    public final StyleableDoubleProperty starSizeProperty() {
        if (starSize == null) {
            starSize = new SimpleStyleableDoubleProperty(STAR_SIZE, this, "starSize", 24.0);
        }
        return starSize;
    }
    public final void setStarSize(double value) { starSizeProperty().set(value); }
    public final double getStarSize() { return starSize == null ? 24.0 : starSize.get(); }

    // === Pseudo-Class ===

    private static final PseudoClass READONLY_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("readonly");

    // === CSS Metadata ===

    private static final CssMetaData<RatingControl, Paint> STAR_COLOR =
            new CssMetaData<>("-fx-star-color", StyleConverter.getPaintConverter(), Color.GOLD) {
                @Override
                public boolean isSettable(RatingControl control) {
                    return control.starColor == null || !control.starColor.isBound();
                }
                @Override
                public StyleableProperty<Paint> getStyleableProperty(RatingControl control) {
                    return control.starColorProperty();
                }
            };

    private static final CssMetaData<RatingControl, Paint> STAR_BORDER_COLOR =
            new CssMetaData<>("-fx-star-border-color", StyleConverter.getPaintConverter(),
                    Color.web("#cccccc")) {
                @Override
                public boolean isSettable(RatingControl control) {
                    return control.starBorderColor == null || !control.starBorderColor.isBound();
                }
                @Override
                public StyleableProperty<Paint> getStyleableProperty(RatingControl control) {
                    return control.starBorderColorProperty();
                }
            };

    private static final CssMetaData<RatingControl, Number> STAR_SIZE =
            new CssMetaData<>("-fx-star-size", StyleConverter.getSizeConverter(), 24.0) {
                @Override
                public boolean isSettable(RatingControl control) {
                    return control.starSize == null || !control.starSize.isBound();
                }
                @Override
                public StyleableProperty<Number> getStyleableProperty(RatingControl control) {
                    return control.starSizeProperty();
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Control.getClassCssMetaData());
        list.add(STAR_COLOR);
        list.add(STAR_BORDER_COLOR);
        list.add(STAR_SIZE);
        STYLEABLES = Collections.unmodifiableList(list);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return STYLEABLES;
    }

    // === Constructor ===

    public RatingControl() {
        getStyleClass().setAll("rating-control");
        setFocusTraversable(true);

        // Clamp rating when maxRating changes
        maxRatingProperty().addListener((obs, old, val) -> {
            if (getRating() > val.doubleValue()) {
                setRating(val.doubleValue());
            }
        });

        // Keyboard navigation
        setOnKeyPressed(event -> {
            if (isReadOnly()) return;
            switch (event.getCode()) {
                case LEFT  -> setRating(Math.max(0, getRating() - getIncrement()));
                case RIGHT -> setRating(Math.min(getMaxRating(), getRating() + getIncrement()));
                default -> { return; }
            }
            event.consume();
        });
    }

    // === Skin ===

    @Override
    protected javafx.scene.control.Skin<?> createDefaultSkin() {
        return new RatingSkin(this);
    }

    // === Stylesheet ===

    @Override
    public String getUserAgentStylesheet() {
        return RatingControl.class.getResource("/css/rating-control.css").toExternalForm();
    }
}
```

### RatingSkin.java（Skin）

```java
package com.example.app.controls;

import javafx.beans.binding.Bindings;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeType;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class RatingSkin extends SkinBase<RatingControl> {

    private final HBox container;
    private final List<SVGPath> stars;

    public RatingSkin(RatingControl control) {
        super(control);
        this.container = new HBox();
        this.stars = new ArrayList<>();
        getChildren().add(container);

        // Build star shapes
        rebuildStars();

        // Rebuild when maxRating changes
        control.maxRatingProperty().addListener((obs, old, val) -> rebuildStars());

        // Update visual when rating or colors change
        control.ratingProperty().addListener((obs, old, val) -> updateDisplay());
        control.starColorProperty().addListener((obs, old, val) -> updateDisplay());
        control.starBorderColorProperty().addListener((obs, old, val) -> updateDisplay());
        control.starSizeProperty().addListener((obs, old, val) -> updateDisplay());
        control.readOnlyProperty().addListener((obs, old, val) -> updateDisplay());

        // Mouse interaction
        container.setOnMousePressed(this::handleMouse);
        container.setOnMouseDragged(this::handleMouse);
    }

    private void rebuildStars() {
        container.getChildren().clear();
        stars.clear();

        int count = (int) Math.round(getSkinnable().getMaxRating());
        double size = getSkinnable().getStarSize();

        for (int i = 0; i < count; i++) {
            SVGPath star = new SVGPath();
            star.setContent(STAR_PATH);
            star.setStrokeType(StrokeType.INSIDE);
            star.setStrokeWidth(1);
            star.setScaleX(size / 24.0);
            star.setScaleY(size / 24.0);
            stars.add(star);
            container.getChildren().add(star);
        }
        updateDisplay();
    }

    private void updateDisplay() {
        RatingControl control = getSkinnable();
        double rating = control.getRating();
        Paint fillColor = control.getStarColor();
        Paint borderColor = control.getStarBorderColor();

        for (int i = 0; i < stars.size(); i++) {
            SVGPath star = stars.get(i);
            double starValue = i + 1;

            if (starValue <= rating) {
                // Fully filled
                star.setFill(fillColor);
                star.setStroke(fillColor);
            } else if (starValue - 0.5 <= rating && control.getIncrement() <= 0.5) {
                // Half filled (for 0.5 increment)
                star.setFill(fillColor);
                star.setStroke(borderColor);
                star.setOpacity(0.6);
            } else {
                // Empty
                star.setFill(javafx.scene.paint.Color.TRANSPARENT);
                star.setStroke(borderColor);
                star.setOpacity(1.0);
            }
        }
    }

    private void handleMouse(MouseEvent event) {
        RatingControl control = getSkinnable();
        if (control.isReadOnly()) return;

        double width = container.getWidth();
        if (width <= 0) return;

        double ratio = Math.max(0, Math.min(1, event.getX() / width));
        double newValue = ratio * control.getMaxRating();

        // Snap to increment
        double increment = control.getIncrement();
        newValue = Math.round(newValue / increment) * increment;

        control.setRating(newValue);
    }

    @Override
    protected double computeMinWidth(double height) {
        return getSkinnable().getStarSize() * getSkinnable().getMaxRating() + 4;
    }

    @Override
    protected double computeMinHeight(double width) {
        return getSkinnable().getStarSize() + 4;
    }

    @Override
    protected double computePrefWidth(double height) {
        return computeMinWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return computeMinHeight(width);
    }

    // SVG path for a 5-pointed star (24x24 viewport)
    private static final String STAR_PATH =
            "M12 2L14.59 8.41L21 9.27L16 14.14L17.18 21.02L12 17.77L6.82 21.02L8 14.14L3 9.27L9.41 8.41Z";
}
```

### rating-control.css

```css
.rating-control {
    -fx-star-color: gold;
    -fx-star-border-color: #cccccc;
    -fx-star-size: 24;
    -fx-padding: 2;
    -fx-spacing: 2;
}

/* Hover effect */
.rating-control:hover {
    -fx-cursor: hand;
}

/* Read-only state */
.rating-control:readonly {
    -fx-star-color: #b0b0b0;
    -fx-cursor: default;
}

/* Disabled state */
.rating-control:disabled {
    -fx-opacity: 0.5;
}

/* Focused state */
.rating-control:focused {
    -fx-background-color: -fx-focus-color;
    -fx-background-insets: -2;
    -fx-background-radius: 3;
}
```

### FXML 用法

```xml
<?import com.example.app.controls.RatingControl?>

<RatingControl fx:id="ratingControl"
               rating="3.5"
               maxRating="5"
               increment="0.5"
               starColor="gold"
               starBorderColor="#ccc"
               starSize="24"/>
```

### Java 用法

```java
RatingControl rating = new RatingControl();
rating.setRating(3.5);
rating.setMaxRating(5);
rating.setIncrement(0.5);
rating.setReadOnly(false);

// Listen to changes
rating.ratingProperty().addListener((obs, old, val) -> {
    System.out.println("New rating: " + val);
});
```

## 控件模板检查清单

构建新的自定义控件时，请遵循此清单：

1. [ ] **Control 类**（`extends Control`）
   - [ ] 定义常规属性（DoubleProperty、BooleanProperty 等），使用延迟初始化 + final getter/setter
   - [ ] 定义 CSS 可样式化属性（StyleableObjectProperty、StyleableDoubleProperty），使用延迟初始化
   - [ ] 为每个可样式化属性定义 CssMetaData（isSettable + getStyleableProperty）
   - [ ] 在静态块中聚合 CssMetaData + 重写 `getControlCssMetaData()`
   - [ ] 定义伪类（PseudoClass.getPseudoClass）+ 在属性变化时更新
   - [ ] 构造函数：设置 styleClass、setFocusTraversable、注册键盘处理器
   - [ ] 重写 `createDefaultSkin()` → 返回你的 Skin
   - [ ] 重写 `getUserAgentStylesheet()` → 返回 CSS 资源 URL
2. [ ] **Skin 类**（`extends SkinBase<MyControl>`）
   - [ ] 构造函数：创建视觉节点、注册事件处理器、绑定到属性
   - [ ] 属性监听器：当控件属性变化时调用 `updateDisplay()`
   - [ ] 事件处理器：鼠标按下/拖拽、键盘
   - [ ] 重写 `computeMinWidth/Height` 和 `computePrefWidth/Height`
   - [ ] 如果使用 Canvas，重写 `layoutChildren()`（调用 resizeRelocate + draw）
3. [ ] **CSS 文件**
   - [ ] 所有可样式化属性的默认值
   - [ ] 伪类状态（:readonly、:disabled、:focused、:hover）
   - [ ] 放置在 `resources/css/{control-name}.css`
4. [ ] **资源**
   - [ ] CSS 文件位于 classpath 的 `/css/{control-name}.css`
   - [ ] 任何 SVG 路径或图片

## 常见陷阱

1. **非 final 的属性方法**：如果 getter/setter 不是 `final`，子类化可能破坏属性契约——始终使用 `final`
2. **属性过早初始化**：在构造函数中初始化所有属性会为用户从未访问过的属性浪费内存——使用延迟初始化
3. **忘记 isSettable**：如果 `isSettable()` 总是返回 `true`，CSS 会覆盖编程式绑定——检查 `!property.isBound()`
4. **Canvas 不缩放**：Canvas 不会自动缩放——必须在 `layoutChildren()` 中调用 `resizeRelocate()`，并在宽度/高度变化时重绘
5. **没有 min/pref 尺寸覆盖**：如果没有 `computeMinWidth/Height` 和 `computePrefWidth/Height`，控件在布局中可能为零尺寸
6. **伪类未更新**：设置一个应改变伪类状态的属性，却没有调用 `pseudoClassStateChanged()`——CSS 不会反映新状态
7. **只在 Skin 的节点上注册事件处理器**：如果在 Skin 的根节点上注册鼠标处理器，但 Control 有内边距，则在内边距区域内的点击不会生效——请在 Control 本身上注册
8. **样式表路径错误**：`getUserAgentStylesheet()` 必须返回有效的 URL——在部署前用 `getResource()` 返回非 null 来测试
