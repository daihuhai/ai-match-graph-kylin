export function isWebGLAvailable() {
  try {
    const canvas = document.createElement('canvas')
    const gl =
      canvas.getContext('webgl') ||
      canvas.getContext('experimental-webgl') ||
      canvas.getContext('webgl2')
    return Boolean(gl)
  } catch {
    return false
  }
}

