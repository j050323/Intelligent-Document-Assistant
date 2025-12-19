import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { useUserStore } from '@/stores/user'
import LoginPage from '@/views/LoginPage.vue'
import RegisterPage from '@/views/RegisterPage.vue'
import EmailVerificationPage from '@/views/EmailVerificationPage.vue'
import ForgotPasswordPage from '@/views/ForgotPasswordPage.vue'
import ResetPasswordPage from '@/views/ResetPasswordPage.vue'
import ProfilePage from '@/views/ProfilePage.vue'
import DocumentListPage from '@/views/DocumentListPage.vue'

// Extend route meta type
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    redirect: '/documents'
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/verify-email',
    name: 'EmailVerification',
    component: EmailVerificationPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: ForgotPasswordPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: ResetPasswordPage,
    meta: { requiresAuth: false }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: ProfilePage,
    meta: { requiresAuth: true }
  },
  {
    path: '/documents',
    name: 'Documents',
    component: DocumentListPage,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// JWT验证路由守卫
router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const userStore = useUserStore()
  const isAuthenticated = userStore.isAuthenticated  // 这是一个 computed getter，不是函数

  // 检查路由是否需要认证
  if (to.meta.requiresAuth && !isAuthenticated) {
    // 未登录用户访问需要认证的页面，重定向到登录页
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 检查路由是否需要管理员权限
  if (to.meta.requiresAdmin) {
    if (!isAuthenticated) {
      // 未登录，重定向到登录页
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    
    // 检查用户角色
    const userRole = userStore.user?.role
    if (userRole !== 'ADMINISTRATOR') {
      // 非管理员用户，拒绝访问
      next({ name: 'Profile' })
      return
    }
  }

  // 已登录用户访问登录/注册页面，重定向到文档管理页面
  if (isAuthenticated && (to.name === 'Login' || to.name === 'Register')) {
    next({ name: 'Documents' })
    return
  }

  next()
})

export default router
