# 后端 JWT：生成、验证与刷新（小白注释版）

> 本文只讲本项目后端已经实现的 JWT 流程，不修改任何业务代码。

## 先用一句话理解

登录成功后，后端会用只有后端知道的密钥，给用户制作一张带有**防伪签名**的电子身份证（JWT）。

之后用户访问需要登录的接口时，前端把这张身份证带在请求头中；后端检查它有没有被伪造、是否过期、是不是本系统签发。检查全部通过，才允许继续执行接口。

```text
登录账号密码
    │
    ▼
后端验证密码正确
    │
    ▼
生成 accessToken + refreshToken
    │
    ├── accessToken：前端放进 Authorization 请求头
    └── refreshToken：后端放进 HttpOnly Cookie
    │
    ▼
访问业务接口时，后端拦截器验证 accessToken
    │
    ▼
验证通过 → 得到当前用户 → 执行业务接口
```

## 1. JWT 是什么：三段字符串

一个 JWT 通常长这样：

```text
Header.Payload.Signature
```

三部分之间用英文句点 `.` 分隔：

| 部分 | 本项目中的作用 | 能否被别人看到 |
| --- | --- | --- |
| `Header` | 声明签名算法是 `HS256` | 可以 |
| `Payload` | 存放用户 ID、用户名、角色、过期时间等 | 可以 |
| `Signature` | 防止前两部分被篡改 | 不能伪造 |

**特别注意：JWT 不是加密。** 前两段只是 Base64 URL 编码，别人可以解码查看；所以不要把密码、身份证号等敏感数据放进 Payload。本项目放的是用户 ID、用户名、角色和时间信息。

---

## 2. JWT 的配置在哪里

**代码位置：** `backend/land/src/main/resources/application.yaml` 第 38–58 行。

```yaml
skylink:
  jwt:
    issuer: sky-link                 # 签发者名称：Token 必须是本系统签发的
    secret: ${JWT_SECRET}            # 签名密钥：从环境变量读取，不能写死并提交到 Git
    ttl: 24h                         # accessToken 的有效期：24 小时
    refresh-ttl: 7d                  # refreshToken 的有效期：7 天
    header: Authorization             # 前端传 Token 时使用的请求头名称
    token-prefix: "Bearer "          # 请求头中 Token 前必须带的固定前缀（末尾有空格）
    refresh-cookie:
      name: skylink_refresh_token    # 保存 refreshToken 的 Cookie 名称
      path: /api/v1/auth             # 只有认证相关接口会自动携带该 Cookie
      http-only: true                # JavaScript 不能读取，降低 XSS 窃取风险
      secure: false                  # 生产环境 HTTPS 应设为 true
      same-site: Lax                 # 限制跨站请求自动携带 Cookie
```

