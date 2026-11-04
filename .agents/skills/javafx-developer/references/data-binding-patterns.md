# JavaFX 数据绑定模式指南

本指南全面介绍 JavaFX 的属性系统、单向与双向绑定、计算绑定、数字绑定、ObservableList/Map、TableView 数据绑定、表单校验模式、内存管理，以及 JavaFX 21+ 基于订阅的监听器。

---

## 1. 属性类型系统

JavaFX 的核心是一个带类型的属性系统，每种基本数据类型都有对应的 Property 接口和实现。

### 1.1 属性类型参考表

| 数据类型 | Property 接口 | 简单实现类 | 只读包装类 |
|------------|--------------------------|-----------------------------|------------------------------|
| String | `StringProperty` | `SimpleStringProperty` | `ReadOnlyStringWrapper` |
| int | `IntegerProperty` | `SimpleIntegerProperty` | `ReadOnlyIntegerWrapper` |
| long | `LongProperty` | `SimpleLongProperty` | `ReadOnlyLongWrapper` |
| float | `FloatProperty` | `SimpleFloatProperty` | `ReadOnlyFloatWrapper` |
| double | `DoubleProperty` | `SimpleDoubleProperty` | `ReadOnlyDoubleWrapper` |
| boolean | `BooleanProperty` | `SimpleBooleanProperty` | `ReadOnlyBooleanWrapper` |
| 任意对象 | `ObjectProperty<T>` | `SimpleObjectProperty<T>` | `ReadOnlyObjectWrapper<T>` |
| List | `ListProperty<E>` | `SimpleListProperty<E>` | `ReadOnlyListWrapper<E>` |
| Map | `MapProperty<K,V>` | `SimpleMapProperty<K,V>` | `ReadOnlyMapWrapper<K,V>` |
| Set | `SetProperty<E>` | `SimpleSetProperty<E>` | `ReadOnlySetWrapper<E>` |

### 1.2 SimpleXxxProperty 基本用法

```java
public class Person {
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final IntegerProperty age = new SimpleIntegerProperty(this, "age", 0);
    private final DoubleProperty salary = new SimpleDoubleProperty(this, "salary", 0.0);
    private final BooleanProperty active = new SimpleBooleanProperty(this, "active", true);
    private final ObjectProperty<LocalDate> birthday =
        new SimpleObjectProperty<>(this, "birthday", LocalDate.now());

    // Constructor parameter description: (bean, name, initialValue)
    // bean is usually the object the property belongs to (this), for property traceability
    // name is the property name, used for debugging and reflection

    // Standard Property accessor pattern
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }

    public IntegerProperty ageProperty() { return age; }
    public int getAge() { return age.get(); }
    public void setAge(int value) { age.set(value); }

    public DoubleProperty salaryProperty() { return salary; }
    public double getSalary() { return salary.get(); }
    public void setSalary(double value) { salary.set(value); }

    public BooleanProperty activeProperty() { return active; }
    public boolean isActive() { return active.get(); }
    public void setActive(boolean value) { active.set(value); }

    public ObjectProperty<LocalDate> birthdayProperty() { return birthday; }
    public LocalDate getBirthday() { return birthday.get(); }
    public void setBirthday(LocalDate value) { birthday.set(value); }
}
```

### 1.3 SimpleXxxProperty 与 ReadOnlyXxxProperty

当一个属性只允许内部修改、对外只读时，使用 `ReadOnlyXxxWrapper` 暴露只读视图。

```java
public class Account {
    // Internally writable Wrapper
    private final ReadOnlyDoubleWrapper balance = new ReadOnlyDoubleWrapper(this, "balance", 0.0);

    // Expose read-only Property externally
    public ReadOnlyDoubleProperty balanceProperty() { return balance.getReadOnlyProperty(); }
    public double getBalance() { return balance.get(); }

    // Only internal methods can modify
    public void deposit(double amount) {
        balance.set(balance.get() + amount);
    }

    public void withdraw(double amount) {
        if (amount > balance.get()) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance.set(balance.get() - amount);
    }
}
```

> 关键区别：`SimpleXxxProperty` 对外完全可读写；`ReadOnlyXxxWrapper` 通过 `getReadOnlyProperty()` 返回只读视图，外部代码不能调用 `set()`，但内部代码仍可通过 wrapper 修改。

