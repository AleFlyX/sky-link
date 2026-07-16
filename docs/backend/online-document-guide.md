# 在线文档：权限、协同票据与 Yjs 实时编辑（小白注释版）

> 本文对应 Sky Link 当前的在线文档实现。它把“文档标题/权限管理”和“多人同时编辑正文”分开处理：前者是普通 REST，后者是 Yjs + Hocuspocus WebSocket 协同。

## 1. 先用一句话理解

在线协作文档不是每打一个字就调用一次 `PUT /documents/{id}`，而是让每个浏览器维护一份 Yjs 文档，并与协同 WebSocket 服务同步修改：

```text
进入文档页
  → REST 获取文档元数据和权限
  → REST 申请 60 秒有效的协同票据（ticket）
  → 用 ticket 连接 Hocuspocus 协同 WebSocket
  → Yjs 在多位用户之间合并编辑操作
  → Tiptap 编辑器显示同步后的共同内容
```

## 2. 系统边界：谁负责什么

| 部分 | 负责内容 |
| --- | --- |
| Spring Boot 主后端 | JWT 身份、文档 CRUD、权限、签发协同 ticket、提供内部二次授权接口。 |
| 前端 Vue | 获取 ticket、创建 Yjs 文档、连接协同服务、创建 Tiptap 编辑器。 |
| Hocuspocus 协同服务 | 接受 Yjs WebSocket 连接、同步正文更新、向客户端发已保存事件。其实现不在当前仓库这组 Java 源码中。 |
| 数据库 | 保存文档基础信息、权限数据，以及协同文档状态。 |

所以要区分两种“保存”：标题、可见范围等元数据通过 REST 保存；协同正文由 Yjs 协同链路保存和同步。

## 3. 关键文件地图

| 用途 | 文件位置 |
| --- | --- |
| 文档 REST 接口 | `backend/land/src/main/java/com/skylink/land/controller/DocumentController.java` |
| 文档 CRUD、权限判断 | `backend/land/src/main/java/com/skylink/land/service/document/impl/DocumentServiceImpl.java` |
| 协同 ticket 签发与二次授权 | `backend/land/src/main/java/com/skylink/land/service/document/DocumentCollaborationService.java` |
| 临时 ticket 的生成/验证 | `backend/land/src/main/java/com/skylink/land/collaboration/CollaborationTicketProvider.java` |
| 协同服务调用的内部授权接口 | `backend/land/src/main/java/com/skylink/land/controller/InternalCollaborationController.java` |
| 前端文档 API | `frontend/sky-link-frontend/src/api/document.js` |
| 前端 Yjs/Hocuspocus 连接 | `frontend/sky-link-frontend/src/views/documents/composables/useCollaborationSession.js` |
| Tiptap 编辑页 | `frontend/sky-link-frontend/src/views/documents/CollaborativeDocumentView.vue` |

---

## 4. 文档权限：先决定“能不能看、能不能改”

### 4.1 Controller 既做 JWT 身份，也做功能权限

**代码位置：** `DocumentController.java` 第 21–39 行。

```java
@PostMapping
@RequirePermission("document:create") // 系统级权限：该账号是否有创建文档的功能权限
public DocumentDetailResponse create(@RequestBody CreateDocumentRequest request) {
    // JWT 已通过后，从 AuthContext 取得真实用户 ID，不信任前端传入的作者 ID。
    return service.createDocument(AuthContext.requireUserId(), request);
}

@GetMapping("/{documentId}")
@RequirePermission("document:get") // 系统级权限：是否能使用“查看文档”功能
public DocumentDetailResponse get(@PathVariable Long documentId) {
    // Service 内还有第二层“这篇具体文档是否授权给此人”的检查。
    return service.getDocument(AuthContext.requireUserId(), documentId);
}

@PostMapping("/{documentId}/collaboration-ticket")
@RequirePermission("document:get")
public CollaborationTicketResponse ticket(@PathVariable Long documentId) {
    // 只有可以查看该文档的人，才有资格申请连接协同服务的临时票据。
    return collaborationService.issueTicket(AuthContext.requireUserId(), documentId);
}
```

可以把它记成两道门：

1. `@RequirePermission`：你是否有“文档功能”的系统权限；
2. `resolvePermission`：你是否被授权访问“这一篇具体文档”。

