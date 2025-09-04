export interface SSEOptions {
  withCredentials?: boolean
  retryMs?: number
}

export function useSSE(featureFlag = (import.meta as any).env.VITE_ENABLE_SSE === 'true') {
  let es: EventSource | null = null

  function connect(url: string, handlers: { onMessage?: (data: MessageEvent) => void; onError?: (e: Event) => void; onOpen?: (e: Event) => void }, opts?: SSEOptions) {
    if (!featureFlag || typeof EventSource === 'undefined') return null
    disconnect()
    es = new EventSource(url, { withCredentials: !!opts?.withCredentials })
    if (handlers.onOpen) es.addEventListener('open', handlers.onOpen)
    if (handlers.onError) es.addEventListener('error', handlers.onError)
    if (handlers.onMessage) es.addEventListener('message', handlers.onMessage)
    return es
  }

  function disconnect() { if (es) { es.close(); es = null } }

  return { connect, disconnect, get connected() { return !!es } }
}

