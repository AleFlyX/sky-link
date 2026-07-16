<script setup>
import { ArrowDown, SwitchButton, User } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '../../api/auth'
import { useUserStore } from '../../stores/user'
import { clearToken } from '../../utils/request'

const router = useRouter()
const userStore = useUserStore()
const loggingOut = ref(false)
const avatarText = userStore.avatarText

async function handleCommand(command) {
  if (command === 'profile') {
    await router.push('/app/profile')
    return
  }

  if (command !== 'logout' || loggingOut.value) return

  loggingOut.value = true
  try {
    await logout()
  } catch {
    // 即使服务端登出失败，也优先清理本地会话，避免用户留在已失效状态。
  } finally {
    clearToken()
    userStore.resetUser()
    loggingOut.value = false
    ElMessage.success('已退出登录')
    await router.replace('/login')
  }
}
</script>

<template>
  <el-dropdown
    trigger="click"
    placement="bottom-end"
    popper-class="layout-profile-menu"
    @command="handleCommand"
  >
    <button
      type="button"
      class="profile-dropdown"
      :aria-label="`${userStore.displayName} 的个人菜单`"
      :aria-busy="loggingOut"
    >
      <el-avatar class="profile-dropdown__avatar" :size="40">
        {{ avatarText }}
      </el-avatar>
      <div class="profile-dropdown__copy">
        <strong>{{ userStore.user.name }}</strong>
        <!-- <span>{{ userStore.user.roleLabel }}</span> -->
      </div>
      <ArrowDown class="profile-dropdown__icon" />
    </button>

    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <User class="profile-dropdown__menu-icon" />
          <span>进入个人主页</span>
        </el-dropdown-item>
        <el-dropdown-item command="logout" divided :disabled="loggingOut">
          <SwitchButton
            class="profile-dropdown__menu-icon profile-dropdown__menu-icon--danger"
          />
          <span>{{ loggingOut ? '正在退出...' : '退出登录' }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.profile-dropdown {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.45rem 0.75rem 0.45rem 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: var(--color-surface);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.profile-dropdown:hover {
  transform: translateY(-1px);
}

.profile-dropdown__avatar {
  background: linear-gradient(135deg, #77a6ff, #3370ff);
  color: #fff;
  font-weight: 700;
}

.profile-dropdown__copy {
  display: flex;
  flex-direction: column;
  gap:0.3rem;
  text-align: left;
}

.profile-dropdown__copy strong {
  font-size: 0.94rem;
}

.profile-dropdown__copy span {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.profile-dropdown__icon {
  width: 1rem;
  height: 1rem;
  color: var(--color-text-muted);
  transition: transform 0.2s ease;
}

.profile-dropdown[aria-expanded='true'] .profile-dropdown__icon {
  transform: rotate(180deg);
}

.profile-dropdown__menu-icon {
  width: 1rem;
  height: 1rem;
  margin-right: 0.45rem;
  color: var(--color-text-muted);
}

.profile-dropdown__menu-icon--danger {
  color: var(--el-color-danger);
}
</style>