### 4.2 每篇文档最终的权限如何计算

**代码位置：** `DocumentServiceImpl.java` 第 235–248 行。

```java
public String resolvePermission(Long userId, Document document) {
    requireUser(userId) // 先确认请求用户存在且可用。
    int level;

    // 创建者和管理员直接拥有 manage（管理）权限。
    if (Objects.equals(document.getCreatorId(), userId) || isAdministrator(userId)) {
        level = PERMISSION_MANAGE;
    } else {
        // 优先读取“对这个用户单独授予”的权限。
        Integer granted = permissionMapper.selectEffectivePermission(document.getDocumentId(), userId);

        // 若没有个人授权且文档是团队可见，允许同部门用户只读。
        if (granted == null && isSameDepartmentShared(userId, document)) {
            granted = PERMISSION_READ;
        }

        // 两种授权都没有，返回 null，调用方会拒绝访问。
        if (granted == null) return null;
        level = granted;
    }

    // 已归档的文档不管原先权限多高，统一降为只读。
    if (document.getStatus() == STATUS_ARCHIVED) level = PERMISSION_READ;
    return permissionName(level); // 将数字权限转为 read、edit、manage。
}
```

| 文档级权限 | 意义 |
| --- | --- |
| `read` | 可以查看，编辑器只读。 |
| `edit` | 可以修改协同正文和允许的普通字段。 |
| `manage` | 可以管理人员授权、删除文档、改变可见范围。 |

---

## 5. 普通文档操作：REST 管理元数据

前端 API 都集中在下面的文件中。

**代码位置：** `frontend/sky-link-frontend/src/api/document.js` 第 3–48 行。

```js
export function getDocument(id) {
  return request.get(`/documents/${id}`) // 读取标题、权限、状态等基础信息。
}

export function updateDocument(documentId, data) {
  // 用于标题、可见范围等“非协同正文”字段。
  return request.put(`/documents/${documentId}`, data)
}

export function createCollaborationTicket(documentId) {
  // 不直接连接协同服务器；先由主后端发一张短期 ticket。
  return request.post(`/documents/${documentId}/collaboration-ticket`)
}
```

文档内容有一个重要保护：如果该文档已存在协同状态，不能再通过普通 REST 更新正文。

**代码位置：** `DocumentServiceImpl.java` 第 133–166 行。

```java
public DocumentDetailResponse updateDocument(Long userId, Long documentId, UpdateDocumentRequest request) {
    Document document = requireDocument(documentId);

    // 归档文档不允许改标题/内容；普通文档至少要求 edit 权限。
    if (document.getStatus() == STATUS_ARCHIVED) {
        if (request.getTitle() != null || request.getContent() != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "archived document is read-only");
        }
    } else {
        requireAtLeast(userId, document, PERMISSION_EDIT);
    }

    // 协同正文必须走 Yjs。这样不会发生 REST 覆盖实时编辑内容的情况。
    if (request.getContent() != null && collaborationStateMapper.selectById(documentId) != null) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "collaborative document content must be updated through Yjs");
    }

    // 标题、状态等元数据仍使用 REST 更新。
    if (request.getTitle() != null) document.setTitle(request.getTitle().trim());
    if (request.getStatus() != null) document.setStatus(parseStatus(request.getStatus(), document.getStatus()));
    documentMapper.updateById(document);
    return getDocument(userId, documentId);
}
```

这就是为什么页面提示“标题、可见范围和协同内容彼此分离”。

---

## 6. 协同 ticket：不是登录 JWT，而是短期通行证

登录 JWT 用于证明“你是谁”；协同 ticket 用于证明“你可以在短时间内访问哪一篇文档、有什么文档级权限”。

**配置位置：** `backend/land/src/main/resources/application.yaml` 第 62–66 行。

```yaml
skylink:
  collaboration:
    ticket-secret: ${SKYLINK_COLLABORATION_TICKET_SECRET} # 专用于协同票据的签名密钥
    ticket-ttl: 60s                                       # ticket 默认只活 60 秒
    websocket-url: ${SKYLINK_COLLABORATION_WEBSOCKET_URL} # Hocuspocus 服务地址
    service-token: ${SKYLINK_COLLABORATION_SERVICE_TOKEN} # 协同服务调用内部授权接口的凭据
```

