export function isMockEnabled() {
  return import.meta.env.VITE_USE_MOCK !== 'false'
}

