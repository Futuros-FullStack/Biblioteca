# JavaFX 数据库集成指南

本指南涵盖 JavaFX 应用的数据库层集成，包括 JPA/Hibernate、MyBatis、Spring Data JPA、Flyway 迁移、HikariCP 连接池、围绕 UI 线程上的线程安全与事务管理的常见陷阱，以及数据库 Entity 与 JavaFX Property/Bean 模式的集成。它扩展了 `spring-boot-integration.md`（第 8 节），提供更深入、更侧重数据库的指导。

---

## 1. 何时需要添加数据库层

JavaFX 桌面应用在需要跨会话持久化数据、处理非平凡的关系数据，或与其他系统共享状态时，就需要一个数据库层。典型场景：持久化到 SQLite/H2 的本地桌面应用、由远程 MySQL/PostgreSQL 服务器支撑的业务客户端，或同步到本地存储的离线优先应用。

**框架选型**：

| 场景 | 推荐技术栈 | 理由 |
|----------|-------------------|-----------|
| 小型本地应用，手写 SQL | JDBC + HikariCP | 占用最小，无 ORM 开销 |
| 中型应用，团队熟悉 SQL | MyBatis + MyBatis-Spring | 完全掌控 SQL，映射简单 |
| 复杂领域模型，快速 CRUD | Spring Data JPA + Hibernate | 样板代码少，仓储抽象 |
| 表结构频繁演进 | 上述 + Flyway | 版本化、可重复的迁移 |

> **默认推荐**：对于 Spring Boot + JavaFX 项目，优先选择 Spring Data JPA + Flyway + HikariCP。对于纯 JavaFX（无 Spring）项目，优先选择 MyBatis 或带 HikariCP 的纯 JDBC。

---

## 2. JPA / Hibernate 集成

### 2.1 依赖

```xml
<!-- Spring Boot: spring-boot-starter-data-jpa pulls Hibernate + HikariCP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database driver (H2 in-memory example) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2.2 JPA Entity（带 JavaFX Properties）

用 `@Entity` 注解的 JPA 实体可以暴露 JavaFX Properties 以直接进行 UI 绑定。关键技巧是**属性访问（property access）**：将底层的 `Property` 字段作为 JPA 管理的属性，让 JPA 通过 `@Access(AccessType.PROPERTY)` 经由 getter/setter 读写。

```java
package com.example.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Entity
@Table(name = "users")
@Access(AccessType.PROPERTY)   // JPA reads/writes via getters/setters, not the field
public class User {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long getId() {
        return id.get();
    }

    public void setId(Long id) {
        // Handle null for primitive-typed Properties (see pitfall in section 8.2)
        this.id.set(id == null ? 0L : id);
    }

    @Transient   // Expose the Property object only to the UI, never persist it
    public LongProperty idProperty() {
        return id;
    }

    @Column(name = "name", nullable = false, length = 100)
    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    @Transient
    public StringProperty nameProperty() {
        return name;
    }

    @Column(name = "email", nullable = false, length = 200)
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    @Transient
    public StringProperty emailProperty() {
        return email;
    }
}
```

**要点**：
1. `@Access(AccessType.PROPERTY)` 告诉 Hibernate 通过 getter/setter 读写，使其读取解包后的值，而不是 `Property` 对象。
2. `xxxProperty()` 访问器标注 `@Transient`——`Property` 对象本身永远不会被持久化。
3. 基本类型的 Properties（`LongProperty`、`IntegerProperty`、`BooleanProperty`）必须在 setter 中防范 `null`（新实体在插入前 id 为 `null`）。

### 2.3 EntityManager（非 Spring，纯 JavaFX）

对于不使用 Spring Boot 的项目，从 `persistence.xml` 获取 `EntityManagerFactory`，并为每个操作创建短生命周期的 `EntityManager` 实例。

**`src/main/resources/META-INF/persistence.xml`**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                                 https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
             version="3.0">
    <persistence-unit name="javafx-pu" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <class>com.example.model.User</class>
        <properties>
            <property name="jakarta.persistence.jdbc.url" value="jdbc:h2:./data/app"/>
            <property name="jakarta.persistence.jdbc.user" value="sa"/>
            <property name="jakarta.persistence.jdbc.password" value=""/>
            <property name="hibernate.hbm2ddl.auto" value="update"/>
            <property name="hibernate.show_sql" value="false"/>
            <property name="hibernate.format_sql" value="true"/>
        </properties>
    </persistence-unit>
</persistence>
```

