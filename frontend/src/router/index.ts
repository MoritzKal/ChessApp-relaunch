import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/pages/Login.vue') },
  {
    path: '/',
    component: () => import('@/layouts/BaseFrame.vue'),
    children: [
      { path: '', component: () => import('@/pages/Overview.vue') },
      { path: 'styleguide', component: () => import('@/pages/Styleguide.vue') },
      { path: 'config', component: () => import('@/pages/ConfigWorkbench.vue') },
      // weitere Seiten wie Data/Datasets/Training/...
    ],
    meta: { requiresAuth: true }
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.path === '/login') return true
  if (to.meta.requiresAuth && !useAuthStore().isAuthed) return '/login'
  return true
})

export default router

