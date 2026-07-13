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
        },
      },
      {
        path: 'departments',
        name: 'departments',
        component: () => import('../views/departments/DepartmentListView.vue'),
        meta: {
          title: '部门管理',
          eyebrow: 'Department Structure',
        },
      },
      {
        path: 'files',
        name: 'files',
        component: () => import('../views/files/FileCenterView.vue'),
        meta: {
          title: '文件中心',
          eyebrow: 'File Workspace',
        },
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: () => import('../views/tasks/TaskListView.vue'),
        meta: {
          title: '任务管理',
          eyebrow: 'Task Delivery',
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