```java
public final class PersistenceManager {
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("javafx-pu");

    private PersistenceManager() {}

    public static EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        emf.close();
    }
}
```

```java
// Usage: each operation opens/closes its own EntityManager on a background thread
public User findById(Long id) {
    EntityManager em = PersistenceManager.createEntityManager();
    try {
        return em.find(User.class, id);
    } finally {
        em.close();   // Always close in finally — EntityManager is NOT thread-safe
    }
}
```

> **陷阱**：`EntityManager` **不是线程安全的**，绝不能在多个线程之间共享，也不能存储在由 FX 线程和后台 `Task` 同时访问的 Controller 字段中。每个操作或每个线程都应新建一个。

---

## 3. MyBatis 集成

### 3.1 依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>
```

### 3.2 Mapper 接口

```java
package com.example.mapper;

import java.util.List;

import com.example.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    List<User> findPage(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

    int insert(User user);

    int update(User user);

    int deleteById(@Param("id") Long id);
}
```

### 3.3 XML 映射（`src/main/resources/mapper/UserMapper.xml`）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <resultMap id="userResultMap" type="com.example.model.User">
        <id     property="id"    column="id"/>
        <result property="name"  column="name"/>
        <result property="email" column="email"/>
    </resultMap>

    <select id="findById" resultMap="userResultMap">
        SELECT id, name, email FROM users WHERE id = #{id}
    </select>

    <select id="findPage" resultMap="userResultMap">
        SELECT id, name, email FROM users
        <where>
            <if test="keyword != null and keyword != ''">
                name LIKE CONCAT('%', #{keyword}, '%')
            </if>
        </where>
        ORDER BY id DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <insert id="insert" parameterType="com.example.model.User"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (name, email) VALUES (#{name}, #{email})
    </insert>

    <update id="update" parameterType="com.example.model.User">
        UPDATE users SET name = #{name}, email = #{email} WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>
</mapper>
```

> **安全**：始终使用 `#{param}`（参数化的 PreparedStatement 占位符）。绝不要对用户控制的值使用 `${param}`——那是字符串拼接，是 SQL 注入的温床（只有对从白名单中选择的静态标识符（如表名）才可接受使用 `${}`）。

> **MyBatis + JavaFX Properties**：MyBatis 调用 getter/setter 来填充实体，因此 JavaFX Properties 开箱即用。在基本类型属性 setter 中应用相同的 null 处理（见第 8.2 节）。

---

## 4. Spring Data JPA 的仓储模式

Spring Data JPA 在运行时从接口生成仓储实现，消除了样板 CRUD 代码。

```java
package com.example.repository;

import java.util.List;
import java.util.Optional;

import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% ORDER BY u.id DESC")
    List<User> search(@Param("keyword") String keyword);
}
```

**要点**：
1. 继承 `JpaRepository<Entity, IdType>` 即可免费获得 `findById`、`findAll`、`save`、`deleteById`、分页和排序。
2. 派生查询方法（`findByEmail`、`findByNameContaining`）从方法名解析而来——无需 SQL。
3. 对于超出简单派生查询的任何需求，使用带 JPQL **命名参数**（`:keyword` + `@Param("keyword")`）的 `@Query`；绝不要将用户输入拼接进 JPQL。
4. 始终将单结果查询包装在 `Optional<T>` 中，以避免 `NullPointerException`。

> **生命周期说明**：Spring Data 仓储是由连接池支撑的线程安全单例。它们可以安全地注入到 Controller/Service 中，也可以从后台 `Task` 线程调用。但对于长时间运行的查询，**不建议**直接在 JavaFX Application Thread 上调用（见第 8.1 节）。

---

## 5. Flyway 数据库迁移

