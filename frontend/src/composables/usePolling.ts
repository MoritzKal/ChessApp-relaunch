import { ref, onBeforeUnmount } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'

export interface PollTarget { key: string; intervalMs: number; run: () => Promise<void> }

export function usePolling(opts?: { autoPauseOnHidden?: boolean }) {
  const autoPause = opts?.autoPauseOnHidden ?? true
  const timers = new Map<string, number>()
  const running = new Set<string>()
  const isPaused = ref(false)

  async function tick(t: PollTarget) {
    if (isPaused.value) return
    if (running.has(t.key)) return
    running.add(t.key)
    try { await t.run() } finally { running.delete(t.key) }
  }

  function start(target: PollTarget) {
    if (timers.has(target.key)) return
    // immediate run then interval
    void tick(target)
    const id = window.setInterval(() => tick(target), target.intervalMs)
    timers.set(target.key, id)
  }

  function startMany(targets: PollTarget[]) { targets.forEach(start) }

  function stop(key?: string) {
    if (key) {
      const id = timers.get(key)
      if (id) { clearInterval(id); timers.delete(key) }
      return
    }
    timers.forEach((id) => clearInterval(id))
    timers.clear()
  }

  function pause() { isPaused.value = true }
  function resume() { isPaused.value = false }

  // pause/resume on page hidden
  function onVis() { document.hidden ? pause() : resume() }
  if (autoPause) document.addEventListener('visibilitychange', onVis)

  // cleanup on route leave and unmount
  onBeforeRouteLeave(() => stop())
  onBeforeUnmount(() => {
    stop()
    if (autoPause) document.removeEventListener('visibilitychange', onVis)
  })

  return { start, startMany, stop, pause, resume, isPaused }
}