前端实际发送 accessToken 的形式如下：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9....
```

其中 `Bearer ` 不能漏掉，因为后端会专门检查这个前缀。

### 启动时的安全检查

**代码位置：** `backend/land/src/main/java/com/skylink/land/auth/JwtProperties.java` 第 45–64 行。

```java
@Override
public void afterPropertiesSet() {
    // 1. 如果没有配置 JWT_SECRET，应用直接启动失败。
    //    因为没有密钥，就既不能安全地生成 Token，也不能验证 Token。
    if (!StringUtils.hasText(secret)) {
        throw new IllegalStateException("JWT_SECRET must be configured");
    }

    // 2. 密钥至少要有 32 字节，太短容易被猜出。
    //    同时禁止使用项目给出的示例密钥。
    if (INSECURE_DEFAULT_SECRET.equals(secret)
        || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
        throw new IllegalStateException(
            "JWT_SECRET must contain at least 32 bytes and must not use the example value"
        );
    }

    // 3. accessToken 与 refreshToken 都必须有正的有效期。
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
        throw new IllegalStateException("JWT token ttl must be positive");
    }
    if (refreshTtl == null || refreshTtl.isZero() || refreshTtl.isNegative()) {
        throw new IllegalStateException("JWT refresh token ttl must be positive");
    }

    // 4. refreshToken 应比 accessToken 活得更久；否则没有“刷新”的意义。
    if (refreshTtl.minus(ttl).isZero() || refreshTtl.minus(ttl).isNegative()) {
        throw new IllegalStateException("JWT refresh token ttl must be longer than access token ttl");
    }
}
```

---

## 3. 登录成功后，Token 从哪里开始生成

### 3.1 Controller 接收登录请求

**代码位置：** `backend/land/src/main/java/com/skylink/land/controller/AuthController.java` 第 35–40 行。

```java
@PostMapping("/login") // 处理 POST /api/v1/auth/login 请求
public AuthDto.TokenResponse login(
    @RequestBody AuthDto.LoginRequest request, // 接收前端提交的账号与密码
    HttpServletResponse response               // 用它向浏览器写入 Cookie
) {
    // 调用 Service：先校验账号密码，正确后生成一对 Token。
    TokenPair tokenPair = authService.login(request);

    // refreshToken 不直接交给 JavaScript，而是写入 HttpOnly Cookie。
    // 浏览器之后访问 /api/v1/auth/refresh 时会自动带上它。
    refreshCookieManager.addRefreshToken(response, tokenPair.getRefreshToken());

    // 返回响应数据，其中包含给前端调用业务接口使用的 accessToken。
    return tokenPair.toResponse();
}
```

### 3.2 Service 先确认“这个人确实登录成功了”

**代码位置：** `backend/land/src/main/java/com/skylink/land/service/auth/impl/AuthServiceImpl.java` 第 92–107 行。

```java
public TokenPair login(AuthDto.LoginRequest request) {
    // 账号或密码没有传，直接拒绝。
    if (request == null || !StringUtils.hasText(request.getAccount())
        || !StringUtils.hasText(request.getPassword())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "account and password are required");
    }

    // 按用户名或邮箱查询用户。
    User user = findByAccount(request.getAccount().trim());

    // passwordEncoder.matches 会把用户输入的明文密码与数据库中的加密密码比较。
    // 密码错误或用户不存在时，不能生成 Token。
    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid account or password");
    }

    // 被禁用的用户即使密码正确，也不能获取 Token。
    if (!Integer.valueOf(1).equals(user.getStatus())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "user is disabled");
    }

    // 只有上述检查都通过，才开始签发 Token。
    return issueTokens(user);
}
```

### 3.3 一次生成 accessToken 和 refreshToken

**代码位置：** `backend/land/src/main/java/com/skylink/land/service/auth/impl/AuthServiceImpl.java` 第 123–145 行。

```java
private TokenPair issueTokens(User user) {
    // 从数据库查出该用户当前拥有的角色，例如 ROLE_ADMIN、ROLE_USER。
    List<String> roles = userService.listRoleCodes(user.getUserId());

    // accessToken：给普通业务请求使用，有效期较短（配置中是 24 小时）。
    String accessToken = jwtTokenProvider.generateToken(
        user.getUserId(),   // 写入 Token 的用户 ID
        user.getUsername(), // 写入 Token 的用户名
        roles               // 写入 Token 的角色列表
    );

    // refreshToken：只用于换取新的 Token，有效期较长（配置中是 7 天）。
    String refreshToken = jwtTokenProvider.generateRefreshToken(
        user.getUserId(),
        user.getUsername(),
        roles
    );

    // 返回这对 Token 及 accessToken 的剩余秒数。
    return TokenPair.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(jwtProperties.getTtl().toSeconds())
        .build();
}
```

---

## 4. JWT 是怎样生成的

**核心代码位置：** `backend/land/src/main/java/com/skylink/land/auth/JwtTokenProvider.java`。

### 4.1 access 与 refresh 只是“类型”和“有效期”不同

**位置：** 第 41–55 行。

```java
public String generateToken(Long userId, String username, List<String> roles) {
    // 普通业务请求用的 Token：类型为 access，时长使用 ttl（24 小时）。
    return generateToken(userId, username, roles, ACCESS_TOKEN_TYPE, properties.getTtl());
}