---

## 2. 单向绑定

单向绑定使目标属性自动跟随源属性的变化，方向为**源 → 目标**。目标变为只读。

### 2.1 基本单向绑定

```java
StringProperty source = new SimpleStringProperty("Hello");
StringProperty target = new SimpleStringProperty();

// target is bound to source: when source changes, target updates automatically
target.bind(source);

System.out.println(target.get());  // "Hello"
source.set("World");
System.out.println(target.get());  // "World"

// After binding, target becomes read-only, calling set() will throw RuntimeException
// target.set("Error");  // ❌ throws exception
```

### 2.2 UI 控件单向绑定示例

```java
// Label displays the content of TextField (unidirectional)
label.textProperty().bind(textField.textProperty());

// Progress bar bound to progress value
progressBar.progressProperty().bind(task.progressProperty());

// Label displays the slider's current value
valueLabel.textProperty().bind(
    slider.valueProperty().asString("Current value: %.1f"));
```

---

## 3. 双向绑定

双向绑定使两个属性相互同步，其中任一属性变化都会更新另一个。

### 3.1 基本双向绑定

```java
StringProperty propA = new SimpleStringProperty("A");
StringProperty propB = new SimpleStringProperty("B");

propA.bindBidirectional(propB);

System.out.println(propA.get());  // "A" (keeps original value, no immediate sync)
propB.set("NewValue");
System.out.println(propA.get());  // "NewValue"

propA.set("Another");
System.out.println(propB.get());  // "Another"
```

### 3.2 带类型转换的双向绑定

不同类型属性之间的双向绑定需要转换器：

```java
// Bidirectional binding between TextField (String) and IntegerProperty
TextField ageField = new TextField();
IntegerProperty age = new SimpleIntegerProperty(25);

ageField.textProperty().bindBidirectional(age, new NumberStringConverter());

// Now both sync with each other, entering numbers in the input field updates age, modifying age updates the input field
```

### 3.3 自定义格式化的双向绑定

```java
// Bidirectional binding between date and string, with specified format
ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
TextField dateField = new TextField();

dateField.textProperty().bindBidirectional(date, new StringConverter<>() {
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String toString(LocalDate date) {
        return date == null ? "" : date.format(fmt);
    }

    @Override
    public LocalDate fromString(String text) {
        return (text == null || text.isEmpty()) ? null : LocalDate.parse(text, fmt);
    }
});
```

### 3.4 内容绑定

内容绑定用于同步两个 `ObservableList` / `ObservableMap` / `ObservableSet` 的**内容**，而不是将一个属性绑定到另一个属性。它通过 `bindContent()`（单向）和 `bindContentBidirectional()`（双向）实现，常用于多个视图共享同一数据源或主从列表同步。

> 注意：内容绑定作用于集合本身；调用方必须是 `ListProperty` / `MapProperty` 等（通过 `SimpleListProperty` 包装）。普通的 `ObservableList` 不能直接支持 `bindContent`。

```java
ObservableList<String> sourceList = FXCollections.observableArrayList("A", "B", "C");
ListProperty<String> targetList = new SimpleListProperty<>(FXCollections.observableArrayList());

// Unidirectional content binding: changes in the source list sync to the target list (target becomes read-only)
targetList.bindContent(sourceList);

sourceList.add("D");
System.out.println(targetList.get());  // [A, B, C, D]
```

```java
ListProperty<String> list1 = new SimpleListProperty<>(FXCollections.observableArrayList("1", "2"));
ListProperty<String> list2 = new SimpleListProperty<>(FXCollections.observableArrayList());

// Bidirectional content binding: the two lists sync both ways
list1.bindContentBidirectional(list2);

list2.add("3");
System.out.println(list1.get());  // [1, 2, 3]

list1.remove("1");
System.out.println(list2.get());  // [2, 3]
```

**适用场景**

- **多个视图共享同一数据源**：主数据列表与多个子视图的局部列表保持同步；任一视图修改数据，其他视图自动更新。
- **主从列表同步**：由主表选择驱动的详情列表与缓存列表双向绑定，避免手动复制元素。
- **聚合视图**：将多个源列表的内容汇集到一个显示列表中。

