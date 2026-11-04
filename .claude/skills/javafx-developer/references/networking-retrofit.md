# Retrofit 网络集成参考

> 将 `retrofit-spring-boot-starter` 集成到 JavaFX + Spring Boot 桌面应用的模式。涵盖 API 接口定义、OkHttpClient 注册、token 认证拦截器、统一响应解析、带 UI 线程安全的异步执行、全局异常处理以及超时/重试配置。
>
> 本参考基于 ruoyi-javafx 项目中的生产验证模式。

## 为什么 JavaFX 使用 Retrofit

与后端 REST API 通信的 JavaFX 桌面应用需要一个干净的 HTTP 层。`retrofit-spring-boot-starter`（来自 lianjiatech）提供：

- **声明式 API 接口** —— 注解式 Java 接口，无样板 HTTP 代码
- **Spring Boot 集成** —— 自动将 `@RetrofitClient` 接口扫描为 Spring Bean，可通过 `@Resource` 注入
- **可插拔的 OkHttpClient** —— 带拦截器、超时、SSL 的自定义 OkHttp 实例
- **全局重试与日志** —— 通过 YAML 配置的声明式重试与日志策略
- **对后端开发者友好** —— 与 Android/后端相同的 Retrofit 模式，学习曲线更低

## Maven 依赖

```xml
<properties>
    <retrofit.version>4.0.8</retrofit.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.github.lianjiatech</groupId>
        <artifactId>retrofit-spring-boot-starter</artifactId>
        <version>${retrofit.version}</version>
    </dependency>
</dependencies>
```

## 架构概览

推荐的分层将关注点分离为四层：

```
Controller (JavaFX UI)
    ↓ calls
Service (business logic + response parsing)
    ↓ calls
Api Interface (Retrofit declarative HTTP)
    ↓ uses
OkHttpClient (interceptors, timeouts, auth)
    ↓ communicates with
Backend REST API
```

| 层 | 职责 | 包约定 |
|-------|---------------|-------------------|
| API 接口 | 声明式 HTTP 方法定义（`@GET`、`@POST` 等） | `retrofit.api` |
| OkHttpClient 配置 | 带拦截器和超时的 OkHttp 客户端注册 | `retrofit.config` |
| 拦截器 | 横切关注点（认证 token、日志） | `retrofit.interceptor` |
| 上下文 | 共享状态（token 存储、API 常量、响应包装器） | `retrofit.context` |
| Service | 业务逻辑 —— 调用 API、解析响应、出错时抛出异常 | `service` |
| 异常 | 统一 API 异常与全局处理器 | `exception` |
| 异步 | 用于非阻塞 UI 执行的线程池 | `util` |

## 配置（application.yml）

```yaml
app:
  retrofit:
    # Backend base URL — all @RetrofitClient interfaces use this via ${app.retrofit.base-url}
    base-url: http://localhost:8080/
    # OkHttp connect timeout
    connect-timeout: 10s
    # OkHttp read timeout (waiting for response data)
    read-timeout: 10s
    # OkHttp write timeout (sending request data)
    write-timeout: 10s

# Retrofit global config (retrofit-spring-boot-starter native)
retrofit:
  global-log:
    enable: false
    log-level: info
    # NONE / BASIC / HEADERS / BODY (BODY may leak sensitive data — use with caution in prod)
    log-strategy: BASIC
  global-retry:
    enable: false
```

## OkHttpClient 注册

注册带拦截器和可配置超时的自定义 OkHttpClient。`SourceOkHttpClientRegistrar` SPI 让你可以为客户端命名，并通过 `@RetrofitClient(sourceOkHttpClient = "...")` 引用它。

```java
package com.example.app.retrofit.config;

import com.github.lianjiatech.retrofit.spring.boot.core.SourceOkHttpClientRegistrar;
import com.github.lianjiatech.retrofit.spring.boot.core.SourceOkHttpClientRegistry;
import com.example.app.retrofit.interceptor.TokenInterceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RetrofitOkHttpClientRegistrar implements SourceOkHttpClientRegistrar {

    private final TokenInterceptor tokenInterceptor;

    @Value("${app.retrofit.connect-timeout:10s}")
    private Duration connectTimeout;

    @Value("${app.retrofit.read-timeout:10s}")
    private Duration readTimeout;

    @Value("${app.retrofit.write-timeout:10s}")
    private Duration writeTimeout;

    public RetrofitOkHttpClientRegistrar(TokenInterceptor tokenInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
    }

    @Override
    public void register(SourceOkHttpClientRegistry registry) {
        registry.register("appOkHttpClient", new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .addInterceptor(tokenInterceptor)
                .build());
    }
}
```

