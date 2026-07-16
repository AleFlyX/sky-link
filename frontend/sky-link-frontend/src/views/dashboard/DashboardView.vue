<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppCard from '../../components/common/AppCard.vue'
import AppButton from '../../components/common/AppButton.vue'
import AppStatusTag from '../../components/common/AppStatusTag.vue'
import { getDocuments, getTasks } from '../../api/workspace'
import { getTaskStatusMeta, normalizeTaskStatus } from '../../constants/enums'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const tasks = ref([])
const documents = ref([])
const dataSource = ref('')

const quickActions = [
  { title: '任务', path: '/app/tasks', meta: '处理分配与截止事项', accent: 'blue' },
  { title: '消息', path: '/app/messages', meta: '进入会话沟通', accent: 'green' },
  { title: '文档', path: '/app/documents', meta: '协同编辑与资料沉淀', accent: 'violet' },
]

function normalizePage(data) {
  if (Array.isArray(data)) return { records: data, total: data.length }
  return {
    records: data?.records || [],
    total: data?.total || 0,
  }
}

function getResultPage(result) {
  return normalizePage(result?.data ?? result)
}

function getTaskId(task) {
  return task.id ?? task.taskId
}

function getTaskTitle(task) {
  return task.title || task.taskName || '未命名任务'
}

function getTaskAssignee(task) {
  const executor = task.executor || {}
  return task.assignee || executor.nickname || executor.username || '未指定'
}

function getTaskDueDate(task) {
  const value = task.dueDate || task.deadline
  if (!value) return '待定'
  return String(value).replace('T', ' ').slice(0, 16)
}

function getPriorityLabel(priority) {
  const priorityMap = {
    1: '低',
    2: '中',
    3: '高',
  }

  return priorityMap[priority] || priority || '普通'
}

function sortTasksByUrgency(items) {
  const weight = {
    blocked: 0,
    doing: 1,
    todo: 2,
    done: 3,
    cancelled: 4,
  }

  return [...items].sort((a, b) => {
    const aStatus = normalizeTaskStatus(a.status)
    const bStatus = normalizeTaskStatus(b.status)
    return (weight[aStatus] ?? 9) - (weight[bStatus] ?? 9)
  })
}

const displayName = computed(() => userStore.displayName || '未登录')
const departmentName = computed(() => userStore.user.department || '未分配部门')
const roleLabel = computed(() => userStore.user.roleLabel || '普通成员')

const activeTasks = computed(() =>
  tasks.value.filter((task) => !['done', 'cancelled'].includes(normalizeTaskStatus(task.status))),
)

const blockedTasks = computed(() =>
  tasks.value.filter((task) => normalizeTaskStatus(task.status) === 'blocked'),
)

const doingTasks = computed(() =>
  tasks.value.filter((task) => normalizeTaskStatus(task.status) === 'doing'),
)

const heroStats = computed(() => [
  {
    label: '待处理任务',
    value: activeTasks.value.length,
    unit: '项',
    tone: activeTasks.value.length > 0 ? 'warning' : 'success',
    hint: blockedTasks.value.length ? `${blockedTasks.value.length} 项阻塞` : '暂无阻塞',
  },
  {
    label: '进行中任务',
    value: doingTasks.value.length,
    unit: '项',
    tone: doingTasks.value.length > 0 ? 'primary' : 'success',
    hint: '正在推进',
  },
  {
    label: '协作文档',
    value: documents.value.length,
    unit: '份',
    tone: 'primary',
    hint: '近期更新资料',
  },
])

const focusTasks = computed(() => sortTasksByUrgency(activeTasks.value).slice(0, 4))
const recentDocuments = computed(() => documents.value.slice(0, 5))