public String generateRefreshToken(Long userId, String username, List<String> roles) {
    // 专门刷新 Token 用的 Token：类型为 refresh，时长使用 refreshTtl（7 天）。
    return generateToken(userId, username, roles, REFRESH_TOKEN_TYPE, properties.getRefreshTtl());
}
```

### 4.2 组装 Header、Payload 和 Signature

**位置：** `JwtTokenProvider.java` 第 57–77 行。

```java
private String generateToken(
    Long userId,
    String username,
    List<String> roles,
    String tokenType,
    Duration ttl
) {
    // 记录当前签发时刻，后面的 iat（签发时间）和 exp（过期时间）都从这里计算。
    Instant now = Instant.now();

    // Header：声明这个 Token 的签名算法和类型。
    // HS256 表示使用 HMAC + SHA-256 算法签名。
    Map<String, Object> header = Map.of(
        "alg", "HS256",
        "typ", "JWT"
    );

    // Payload：Token 中要携带的业务信息。
    Map<String, Object> payload = new HashMap<>();
    payload.put("iss", properties.getIssuer());     // issuer：签发者，固定为 sky-link
    payload.put("sub", String.valueOf(userId));     // subject：主体，这里就是用户 ID
    payload.put("token_type", tokenType);           // access 或 refresh，防止两者混用
    payload.put("username", username);              // 当前用户名
    payload.put("roles", CollectionUtils.isEmpty(roles) ? List.of() : roles); // 用户角色
    payload.put("iat", now.getEpochSecond());       // issued at：签发时间，单位是秒
    payload.put("exp", now.plus(ttl).getEpochSecond()); // expiration：过期时间，单位是秒

    // 将 Header 和 Payload 转成 JSON 后再进行 Base64 URL 编码。
    // 这是“编码”，不是加密，因此不要放敏感信息。
    String encodedHeader = encodeJson(header);
    String encodedPayload = encodeJson(payload);

    // 用密钥对“前两段”签名。任何一段内容被改，重新计算出的签名都会不同。
    String signature = sign(encodedHeader + "." + encodedPayload);

    // 三段以点号拼接，形成最终 JWT。
    return encodedHeader + "." + encodedPayload + "." + signature;
}
```

### 4.3 签名的关键：只有后端知道 secret

**位置：** `JwtTokenProvider.java` 第 147–155 行。

```java
private String sign(String content) {
    try {
        // 创建 HMAC-SHA256 签名器。
        Mac mac = Mac.getInstance(HMAC_SHA256);

        // 把 application.yaml 中 JWT_SECRET 作为签名密钥装入签名器。
        // 前端和普通用户都不应知道这个值。
        mac.init(new SecretKeySpec(
            properties.getSecret().getBytes(StandardCharsets.UTF_8),
            HMAC_SHA256
        ));

        // 对 Header.Payload 计算签名，再使用 Base64 URL 编码变成字符串。
        return BASE64_URL_ENCODER.encodeToString(
            mac.doFinal(content.getBytes(StandardCharsets.UTF_8))
        );
    } catch (Exception exception) {
        // 签名器初始化异常属于服务器配置/程序异常，不是用户输入错误。
        throw new IllegalStateException("Failed to sign token", exception);
    }
}
```

---

## 5. 每次访问业务接口时，JWT 如何被验证

### 5.1 哪些接口会被拦截

**代码位置：** `backend/land/src/main/java/com/skylink/land/config/WebMvcConfiguration.java` 第 50–58 行。

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // 给所有 /api/v1/** 接口加上 JWT 登录验证。
    registry.addInterceptor(jwtAuthenticationInterceptor)
        .addPathPatterns("/api/v1/**")

        // 登录、注册、刷新、退出等接口不要求先带 accessToken。
        .excludePathPatterns(jwtProperties.getExcludePaths());

    // JWT 验证成功后，才继续执行后面的权限验证拦截器。
    registry.addInterceptor(permissionAuthorizationInterceptor)
        .addPathPatterns("/api/v1/**")
        .excludePathPatterns(jwtProperties.getExcludePaths());
}
```

不需要 accessToken 的路径配置在 `application.yaml` 第 53–59 行，例如 `/api/v1/auth/login`、`/api/v1/auth/refresh`。

### 5.2 从请求头拿出 accessToken