## API 接口定义

将 HTTP API 定义为注解式 Java 接口。每个接口都用 `@RetrofitClient` 注解，指定基础 URL 和 OkHttpClient。方法返回 `Map<String, Object>`（由 Service 层解析为类型化对象）。

```java
package com.example.app.retrofit.api;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

@RetrofitClient(baseUrl = "${app.retrofit.base-url}", sourceOkHttpClient = "appOkHttpClient")
public interface SysUserApi {

    @GET("system/user/list")
    Map<String, Object> list(@QueryMap Map<String, Object> queryParams);

    @GET("system/user/{userId}")
    Map<String, Object> getById(@Path("userId") Long userId);

    @POST("system/user")
    Map<String, Object> add(@Body Map<String, Object> user);

    @PUT("system/user")
    Map<String, Object> edit(@Body Map<String, Object> user);

    @DELETE("system/user/{userIds}")
    Map<String, Object> remove(@Path("userIds") String userIds);
}
```

### 注解快速参考

| 注解 | HTTP 方法 | 典型用途 |
|-----------|-------------|-------------|
| `@GET("path")` | GET | 查询资源，配合 `@QueryMap` 用于列表 |
| `@POST("path")` | POST | 配合 `@Body` 创建资源 |
| `@PUT("path")` | PUT | 配合 `@Body` 更新资源 |
| `@DELETE("path")` | DELETE | 配合 `@Path` 删除资源 |
| `@QueryMap` | — | 查询参数映射（分页、过滤） |
| `@Path("name")` | — | URL 路径替换（`/user/{userId}`） |
| `@Body` | — | 请求体（序列化为 JSON） |

## Token 认证

### Token 存储

将认证 token 存储在带 `volatile` 可见性和 `char[]` 的静态上下文中，以减小内存占用。`clear()` 方法主动将数组清零，以最小化 token 在内存中的驻留时间。

```java
package com.example.app.retrofit.context;

public final class TokenContext {

    private static volatile char[] tokenChars;

    private TokenContext() {}

    public static String getToken() {
        char[] chars = tokenChars;
        return chars != null ? new String(chars) : null;
    }

    public static void setToken(String token) {
        clear();
        tokenChars = (token != null) ? token.toCharArray() : null;
    }

    public static void clear() {
        char[] chars = tokenChars;
        if (chars != null) {
            java.util.Arrays.fill(chars, '\0');
        }
        tokenChars = null;
    }
}
```

### Token 拦截器

一个 OkHttp 拦截器，当 token 可用时自动为每个请求附加 `Authorization: Bearer <token>` 头。

```java
package com.example.app.retrofit.interceptor;

import com.example.app.retrofit.context.TokenContext;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenInterceptor implements Interceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = TokenContext.getToken();
        if (token != null && !token.isEmpty()) {
            Request request = original.newBuilder()
                    .header(AUTH_HEADER, BEARER_PREFIX + token)
                    .build();
            return chain.proceed(request);
        }
        return chain.proceed(original);
    }
}
```

## 统一响应解析（ApiResult）

后端返回统一的响应信封 `{code, msg, data/rows, total}`。`ApiResult` 包装器集中处理响应解析、成功断言和类型化数据提取。

