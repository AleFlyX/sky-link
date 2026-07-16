# 在线聊天：REST 保存消息 + WebSocket 实时推送（小白注释版）

> 本文对应 Sky Link 当前的“消息中心”实现。它不是“前端通过 WebSocket 把消息发给后端”的设计，而是：**用 HTTP REST 接口发送并保存消息，用 WebSocket 把保存成功的结果实时推送给在线用户。**

## 1. 先用一句话理解

聊天的可靠性来自数据库，实时性来自 WebSocket：

```text
发送者点击发送
  → REST 接口验证身份和聊天资格
  → 写入 message 表（成功提交事务）
  → WebSocket 推送给发送者和接收者的在线页面
  → 两端页面立即更新；不在线的人下次用 REST 查询历史记录
```

这意味着 WebSocket 推送失败不会让消息丢失：消息已经先保存到数据库。

## 2. 关键文件地图

| 用途 | 文件位置 |
| --- | --- |
| 消息 REST 接口 | `backend/land/src/main/java/com/skylink/land/controller/MessageController.java` |
| 发送、查询、撤回等核心业务 | `backend/land/src/main/java/com/skylink/land/service/chat/impl/MessageServiceImpl.java` |
| WebSocket 地址注册 | `backend/land/src/main/java/com/skylink/land/config/WebSocketConfiguration.java` |
| WebSocket 握手 JWT 验证 | `backend/land/src/main/java/com/skylink/land/websocket/WebSocketAuthHandshakeInterceptor.java` |
| 在线连接登记与定向发送 | `backend/land/src/main/java/com/skylink/land/websocket/MessageWebSocketSessionRegistry.java` |
| 推送事件封装 | `backend/land/src/main/java/com/skylink/land/websocket/MessagePushService.java` |
| 前端聊天状态与 WebSocket | `frontend/sky-link-frontend/src/views/messages/composables/useMessageCenter.js` |
| 前端 HTTP 请求封装 | `frontend/sky-link-frontend/src/utils/request.js` |

---

## 3. 两种通信各做什么

| 场景 | 使用方式 | 为什么 |
| --- | --- | --- |
| 发消息 | `POST /api/v1/messages` | 需要验证、写数据库、返回明确成功/失败结果。 |
| 查会话 | `GET /api/v1/messages/sessions` | 页面首次加载时拿到历史会话。 |
| 查历史消息 | `GET /api/v1/messages` | 支持分页和“加载更早消息”。 |
| 撤回消息 | `DELETE /api/v1/messages/{messageId}` | 需要严格检查发送者和两分钟限制。 |
| 接收新消息/撤回结果 | `ws://.../ws/messages` | 服务器能主动把变化推到在线页面。 |

REST 是“客户端主动问服务器”，WebSocket 是“服务器可以主动通知客户端”。本项目同时用两者，各司其职。

---

## 4. 前端怎样带上登录身份

### 4.1 REST 请求：自动添加 Authorization

**代码位置：** `frontend/sky-link-frontend/src/utils/request.js` 第 28–40 行。

```js
function attachAuthToken(config) {
  // 从浏览器 localStorage 读取登录成功时保存的 accessToken。
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    // 每个普通 HTTP 请求都会自动获得这个请求头。
    // 最终效果：Authorization: Bearer <accessToken>
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config // 把加好请求头的配置交给 axios 继续发送。
}
```

后端 JWT 拦截器验证成功后，会把用户放进 `AuthContext`。因此聊天 Controller 不相信前端提交的“发送者 ID”，而是自己取得当前登录用户：

**代码位置：** `MessageController.java` 第 27–30 行。

```java
@PostMapping
public ApiResponse<MessageDto.MessageResponse> sendMessage(
    @RequestBody MessageDto.SendMessageRequest request // 前端只提交聊天目标、类型和内容
) {
    // AuthContext.requireUserId() 来自已验证的 JWT，不能由前端伪造。
    return ApiResponse.success(
        "message sent",
        messageService.sendMessage(AuthContext.requireUserId(), request)
    );
}
```

### 4.2 WebSocket：把 Token 放进连接地址

浏览器原生 `WebSocket` API 不方便像 axios 一样自定义 `Authorization` 请求头，所以本项目把 accessToken 放进 URL 查询参数。

**代码位置：** `useMessageCenter.js` 第 66–81 行。

```js
function buildWebSocketUrl() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null // 没有登录 Token 就不建立聊天实时连接。

  // 优先使用环境变量配置的 WebSocket 地址；未配置时使用当前站点地址。
  const apiBase = import.meta.env.VITE_WS_URL || import.meta.env.VITE_API_BASE_URL || window.location.origin
  const url = new URL(apiBase, window.location.origin)

  // HTTPS 页面必须使用 wss，HTTP 页面使用 ws。
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = import.meta.env.VITE_WS_URL ? url.pathname : '/ws/messages'
  url.search = ''

  // 当前实现把 Token 放在 ?token= 中，后端握手拦截器会读取它。
  url.searchParams.set('token', token)
  return url.toString()
}
```