Flyway 对表结构变更进行版本控制，使每个环境都按相同的确定性顺序应用迁移。

### 5.1 依赖

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- Database-specific Flyway module (example: MySQL) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 5.2 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true   # For existing DBs without Flyway history
    baseline-version: 0
    validate-on-migrate: true    # Fail fast if checksums differ (detects manual edits)
```

### 5.3 迁移文件命名约定

Flyway 按文件名模式发现迁移脚本：

```
src/main/resources/db/migration/
├── V1__Create_users_table.sql        # Versioned migration
├── V2__Add_user_status_column.sql
├── V3__Seed_default_users.sql
└── R__Refresh_user_views.sql        # Repeatable migration (runs after every V* change)
```

- **版本化**（`V{version}__{Description}.sql`）：恰好运行一次，按版本顺序执行。
- **可重复**（`R__{Description}.sql`）：每当其校验和变化时重新运行；用于视图、存储过程、触发器。

### 5.4 规则

1. **绝不编辑已应用的迁移**：更改其校验和会导致 `validate-on-migrate` 失败。应新增一个 `V{n}` 迁移。
2. **保持迁移可幂等友好**：每个 `V` 脚本应能在全新数据库上运行一次以生成当前表结构。
3. **大型数据迁移**：对视图/存储过程定义使用 `R__` 脚本；在独立的 `V` 脚本中批量执行 DML 并带显式事务。
4. **带嵌入式数据库的桌面应用**：启用 `baseline-on-migrate: true`，使首次针对空/文件数据库启动时无需手动基线化。

---

## 6. 连接池配置（HikariCP）

HikariCP 是 Spring Boot 2+ 中的默认连接池。桌面应用必须保守地调整池大小——桌面客户端很少需要超过几个连接。

### 6.1 依赖

Spring Boot starter data-jpa 已内置 HikariCP。对于非 Spring 项目：

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

### 6.2 application.yml（Spring Boot）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp?useSSL=false&serverTimezone=UTC
    username: ${DB_USER:admin}
    password: ${DB_PASSWORD}      # From env var, never hardcoded
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      pool-name: JavaFXApp-Pool
      maximum-pool-size: 5         # Desktop apps: 3-10 is plenty
      minimum-idle: 1
      idle-timeout: 600000         # 10 min
      max-lifetime: 1800000        # 30 min (must be < DB wait_timeout)
      connection-timeout: 30000    # 30 s — fail fast if pool exhausted
      leak-detection-threshold: 60000   # Log leaked connections after 60 s
```

### 6.3 编程式配置（非 Spring）

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:h2:./data/app");
config.setUsername("sa");
config.setPassword("");
config.setMaximumPoolSize(5);
config.setLeakDetectionThreshold(60_000);
config.setPoolName("JavaFXApp-Pool");

