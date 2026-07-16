# 群聊与群成员管理（小白版）

群聊不是只往 `chat_group` 表插一行。创建群时还要把创建者写入成员表并标成群主；之后每项成员操作都要根据当前人在该群的角色重新判断，避免普通成员把自己升管理员或把群主移出群。

## 完整流程

```text
用户创建群
  -> POST /api/v1/groups
  -> JWT 确认 currentUserId
  -> GroupServiceImpl 开启事务
  -> 新增 chat_group
  -> 新增 group_member（创建者角色 = owner）
  -> 依次邀请初始成员
  -> 返回群信息

成员管理动作
  -> 根据 groupId 找群成员记录
  -> 判断当前操作人是 owner / admin / member
  -> 判断目标成员角色是否允许被操作
  -> 更新或删除 group_member
```

## 关键文件地图

| 作用 | 真实代码位置 |
| --- | --- |
| 群接口 | `backend/land/src/main/java/com/skylink/land/controller/GroupController.java` |
| 群与成员的业务规则 | `backend/land/src/main/java/com/skylink/land/service/chat/impl/GroupServiceImpl.java` |
| 群实体 | `backend/land/src/main/java/com/skylink/land/entity/chat/ChatGroup.java` |
| 成员实体 | `backend/land/src/main/java/com/skylink/land/entity/chat/GroupMember.java` |
| 群表 Mapper | `backend/land/src/main/java/com/skylink/land/mapper/chat/ChatGroupMapper.java` |
| 成员表 Mapper | `backend/land/src/main/java/com/skylink/land/mapper/chat/GroupMemberMapper.java` |
| 前端群管理组合逻辑 | `frontend/sky-link-frontend/src/views/contacts/composables/useGroupManagement.js` |

## 先认识四个成员角色

代码位置：`GroupServiceImpl.java` 顶部的角色常量。

| 数字 | 含义 | 可以理解成 |
| --- | --- | --- |
| 1 | owner | 群主，最高管理者 |
| 2 | admin | 管理员，可协助管理普通成员 |
| 3 | member | 普通成员 |
| 4 | exited | 已退出（历史状态） |

前端显示名称并不是安全依据。后端以成员表中该群对应的角色值为准。

## 创建群要写两类数据

代码位置：`GroupServiceImpl.java` 的创建群方法（带 `@Transactional`）。

```java
@Transactional(rollbackFor = Exception.class) // 1. 群和成员记录必须一起成功或一起失败。
public GroupDto.GroupResponse createGroup(Long currentUserId, GroupDto.CreateGroupRequest request) {
    // 2. 先校验请求、当前用户和群名称等基础数据。
    ChatGroup group = new ChatGroup();
    group.setGroupName(request.getGroupName().trim()); // 3. 创建真正的群记录。
    group.setOwnerId(currentUserId); // 4. 群的 ownerId 固定为当前登录用户。
    chatGroupMapper.insert(group); // 5. 插入 chat_group，得到 groupId。

    GroupMember owner = new GroupMember();
    owner.setGroupId(group.getGroupId()); // 6. 成员记录关联刚创建的群。
    owner.setUserId(currentUserId); // 7. 创建者本人也必须是成员。
    owner.setRole(1); // 8. 角色 1 表示 owner（群主）。
    groupMemberMapper.insert(owner); // 9. 插入 group_member。

    // 10. 如果请求带了初始成员，再按邀请规则逐一加入。
    return buildGroupResponse(group);
}
```

事务的意义是：如果群表插入成功、邀请初始成员时却失败，前面插入的群和群主成员记录都会回滚，不留下“没有群主的半成品群”。

## 后端如何做“群内角色”授权

群接口 Controller 没有像任务模块那样在每个方法上写 `@RequirePermission`；但它仍位于后端受 JWT 认证保护的链路里。真正细到“这个群里谁能做什么”的判断，放在 `GroupServiceImpl`：先找当前用户的成员记录，再根据 owner/admin/member 做判断。

| 动作 | 当前实现的角色规则 |
| --- | --- |
| 查看群与成员 | 群成员可查看 |
| 编辑群资料 | owner 或 admin |
| 邀请成员 | owner 或 admin |
| 解散群 | 仅 owner |
| 提升/降级管理员 | 仅 owner |
| 管理员移除成员 | 只能移除普通成员，不能移除 owner |
| 退出群 | owner 不能直接退出，需先解散群 |

## “管理员不能踢群主”为什么是必要的

典型的成员移除逻辑会先验证两件事：

```java
// 伪代码描述真实业务规则，变量命名便于理解。
requireOwnerOrAdmin(currentMember); // 1. 普通成员没有管理他人的资格。
if (targetMember.getRole() == OWNER) { // 2. 群主不允许被移除。
    throw new BusinessException(ErrorCode.FORBIDDEN, "owner cannot be removed");
}
if (currentMember.getRole() == ADMIN && targetMember.getRole() != MEMBER) {
    // 3. 管理员不能管理同级管理员；只有群主可以。
    throw new BusinessException(ErrorCode.FORBIDDEN, "admin can only remove members");
}
```

请把这段当作规则解读，不要把它误认为可直接复制的完整源码；实际方法和校验在 `GroupServiceImpl.java` 中。核心点是：后端既检查“操作人有管理身份”，也检查“目标成员是否处于可管理范围”。

## 解散群和退出群的区别

解散群会先处理群成员关系，再删除群本身；这也是为什么它需要事务。群主不能用“退出群”逃离群：若允许这样做，群会留下没有所有者的状态。因此当前实现要求群主解散，而普通成员可以退出。

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| 建群人只需要写在群表 ownerId | 不够，还要写入成员表，否则成员查询和群内权限会不一致。 |
| 管理员等于群主 | 不等于；管理员有部分管理权，群主有最终控制权。 |
| 只在前端隐藏“踢人”按钮就安全 | 不安全；Service 仍会再次验证角色。 |
| 群主退出后群仍正常 | 当前规则不允许群主直接退出。 |

## 人话复盘

群聊管理的关键不是页面上有多少按钮，而是每次动作都问两个问题：操作人是谁、目标成员是谁。群主、管理员、普通成员的边界由后端成员记录决定；创建与解散这类多表操作用事务保证不留下脏数据。