> 维护提示：URL 查询参数可能进入代理访问日志；生产环境应使用 HTTPS/WSS，并确保网关不要记录完整敏感查询参数。

---

## 5. WebSocket 如何建立并验证身份

### 5.1 后端注册聊天 WebSocket 地址

**代码位置：** `backend/land/src/main/java/com/skylink/land/config/WebSocketConfiguration.java` 第 31–36 行。

```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(messageWebSocketHandler, "/ws/messages")
        // 在真正建立连接前，先执行 JWT 认证。
        .addInterceptors(authHandshakeInterceptor)

        // 只允许配置过的前端来源连接，避免任意网页建立 WebSocket。
        .setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new));
}
```

### 5.2 握手拦截器验证 Token

**代码位置：** `backend/land/src/main/java/com/skylink/land/websocket/WebSocketAuthHandshakeInterceptor.java` 第 29–50 行。

```java
public boolean beforeHandshake(..., Map<String, Object> attributes) {
    try {
        // 从 Authorization 头或 URL 的 ?token= 参数取 Token。
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false; // 没 Token，拒绝升级为 WebSocket。
        }

        // 验证签名、签发者和过期时间；失败会抛 UnauthorizedException。
        JwtClaims claims = jwtTokenProvider.parseToken(token);

        // 握手成功后，下面两个值会挂在 WebSocketSession 上。
        // 后面的连接登记绝不依赖客户端自己声称的 userId。
        attributes.put(WebSocketSessionKeys.USER_ID, claims.getUserId());
        attributes.put(WebSocketSessionKeys.USERNAME, claims.getUsername());
        return true;
    } catch (UnauthorizedException exception) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }
}
```

注意：这里调用的是 `parseToken(token)`，它验证签名和过期时间，但**当前代码没有限定 `token_type=access`**；REST 业务接口则使用 `parseAccessToken`。这是阅读当前实现时需要特别知道的差异。

### 5.3 一个用户可以有多个在线页面

**代码位置：** `MessageWebSocketSessionRegistry.java` 第 14–18、37–59 行。

```java
// key 是用户 ID；value 是该用户在浏览器多个标签页/设备上的连接集合。
private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

public void register(Long userId, WebSocketSession session) {
    // 没有集合就新建；同一用户的每个页面连接都加入集合。
    userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
}

public void sendToUsers(Collection<Long> userIds, String payload) {
    for (Long userId : userIds) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) continue; // 对方不在线，跳过推送。

        for (WebSocketSession session : sessions.toArray(WebSocketSession[]::new)) {
            if (!session.isOpen()) { unregister(session); continue; }
            try {
                // 同一个 WebSocket 连接不能被多个线程同时写入，因此加锁。
                synchronized (session) {
                    if (session.isOpen()) session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException exception) {
                unregister(session); // 网络已坏，移除失效连接。
            }
        }
    }
}
```

---

## 6. 发送一条消息的完整流程

### 6.1 前端发送 REST 请求

**代码位置：** `useMessageCenter.js` 第 294–319 行。

```js
async function handleSend() {
  const content = draft.value.trim() // 去掉用户输入两端多余空格。
  if (!content || !activeSessionId.value) return

  try {
    // activeSessionId 会被转换成 receiverId 或 groupId，随后 POST 到后端。
    const result = await sendWorkspaceMessage(activeSessionId.value, {
      messageType: 'text',
      content,
    })

    // REST 成功后立刻把自己的消息显示出来。
    // 即使稍后 WebSocket 又推同一条，upsertMessage 也会按消息 ID 合并，不会重复。
    const created = result.data || result
    draft.value = ''
    if (created) messages.value = upsertMessage(messages.value, created)
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  }
}
```

### 6.2 后端验证聊天目标和内容

**代码位置：** `MessageServiceImpl.java` 第 94–110 行。

```java
@Transactional(rollbackFor = Exception.class) // 任一步失败，数据库操作整体回滚。
public MessageDto.MessageResponse sendMessage(Long currentUserId, SendMessageRequest request) {
    if (request == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");

    // receiverId（单聊对象）与 groupId（群聊对象）必须恰好传一个：
    // 不能两个都传，也不能两个都不传。
    requireExactlyOneTarget(request.getReceiverId(), request.getGroupId());

    // 确认发送者账号仍存在且处于启用状态。
    User sender = requireEnabledUser(currentUserId, "sender user not found");

    // 清理并校验内容，转换文本/表情的消息类型为数据库数字编码。
    String content = normalizeContent(request.getContent());
    int messageType = toMessageTypeCode(request.getMessageType());

    // 按单聊或群聊分别做权限判断、入库和推送。
    if (request.getReceiverId() != null) {
        return sendSingleMessage(currentUserId, sender, request.getReceiverId(), messageType, content);
    }
    return sendGroupMessage(currentUserId, sender, request.getGroupId(), messageType, content);
}
```

单聊会确认接收者存在、未被禁用、双方具备单聊关系；群聊会确认发送者属于该群。也就是说，前端即使手工改了 `receiverId` 或 `groupId`，后端仍会重新做访问检查。

