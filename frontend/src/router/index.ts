import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const routes =
[
  {
    path: '/login',
    component: () => import('@/pages/Login.vue')
  },
  { path: '/', children: [{ path: 'account/users', component: () => import('@/views/UserManagementView.vue') },
    { path: '', component: () => import('@/pages/Overview.vue') },
    { path: 'training/:runId?', component: () => import('@/pages/TrainingDashboard.vue') },
    { path: 'datasets-overview', component: () => import('@/pages/DatasetsDashboard.vue') },
    { path: 'datasets', component: () => import('@/views/DatasetsIndexView.vue') },
    { path: 'datasets/import', component: () => import('@/views/DatasetImportView.vue') },
    { path: 'datasets/:id', component: () => import('@/views/DatasetView.vue') },
    { path: 'play', component: () => import('@/views/PlayGameView.vue') },
    { path: 'play/config', component: () => import('@/views/PlayConfigView.vue') },
    { path: 'account', component: () => import('@/views/AccountView.vue') },
    { path: 'models', component: () => import('@/views/ModelsView.vue') },
    { path: 'evaluation', component: () => import('@/views/EvaluationView.vue') },
    { path: 'observability', component: () => import('@/views/ObservabilityView.vue') },
    { path: 'help', component: () => import('@/views/HelpView.vue') },
    { path: 'styleguide', component: () => import('@/pages/Styleguide.vue') },
    { path: 'config', component: () => import('@/pages/ConfigWorkbench.vue') },
    { path: 'config/training', component: () => import('@/views/TrainingConfigView.vue') },
    // weitere Seiten wie Data/Datasets/Training/...
    ],
    meta: { requiresAuth: true }
  }
  ]
  const router = createRouter({ history: createWebHistory(), routes })
  router.beforeEach((to) => {  if (to.path === '/login') return true
    if (to.meta.requiresAuth && !useAuthStore().isAuthed)
      return '/login'
    return true
  }
)
export default router
