# 好友申请与通讯录：从选择用户到开始单聊（小白注释版）

> 本文讲解 Sky Link 当前的好友和通讯录实现：用户如何选择好友、提交申请、接收方如何同意或拒绝、数据库如何保存好友关系，以及成为好友后如何进入单聊。

## 1. 先用一句话理解

好友功能分为两张不同的关系表：

```text
申请阶段：friend_request（有 pending / accepted / rejected 状态）
    │
    └─ 接收方点击“同意”
         │
         ▼
正式好友：friendship（只表示两人已经是好友）
```

所以“好友申请”是一个有状态的流程；“好友关系”是申请同意后的最终结果。

## 2. 完整流程图

```text
通讯录页面加载
  ├─ GET /friends                 → 已有好友
  ├─ GET /friends/requests        → 收到的申请
  ├─ GET /friends/requests/sent   → 发出的申请
  └─ GET /users?page=1&size=500   → 添加好友弹窗的候选用户

选择用户并发送申请
  └─ POST /friends/requests
       → 写入 friend_request，状态 pending

接收方同意
  └─ PUT /friends/requests/{id} { action: "accept" }
       → 更新申请状态 accepted
       → 写入 friendship

点击好友的“聊天”
  └─ 跳转 /app/messages?type=single&id=对方ID
```

## 3. 关键文件地图

| 用途 | 文件位置 |
| --- | --- |
| 好友 REST 接口 | `backend/land/src/main/java/com/skylink/land/controller/FriendController.java` |
| 好友申请、同意、查询、删除的核心逻辑 | `backend/land/src/main/java/com/skylink/land/service/chat/impl/FriendServiceImpl.java` |
| 好友数据表定义 | `backend/land/src/main/resources/schema.sql` |
| 前端通讯录状态 | `frontend/sky-link-frontend/src/views/contacts/composables/useContactsDirectory.js` |
| 添加好友弹窗 | `frontend/sky-link-frontend/src/views/contacts/components/AddFriendDialog.vue` |
| 通讯录进入单聊 | `frontend/sky-link-frontend/src/views/contacts/ContactsView.vue` |
| 前端好友 API | `frontend/sky-link-frontend/src/api/friend.js` |
| 用户列表 API | `frontend/sky-link-frontend/src/api/user.js` |

---

## 4. 数据库：申请和正式关系为什么要分开

**代码位置：** `backend/land/src/main/resources/schema.sql` 第 89–113 行。

```sql
-- 好友申请表：记录“谁申请谁”、附言和当前审批状态。
CREATE TABLE `friend_request` (
  `request_id` BIGINT NOT NULL AUTO_INCREMENT,
  `requester_id` BIGINT NOT NULL,  -- 申请发起人
  `receiver_id` BIGINT NOT NULL,   -- 接收并决定是否同意的人
  `message` VARCHAR(255) DEFAULT NULL, -- 申请附言
  `status` TINYINT NOT NULL DEFAULT 0, -- 0待处理、1同意、2拒绝
  PRIMARY KEY (`request_id`),

  -- 禁止用户给自己发申请。
  CONSTRAINT `ck_friend_request_users` CHECK (`requester_id` <> `receiver_id`)
);

-- 好友关系表：只记录已经互相成为好友的两个人。
CREATE TABLE `friendship` (
  `user_id` BIGINT NOT NULL,        -- 两人中 ID 较小的一方
  `friend_user_id` BIGINT NOT NULL, -- 两人中 ID 较大的一方
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `friend_user_id`),

  -- 同一对好友无论从谁的角度看，都只存一条记录。
  CONSTRAINT `ck_friendship_user_order` CHECK (`user_id` < `friend_user_id`)
);
```

例如用户 8 和用户 20 成为好友，表中永远存 `(8, 20)`，不会出现 `(20, 8)`。这避免了同一对好友被重复保存两次。

## 5. 好友接口由谁调用

