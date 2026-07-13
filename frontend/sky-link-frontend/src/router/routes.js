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
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('../views/users/UserListView.vue'),
      },
      {
        path: 'files',
        name: 'files',
        component: () => import('../views/files/FileCenterView.vue'),
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: () => import('../views/tasks/TaskListView.vue'),
      },
      {
        path: 'notices',
        name: 'notices',
        component: () => import('../views/notices/NoticeListView.vue'),
      },
    ],
  },
]

export default routes