> 与普通属性绑定的区别：`bindContent` 同步的是集合元素；源列表中添加/移除元素会反映到目标。但如果源列表的元素自身修改了自己的属性，必须使用 extractor 才能触发更新。

---

## 4. 计算绑定

`Bindings` 工具类提供工厂方法，用于基于一个或多个源属性创建派生的只读绑定。

### 4.1 createStringBinding

```java
StringProperty firstName = new SimpleStringProperty("John");
StringProperty lastName = new SimpleStringProperty("Doe");

// Concatenate full name, depends on firstName and lastName
StringBinding fullName = Bindings.createStringBinding(
    () -> firstName.get() + lastName.get(),
    firstName, lastName
);

System.out.println(fullName.get());  // "JohnDoe"
lastName.set("Smith");
System.out.println(fullName.get());  // "JohnSmith"
```

### 4.2 createBooleanBinding

```java
StringProperty username = new SimpleStringProperty();
StringProperty password = new SimpleStringProperty();

// Whether the form is valid
BooleanBinding formValid = Bindings.createBooleanBinding(
    () -> username.get() != null && username.get().length() >= 3
       && password.get() != null && password.get().length() >= 6,
    username, password
);

// Bind to button disabled state
loginButton.disableProperty().bind(formValid.not());
```

### 4.3 createIntegerBinding / createObjectBinding

```java
ObservableList<Item> items = FXCollections.observableArrayList();

// Count the number of completed items in the list
IntegerBinding completedCount = Bindings.createIntegerBinding(
    () -> (int) items.stream().filter(Item::isCompleted).count(),
    items
);

// Compute the object of the selected item
ObjectProperty<Item> selected = new SimpleObjectProperty<>();
ObjectBinding<String> selectedName = Bindings.createObjectBinding(
    () -> selected.get() == null ? "None selected" : selected.get().getName(),
    selected
);
```

### 4.4 selectBinding（嵌套属性访问）

```java
// Access properties of nested objects
ObjectProperty<Person> person = new SimpleObjectProperty<>(new Person("Alice"));

// Method 1: Bindings.select (string-based, deprecated but works)
StringBinding name1 = Bindings.selectString(person, "name");

// Method 2 (recommended): createStringBinding with null safety
StringBinding name2 = Bindings.createStringBinding(
    () -> person.get() == null ? "" : person.get().getName(),
    person
);
// Automatically updates when person or person.name changes
```

---

## 5. 数字绑定

数值属性支持算术运算绑定，结果是 `NumberBinding`。

### 5.1 算术运算

```java
IntegerProperty quantity = new SimpleIntegerProperty(5);
DoubleProperty price = new SimpleDoubleProperty(19.99);

// Multiplication: total price
NumberBinding total = quantity.multiply(price);
System.out.println(total.doubleValue());  // 99.95

// Addition
IntegerProperty a = new SimpleIntegerProperty(10);
IntegerProperty b = new SimpleIntegerProperty(3);
NumberBinding sum = a.add(b);        // 13
NumberBinding diff = a.subtract(b);  // 7
NumberBinding product = a.multiply(b); // 30
NumberBinding quotient = a.divide(b);  // 3.333...
```

### 5.2 链式运算

```java
DoubleProperty basePrice = new SimpleDoubleProperty(100.0);
DoubleProperty taxRate = new SimpleDoubleProperty(0.08);   // 8% tax rate
DoubleProperty discount = new SimpleDoubleProperty(10.0);  // discount

// Final price = (basePrice - discount) * (1 + taxRate)
NumberBinding finalPrice = basePrice
    .subtract(discount)
    .multiply(taxRate.add(1.0));

System.out.println(finalPrice.doubleValue());  // (100-10) * 1.08 = 97.2
```

### 5.3 数值比较绑定

```java
IntegerProperty stock = new SimpleIntegerProperty(5);
IntegerProperty threshold = new SimpleIntegerProperty(10);

// Whether stock is below threshold
BooleanBinding lowStock = stock.lessThan(threshold);
warningLabel.visibleProperty().bind(lowStock);
```

### 5.4 条件绑定（when/then/otherwise）

```java
IntegerProperty score = new SimpleIntegerProperty(75);

// Return pass/fail text based on score
StringBinding result = Bindings.when(score.greaterThanOrEqualTo(60))
    .then("Pass")
    .otherwise("Fail");
```