```java
package com.example.app.retrofit.context;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.app.exception.ApiException;
import lombok.Getter;

import java.util.Map;

@Getter
public class ApiResult {

    private final int code;
    private final String msg;
    private final Map<String, Object> raw;

    private ApiResult(int code, String msg, Map<String, Object> raw) {
        this.code = code;
        this.msg = msg;
        this.raw = raw;
    }

    public static ApiResult of(Map<String, Object> raw) {
        if (raw == null) {
            throw new ApiException("Server returned null");
        }
        Object codeObj = raw.get("code");
        int code = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
        String msg = String.valueOf(raw.getOrDefault("msg", ""));
        return new ApiResult(code, msg, raw);
    }

    /**
     * Assert success. If unauthorized (401), fire auth-expired callback before throwing.
     * Chainable — returns this on success.
     */
    public ApiResult assertSuccess() {
        if (AuthContextHolder.isUnauthorized(code)) {
            AuthContextHolder.fireAuthExpired();
            throw new ApiException(code, msg);
        }
        if (code != ApiConstants.CODE_SUCCESS) {
            throw new ApiException(code, msg);
        }
        return this;
    }

    public Integer getInt(String key) {
        Object val = raw.get(key);
        return (val instanceof Number) ? ((Number) val).intValue() : null;
    }

    public String getString(String key) {
        Object val = raw.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    public <T> T getDataAs(Class<T> clazz) {
        Object data = raw.get("data");
        if (data == null) return null;
        if (clazz.isInstance(data)) return clazz.cast(data);
        return JSON.to(clazz, data);
    }

    public JSONObject getDataAsJson() {
        Object data = raw.get("data");
        if (data == null) return new JSONObject();
        if (data instanceof Map) {
            return new JSONObject((Map<String, Object>) data);
        }
        return JSONObject.from(data);
    }

    public JSONArray getDataArray() {
        Object data = raw.get("data");
        if (data == null) data = raw.get("rows");
        if (data == null) return new JSONArray();
        if (data instanceof java.util.List) return new JSONArray((java.util.List<?>) data);
        return JSONArray.from(data);
    }
}
```

### API 常量

```java
package com.example.app.retrofit.context;

public final class ApiConstants {
    public static final int CODE_SUCCESS = 200;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_SERVER_ERROR = 500;

    private ApiConstants() {
        throw new UnsupportedOperationException("Constants class not instantiable");
    }
}
```

## 认证过期处理

当收到 401 响应时，应用必须将用户导航回登录界面。`AuthContextHolder` 使用 CAS 保护的导航来管理认证过期监听器，以防止重复弹窗。

```java
package com.example.app.retrofit.context;

import com.example.app.exception.ApiException;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AuthContextHolder {

    private static final List<Runnable> authExpiredListeners = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean navigating = new AtomicBoolean(false);

    private AuthContextHolder() {}

    public static void addAuthExpiredListener(Runnable listener) {
        if (listener != null) authExpiredListeners.add(listener);
    }

    public static void fireAuthExpired() {
        if (!authExpiredListeners.isEmpty() && navigating.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                try {
                    for (Runnable listener : authExpiredListeners) {
                        try {
                            listener.run();
                        } catch (Exception e) {
                            Thread.currentThread().getUncaughtExceptionHandler()
                                    .uncaughtException(Thread.currentThread(), e);
                        }
                    }
                } finally {
                    navigating.set(false);
                }
            });
        }
    }

    public static boolean isUnauthorized(int code) {
        return code == ApiConstants.CODE_UNAUTHORIZED;
    }

    /** Recursively check if an exception chain contains a 401 ApiException */
    public static boolean isAuthExpired(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ApiException apiEx && isUnauthorized(apiEx.getCode())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
```

## Service 层

Service 层用 `ApiResult.of(...).assertSuccess()` 包装 API 调用——这是核心模式。Service 处理响应解析、类型化转换和业务特定参数组装，使 Controller 专注于 UI。

```java
package com.example.app.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.app.model.UserVO;
import com.example.app.retrofit.api.SysUserApi;
import com.example.app.retrofit.context.ApiResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserService {

    @Resource
    private SysUserApi sysUserApi;

    /**
     * Paginated user list (strongly typed).
     * Core pattern: ApiResult.of(api.call(params)).assertSuccess() → extract data
     */
    public PageResult<UserVO> listVOs(Map<String, Object> params) {
        ApiResult result = ApiResult.of(sysUserApi.list(params)).assertSuccess();
        int total = result.getInt("total") != null ? result.getInt("total") : 0;
        JSONArray rows = result.getRows();
        List<UserVO> voList = rows != null ? rows.toJavaList(JSONObject.class).stream()
                .map(UserVO::from)
                .toList() : List.of();
        return PageResult.of(voList, total);
    }

    public void add(Map<String, Object> user) {
        ApiResult.of(sysUserApi.add(user)).assertSuccess();
    }

    public void edit(Map<String, Object> user) {
        ApiResult.of(sysUserApi.edit(user)).assertSuccess();
    }

    public void remove(String userIds) {
        ApiResult.of(sysUserApi.remove(userIds)).assertSuccess();
    }

    public void resetPwd(Long userId, String password) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", userId);
        params.put("password", password);
        ApiResult.of(sysUserApi.resetPwd(params)).assertSuccess();
    }
}
```