**代码位置：** `FriendController.java` 第 18–89 行。

```java
@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    @PostMapping("/requests")
    public ApiResponse<FriendRequestResultResponse> createRequest(
        @RequestBody CreateFriendRequest request
    ) {
        // 用户 ID 来自 JWT 验证后的 AuthContext，而不是前端自己提交。
        return ApiResponse.success(
            "request sent",
            friendService.createRequest(AuthContext.requireUserId(), request)
        );
    }

    @PutMapping("/requests/{requestId}")
    public ApiResponse<HandleFriendResponse> handleRequest(...) {
        // 只有真正收到这条申请的用户，Service 才会允许处理。
        return ApiResponse.success(..., friendService.handleRequest(AuthContext.requireUserId(), requestId, request));
    }

    @GetMapping
    public PageResponse<FriendItemResponse> listFriends(FriendListQueryRequest request) {
        // 只能查“当前登录用户自己的”好友列表。
        return friendService.listFriends(AuthContext.requireUserId(), request);
    }
}
```

该 Controller 没有单独的 `@RequirePermission`，但它仍在 `/api/v1/**` 路径下，因此必须先通过 JWT 登录验证。换句话说：**已登录用户可以使用好友申请功能，不能匿名调用。**

---

## 6. 添加好友弹窗的候选用户从哪里来

### 6.1 前端请求的是通用用户列表

**代码位置：** `useContactsDirectory.js` 第 229–242 行。

```js
async function loadSelectableUsers() {
  userOptionsLoading.value = true
  userOptionsError.value = ''

  try {
    // 这里没有传 departmentId：按设计，它请求的是最多 500 个用户的通用列表。
    const response = await getUsers({ page: 1, size: 500 })

    // 统一成弹窗所需的 userId、用户名、昵称、部门名等格式。
    selectableUsers.value = (response?.data?.records || []).map(normalizeUser)
  } catch (error) {
    // 请求失败时，清空候选项并显示“用户列表加载失败”。
    selectableUsers.value = []
    userOptionsError.value = error.message || '用户列表加载失败'
  } finally {
    userOptionsLoading.value = false
  }
}
```

因此，**当前代码没有“只请求同部门用户”的前端筛选条件**。部门名称只是显示在候选人标签中：

```js
label: `${user.nickname || user.username}${user.departmentName ? ` · ${user.departmentName}` : ''}`
```

### 6.2 前端还会过滤哪些人

**代码位置：** `useContactsDirectory.js` 第 135–151 行。

```js
const addFriendUserOptions = computed(() => {
  // 已经是好友的人，不应再出现在“添加好友”候选项中。
  const friendIds = new Set(friends.value.map((item) => Number(item.userId)))

  // 已经由当前用户发出且仍待处理的申请，也不应重复显示。
  const outgoingIds = new Set(
    outgoingRequests.value
      .filter((item) => item.status === 'pending')
      .map((item) => Number(item.userId)),
  )

  return selectableUsers.value
    .filter((user) => Number(user.userId) !== currentUserId.value) // 排除自己
    .filter((user) => !friendIds.has(Number(user.userId)))         // 排除已有好友
    .filter((user) => !outgoingIds.has(Number(user.userId)))       // 排除已发申请对象
    .map(normalizeUserOption)
})
```

这里也没有按 `departmentId` 过滤。

### 6.3 当前实现中的接口权限

候选用户复用了通用用户接口：

**代码位置：** `frontend/sky-link-frontend/src/api/user.js` 第 15–17 行。

```js
export function getUsers(params) {
  return request.get('/users', params)
}
```

后端 `GET /api/v1/users` 当前使用的是 `@RequirePermission("user:get")`（见 `UserController.java` 第 64–74 行），而默认 `ROLE_USER` 已拥有 `user:get`。