### 6.1 主后端签发 ticket 前会重新检查文档权限

**代码位置：** `DocumentCollaborationService.java` 第 32–39、49–58 行。

```java
public CollaborationTicketResponse issueTicket(Long userId, Long documentId) {
    // 每次申请都实时检查用户、文档、用户状态和文档权限。
    Authorization authorization = authorize(userId, documentId);
    if (!authorization.allowed()) {
        // 故意返回“找不到文档”，避免泄露某篇无权文档的存在。
        throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
    }

    // ticket 只带本次协同连接需要的最小信息。
    CollaborationTicket ticket = ticketProvider.issue(
        userId, documentId, authorization.permission(), authorization.displayName());

    return CollaborationTicketResponse.builder()
        .token(ticket.token())                   // 前端交给协同 WebSocket 服务的临时令牌
        .websocketUrl(properties.getWebsocketUrl())
        .expiresAt(ticket.expiresAt())
        .permission(authorization.permission())
        .build();
}
```

### 6.2 ticket 内部携带什么，怎样防伪造

**代码位置：** `CollaborationTicketProvider.java` 第 29–42 行。

```java
public CollaborationTicket issue(Long userId, Long documentId, String permission, String displayName) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(properties.getTicketTtl()); // 60 秒后失效。

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("iss", properties.getIssuer());       // 谁签发
    payload.put("aud", properties.getAudience());     // 交给哪个协同系统使用
    payload.put("sub", String.valueOf(userId));       // 用户 ID
    payload.put("documentId", documentId);            // 只允许连接的那篇文档
    payload.put("permission", permission);            // read/edit/manage
    payload.put("displayName", displayName);          // 光标协作者名称
    payload.put("jti", UUID.randomUUID().toString()); // 此次票据的唯一编号
    payload.put("iat", now.getEpochSecond());         // 签发时间
    payload.put("exp", expiresAt.getEpochSecond());   // 过期时间

    String header = encode(Map.of("alg", "HS256", "typ", "JWT"));
    String body = encode(payload);
    String unsigned = header + "." + body;

    // 用 collaboration.ticket-secret 签名，不与用户登录 JWT 共用密钥。
    return new CollaborationTicket(unsigned + "." + sign(unsigned), expiresAt);
}
```

协同服务验证 ticket 时会检查三段格式、签名、算法、签发者、受众、用户 ID、文档 ID、权限枚举和过期时间。对应 `CollaborationTicketProvider.java` 第 44–64 行。

---

## 7. 前端如何连接 Yjs 协同会话

**代码位置：** `frontend/sky-link-frontend/src/views/documents/composables/useCollaborationSession.js` 第 6–79 行。

```js
export function useCollaborationSession(documentId) {
  // 每一个打开的文档页面都创建自己的 Yjs 内存文档。
  const ydoc = new Y.Doc()
  const provider = ref(null)
  const permission = ref('read')

  const editable = computed(() =>
    // 只有 edit/manage 且未进入只读状态，编辑器才能输入。
    ['edit', 'manage'].includes(permission.value) && status.value !== 'readonly',
  )

  async function connect() {
    const firstTicket = await requestTicket() // REST 向主后端申请短期 ticket。
    permission.value = firstTicket.permission

    provider.value = new HocuspocusProvider({
      url: firstTicket.websocketUrl, // 协同服务，不是 Spring 的 /ws/messages。
      name: String(documentId),      // 房间名：同一文档 ID 的用户进入同一协同房间。
      document: ydoc,                // 要同步的 Yjs 文档实例。
      token: async () => {
        // Hocuspocus 需要认证时交出 ticket；需要时可重新向后端申请新的。
        const ticket = firstTicket || (await requestTicket())
        permission.value = ticket.permission
        return ticket.token
      },
    })
  }
}
```

连接状态监听也很重要：

```js
provider.value.on('status', ({ status: providerStatus }) => {
  if (providerStatus === 'connected') status.value = 'syncing' // 已连上，正在同步初始内容。
  else if (connectedOnce) status.value = 'offline'             // 曾连上后断线，提示离线。
})

provider.value.on('synced', () => {
  // 服务端同步完成；只读用户显示 readonly，编辑用户显示 synced。
  status.value = permission.value === 'read' ? 'readonly' : 'synced'
})

ydoc.on('update', (_update, origin) => {
  // 本地修改不是 provider 带回来的远端修改，并且用户可编辑时，显示“保存中”。
  if (origin !== provider.value && editable.value) status.value = 'saving'
})
```