**代码位置：** `backend/land/src/main/java/com/skylink/land/auth/JwtAuthenticationInterceptor.java` 第 23–37 行。

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 浏览器跨域预检 OPTIONS 请求不需要登录，直接放行。
    if (CorsUtils.isPreFlightRequest(request)) {
        return true;
    }

    // 从 Authorization 请求头读取内容。
    String authorization = request.getHeader(properties.getHeader());

    // 如果请求头不存在，或不是以 "Bearer " 开头，说明没有携带正确 Token。
    if (!StringUtils.hasText(authorization)
        || !authorization.startsWith(properties.getTokenPrefix())) {
        throw new UnauthorizedException("请先登录");
    }

    // 去掉固定前缀 "Bearer "，得到真正的 JWT 字符串。
    String token = authorization
        .substring(properties.getTokenPrefix().length())
        .trim();

    // 注意：这里调用的是 parseAccessToken，refreshToken 不能通过此处验证。
    JwtClaims claims = tokenProvider.parseAccessToken(token);

    // 将验证成功后的用户信息放到当前请求的上下文，供 Controller/Service 取用。
    AuthContext.setCurrentUser(tokenProvider.toAuthenticatedUser(claims));
    return true; // 返回 true 表示可以继续进入 Controller 方法。
}
```

### 5.3 真正的验签、验签发者、验类型、验过期

**代码位置：** `backend/land/src/main/java/com/skylink/land/auth/JwtTokenProvider.java` 第 83–119 行。

```java
private JwtClaims parseToken(String token, String expectedTokenType) {
    // 1. Token 不能为空。
    if (!StringUtils.hasText(token)) {
        throw new UnauthorizedException("Token 不能为空");
    }

    // 2. JWT 必须由 Header、Payload、Signature 三段组成。
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
        throw new UnauthorizedException("Token 格式错误");
    }

    // 3. 使用服务器保存的 secret，对 Header.Payload 重新计算一次签名。
    String unsignedToken = parts[0] + "." + parts[1];

    // 4. 将新签名与客户端带来的第三段签名比较。
    //    不同说明内容被改过，或 Token 根本不是本系统发的。
    if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
        throw new UnauthorizedException("Token 签名无效");
    }

    // 5. 签名通过后，才读取 Payload 内容。
    Map<String, Object> payload = decodeJson(parts[1]);

    // 6. 检查签发者，避免接收其他系统生成的 Token。
    if (!properties.getIssuer().equals(payload.get("iss"))) {
        throw new UnauthorizedException("Token 签发方无效");
    }

    // 7. 检查 Token 类型。
    //    业务接口要求 access；刷新接口要求 refresh，不能互相冒充。
    if (StringUtils.hasText(expectedTokenType)
        && !expectedTokenType.equals(payload.get("token_type"))) {
        throw new UnauthorizedException("Token 类型无效");
    }

    // 8. 取出 exp，并与服务器当前时间比较；过期就拒绝。
    Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
    if (Instant.now().isAfter(expiresAt)) {
        throw new UnauthorizedException("Token 已过期");
    }

    // 9. 将 Payload 转成项目自己的 JwtClaims 对象，交给后续业务代码使用。
    return JwtClaims.builder()
        .userId(Long.valueOf(String.valueOf(payload.get("sub"))))
        .username(String.valueOf(payload.get("username")))
        .tokenType(String.valueOf(payload.get("token_type")))
        .roles(asStringList(payload.get("roles")))
        .issuedAt(Instant.ofEpochSecond(asLong(payload.get("iat"))))
        .expiresAt(expiresAt)
        .build();
}
```

这里的检查顺序很重要：**先验签，再相信 Payload。** 因为没有验签之前，Token 中的任何内容都可能是攻击者伪造的。

### 5.4 验证后的用户信息放在哪里

**代码位置：** `backend/land/src/main/java/com/skylink/land/auth/AuthContext.java` 第 5–28 行。

```java
public final class AuthContext {
    // ThreadLocal 表示“当前处理这一个 HTTP 请求的线程专用数据”。
    // 不同用户的并发请求不会互相覆盖当前用户信息。
    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(AuthenticatedUser user) {
        // JWT 已验证通过后，把当前用户保存起来。
        CURRENT_USER.set(user);
    }

