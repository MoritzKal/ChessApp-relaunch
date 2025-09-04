export function usePlanned() {
  const enabled = import.meta.env.VITE_ENABLE_CONFIG_ENDPOINTS === 'true'
  return { plannedEnabled: enabled }
}