因此，当前代码中普通用户理论上可以读取该候选列表；“暂无可添加的用户”或“用户列表加载失败”不能简单归因于 `user:list` 权限，也不是同部门限制。还应检查接口返回数据、当前账号是否有有效 Token，以及前端是否处于演示数据降级状态。

---

## 7. 点击发送后，后端怎样防止无效申请

**代码位置：** `FriendServiceImpl.java` 第 49–89 行。

```java
@Transactional(rollbackFor = Exception.class)
public FriendRequestResultResponse createRequest(Long currentUserId, CreateFriendRequest request) {
    // 1. 前端必须提交目标用户 ID。
    if (request == null || request.getFriendUserId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "friendUserId is required");
    }

    Long targetUserId = request.getFriendUserId();

    // 2. 不允许加自己为好友。
    if (currentUserId.equals(targetUserId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "cannot add yourself as a friend");
    }

    // 3. 目标用户必须真实存在。
    User targetUser = userMapper.selectById(targetUserId);
    if (targetUser == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "target user not found");
    }

    // 4. 按“较小 ID、较大 ID”生成统一好友对，再检查是否已经是好友。
    FriendPair pair = FriendPair.of(currentUserId, targetUserId);
    if (friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId()) != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "you are already friends");
    }

    // 5. 双向检查是否已有待处理申请。
    FriendRequest pendingRequest = friendRequestMapper.selectPendingBetween(currentUserId, targetUserId);
    if (pendingRequest != null) {
        if (currentUserId.equals(pendingRequest.getRequesterId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "friend request already sent");
        }
        // 对方已经向我申请，我不该再反向发一条，应去处理收到的申请。
        throw new BusinessException(ErrorCode.CONFLICT, "incoming friend request is waiting for you");
    }

    // 6. 写入待处理申请。真正的 friendship 关系此刻还没有建立。
    FriendRequest friendRequest = new FriendRequest();
    friendRequest.setRequesterId(currentUserId);
    friendRequest.setReceiverId(targetUserId);
    friendRequest.setMessage(trimToNull(request.getMessage()));
    friendRequest.setStatus(STATUS_PENDING);
    friendRequestMapper.insert(friendRequest);

    return FriendRequestResultResponse.builder()
        .requestId(friendRequest.getRequestId())
        .status(toStatusName(friendRequest.getStatus())) // 返回 pending
        .build();
}
```

这里同样没有部门比较；只验证目标用户存在、申请关系没有冲突。

## 8. 接收方同意申请后发生什么

**代码位置：** `FriendServiceImpl.java` 第 91–150 行。

```java
@Transactional(rollbackFor = Exception.class)
public HandleFriendResponse handleRequest(Long currentUserId, Long requestId, HandleFriendRequest request) {
    FriendRequest friendRequest = friendRequestMapper.selectById(requestId);
    if (friendRequest == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "friend request not found");
    }

    // 安全边界：只有申请接收人可以决定同意或拒绝。
    if (!currentUserId.equals(friendRequest.getReceiverId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN,
            "only the receiver can process this friend request");
    }

    // 已经处理过的申请不可重复处理。
    if (!Integer.valueOf(STATUS_PENDING).equals(friendRequest.getStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "friend request has already been processed");
    }

    // 字符串 action 只能是 accept 或 reject，其他值会报参数错误。
    int nextStatus = switch (request.getAction().trim().toLowerCase()) {
        case "accept" -> STATUS_ACCEPTED;
        case "reject" -> STATUS_REJECTED;
        default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "action must be accept or reject");
    };

    // 更新时额外限定旧状态仍为 pending，避免两个并发请求都成功。
    int updated = friendRequestMapper.update(null,
        new LambdaUpdateWrapper<FriendRequest>()
            .eq(FriendRequest::getRequestId, requestId)
            .eq(FriendRequest::getStatus, STATUS_PENDING)
            .set(FriendRequest::getStatus, nextStatus));
    if (updated != 1) throw new BusinessException(ErrorCode.CONFLICT, "friend request has already been processed");

    // 拒绝到此结束：只留下 rejected 状态，不创建 friendship。
    if (nextStatus == STATUS_REJECTED) return null;

    // 同意时，按照统一的 ID 顺序插入正式好友关系。
    FriendPair pair = FriendPair.of(friendRequest.getRequesterId(), friendRequest.getReceiverId());
    if (friendshipMapper.selectByUsers(pair.userId(), pair.friendUserId()) == null) {
        Friendship friendship = new Friendship();
        friendship.setUserId(pair.userId());
        friendship.setFriendUserId(pair.friendUserId());
        friendshipMapper.insert(friendship);
    }
    // 整个方法带事务：任一写入失败，状态更新与好友关系都会回滚。
}
```