    public static Long requireUserId() {
        // 业务代码调用这个方法即可得到当前登录用户的 ID。
        // 如果没有登录用户，直接抛出异常，避免误以匿名身份继续处理。
        return getCurrentUser()
            .map(AuthenticatedUser::getUserId)
            .orElseThrow(() -> new IllegalStateException("Current user is missing"));
    }

    public static void clear() {
        // 请求结束必须清理，防止线程被复用时串到下一位用户。
        CURRENT_USER.remove();
    }
}
```

清理发生在拦截器的请求结束阶段：

**代码位置：** `JwtAuthenticationInterceptor.java` 第 40–43 行。

```java
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                            Object handler, Exception ex) {
    // 无论接口成功还是抛异常，最终都移除当前用户信息。
    AuthContext.clear();
}
```

---

## 6. accessToken 过期后，refreshToken 如何换新 Token

accessToken 故意设置得较短。它过期时，前端调用 `/api/v1/auth/refresh`；浏览器会自动携带 HttpOnly Cookie 中的 refreshToken。

**代码位置：** `backend/land/src/main/java/com/skylink/land/controller/AuthController.java` 第 42–50 行。

```java
@PostMapping("/refresh")
public AuthDto.TokenResponse refresh(
    // 从 Cookie 读取 refreshToken；required = false 表示 Cookie 缺失时参数会是 null。
    @CookieValue(name = "${skylink.jwt.refresh-cookie.name}", required = false)
    String refreshToken,
    HttpServletResponse response
) {
    // 验证 refreshToken，并生成一对全新的 Token。
    TokenPair tokenPair = authService.refresh(refreshToken);

    // 用新的 refreshToken 覆盖旧 Cookie，实现 refreshToken 轮换。
    refreshCookieManager.addRefreshToken(response, tokenPair.getRefreshToken());

    // 把新的 accessToken 返回给前端。
    return tokenPair.toResponse();
}
```

**代码位置：** `backend/land/src/main/java/com/skylink/land/service/auth/impl/AuthServiceImpl.java` 第 109–121 行。

```java
public TokenPair refresh(String refreshToken) {
    // parseRefreshToken 会验签、验过期，并额外确认 token_type 必须等于 refresh。
    JwtClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);

    // 即使 refreshToken 本身有效，也再次查询数据库确认用户仍存在。
    User user = userMapper.selectById(claims.getUserId());
    if (user == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid refresh token");
    }

    // 用户被禁用后，不能靠旧 Token 继续获得新 Token。
    if (!Integer.valueOf(1).equals(user.getStatus())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "user is disabled");
    }

    // 用户状态正常，重新签发 accessToken 与 refreshToken。
    return issueTokens(user);
}
```

## 7. 最后用“人话”串起整个流程

1. 用户提交账号和密码给 `/api/v1/auth/login`。
2. 后端确认密码正确、账号未禁用。
3. 后端将用户 ID、用户名、角色、过期时间写入 JWT Payload。
4. 后端用 `JWT_SECRET` 对内容签名，生成 accessToken 与 refreshToken。
5. 前端以后请求业务接口时带上 `Authorization: Bearer accessToken`。
6. JWT 拦截器检查格式、签名、签发者、类型与过期时间。
7. 通过后，把用户信息存入 `AuthContext`，接口可用 `AuthContext.requireUserId()` 得到当前用户 ID。
8. accessToken 过期时，使用 Cookie 中的 refreshToken 调用刷新接口，换取一对新 Token。

## 8. 常见误解

| 误解 | 正确理解 |
| --- | --- |
| JWT 内容看不到 | 错。Payload 可以被解码查看，只是不能被安全地篡改。 |
| 有 Token 就永远有效 | 错。本项目会检查 `exp` 过期时间。 |
| refreshToken 可以调用业务接口 | 错。业务拦截器明确要求 `token_type=access`。 |
| 前端可以保存 `JWT_SECRET` | 绝对不可以。密钥只能在后端环境变量中保存。 |
| 验证 JWT 后就不需要查数据库 | 不完全对。普通接口依赖 JWT 身份；刷新 Token 时本项目仍会查询用户是否存在、是否被禁用。 |

