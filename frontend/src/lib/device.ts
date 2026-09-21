const DEVICE_STORAGE_KEY = 'handoff_device_id'

export function getOrCreateDeviceId(): string {
  try {
    let deviceId = localStorage.getItem(DEVICE_STORAGE_KEY)
    if (!deviceId) {
      deviceId = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `dev-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
      localStorage.setItem(DEVICE_STORAGE_KEY, deviceId)
    }
    return deviceId
  } catch {
    // Fallback if localStorage is disabled/blocked
    return 'fallback-browser-device'
  }
}
