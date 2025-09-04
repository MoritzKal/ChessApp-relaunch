<template>
  <div class="play_grid">
    <div class="left">
      <DashboardGrid>
        <!-- Row 1: KPIs -->
        <div class="is_small"><InfoMetricTile title="Latency P50" icon="mdi-timer-sand" :value="latP50" /></div>
        <div class="is_small"><InfoMetricTile title="Latency P95" icon="mdi-timer-sand-complete" :value="latP95" /></div>
        <div class="is_small"><InfoMetricTile title="Moves/sec" icon="mdi-swap-horizontal" :value="mps" /></div>
        <div class="is_small"><InfoMetricTile title="Online Games" icon="mdi-account-group-outline" :value="online" /></div>

        <!-- Row 2: Charts -->
        <div class="is_large"><ChartTile title="Requests/sec (24h)" icon="mdi-chart-areaspline" :vm="rpsVm" :loading="rpsLoading" /></div>
        <div class="is_large"><ChartTile title="Error Rate (24h)" icon="mdi-alert-circle-outline" :vm="errVm" :loading="errLoading" /></div>

        <!-- Row 3: Live Latency + Moves table -->
        <div class="is_large"><ChartTile title="Live Latency (5m)" icon="mdi-timer-outline" :vm="latLiveVm" :loading="latLiveLoading" /></div>
        <div class="is_large">
          <TableTile title="Moves/PGN" icon="mdi-format-list-numbered" :vm="movesVm">
            <template #tools>
              <v-btn size="small" @click="downloadPgn" prepend-icon="mdi-download">PGN exportieren</v-btn>
            </template>
          </TableTile>
        </div>
      </DashboardGrid>
    </div>
    <aside class="right">
      <ChessBoard ref="boardRef" :initial-fen="fen" :orientation="orientation" @move="onUserMove" />
      <div class="board_tools">
        <v-btn size="small" variant="tonal" @click="onNewGame" prepend-icon="mdi-restart">Neu starten</v-btn>
        <div class="lat" v-if="latencyMs !== null">KI Latenz: {{ latencyMs }} ms</div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import ChessBoard from '@/components/ChessBoard.vue'
import { useMetricsStore } from '@/stores/metrics'
import { useGamesStore } from '@/stores/games'
import { usePlayGameStore } from '@/stores/usePlayGameStore'
import obsApi from '@/plugins/obsAxios'

const mt = useMetricsStore()
const gm = useGamesStore()
const pg = usePlayGameStore()

const boardRef = ref<InstanceType<typeof ChessBoard> | null>(null)
const fen = computed(() => pg.fen)
const orientation = ref<'white'|'black'>('white')

onMounted(async () => {
  await Promise.all([
    mt.fetchLatencyP50(), mt.fetchLatencyP95(), mt.fetchMps(),
    mt.fetchRps('24h'), mt.fetchErrorRate('24h'),
    gm.fetchOnline(),
    fetchLatencyLive(),
  ])
})

const latP50 = computed(() => mt.scalars.get('latency:p50')?.value ?? '—')
const latP95 = computed(() => mt.scalars.get('latency:p95')?.value ?? '—')
const mps = computed(() => mt.scalars.get('mps')?.value ?? '—')
const online = computed(() => gm.online ?? '—')

const rpsLoading = computed(() => mt.loading.has('rps:24h'))
const errLoading = computed(() => mt.loading.has('error_rate:24h'))
const rpsVm = mt.selectSeriesVm('rps:24h')
const errVm = mt.selectSeriesVm('error_rate:24h')

// Live latency (5m) via PromQL direct
const latLiveVm = ref<any>(null)
const latLiveLoading = ref(false)
async function fetchLatencyLive() {
  latLiveLoading.value = true
  try {
    const now = Math.floor(Date.now()/1000)
    const start = now - 5*60
    const step = '5s'
    const query = 'histogram_quantile(0.5, sum(rate(http_request_duration_seconds_bucket[1m])) by (le))'
    const res = await obsApi.get('/obs/prom/range', { params: { query, start, end: now, step } })
    const vm = { series: [] as any[] }
    const ok = res.data && res.data.status === 'success'
    if (ok) {
      const vals = (res.data.data.result?.[0]?.values || []).map((t: [number|string, string]) => ({ x: Number(t[0])*1000, y: Number(t[1]) }))
      vm.series = [{ label: 'latency_p50', data: vals }]
    }
    latLiveVm.value = vm
  } catch {
    latLiveVm.value = { series: [] }
  } finally { latLiveLoading.value = false }
}

// Moves table VM
const movesVm = computed(() => ({
  columns: [
    { key: 'num', label: '#' },
    { key: 'san', label: 'Move (SAN)' },
    { key: 'time', label: 'Time' },
    { key: 'side', label: 'Side' },
  ],
  rows: pg.moves,
}))

const latencyMs = computed(() => pg.latencyMs)

function onNewGame(){ pg.newGame(); boardRef.value?.reset(pg.fen) }

async function onUserMove(uci: string) {
  // user already moved on board; sync store fen and request AI move
  const curFen = boardRef.value?.boardToFen() || pg.fen
  pg.makeUserMove(uci, curFen)
  const ai = await pg.fetchAIMove(curFen, pg.moves.map(m => m.uci), { temperature: pg.prefs.temperature, topk: pg.prefs.topk })
  if (ai) {
    boardRef.value?.applyMove(ai)
    const newFen = boardRef.value?.boardToFen()
    pg.makeUserMove(ai, newFen)
  }
}

function downloadPgn(){
  const blob = new Blob([pg.exportPgn()], { type: 'application/x-chess-pgn' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `game_${new Date().toISOString().replace(/[:.]/g,'-')}.pgn`
  a.click()
  URL.revokeObjectURL(a.href)
}
</script>

<style scoped>
.play_grid{ display:grid; grid-template-columns: 1fr 360px; gap: 16px; align-items:start }
.right{ position: sticky; top: 80px; display:flex; flex-direction:column; gap:8px }
.board_tools{ display:flex; align-items:center; gap:8px; justify-content:space-between }
.lat{ opacity:.8; font-size:.9rem }
@media (max-width: 1200px){ .play_grid{ grid-template-columns: 1fr } .right{ position:relative; top:auto } }
</style>

