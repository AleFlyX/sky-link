# 前端路由与按钮权限（小白版）

前端权限有两层体验控制：路由守卫防止用户进入不该看的页面，`v-permission` 指令隐藏不该用的按钮。但它们都不是最终安全边界——用户仍可能直接伪造 HTTP 请求，所以后端必须继续校验 JWT 与权限。

RBAC 数据怎样在后端生效，请配合阅读：[RBAC 角色权限教程](../backend/rbac-role-permission-guide.md)。

## 完整流程

```text
登录成功 -> 用户信息进入 Pinia user.permissions 并写入 localStorage
  -> 用户跳转 /app/users
  -> router.beforeEach 检查 token
  -> 读取 route.meta.permissions
  -> 与 user.permissions 做匹配
  -> 有任意一个权限：进入；没有：跳 /401

页面渲染按钮
  -> v-permission 读取同一份 user.permissions
  -> 有权限显示；无权限设置 display: none
  -> 后端接口仍再次校验
```

## 关键文件地图

| 作用 | 真实代码位置 |
| --- | --- |
| 全局路由守卫 | `frontend/sky-link-frontend/src/router/index.js:1-71` |
| 路由与页面权限声明 | `frontend/sky-link-frontend/src/router/routes.js:3-184` |
| `v-permission` 指令 | `frontend/sky-link-frontend/src/directives/permission.js:1-85` |
| 用户/权限状态 | `frontend/sky-link-frontend/src/stores/user.js:71-154` |
| 路由测试 | `frontend/sky-link-frontend/src/__tests__/router.spec.js` |
| 指令测试 | `frontend/sky-link-frontend/src/__tests__/permission.spec.js` |

## 路由表如何声明页面权限

代码位置：`routes.js:59-76`

```js
{
  path: 'users', // 1. 完整地址是父路由 /app 加上 users，即 /app/users。
  name: 'users', // 2. 可以用名称跳转，避免在业务代码里到处写 URL。
  component: () => import('../views/users/UserListView.vue'), // 3. 懒加载页面组件。
  meta: {
    title: '用户管理',
    permissions: ['user:list'], // 4. 此页面要求 user:list 权限。
  },
}
```

例如 `contacts` 页面在 `routes.js:128-135` 声明 `['friend:list', 'group:list']`。本项目路由守卫的语义是“数组中任意一个满足即可进入”，不是“必须两个都具备”。

## 路由守卫怎样挡住未登录和无权限访问

代码位置：`router/index.js:34-69`

```js
router.beforeEach((to) => {
  if (!String(to.path).startsWith('/app')) return true // 1. 非业务区页面不按这里的登录规则拦截。

  const token = window.localStorage.getItem(TOKEN_KEY)
  if (!token) {
    // 2. 没有 token，去登录页，并记住原目标地址以便登录后回来。
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const requiredPermissions = Array.isArray(to.meta.permissions) ? to.meta.permissions : []
  if (!requiredPermissions.length) return true // 3. 页面未声明权限，已登录即可访问。

  const userStore = useUserStore()
  const permissions = new Set(userStore.user.permissions || []) // 4. Set 适合频繁做包含判断。
  const allowed = requiredPermissions.some((permission) => permissions.has(permission))
  if (!allowed) return { name: 'unauthorized', query: { redirect: to.fullPath } }
  // 5. some 代表“任一权限匹配即可”。

  return true // 6. 放行。
})
```

注意：本地有 token 只说明前端允许尝试进入。Token 已过期时，真正调用 API 仍会由后端拒绝，并由请求模块尝试刷新。

## 用户权限来自哪里

代码位置：`stores/user.js:71-87`、`122-143`

```js
function normalizeUser(user = {}) {
  // 1. 兼容后端不同用户字段名，最后得到前端固定结构。
  return {
    ...emptyUser,
    ...rest,
    id: rest.id || rest.userId || null,
    roles,
    permissions: Array.isArray(rest.permissions) ? [...rest.permissions] : [],
    // 2. 复制权限数组，避免外部引用意外修改 Store 状态。
  }
}

async function loadCurrentUser() {
  const response = await fetchCurrentUser() // 3. 请求当前登录用户资料。
  setUser(unwrapUserResponse(response)) // 4. 规范化、写入 Pinia 和 localStorage。
  return user.value
}
```

## `v-permission` 怎样隐藏按钮

代码位置：`directives/permission.js:32-50`、`58-82`

```js
export function hasPermission(userPermissions, value) {
  const { permissions, mode } = getPermissionOptions(value) // 1. 支持字符串、数组或带 mode 的对象。
  const grantedPermissions = new Set(normalizePermissions(userPermissions))
  if (!permissions.length) return false // 2. 没有要求的权限时，保守地不显示。

  return mode === 'all'
    ? permissions.every((permission) => grantedPermissions.has(permission)) // 3. all：全部具备。
    : permissions.some((permission) => grantedPermissions.has(permission)) // 4. 默认 any：具备一个即可。
}

function updateVisibility(el, value) {
  const state = el[permissionState]
  const allowed = hasPermission(state.userStore.user.permissions, value)
  el.style.display = allowed ? state.originalDisplay : 'none' // 5. 这里只隐藏，不删除业务数据。
}
```

使用示例：

```vue
<!-- 有 user:create 才显示新增按钮。 -->
<button v-permission="'user:create'">新增用户</button>

<!-- 同时拥有两个权限才显示。 -->
<button v-permission="{ permissions: ['task:update', 'task:status:update'], mode: 'all' }">
  编辑并更新状态
</button>
```

指令在 `mounted` 时监听 `userStore.user.permissions`（`permission.js:59-72`）。权限信息发生变化时会重新计算显示状态；组件卸载时还会停止监听，避免泄漏。

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| 页面能打开就一定能调接口 | 不一定，后端仍会校验 Token 和权限。 |
| `permissions: ['a', 'b']` 必须都有 | 路由和指令默认都是任意一个；指令可用 `mode: 'all'` 改为全部。 |
| 隐藏按钮就是安全控制 | 只是体验控制，不能代替后端授权。 |
| 修改 localStorage 里的 permissions 就能越权 | 只能影响页面外观，后端不会相信它。 |

## 人话复盘

路由守卫负责“不让你随便进页面”，权限指令负责“不让你看见不该用的按钮”，它们共享 Pinia 中的权限数组。真正决定能否改数据的，是后端的 JWT、权限注解和业务层对象权限。
