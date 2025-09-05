import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/lib/api'
import { Endpoints as ep } from '@/lib/endpoints'

export interface PlayPrefs {
  temperature: number
  topk: number
  sideToMove: 'auto' | 'white' | 'black'
  startFEN?: string
  showTopK?: boolean
  useForTraining?: boolean
}

export interface MoveItem { num: number; uci: string; san: string; time: string; side: 'white'|'black' }

export const usePlayGameStore = defineStore('playGame', () => {
  const gameId = ref<string | null>(null)
  const fen = ref<string>('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1')
  const moves = ref<MoveItem[]>([])
  const latencyMs = ref<number | null>(null)
  const prefs = ref<PlayPrefs>(loadPrefs())

  function loadPrefs(): PlayPrefs {
    try {
      const raw = localStorage.getItem('chs_play_prefs_v1')
      if (raw) return JSON.parse(raw)
    } catch {}
    return { temperature: 0.2, topk: 5, sideToMove: 'auto', showTopK: false, useForTraining: false }
  }
  function savePrefs(p: PlayPrefs) { prefs.value = p; localStorage.setItem('chs_play_prefs_v1', JSON.stringify(p)) }

  function newGame(startFen?: string) {
    moves.value = []
    fen.value = startFen || prefs.value.startFEN || 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1'
    gameId.value = null
  }

  function makeUserMove(uci: string, fenAfter?: string) {
    const num = Math.floor(moves.value.length / 2) + 1
    const side: 'white'|'black' = moves.value.length % 2 === 0 ? 'white' : 'black'
    moves.value.push({ num, uci, san: uci, time: new Date().toISOString(), side })
    if (fenAfter) fen.value = fenAfter
  }

  async function fetchAIMove(curFen: string, history?: string[], p?: { temperature?: number; topk?: number }): Promise<string | null> {
    const t0 = performance.now()
    try {
      const body: any = { fen: curFen }
      if (history) body.history = history
      if (p?.temperature !== undefined) body.temperature = p.temperature
      if (p?.topk !== undefined) body.topk = p.topk
      const res = await api.post(ep.serving.predict(), body)
      const move = (res.data as any)?.move || (res.data as any)?.uci || null
      return move
    } finally {
      latencyMs.value = Math.round(performance.now() - t0)
    }
  }

  function exportPgn(): string {
    // very naive PGN writer using UCI as SAN placeholder
    const header = [
      `[Event "Casual Game"]`,
      `[Site "ChessApp"]`,
      `[Date "${new Date().toISOString().slice(0,10)}"]`,
      `[Round "-"]`,
      `[White "You"]`,
      `[Black "Engine"]`,
      `[Result "*"]`,
    ].join('\n')
    const body: string[] = []
    for (let i = 0; i < moves.value.length; i += 2) {
      const num = Math.floor(i / 2) + 1
      const white = moves.value[i]?.san || moves.value[i]?.uci || ''
      const black = moves.value[i+1]?.san || moves.value[i+1]?.uci || ''
      body.push(`${num}. ${white}${black ? ' ' + black : ''}`)
    }
    return header + '\n\n' + body.join(' ')
  }

  return {
    gameId, fen, moves, latencyMs, prefs,
    savePrefs, newGame, makeUserMove, fetchAIMove, exportPgn,
  }
})