---

## 6. ObservableList 与 ListChangeListener

`ObservableList` 是一个可观察列表，任何添加/移除/更新操作都会通知监听器。

### 6.1 创建与基本操作

```java
ObservableList<String> names = FXCollections.observableArrayList();
names.add("Alice");
names.addAll("Bob", "Charlie");
names.remove("Bob");
names.set(0, "Alicia");  // replace
```

### 6.2 ListChangeListener

```java
names.addListener((ListChangeListener<String>) change -> {
    while (change.next()) {
        if (change.wasAdded()) {
            System.out.println("Added: " + change.getAddedSubList());
        }
        if (change.wasRemoved()) {
            System.out.println("Removed: " + change.getRemoved());
        }
        if (change.wasUpdated()) {
            System.out.println("Updated: index " + change.getFrom() + " to " + change.getTo());
        }
        if (change.wasReplaced()) {
            System.out.println("Replace operation");
        }
    }
});
```

### 6.3 使用 Extractor 自动观察元素属性

默认情况下，ObservableList 只监听列表的结构变化（添加/移除）。要监听元素的内部属性变化，需要使用 extractor：

```java
// Also triggers list update events when Task's title or completed property changes
ObservableList<Task> tasks = FXCollections.observableArrayList(
    task -> new Observable[]{ task.titleProperty(), task.completedProperty() }
);

tasks.addListener((ListChangeListener<Task>) c -> {
    while (c.next()) {
        if (c.wasUpdated()) {
            System.out.println("Task property modified: " + c.getList().subList(c.getFrom(), c.getTo()));
        }
    }
});

Task t = new Task("Study");
tasks.add(t);
t.setCompleted(true);  // triggers wasUpdated event
```

---

## 7. ObservableMap 与 MapChangeListener

```java
ObservableMap<String, Integer> scores = FXCollections.observableHashMap();

scores.addListener((MapChangeListener<String, Integer>) change -> {
    if (change.wasAdded()) {
        System.out.println("Added/Updated key: " + change.getKey()
            + " = " + change.getValueAdded());
    }
    if (change.wasRemoved()) {
        System.out.println("Removed key: " + change.getKey()
            + " old value: " + change.getValueRemoved());
    }
});

scores.put("Math", 95);    // triggers wasAdded
scores.put("Math", 98);    // triggers wasAdded + wasRemoved (update)
scores.remove("Math");     // triggers wasRemoved
```

---

## 8. TableView 数据绑定（FilteredList + SortedList）

`FilteredList` 和 `SortedList` 是在 TableView 中实现搜索过滤和排序的标准模式。

### 8.1 完整示例

```java
public class UserTableController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, Integer> ageCol;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> ageFilterCombo;

    // Original data
    private final ObservableList<User> masterData = FXCollections.observableArrayList();

    // Filtered data
    private final FilteredList<User> filteredData = new FilteredList<>(masterData, p -> true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load initial data
        masterData.addAll(
            new User("Alice", 25),
            new User("Bob", 30),
            new User("Charlie", 22),
            new User("David", 35)
        );

        // Sorted data (wrapping FilteredList)
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());

        // Bind to TableView
        userTable.setItems(sortedData);

        // Column mapping
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        ageCol.setCellValueFactory(c -> c.getValue().ageProperty().asObject());

        // Search filter: by name containing keyword
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        ageFilterCombo.getItems().addAll("All", "<30", ">=30");
        ageFilterCombo.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> updateFilter());
    }

    private void updateFilter() {
        String keyword = searchField.getText();
        String ageFilter = ageFilterCombo.getValue();

        filteredData.setPredicate(user -> {
            // Name filter
            boolean nameMatch = keyword == null || keyword.isEmpty()
                || user.getName().toLowerCase().contains(keyword.toLowerCase());

            // Age filter
            boolean ageMatch = true;
            if (">=30".equals(ageFilter)) {
                ageMatch = user.getAge() >= 30;
            } else if ("<30".equals(ageFilter)) {
                ageMatch = user.getAge() < 30;
            }

            return nameMatch && ageMatch;
        });
    }
}
```

### 8.2 数据流

```
masterData (ObservableList)
    ↓ filter
filteredData (FilteredList)
    ↓ sort
sortedData (SortedList)
    ↓ bind
userTable (TableView)
```