HikariDataSource ds = new HikariDataSource(config);
// Shutdown on Application.stop() to release pool and avoid process hanging
```

### 6.4 调优指南

| 参数 | 桌面默认值 | 理由 |
|-----------|-----------------|-----------|
| `maximum-pool-size` | 3–10 | 单个用户，低并发；过大的池浪费内存 |
| `connection-timeout` | 30s | 快速失败，让 UI 显示错误而不是挂起 |
| `leak-detection-threshold` | 60s | 捕获忘记的 `em.close()` / 未关闭的连接 |
| `max-lifetime` | 30 min | 必须短于数据库 `wait_timeout`，以避免失效连接 |

> **陷阱**：在桌面应用中把 `maximum-pool-size` 设置得过高会浪费资源，甚至可能耗尽小型数据库服务器（例如 SQLite 只允许一个写入者）。对 SQLite 使用 `maximum-pool-size: 1` 以避免 `database is locked` 错误。

---

## 7. 与 JavaFX Property/Bean 模式的集成

### 7.1 实体即模型的做法

当 JPA 实体同时也是 JavaFX 模型（直接绑定到 UI）时，遵守以下规则：

1. 在实体上使用 **`@Access(AccessType.PROPERTY)`**，使 Hibernate 使用 getter/setter（而不是 `Property` 字段）。
2. 在 `xxxProperty()` 访问器上使用 **`@Transient`**，使 `Property` 对象永远不会被持久化。
3. 对基本类型的 Properties（`LongProperty`、`IntegerProperty`、`BooleanProperty`）使用 **null 安全的 setter**。
4. **不要在持久化上下文持有的实体属性上注册长生命周期监听器**——实体 detach/merge 可能导致陈旧的监听器引用（见第 8.3 节）。

### 7.2 绑定到 UI 的 DTO 与实体

| 做法 | 何时使用 | 权衡 |
|----------|-------------|-----------|
| **实体直接绑定到 UI** | 简单的 CRUD 表单、单屏编辑 | 代码更少；但半编辑的脏实体可能泄漏到持久化上下文 |
| **DTO / ViewModel 绑定到 UI** | 多步骤向导、复杂校验，或跨视图共享实体 | 更安全；在保存时于事务内将 DTO 复制到实体 |

> **推荐**：对于超出单屏表单的任何场景，将 UI 绑定到独立的 ViewModel/DTO，并在 Service 的 `@Transactional` 方法内部将值复制到新实体中。这可以避免持久化部分编辑的、无效的状态。

### 7.3 从查询生成 ObservableList

当为 `TableView` 加载列表时，用 `setAll()` 原子地替换列表，使只有一个变更事件触发：

```java
Task<List<User>> loadTask = new Task<>() {
    @Override
    protected List<User> call() {
        return userRepository.findAll();   // background thread
    }
};
loadTask.setOnSucceeded(e ->
        userTable.getItems().setAll(loadTask.getValue()));   // FX thread, atomic
new Thread(loadTask).start();
```

---

## 8. 常见陷阱

### 8.1 在 UI 线程上阻塞数据库调用（最常见）

**症状**：保存/加载期间 UI 冻结；窗口显示"未响应"。

**原因**：在 `@FXML` 事件处理器中同步调用 `repository.findAll()` 或 `mapper.findPage(...)`，而事件处理器运行在 JavaFX Application Thread 上。JDBC 调用阻塞了 UI 线程。

**解决方案**：将每次数据库调用包装在 `Task<T>`（或 `javafx.concurrent.Service`）中，并通过 `setOnSucceeded` / `Platform.runLater()` 更新 UI：

```java
@FXML
private void handleSearch() {
    Task<List<User>> task = new Task<>() {
        @Override
        protected List<User> call() {
            return userService.search(keywordField.getText());   // background
        }
    };
    task.setOnSucceeded(e -> userTable.getItems().setAll(task.getValue()));
    task.setOnFailed(e -> showError(task.getException()));
    new Thread(task).start();
}
```

> 即使是"快速"的查询也可能因数据库锁定或网络延迟而停滞；始终将数据库 I/O 从 FX 线程上卸载。

### 8.2 基本类型 Properties 的 Null 处理

**症状**：插入新实体时出现 `NullPointerException`；JPA/MyBatis 将 `null` 注入到 `SimpleLongProperty.set(long)`。

**原因**：`SimpleLongProperty.set(long)`、`SimpleIntegerProperty.set(int)` 和 `SimpleBooleanProperty.set(boolean)` 只接受基本类型，遇到 `null` 会抛出 NPE。

**解决方案**：在 setter 中将 null 转换为默认值：

```java
public void setId(Long id) {
    this.id.set(id == null ? 0L : id);
}
```

### 8.3 UI 线程上的事务边界

**症状**：`LazyInitializationException`、部分保存，或一个事务跨越多次用户交互。

**原因**：在 FX 线程上调用 `@Transactional` Service 方法（冻结 UI），或者一个实体从持久化上下文 detach，随后在 Controller 中访问其懒加载关联（此时 session 已关闭）。

**解决方案**：
1. 保持事务**简短且位于服务端**——在后台线程执行的单个 Service 方法内部开启和关闭事务。
2. 绝不让 Hibernate 管理的实体跨越用户交互。在返回给 Controller 之前转换为 detach 的 DTO/ViewModel。
3. 对仅查询的方法使用 `@Transactional(readOnly = true)`——它可以跳过 flush 并允许优化。

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User save(User input) {
        // Whole transaction lives and dies inside this method, on a background thread
        User merged = userRepository.save(input);
        return toDetached(merged);   // return a copy free of proxies
    }
}
```