### Service 模式总结

每个 Service 方法都遵循相同的结构：

```java
public ReturnType doSomething(params) {
    // 1. Call API → get raw Map
    // 2. Wrap with ApiResult.of() → parse code/msg
    // 3. assertSuccess() → throw ApiException if code != 200 (or fire auth-expired if 401)
    // 4. Extract typed data from result
    // 5. Return typed object
}
```

## 异步执行与 UI 线程安全

**严重**：Retrofit 调用是同步且阻塞的——**绝不能**运行在 JavaFX Application Thread 上。请使用专用线程池，并在渲染时切换回 UI 线程。

### AsyncUtil

一个带 daemon 线程的固定线程池、自定义拒绝策略（丢弃最旧任务，绝不阻塞 UI 线程），以及用于安全切换 UI 线程的 `runLater()` 辅助方法。

```java
package com.example.app.util;

import com.example.app.retrofit.context.AuthContextHolder;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public final class AsyncUtil {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final long KEEP_ALIVE_SECONDS = 60L;
    private static final int QUEUE_CAPACITY = 256;

    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread t = new Thread(r, "fx-async-" + new AtomicInteger().incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.warn("Async queue full, discarding oldest task");
                    super.rejectedExecution(r, executor);
                }
            }
    );

    private AsyncUtil() {}

    /** Run task in background. Auth-expired exceptions are silently ignored. */
    public static void run(Runnable task) {
        EXECUTOR.submit(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                if (AuthContextHolder.isAuthExpired(e)) return;
                log.error("Async task failed", e);
                runLater(() -> AlertUtil.error("Async error: " + e.getMessage()));
            }
        });
    }

    /** Run task in background with error callback on UI thread. */
    public static void run(Runnable task, Runnable onError) {
        EXECUTOR.submit(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                if (AuthContextHolder.isAuthExpired(e)) return;
                log.error("Async task failed", e);
                runLater(onError);
            }
        });
    }

    /** Switch to JavaFX Application Thread. Runs immediately if already on UI thread. */
    public static void runLater(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### Controller 调用模式

Controller 异步调用 Service，然后在 JavaFX Application Thread 上更新 UI：

```java
// In a Controller event handler
AsyncUtil.run(() -> {
    // Background thread — blocking API call is safe here
    PageResult<UserVO> result = userService.listVOs(params);

    // Switch to UI thread for rendering
    AsyncUtil.runLater(() -> {
        tableView.getItems().setAll(result.getList());
        pagination.setTotalCount(result.getTotal());
    });
});
```

## 异常处理

### ApiException

携带后端响应代码和消息：

```java
package com.example.app.exception;

import com.example.app.retrofit.context.ApiConstants;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApiException(String message) {
        this(ApiConstants.CODE_SERVER_ERROR, message);
    }

    public ApiException(String message, Throwable cause) {
        this(ApiConstants.CODE_SERVER_ERROR, message, cause);
    }
}
```

### GlobalExceptionHandler

安装为线程的未捕获异常处理器。特性：
- 认证过期异常被静默忽略（由 `AuthContextHolder.fireAuthExpired()` 处理）
- 弹窗节流：2 秒内的相同消息只显示一次（防止批量失败时的弹窗轰炸）
- 始终通过 `Platform.runLater` 切换到 UI 线程显示弹窗

```java
package com.example.app.exception;

