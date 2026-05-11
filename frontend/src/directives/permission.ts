import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

export const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const auth = useAuthStore()
    const required = Array.isArray(binding.value) ? binding.value : [binding.value]
    const ok = required.length === 0 || required.some((p) => auth.permissions.includes(p))

    if (!ok) {
      el.style.display = 'none'
    }
  },
}

