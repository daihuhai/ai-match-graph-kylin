import { onBeforeUnmount, ref } from 'vue'

export function usePolling<T>(fn: () => Promise<T>, intervalMs: number) {
  const running = ref(false)
  let timer: number | null = null

  const stop = () => {
    if (timer) window.clearInterval(timer)
    timer = null
    running.value = false
  }

  const start = async () => {
    if (running.value) return
    running.value = true
    await fn()
    timer = window.setInterval(fn, intervalMs)
  }

  onBeforeUnmount(() => stop())

  return { start, stop, running }
}