## 8. Tiptap 编辑器怎样接入 Yjs

**代码位置：** `CollaborativeDocumentView.vue` 第 83–110 行。

```js
async function load() {
  // 先读取标题、状态、创建人等元数据；这一步是普通 REST。
  const payload = await getDocument(documentId)
  document.value = payload?.data ?? payload

  // 再建立 Yjs/Hocuspocus 协同连接，取得实时正文状态。
  await session.connect()

  editor.value = new Editor({
    editable: session.editable.value, // read 用户看到的是不可编辑的编辑器。
    extensions: [
      StarterKit.configure({ undoRedo: false }),
      Image.configure({ allowBase64: true, inline: false }),
      Table.configure({ resizable: true }),

      // Collaboration 将 Tiptap 的默认内容字段绑定到同一个 Y.Doc。
      // 用户的输入会成为 Yjs update，而非调用普通 PUT 更新 content。
      Collaboration.configure({ document: session.ydoc, field: 'default' }),

      // 显示其他协作者的光标、名字和颜色。
      CollaborationCaret.configure({
        provider: session.provider.value,
        user: { name: userStore.displayName || '协作者', color: userColor },
      }),
    ],
  })
}
```

`StarterKit.configure({ undoRedo: false })` 是有意设置：协同编辑的撤销/重做需要与共享文档的更新模型协调，不使用普通本地历史功能。

## 9. 协同服务如何再次询问主后端

主后端提供一个仅供协同服务调用的内部接口：

**代码位置：** `InternalCollaborationController.java` 第 13–19 行。

```java
@PostMapping("/authorize")
public CollaborationAuthorizationResponse authorize(
    // 协同服务必须携带服务间密钥；普通浏览器不应直接调用。
    @RequestHeader("X-SkyLink-Service-Token") String serviceToken,
    @RequestBody CollaborationAuthorizationRequest request
) {
    // 根据 userId 和 documentId 重新确认：用户仍可用吗？权限还存在吗？文档归档了吗？
    return service.reauthorize(serviceToken, request.getUserId(), request.getDocumentId());
}
```

`DocumentCollaborationService.reauthorize` 会使用常量时间比较服务密钥，并返回 `allowed`、`permission`、`displayName` 与 `documentStatus`。这让协同服务能够在需要时向“权限真相来源”重新确认授权。

## 10. 一次多人编辑的完整复盘

1. 用户带 JWT 调用 `GET /documents/{id}`，主后端确认其能访问该文档。
2. 前端调用 `POST /documents/{id}/collaboration-ticket`，主后端再次做文档级权限检查。
3. 主后端签发只对该用户、该文档、该权限有效且 60 秒过期的 ticket。
4. 前端用 ticket 连接 Hocuspocus，房间名为 `documentId`。
5. Tiptap 操作写进本地 `Y.Doc`；Yjs 将可合并的更新同步给协同服务和其他用户。
6. read 用户的编辑器禁用输入；edit/manage 用户可以编辑；归档文档统一只读。
7. 标题、可见范围、权限管理等仍通过 REST 修改，协同正文不能再用 REST 覆盖。
8. 页面离开时执行 `provider.destroy()` 和 `ydoc.destroy()`，关闭连接并释放内存。

## 11. 初学者最容易混淆的点

| 容易混淆 | 正确理解 |
| --- | --- |
| JWT 和协同 ticket 是同一个东西 | 不是。JWT 是登录身份；ticket 是 60 秒的、仅针对一篇文档的协同通行证。 |
| 文档正文通过 `PUT /documents/{id}` 实时保存 | 协同状态存在时不是。正文必须由 Yjs 同步。 |
| `read` 用户连上协同服务就能修改 | 不能。`editable` 只对 `edit`、`manage` 返回真。 |
| 归档只是视觉状态 | 不是。后端会把归档文档权限降为 `read`，并拒绝修改正文和标题。 |
| Hocuspocus 服务就是 Spring 的聊天 WebSocket | 不是。聊天地址是 `/ws/messages`；文档协同地址来自 `skylink.collaboration.websocket-url`。 |

