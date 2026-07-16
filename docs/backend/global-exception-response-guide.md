# 全局异常与统一响应：让接口“长得一样”（小白版）

业务代码可以专心返回数据或抛出明确异常，不必每个 Controller 都手工写相同的 `{ code, message, data }`。项目用全局响应包装器处理成功结果，用全局异常处理器把失败结果也变成同一种结构。

## 完整流程

```text
Controller 返回普通对象
  -> GlobalResponseAdvice
  -> ApiResponse.success(普通对象)
  -> { code: 200, message: "success", data: ... }

Service 发现业务不允许
  -> throw new BusinessException(ErrorCode.FORBIDDEN, "...")
  -> GlobalExceptionHandler
  -> 对应 HTTP 403 + ApiResponse.fail(...)
  -> { code: 403, message: "...", data: null }
```

## 关键文件地图

| 作用 | 真实代码位置 |
| --- | --- |
| 成功响应自动包装 | `backend/land/src/main/java/com/skylink/land/web/GlobalResponseAdvice.java:15-52` |
| 异常统一处理 | `backend/land/src/main/java/com/skylink/land/web/GlobalExceptionHandler.java:21-94` |
| 响应结构 | `backend/land/src/main/java/com/skylink/land/dto/common/ApiResponse.java` |
| 分页结构 | `backend/land/src/main/java/com/skylink/land/dto/common/PageResponse.java` |
| 错误码表 | `backend/land/src/main/java/com/skylink/land/exception/ErrorCode.java:6-23` |
| 业务异常类型 | `backend/land/src/main/java/com/skylink/land/exception/BusinessException.java` |

## 错误码是共同语言

代码位置：`ErrorCode.java:6-23`

```java
public enum ErrorCode {
    SUCCESS(200, "success"), // 1. 业务成功。
    BAD_REQUEST(400, "请求参数错误"), // 2. 传参格式不对或缺少必要字段。
    UNAUTHORIZED(401, "未认证或 Token 无效"), // 3. 没有有效登录身份。
    FORBIDDEN(403, "无权限访问"), // 4. 已登录，但不能访问此资源。
    NOT_FOUND(404, "资源不存在"), // 5. 目标 ID 不存在。
    CONFLICT(409, "数据冲突"), // 6. 例如重复数据。
    INTERNAL_ERROR(500, "服务器内部错误"); // 7. 未预料的服务端错误。
}
```

这里的 `code` 同时与常见 HTTP 状态语义对应。客户端仍应先看 HTTP 状态处理网络层问题，再按统一 JSON 结构读取 `message` 与 `data`。

## 普通返回值怎样自动包一层

代码位置：`GlobalResponseAdvice.java:24-52`

```java
@Override
public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true; // 1. 声明：所有 Controller 响应都进入 beforeBodyWrite。
}

@Override
public Object beforeBodyWrite(Object body, /* 其余 Spring 参数省略 */) {
    if (body instanceof ApiResponse<?> || body instanceof Resource || body instanceof byte[]) {
        return body; // 2. 已包装 JSON、文件资源、字节流不能再包装。
    }

    ApiResponse<Object> apiResponse = ApiResponse.success(body); // 3. 普通对象统一变成功响应。
    if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
        // 4. String 有自己的转换器；必须手工序列化为 JSON 字符串。
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return objectMapper.writeValueAsString(apiResponse);
    }
    return apiResponse; // 5. 其余对象交给正常 JSON 转换器。
}
```

`Resource` 和 `byte[]` 被排除很重要：下载文件若被包装成 JSON，浏览器就拿不到原始文件内容了。

## 业务异常怎样成为正确 HTTP 状态

代码位置：`GlobalExceptionHandler.java:25-34`、`83-92`

```java
@ExceptionHandler(BusinessException.class) // 1. 只要任一层抛出 BusinessException，就由此接住。
public ResponseEntity<ApiResponse<Void>> handleBusinessException(
    BusinessException exception, HttpServletRequest request
) {
    log.warn("Business exception on {} {}: {}", // 2. 服务端日志保留请求位置和原因。
        request.getMethod(), request.getRequestURI(), exception.getMessage());
    return ResponseEntity
        .status(resolveStatus(exception.getErrorCode())) // 3. ErrorCode 转为真实 HTTP 状态。
        .body(ApiResponse.fail(exception.getErrorCode(), exception.getMessage())); // 4. JSON 保持统一形状。
}
```

例如任务模块抛出 `new BusinessException(ErrorCode.FORBIDDEN, "...")`，用户收到 HTTP 403 和失败结构，而不是一段难以解析的 Java 堆栈。

## 参数校验失败不是服务器崩了

代码位置：`GlobalExceptionHandler.java:36-62`

`MethodArgumentNotValidException` 与 `BindException` 会把各字段的错误拼成如 `title 不能为空; page 必须大于 0` 的消息，并返回 HTTP 400。缺少请求参数、JSON 无法读取、非法参数也统一返回 400。这告诉前端“请改请求”，而不是“系统坏了”。

## 真正未知的异常为何不返回细节

代码位置：`GlobalExceptionHandler.java:76-80`

```java
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 1. 对外是 HTTP 500。
@ExceptionHandler(Exception.class) // 2. 最后兜底，接住未被前面规则处理的异常。
public ApiResponse<Void> handleException(Exception exception, HttpServletRequest request) {
    log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
    // 3. 日志记录完整堆栈给开发者；响应不暴露数据库、路径或堆栈。
    return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
}
```

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| Controller 必须手工 `ApiResponse.success` | 可以手工返回，普通对象也会被 Advice 自动包装。 |
| 所有返回都能包装 | 文件和字节流会被刻意跳过。 |
| 业务拒绝就是 500 | `BusinessException` 会按错误码返回 400/401/403/404/409 等。 |
| 500 要把堆栈返回给前端方便排查 | 不应这样做，细节只写服务端日志。 |

## 人话复盘

统一响应解决“成功接口长得一致”，全局异常解决“失败接口也长得一致”。页面因此可以稳定地按 `code/message/data` 处理；后端业务代码则只要在不允许时抛出合适的业务异常。
