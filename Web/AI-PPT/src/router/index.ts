import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import authApi from '@/services/auth'
import message from '@/utils/message'

const routes = [
  {
    path: '/',
    redirect: '/dashboard' // 默认跳转到项目列表
  },
  {
    // 项目列表页
    path: '/dashboard',
    name: 'Projects',
    component: () => import('@/views/Projects/index.vue')
  },
  {
    // 编辑器页面（带项目 ID 参数）
    path: '/editor/:id',
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: '',
        name: 'Editor',
        component: () => import('@/views/Editor/index.vue')
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue')
  }
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to, from) => {
  const userStore = useUserStore()

  if (!userStore.token) {
    if (to.path === '/login') {
      return true
    }
    message.warning('请先登录')
    return '/login'
  }

  if (to.path === '/login') {
    return '/dashboard'
  }

  if (userStore.userInfo) {
    return true
  }

  try {
    const userInfo = await authApi.getUserInfo()
    if (userInfo && userInfo.data) {
      userStore.userInfo = userInfo.data
    }
    return true
  } catch (error) {
    userStore.logout()
    message.warning('登录已过期，请重新登录')
    return '/login'
  }
})

export default router
