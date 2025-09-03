import { defineStore } from 'pinia'
export const useUiStore = defineStore('ui', {
  state: () => ({
    snackbar: { show: false, text: '', color: 'info' as 'info' | 'success' | 'warning' | 'error' }
  }),
  actions: {
    notify(text: string, color: 'info' | 'success' | 'warning' | 'error' = 'info') {
      this.snackbar = { show: true, text, color }
    }
  }
})

// wire global error event
window.addEventListener('chs:error', async () => {
  const { useUiStore } = await import('./ui')
  useUiStore().notify('Request failed', 'error')
})
