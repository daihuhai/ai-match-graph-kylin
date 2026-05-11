export type StorageMode = 'local' | 'session'

export function getDataStorage(): Storage {
  const m = import.meta.env.VITE_STORAGE
  if (m === 'session') return sessionStorage
  return localStorage
}