## 9. 前端怎样打开好友申请弹窗和提交

**代码位置：** `AddFriendDialog.vue` 第 49–59 行。

```js
function handleSubmit() {
  // 先做简单的前端体验校验；真正安全校验仍在后端。
  if (!form.friendUserId) {
    ElMessage.warning('请选择要添加的用户')
    return
  }

  // 弹窗不直接请求接口，而是把干净数据交给父组件。
  emit('submit', {
    friendUserId: Number(form.friendUserId),
    message: form.message.trim(),
  })
}
```

父组件状态逻辑接到数据后调用 `addFriend`，随后重新拉取好友、收到申请和已发申请，刷新页面显示：

**代码位置：** `useContactsDirectory.js` 第 244–250 行。

```js
async function handleAddFriend(form) {
  const result = await addFriend(form) // POST /friends/requests
  friendDialog.value = false
  ElMessage.success('好友申请已发送')
  await loadData() // 重新拉取列表，已发申请会进入 outgoingRequests
}
```

## 10. 成为好友后怎样进入在线聊天

**代码位置：** `ContactsView.vue` 第 107–121 行。

```js
function openSingleChat(friend) {
  // 好友数据中的 userId 是单聊的目标用户 ID。
  const targetId = friend.userId ?? friend.id
  if (!targetId) return

  router.push({
    path: '/app/messages',
    query: {
      type: 'single', // 明确这是单聊而不是群聊
      id: targetId,   // 聊天对象 ID
      name: friend.name || `用户#${targetId}`,
    },
  })
}
```

消息中心会读取这些路由参数，建立 `single-{targetId}` 会话。真正发送消息时，后端还会再次确认双方具备单聊资格；详见 [在线聊天讲解](online-chat-guide.md)。

## 11. 常见误解

| 容易混淆 | 正确理解 |
| --- | --- |
| 添加好友只允许同部门 | 当前代码没有部门筛选或部门校验。部门名只是候选项/通讯录中的展示信息。 |
| 发出申请后两人已经是好友 | 不是。申请状态是 `pending` 时，`friendship` 还没有记录。 |
| 申请人可以自己同意申请 | 不可以。后端只允许 `receiverId` 对应的接收人处理。 |
| 前端排除重复候选人就够了 | 不够。后端还会检查已有好友、同向待处理申请和反向待处理申请。 |
| 普通用户无法申请好友是好友接口权限问题 | 当前候选用户接口要求 `user:get`，默认 `ROLE_USER` 已拥有它；应继续检查实际接口返回和前端错误提示。 |

## 12. 最后用人话复盘

1. 通讯录加载好友、申请记录，并额外加载添加好友候选用户。
2. 候选用户逻辑不限制部门；当前复用的用户列表接口受 `user:get` RBAC 权限保护，默认 `ROLE_USER` 已拥有该权限。
3. 发送申请只写 `friend_request(pending)`，并由后端防止自己、重复好友和重复申请。
4. 接收人同意后，申请变为 `accepted`，再写入唯一的 `friendship` 关系。
5. 好友列表只查询 `friendship`，因此只显示已经同意的关系。
6. 点击好友即可携带对方 ID 跳转到消息中心开始单聊。