> 关键点：`SortedList` 的比较器绑定到 TableView 的比较器，因此点击列头排序会自动生效。

---

## 9. 表单校验模式

使用绑定实现声明式表单校验。

### 9.1 校验绑定示例

```java
public class RegistrationForm {

    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty confirmPassword = new SimpleStringProperty();

    // Validation result for each field
    private final BooleanBinding usernameValid = Bindings.createBooleanBinding(
        () -> username.get() != null && username.get().matches("[a-zA-Z0-9_]{3,20}"),
        username
    );

    private final BooleanBinding emailValid = Bindings.createBooleanBinding(
        () -> email.get() != null && email.get().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$"),
        email
    );

    private final BooleanBinding passwordValid = Bindings.createBooleanBinding(
        () -> password.get() != null && password.get().length() >= 8
           && password.get().matches(".*[A-Z].*") && password.get().matches(".*[0-9].*"),
        password
    );

    private final BooleanBinding passwordsMatch = Bindings.createBooleanBinding(
        () -> password.get() != null && password.get().equals(confirmPassword.get()),
        password, confirmPassword
    );

    // Whether the entire form is valid
    private final BooleanBinding formValid = usernameValid
        .and(emailValid)
        .and(passwordValid)
        .and(passwordsMatch);

    // Error messages
    private final StringBinding usernameError = Bindings.when(usernameValid)
        .then("").otherwise("Username must be 3-20 alphanumeric characters or underscores");
    private final StringBinding emailError = Bindings.when(emailValid)
        .then("").otherwise("Invalid email format");
    private final StringBinding passwordError = Bindings.when(passwordValid)
        .then("").otherwise("Password must be at least 8 characters, with uppercase letters and numbers");
    private final StringBinding confirmError = Bindings.when(passwordsMatch)
        .then("").otherwise("Passwords do not match");

    // Bind to UI in Controller
    public void bindToUI(TextField userField, TextField emailField,
                         TextField passField, TextField confirmField,
                         Label userErrLabel, Label emailErrLabel,
                         Label passErrLabel, Label confirmErrLabel,
                         Button submitButton) {
        userField.textProperty().bindBidirectional(username);
        emailField.textProperty().bindBidirectional(email);
        passField.textProperty().bindBidirectional(password);
        confirmField.textProperty().bindBidirectional(confirmPassword);

        userErrLabel.textProperty().bind(usernameError);
        emailErrLabel.textProperty().bind(emailError);
        passErrLabel.textProperty().bind(passwordError);
        confirmErrLabel.textProperty().bind(confirmError);

        // Error label visibility
        userErrLabel.visibleProperty().bind(usernameValid.not());
        emailErrLabel.visibleProperty().bind(emailValid.not());
        passErrLabel.visibleProperty().bind(passwordValid.not());
        confirmErrLabel.visibleProperty().bind(passwordsMatch.not());

        // Submit button enabled state
        submitButton.disableProperty().bind(formValid.not());
    }
}
```

---

## 10. 内存管理：解绑与监听器移除

绑定和监听器会持有对象引用，不及时清理会导致内存泄漏。

### 10.1 解绑单向绑定

```java
StringProperty target = new SimpleStringProperty();
target.bind(source);

// Unbind
target.unbind();
// After unbinding, target becomes writable again, no longer follows source
```

### 10.2 解绑双向绑定

```java
propA.bindBidirectional(propB);
// Unbind bidirectional binding
propA.unbindBidirectional(propB);
```

### 10.3 移除 ChangeListener / InvalidationListener

```java
ChangeListener<String> listener = (obs, oldVal, newVal) -> {
    System.out.println("Changed: " + oldVal + " -> " + newVal);
};

nameProperty.addListener(listener);

// Must remove the listener with the same reference
nameProperty.removeListener(listener);
```

> 注意：使用 Lambda 表达式添加监听器时，必须保存引用才能移除。匿名内部类同理。

### 10.4 使用 WeakChangeListener 避免泄漏

当无法保证监听器被移除时，使用弱引用监听器：

```java
// WeakChangeListener does not prevent the listener object from being GC'd
nameProperty.addListener(new WeakChangeListener<>((obs, oldVal, newVal) -> {
    System.out.println("Weak listener: " + newVal);
}));
```