### 8.4 忘记关闭 DataSource / EntityManagerFactory

**症状**：窗口关闭后 JavaFX 进程不退出；数据库连接泄漏。

**原因**：连接池 / `EntityManagerFactory` 在启动时打开但从未释放。

**解决方案**：在 `Application.stop()` 中关闭资源：

```java
@Override
public void stop() {
    if (springContext != null) {
        springContext.close();   // closes HikariCP, EntityManagerFactory, Flyway
    } else {
        PersistenceManager.close();   // non-Spring path
        if (dataSource != null) dataSource.close();
    }
}
```

### 8.5 连接池泄漏（未关闭的 EntityManager / Connection）

**症状**：连接池耗尽（`HikariPool-1 - Connection is not available`），进行若干操作后应用挂起。

**原因**：手动获取的 `EntityManager` 或 `Connection` 没有在 `finally` 块中关闭，或者在 `close()` 之前抛出了异常。

**解决方案**：对每个手动获取的连接使用 try-with-resources 或 try/finally：

```java
public User findById(Long id) {
    EntityManager em = emf.createEntityManager();
    try {
        return em.find(User.class, id);
    } finally {
        em.close();   // ALWAYS close, even on exception
    }
}
```

在 HikariCP 中启用 `leak-detection-threshold` 以在开发环境捕获此问题（见第 6.2 节）。

### 8.6 SQLite "database is locked"

**症状**：`org.sqlite.SQLiteException: [SQLITE_BUSY] The database file is locked`。

**原因**：SQLite 只允许一个写入者。`maximum-pool-size > 1` 的池加上并发写入会导致争用。

**解决方案**：对 SQLite，设置 `maximum-pool-size: 1`，或使用单写入者队列。对于并发写入负载，优先选择 H2 或真正的服务器数据库。

### 8.7 编辑已应用的迁移后 Flyway 校验和失败

**症状**：`Flyway: Migration checksum mismatch`。

**原因**：一个已经运行过的迁移脚本被就地编辑。

**解决方案**：绝不编辑已应用的迁移。新增 `V{n}__....sql`。如果必须修复（仅限开发环境），运行 `flyway repair` 来更新 schema history 校验和。

---

## 9. 推荐技术栈总结

| 关注点 | 选择 | 说明 |
|---------|--------|-------|
| ORM / SQL 映射器 | Spring Data JPA（默认）/ MyBatis | JPA 适用于领域模型；MyBatis 适用于 SQL 密集型应用 |
| 连接池 | HikariCP | Spring Boot 默认；为桌面应用调优池大小 |
| 表结构迁移 | Flyway | 版本化 `V{n}` 脚本；绝不编辑已应用的脚本 |
| 实体 ↔ UI | `@Access(PROPERTY)` + `@Transient` Property 访问器 | 见第 7 节 |
| 后台数据库调用 | `javafx.concurrent.Task` / `Service` | 绝不在 FX 线程上阻塞 |
| 事务范围 | 单个 `@Transactional` Service 方法 | 简短、服务端、在后台线程上 |
| 桌面嵌入式数据库 | H2（mode=MySQL）或 SQLite | SQLite：池大小 = 1 |

---

## 10. 相关文档

- `references/spring-boot-integration.md` — Spring Boot + JavaFX 启动、DI，以及本指南扩展的持久化层介绍（第 8 节）
- `references/data-binding-patterns.md` — JavaFX Property 类型与绑定模式
- `references/architecture-patterns.md` — MVC/MVVM/MVP 分层，包括仓储/服务层所处位置
- `javafx-code-reviewer` 的 `references/security-checklist.md` — SQL 注入与密钥处理的审查标准（与本文档的陷阱交叉引用）
- 模板：`templates/dao/Entity.java`、`templates/dao/Repository.java`、`templates/dao/FlywayMigration.sql`
