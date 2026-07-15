import AppLayout from '../layouts/AppLayout.vue'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: {
      title: '登录',
    },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/auth/RegisterView.vue'),
    meta: {
      title: '注册',
    },
  },
  {
    path: '/401',
    name: 'unauthorized',
    component: () => import('../views/error/UnauthorizedView.vue'),
    meta: {
      title: '无权限',
    },
  },
  {
    path: '/app',
    component: AppLayout,
    children: [
      {
        path: '',
        redirect: '/app/dashboard',
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/dashboard/DashboardView.vue'),
        meta: {
          title: '工作台',
          eyebrow: 'Workspace Overview',
        },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/profile/ProfileView.vue'),
        meta: {
          title: '个人中心',
          eyebrow: 'Profile Center',
        },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/users/UserListView.vue'),
        meta: {
          title: '用户管理',
          eyebrow: 'Member Directory',
          permissions: ['user:list'],
        },
      },
      {
        path: 'departments',
        name: 'departments',
        component: () => import('../views/departments/DepartmentListView.vue'),
        meta: {
          title: '部门管理',
          eyebrow: 'Department Structure',
          permissions: ['department:list'],
        },
      },
      {
        path: 'files',
        name: 'files',
        component: () => import('../views/files/FileCenterView.vue'),
        meta: {
          title: '文件中心',
          eyebrow: 'File Workspace',
          permissions: ['document:list'],
        },
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: () => import('../views/tasks/TaskListView.vue'),
        meta: {
          title: '任务管理',
          eyebrow: 'Task Delivery',
          permissions: ['task:list'],
        },
      },
      {
        path: 'notices',
        name: 'notices',
        component: () => import('../views/notices/NoticeListView.vue'),
        meta: {
          title: '公告通知',
          eyebrow: 'Announcements',
        },
      },
      {
        path: 'contacts',
        name: 'contacts',
        component: () => import('../views/contacts/ContactsView.vue'),
        meta: {
          title: '通讯录',
          eyebrow: 'Friends & Groups',
          permissions: ['friend:list', 'group:list'],
        },
      },
      {
        path: 'messages',
        name: 'messages',
        component: () => import('../views/messages/MessageCenterView.vue'),
        meta: {
          title: '消息中心',
          eyebrow: 'Message Center',
          permissions: ['message:list'],
        },
      },
      {
        path: 'documents',
        name: 'documents',
        component: () => import('../views/documents/DocumentCenterView.vue'),
        meta: {
          title: '在线文档',
          eyebrow: 'Document Workspace',
          permissions: ['document:list'],
        },
      },
      {
        path: 'documents/:documentId/edit',
        name: 'document-edit',
        component: () => import('../views/documents/CollaborativeDocumentView.vue'),
        meta: { title: '协同编辑', eyebrow: 'Live Collaboration' },
      },
      {
        path: 'schedules',
        name: 'schedules',
        component: () => import('../views/schedules/ScheduleView.vue'),
        meta: {
          title: '日程安排',
          eyebrow: 'Calendar & Schedule',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/error/NotFoundView.vue'),
    meta: {
      title: '页面未找到',
    },
  },
]

export default routes