> 弱监听器适用于监听器生命周期短于被观察属性的场景。但注意：如果监听器 Lambda 没有其他强引用，可能被过早回收。

### 10.5 常见内存泄漏场景与对策

| 场景 | 对策 |
|---------------------------------------------------------|-------------------------------------------------------------|
| 在 Controller 中添加的监听器未移除 | 在自定义 `dispose()` 方法中移除，通过 `stage.setOnCloseRequest()` 或视图切换回调手动触发 |
| 短生命周期对象监听长生命周期属性 | 使用 `WeakChangeListener` |
| 静态属性上的监听器 | 在应用关闭时显式移除 |
| 双向绑定未解绑 | 不再需要时调用 `unbindBidirectional()` |
| ObservableList extractor 监听器 | 清空或替换列表时注意元素监听器的释放 |

### 10.6 绑定链内存泄漏专题

以下三个是 JavaFX 数据绑定中隐蔽且常见的内存泄漏场景，单独说明以便排查。

#### 10.6.1 Bindings.createXxxBinding() 中的强引用问题

`Bindings.createStringBinding(calc, deps...)` 对传入的所有依赖（deps）以及计算回调中捕获的对象**持有强引用**。如果依赖是长生命周期对象（例如全局 ViewModel、静态属性），而结果绑定被短生命周期节点（例如临时弹窗）订阅，则该节点无法被 GC 回收。

```java
// Leak example: after the popup closes, detailLabel is still held by the binding on the global vm
Label detailLabel = new Label();
detailLabel.textProperty().bind(Bindings.createStringBinding(
    () -> globalVM.currentUserProperty().get().getName(),
    globalVM.currentUserProperty()   // strong reference to a long-lived object
));

// Correct approach: actively unbind when the popup closes
popup.setOnHidden(e -> detailLabel.textProperty().unbind());
```

#### 10.6.2 ObjectProperty<UI 控件> 之间的绑定链阻止 UI 子树 GC

当多个 UI 控件通过 `bind()` 链式绑定（例如 `A.bind(B); B.bind(C);`）时，只要其中任何一个仍存活，链上涉及的所有控件都不会被回收。这在动态构建的菜单、标签页和缓存的节点树中很常见。

```java
// Leak example: the cached panel's content is bound to the main panel's; after the main panel is destroyed, the cached panel still holds the reference
cachedPanel.titleProperty().bind(mainPanel.titleProperty());

// Correct approach: unbind node by node when switching views or destroying
mainPanel.titleProperty().unbind();   // if upstream bindings remain, unbind recursively
```

#### 10.6.3 TableView / ListView cellFactory 中的监听器清理

单元格被高频复用。如果在 `updateItem()` 中给单元格数据添加监听器，但在复用时没有清理，同一个数据对象会被反复监听，监听器无限累积，最终泄漏。

```java
// Leak example: a new listener is added on every reuse and never removed
listView.setCellFactory(lv -> new ListCell<Item>() {
    @Override
    protected void updateItem(Item item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
            item.nameProperty().addListener((obs, o, n) -> setText(n)); // repeated additions, leak!
        }
    }
});

// Correct approach: track the previously listened data and listener, and remove the old listener on reuse
listView.setCellFactory(lv -> new ListCell<Item>() {
    private Item lastItem;
    private ChangeListener<String> nameListener;

    @Override
    protected void updateItem(Item item, boolean empty) {
        super.updateItem(item, empty);
        // first clean up the previous round's listener
        if (lastItem != null && nameListener != null) {
            lastItem.nameProperty().removeListener(nameListener);
        }
        if (empty || item == null) {
            setText(null);
            lastItem = null;
            return;
        }
        setText(item.getName());
        nameListener = (obs, o, n) -> setText(n);
        item.nameProperty().addListener(nameListener);
        lastItem = item;
    }
});
```

> 总结：无论何时使用 `createXxxBinding`、跨控件 `bind` 链，或遇到单元格复用场景，都要确保在节点销毁或复用之前主动 `unbind()` / `removeListener()`；否则绑定链会持续持有 UI 子树的引用。

---

## 11. JavaFX 21+ 基于订阅的监听器

JavaFX 21 引入了基于 `Subscription` 的监听器 API，提供了更现代、更安全的资源管理方式，取代了传统的 `addListener` / `removeListener` 模式。