import com.example.app.retrofit.context.AuthContextHolder;
import com.example.app.util.AlertUtil;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final long THROTTLE_MS = 2000L;
    private String lastMessage = null;
    private long lastShownTime = 0;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception [{}]: {}", t.getName(), e.getMessage(), e);
        if (AuthContextHolder.isAuthExpired(e)) return;

        String message = extractMessage(e);
        if (shouldThrottle(message)) {
            log.debug("Dialog throttled: '{}'", message);
            return;
        }
        Platform.runLater(() -> AlertUtil.error(message));
    }

    private synchronized boolean shouldThrottle(String message) {
        long now = System.currentTimeMillis();
        if (message.equals(lastMessage) && (now - lastShownTime) < THROTTLE_MS) {
            return true;
        }
        lastMessage = message;
        lastShownTime = now;
        return false;
    }

    private String extractMessage(Throwable e) {
        String message = null;
        if (e instanceof ApiException) {
            message = e.getMessage();
        } else if (e.getCause() != null) {
            message = e.getCause().getMessage();
        }
        if (message == null) message = e.getMessage();
        return message != null ? message : "Unknown error";
    }
}
```

## 密码加密（RSA）

对于登录/注册流程，在传输前使用 RSA 加密密码。公钥从后端获取：

```java
public String login(LoginRequest request) {
    // 1. Fetch RSA public key from backend
    String publicKey = getPublicKey();
    // 2. Encrypt password
    String encryptedPassword = RsaEncryptor.encrypt(request.getPassword(), publicKey);
    request.setPassword(encryptedPassword);
    // 3. Call login API
    ApiResult result = ApiResult.of(sysLoginApi.login(request)).assertSuccess();
    String token = result.getString("token");
    if (token == null || token.isEmpty()) {
        throw new ApiException("Login returned empty token");
    }
    return token;
}
```

## 完整集成检查清单

当为新的 JavaFX + Spring Boot 项目添加 Retrofit 时：

1. [ ] 在 `pom.xml` 中添加 `retrofit-spring-boot-starter` 依赖
2. [ ] 在 `application.yml` 中配置 `app.retrofit.*` 和 `retrofit.*`
3. [ ] 创建 `retrofit/config/RetrofitOkHttpClientRegistrar.java` —— 注册命名的 OkHttpClient
4. [ ] 创建 `retrofit/interceptor/TokenInterceptor.java` —— 认证头注入
5. [ ] 创建 `retrofit/context/TokenContext.java` —— volatile char[] token 存储
6. [ ] 创建 `retrofit/context/ApiConstants.java` —— 响应代码常量
7. [ ] 创建 `retrofit/context/ApiResult.java` —— 带 assertSuccess() 的统一响应包装器
8. [ ] 创建 `retrofit/context/AuthContextHolder.java` —— 认证过期监听器管理
9. [ ] 创建 `exception/ApiException.java` —— 带代码的 API 异常
10. [ ] 创建 `exception/GlobalExceptionHandler.java` —— 带节流的未捕获处理器
11. [ ] 创建 `util/AsyncUtil.java` —— 线程池 + runLater 辅助
12. [ ] 创建 `retrofit/api/*Api.java` —— 每个后端模块一个接口
13. [ ] 创建 `service/*Service.java` —— 每个 API 一个 Service，用 ApiResult.of().assertSuccess() 包装
14. [ ] 在应用启动时将 `GlobalExceptionHandler` 注册为线程默认未捕获异常处理器
15. [ ] 通过 `AuthContextHolder.addAuthExpiredListener()` 注册认证过期监听器以导航到登录页

## 常见陷阱

1. **阻塞 UI 线程**：绝不要在 JavaFX 事件处理器中直接调用 API 方法——始终用 `AsyncUtil.run()` 包装。Retrofit 调用是同步的
2. **忘记 assertSuccess()**：调用 `ApiResult.of(api.call())` 而不调用 `assertSuccess()` 会静默忽略错误——始终链式调用 `.assertSuccess()`
3. **认证过期弹窗轰炸**：没有 `AuthContextHolder` 的 CAS 保护，多个并发 401 响应可能触发多次登录导航
4. **CallerRunsPolicy 拒绝策略**：在线程池中使用 `CallerRunsPolicy` 会让被拒绝的任务在 UI 线程上运行——应改用 `DiscardOldestPolicy`
5. **Token 进入字符串池**：用 `String` 存储 token 会让它更长驻留于 JVM 字符串池——应使用带主动 `clear()` 清零的 `char[]`
6. **没有弹窗节流**：批量操作同时失败会产生几十个错误弹窗——应按消息在时间窗口内节流