async function loadDashboardData() {
  loading.value = true
  loadError.value = ''

  try {
    const [taskResult, documentResult] = await Promise.all([
      getTasks({ page: 1, size: 20 }),
      getDocuments({ page: 1, size: 8 }),
    ])

    tasks.value = getResultPage(taskResult).records
    documents.value = getResultPage(documentResult).records
    dataSource.value = [taskResult, documentResult].some(
      (result) => result?.source === 'demo' || result?.degraded,
    )
      ? '演示'
      : '在线'
  } catch (error) {
    tasks.value = []
    documents.value = []
    loadError.value = error.message || '工作台数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboardData)
</script>

<template>
  <div class="dashboard">
    <AppCard variant="hero" padding="lg" body-class="dashboard-hero">
      <div class="hero-main">
        <div class="avatar" aria-hidden="true">{{ userStore.avatarText }}</div>
        <div class="hero-copy">
          <p class="eyebrow">今日工作台</p>
          <h1>{{ displayName }}，欢迎回来</h1>
          <p>
            {{ departmentName }} · {{ roleLabel }}
          </p>
        </div>
      </div>
      <div class="hero-actions">
        <AppStatusTag :label="dataSource || '加载中'" :tone="loadError ? 'danger' : 'primary'" />
        <AppButton variant="primary" @click="loadDashboardData" :loading="loading">刷新</AppButton>
      </div>
    </AppCard>

    <p v-if="loadError" class="load-error" role="alert">{{ loadError }}</p>

    <section class="metric-grid" aria-label="工作台关键指标">
      <AppCard v-for="item in heroStats" :key="item.label" padding="sm">
        <div class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
          <AppStatusTag :label="item.hint" :tone="item.tone" />
        </div>
      </AppCard>
    </section>

    <section class="dashboard-columns">
      <div class="dashboard-column dashboard-column--main">
        <AppCard title="优先处理" subtitle="阻塞和进行中的任务会排在前面">
          <ul v-if="focusTasks.length" class="task-list">
            <li v-for="task in focusTasks" :key="getTaskId(task)" class="task-item">
              <div>
                <strong>{{ getTaskTitle(task) }}</strong>
                <span>{{ getTaskAssignee(task) }} · {{ getTaskDueDate(task) }}</span>
              </div>
              <div class="task-meta">
                <span>{{ getPriorityLabel(task.priority) }}</span>
                <AppStatusTag
                  :label="getTaskStatusMeta(task.status).label"
                  :tone="getTaskStatusMeta(task.status).tone"
                />
              </div>
            </li>
          </ul>
          <div v-else class="empty-state">当前没有待处理任务。</div>
          <template #footer>
            <RouterLink to="/app/tasks">
              <AppButton>查看全部任务</AppButton>
            </RouterLink>
          </template>
        </AppCard>

        <AppCard title="快捷入口" subtitle="进入最常用的协作页面">
          <div class="quick-grid">
            <RouterLink
              v-for="action in quickActions"
              :key="action.path"
              :to="action.path"
              :class="['quick-action', `quick-action--${action.accent}`]"
            >
              <span class="quick-action__mark" aria-hidden="true"></span>
              <strong>{{ action.title }}</strong>
              <small>{{ action.meta }}</small>
            </RouterLink>
          </div>
        </AppCard>
      </div>

      <div class="dashboard-column dashboard-column--side">
        <AppCard title="近期文档" subtitle="快速确认最近协作资料">
          <ul v-if="recentDocuments.length" class="document-list">
            <li v-for="document in recentDocuments" :key="document.id">
              <strong>{{ document.title }}</strong>
              <span>{{ document.creatorName || '未知作者' }} · {{ document.updatedAt || '最近更新' }}</span>
            </li>
          </ul>
          <div v-else class="empty-state">暂无近期协作文档。</div>
          <template #footer>
            <RouterLink to="/app/documents">
              <AppButton>进入在线文档</AppButton>
            </RouterLink>
          </template>
        </AppCard>

        <!-- <AppCard title="当前身份" subtitle="用于确认权限和协作上下文">
          <div class="profile-summary">
            <strong>{{ displayName }}</strong>
            <span>{{ departmentName }}</span>
            <AppStatusTag :label="roleLabel" tone="primary" />
          </div>
        </AppCard> -->
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 1rem;
}

:deep(.dashboard-hero) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.hero-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 1rem;
}

.avatar {
  display: grid;
  flex: 0 0 auto;
  width: 4rem;
  height: 4rem;
  place-items: center;
  border-radius: 18px;
  background: var(--color-primary);
  color: #ffffff;
  font-size: 1.55rem;
  font-weight: 800;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 0.2rem;
  color: var(--color-primary);
  font-size: 0.84rem;
  font-weight: 800;
}

.hero-copy h1 {
  margin: 0;
  color: var(--color-text);
  font-size: clamp(1.45rem, 2vw, 2rem);
  line-height: 1.25;
}

.hero-copy p {
  margin: 0.45rem 0 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
}

.load-error {
  margin: 0;
  padding: 0.8rem 1rem;
  border: 1px solid #f2c3c3;
  border-radius: var(--radius-md);
  background: #ffecec;
  color: #be2f2f;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.metric-card {
  display: grid;
  min-height: 8.25rem;
  align-content: space-between;
  gap: 0.75rem;
}

.metric-card > span,
.task-item span,
.document-list span,
.profile-summary span,
.quick-action small {
  color: var(--color-text-muted);
  line-height: 1.5;
}

.metric-card strong {
  color: var(--color-text);
  font-size: 2rem;
  line-height: 1;
}

.metric-card small {
  margin-left: 0.2rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
}

.dashboard-columns {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.dashboard-column {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1rem;
}

.dashboard-column--main {
  flex: 1 1 58%;
}

.dashboard-column--side {
  flex: 1 1 42%;
  min-width: 20rem;
}

.task-list,
.document-list {
  display: grid;
  gap: 0.7rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.task-item,
.document-list li {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fbfdff;
}

.task-item > div:first-child,
.document-list li {
  display: grid;
  min-width: 0;
  gap: 0.3rem;
}

.task-item strong,
.document-list strong,
.profile-summary strong,
.quick-action strong {
  color: var(--color-text);
  line-height: 1.35;
}

.task-meta {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.55rem;
}

.task-meta > span {
  color: var(--color-text-muted);
  font-weight: 700;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.quick-action {
  display: grid;
  min-height: 7.25rem;
  gap: 0.4rem;
  align-content: start;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #ffffff;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.quick-action:hover {
  transform: translateY(-2px);
  border-color: rgba(51, 112, 255, 0.24);
  box-shadow: 0 14px 28px rgba(31, 35, 41, 0.07);
}

.quick-action__mark {
  width: 2rem;
  height: 0.3rem;
  border-radius: 999px;
  background: var(--color-primary);
}

.quick-action--green .quick-action__mark {
  background: #1c8b4d;
}

.quick-action--violet .quick-action__mark {
  background: #7c4dff;
}

.profile-summary {
  display: grid;
  gap: 0.7rem;
}

.empty-state {
  padding: 1rem;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  text-align: center;
  line-height: 1.6;
}

@media (max-width: 900px) {
  :deep(.dashboard-hero),
  .hero-main,
  .hero-actions {
    align-items: flex-start;
  }

  :deep(.dashboard-hero) {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-columns {
    flex-direction: column;
  }

  .dashboard-column {
    width: 100%;
  }

  .dashboard-column--side {
    min-width: 0;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .hero-main,
  .task-item {
    align-items: flex-start;
  }

  .hero-main,
  .task-item,
  .task-meta {
    flex-direction: column;
  }
}
</style>