### 11.1 基本用法

```java
// JavaFX 21+: subscribe returns a Subscription
Subscription subscription = nameProperty.subscribe((obs, oldVal, newVal) -> {
    System.out.println("Name changed: " + oldVal + " -> " + newVal);
});

// Unsubscribe when no longer needed
subscription.unsubscribe();
// After unsubscribing, the listener is automatically removed, and unsubscribe() is idempotent (can be called repeatedly)
```

### 11.2 ObservableValue 的订阅方法

```java
// Subscribe to value changes (with old and new values)
Subscription sub1 = property.subscribe((observable, oldValue, newValue) -> {
    System.out.println("Changed: " + oldValue + " -> " + newValue);
});

// Subscribe to only the new value
Subscription sub2 = property.subscribe(newValue -> {
    System.out.println("New value: " + newValue);
});

// Subscribe to invalidation (invalidity notification)
Subscription sub3 = property.subscribe((InvalidationListener) observable -> {
    System.out.println("Property invalidated");
});
```

### 11.3 ObservableList 的订阅方法

```java
ObservableList<String> list = FXCollections.observableArrayList();

// Subscribe to list changes
Subscription sub = list.subscribe(change -> {
    while (change.next()) {
        if (change.wasAdded()) {
            System.out.println("Added: " + change.getAddedSubList());
        }
        if (change.wasRemoved()) {
            System.out.println("Removed: " + change.getRemoved());
        }
    }
});
```

### 11.4 组合订阅管理

多个 Subscription 可以合并管理：

```java
Subscription sub1 = propA.subscribe(...);
Subscription sub2 = propB.subscribe(...);
Subscription sub3 = list.subscribe(...);

// Merge into a single Subscription
Subscription combined = Subscription.combine(sub1, sub2, sub3);

// Unsubscribe all at once
combined.unsubscribe();
```

### 11.5 try-with-resources 模式

`Subscription` 实现了 `AutoCloseable`，支持 try-with-resources：

```java
try (Subscription sub = property.subscribe(val -> update(val))) {
    // Listener is active within this scope
    doWork();
} // automatically unsubscribes
```

### 11.6 传统 API 与订阅 API 对比

| 特性 | addListener / removeListener | subscribe（Subscription） |
|----------------|-------------------------------------------|-----------------------------------------|
| 移除方式 | 需要保存监听器引用，手动移除 | 调用 `subscription.unsubscribe()` |
| 幂等性 | 重复移除无害，但需注意引用一致性 | `unsubscribe()` 是幂等的，可安全多次调用 |
| 组合管理 | 需要手动逐个移除 | `Subscription.combine()` 一键管理 |
| 资源安全 | 容易忘记移除，导致泄漏 | 配合 try-with-resources 更安全 |
| 可用版本 | 所有 JavaFX 版本 | JavaFX 21+ |

### 11.7 迁移建议

- 新项目（JavaFX 21+）应优先使用 `subscribe` API。
- 需要兼容 JavaFX 17 的项目应继续使用 `addListener`，但必须配合 `removeListener`。
- 混用时保持一致性，同一模块内使用统一风格。

---

## 12. 绑定模式最佳实践总结

| 实践 | 描述 |
|----------------------------------------|-------------------------------------------------------------------|
| 优先使用声明式绑定 | 使用 `bind()` / `Bindings.createXxxBinding()` 而不是手动监听器刷新 |
| 区分单向与双向绑定 | 仅用于显示时用单向，表单输入用双向 |
| 使用 createXxxBinding 处理计算属性 | 避免在监听器中手动计算，用绑定声明依赖 |
| 使用 NumberBinding 处理数值运算 | 链式算术绑定清晰表达计算逻辑 |
| TableView 使用 FilteredList+SortedList | 标准过滤和排序模式，避免手动操作列表 |
| 表单校验使用 BooleanBinding 组合 | 声明式校验，自动驱动 UI 状态 |
| 及时解绑并移除监听器 | 防止内存泄漏，JavaFX 21+ 优先使用 Subscription |
| 使用 extractor 观察元素属性 | ObservableList 需要 extractor 才能监听元素内部变化 |
| 只读属性使用 ReadOnlyXxxWrapper | 保护内部状态，只暴露只读视图 |
