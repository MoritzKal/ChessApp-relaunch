<template>
  <DashboardGrid>
    <!-- Row 1 -->
    <div class="is_large"><ChartTile title="RPS (24h)" icon="mdi-chart-areaspline" :vm="rpsVm" :loading="rpsLoading" /></div>
    <div class="is_large"><ChartTile title="Error Rate (24h)" icon="mdi-alert-circle-outline" :vm="errVm" :loading="errLoading" /></div>

    <!-- Row 2 -->
    <div class="is_large"><ChartTile title="Training Loss (24h)" icon="mdi-chart-line" :vm="lossVm" :loading="lossLoading" /></div>
    <div class="is_large"><ChartTile title="Latency P95 (24h)" icon="mdi-timer-sand-complete" :vm="lat95Vm" :loading="lat95Loading" /></div>

    <!-- Row 3 -->
    <div class="is_large">
      <TableTile title="Recent Errors (Loki)" icon="mdi-bug" :vm="logsVm" :loading="logsLoading" />
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import { useMetricsStore } from '@/stores/metrics'
import obsApi from '@/plugins/obsAxios'

const mt = useMetricsStore()
const rpsLoading = computed(() => mt.loading.has('rps:24h'))
const errLoading = computed(() => mt.loading.has('error_rate:24h'))
const lossLoading = computed(() => mt.loading.has('loss:24h'))
const rpsVm = mt.selectSeriesVm('rps:24h')
const errVm = mt.selectSeriesVm('error_rate:24h')
const lossVm = mt.selectSeriesVm('loss:24h')

const lat95Vm = ref<any>(null)
const lat95Loading = ref(false)
const logsVm = ref<any>({ columns: [], rows: [] })
const logsLoading = ref(false)

onMounted(async () => {
  await Promise.all([mt.fetchRps('24h'), mt.fetchErrorRate('24h'), mt.fetchLoss('24h')])
  await fetchLat95()
  await fetchLogs()
})

async function fetchLat95(){
  lat95Loading.value = true
  try {
    const now = Math.floor(Date.now()/1000)
    const start = now - 24*3600
    const step = '2m'
    const query = 'histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))'
    const res = await obsApi.get('/obs/prom/range', { params: { query, start, end: now, step } })
    const vals = (res.data?.data?.result?.[0]?.values || []).map((t: [number|string,string]) => ({ x: Number(t[0])*1000, y: Number(t[1]) }))
    lat95Vm.value = { series: [{ label: 'latency_p95', data: vals }] }
  } catch { lat95Vm.value = { series: [] } } finally { lat95Loading.value = false }
}

async function fetchLogs(){
  logsLoading.value = true
  try {
    const now = Math.floor(Date.now()/1000)
    const start = now - 3600
    const res = await obsApi.get('/obs/loki/query_range', { params: { query: '{level=~"error|warn"}', start, end: now, limit: 100 } })
    const streams = res.data?.data?.result || []
    const rows: any[] = []
    for (const s of streams) {
      for (const [ts, line] of (s.values || [])) {
        rows.push({ time: new Date(Number(ts)/1_000_000).toISOString(), level: s.stream?.level || 'info', msg: String(line || '').slice(0, 120) })
      }
    }
    logsVm.value = { columns: [ { key:'time', label:'Time' }, { key:'level', label:'Level' }, { key:'msg', label:'Message' } ], rows }
  } catch { logsVm.value = { columns: [ { key:'time', label:'Time' }, { key:'level', label:'Level' }, { key:'msg', label:'Message' } ], rows: [] } } finally { logsLoading.value = false }
}
</script>