### 6.3 先提交数据库，再做实时推送

**代码位置：** `MessageServiceImpl.java` 第 219–251 行。

```java
private MessageDto.MessageResponse sendSingleMessage(...) {
    // 禁止给自己发送单聊消息。
    if (currentUserId.equals(receiverId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot send a message to yourself");
    }

    User receiver = requireEnabledUser(receiverId, "receiver user not found");
    requireSingleChatAccess(currentUserId, receiverId); // 验证双方聊天资格。

    // createMessage 内部会插入 message 表；到这里消息已经准备持久化。
    Message message = createMessage(currentUserId, receiverId, null, messageType, content);
    MessageDto.MessageResponse response = toMessageResponse(message, displayName(sender));

    // 为发送者和接收者分别组装会话预览：对不同用户，“聊天对象”不同。
    MessageDto.MessageSessionResponse senderSession = buildSessionResponse(
        SESSION_TYPE_SINGLE, receiverId, displayName(receiver), response);
    MessageDto.MessageSessionResponse receiverSession = buildSessionResponse(
        SESSION_TYPE_SINGLE, currentUserId, displayName(sender), response);

    runAfterCommit(() -> {
        // 只有数据库事务真正提交成功后才通知在线页面。
        // 这样不会出现“页面看到了消息、数据库却回滚”的假消息。
        messagePushService.push("message.created", response, senderSession, List.of(currentUserId));
        messagePushService.push("message.created", response, receiverSession, List.of(receiverId));
    });
    return response;
}
```

### 6.4 WebSocket 推送的数据结构

**代码位置：** `MessagePushService.java` 第 21–40 行。

```java
public void push(String type, MessageResponse message, MessageSessionResponse session,
                 Collection<Long> recipientIds) {
    // 给前端的事件包含“发生了什么、哪一条消息、哪个会话要更新”。
    MessageRealtimeEvent event = MessageRealtimeEvent.builder()
        .type(type)       // 例如 message.created 或消息撤回事件
        .message(message) // 消息正文和发送者信息
        .session(session) // 左侧会话列表要显示的最新预览
        .build();

    try {
        // Java 对象转 JSON 字符串，再只发送给指定用户的在线连接。
        sessionRegistry.sendToUsers(recipientIds, objectMapper.writeValueAsString(event));
    } catch (Exception exception) {
        throw new IllegalStateException("failed to serialize websocket message", exception);
    }
}
```

---

## 7. 前端怎样接收实时消息

**代码位置：** `useMessageCenter.js` 第 139–200 行。

```js
function connectSocket() {
  // 已经连接或正在连接时不重复创建连接。
  if (socket.value && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.value.readyState)) return

  const ws = new WebSocket(buildWebSocketUrl())
  socket.value = ws

  ws.onopen = () => { connectionState.value = 'connected' }

  ws.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data) // 后端推送的是 JSON 字符串。
      if (!payload?.type || !payload?.message) return // 格式不完整就忽略。

      // 无论用户当前是否打开这个会话，先更新左侧会话列表和最后一条预览。
      if (payload.session) sessions.value = upsertSession(sessions.value, payload.session)

      // 只有当前正打开该会话时，才把消息加入右侧聊天窗口。
      const key = getConversationKeyFromMessage(payload.message, currentUserId.value)
      if (key && key === activeSessionId.value) {
        messages.value = upsertMessage(messages.value, payload.message)
        scrollThreadToBottom(payload.type === 'message.created')
      }
    } catch (error) {
      console.warn('ignored websocket message', error) // 单个坏包不影响整条连接。
    }
  }

  ws.onclose = () => {
    socket.value = null
    if (!closingSocket.value) scheduleReconnect() // 非主动关闭时，3 秒后自动重连。
  }
}
```

## 8. 查询历史与撤回为什么仍用 REST

- 历史消息不是只依靠 WebSocket：页面进入会话时调用 `GET /messages`，并通过 `before` 参数加载更早消息。
- 撤回不是删除记录，而是检查“当前用户就是发送者、尚未撤回、发送不超过 120 秒”，再把撤回状态更新为已撤回，并推送更新事件。
- 用户离线时不会收到 WebSocket 推送；下次进入页面的会话与历史查询会从数据库恢复完整状态。

撤回的关键检查在 `MessageServiceImpl.java` 第 181–216 行，其中 `RECALL_WINDOW_SECONDS = 120`（第 58 行）。

## 9. 最后用人话复盘

1. JWT 证明“你是谁”；`AuthContext` 给后端拿到可信的用户 ID。
2. REST 接口证明“你有没有资格给这个人/群发消息”，并把消息写入数据库。
3. 数据库事务成功后，后端才将 `message.created` 事件用 WebSocket 定向推送。
4. 在线页面立即更新；离线页面以后从 REST 历史接口补齐。
5. 前端重连、消息按 ID 合并、后端一个用户可登记多个连接，共同保证实际使用体验。

