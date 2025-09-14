<template>
  <DashboardGrid>
    <!-- Row 1: KPIs (from stores) -->
    <div class="is_small"><InfoMetricTile title="Run Status" icon="mdi-progress-clock" :value="runStatus" /></div>
    <div class="is_small"><InfoMetricTile title="Throughput it/s" icon="mdi-speedometer" :value="throughputText" /></div>
    <div class="is_small"><InfoMetricTile title="Loss (2h)" icon="mdi-chart-line" :value="loss2hText" /></div>
    <div class="is_small"><InfoMetricTile title="Val-Acc (2h)" icon="mdi-chart-bell-curve" :value="valAcc2hText" /></div>

    <!-- Row 2: Large -->
    <div class="is_large">
      <ChartTile title="Loss & Val-Acc (24h)" icon="mdi-chart-timeline-variant" :vm="lossValVm" :loading="comboLoading" />
    </div>
    <div class="is_large">
      <TableTile title="Logs Stream" icon="mdi-console" :vm="logsVm" :loading="logsLoading" />
    </div>

    <!-- Row 3: Large -->
    <div class="is_large">
      <ChartTile title="Resource Utilization" icon="mdi-chip" :vm="utilVm" :loading="utilLoading" />
    </div>
    <div class="is_large">
      <ArtifactsParamsTile title="Artifacts & Hyperparams" icon="mdi-file-table"
        :artifacts="artifacts"
        :params="hyperparams"
        :loading-artifacts="artifactsLoading"
        :loading-params="paramsLoading"
      />
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, watch, computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import ArtifactsParamsTile from '@/components/panels/ArtifactsParamsTile.vue'
import { useTrainingStore } from '@/stores/training'
import { useMetricsStore } from '@/stores/metrics'
import { usePolling } from '@/composables/usePolling'
import { getTrainingLogs } from '@/services/logs'
import { listArtifacts, getHyperparams, type TrainingArtifact } from '@/services/training'
import type { TableVM, SeriesVM } from '@/types/vm'

const route = useRoute()
const runId = computed(() => route.params.runId as string | undefined)
const tr = useTrainingStore()
const mt = useMetricsStore()
const { startMany } = usePolling()

async function loadAll() {
  if (!runId.value) return
  await Promise.all([
    tr.fetchRun(runId.value),
    mt.fetchThroughput(runId.value, '24h'),
    mt.fetchTrainingSeries(runId.value, 'loss', '2h'),
    mt.fetchTrainingSeries(runId.value, 'val_acc', '2h'),
    mt.fetchTrainingSeries(runId.value, 'loss', '24h'),
    mt.fetchTrainingSeries(runId.value, 'val_acc', '24h'),
    mt.fetchUtilization(runId.value, '24h'),
    loadLogs(),
    loadArtifacts(),
    loadHyperparams(),
  ])
  startMany([
    { key: `t.run:${runId.value}`, intervalMs: 4000, run: () => tr.fetchRun(runId.value!) },
    { key: `t.tp:${runId.value}`, intervalMs: 4000, run: () => mt.fetchThroughput(runId.value!, '24h') },
    { key: `t.loss2h:${runId.value}`, intervalMs: 4000, run: () => mt.fetchTrainingSeries(runId.value!, 'loss', '2h') },
    { key: `t.val2h:${runId.value}`, intervalMs: 4000, run: () => mt.fetchTrainingSeries(runId.value!, 'val_acc', '2h') },
    { key: `t.util:${runId.value}`, intervalMs: 8000, run: () => mt.fetchUtilization(runId.value!, '24h') },
    { key: `t.logs:${runId.value}`, intervalMs: 3000, run: () => loadLogs() },
  ])
}

onMounted(loadAll)
watch(runId, loadAll)

// Selectors/texts
const runStatus = computed(() => runId.value ? (tr.selectRun(runId.value).value?.status ?? '—') : 'Pick a Run')
const tpKey = computed(() => `throughput:${runId.value || 'all'}:24h`)
function lastSeriesPointValue(key: string | undefined) {
  if (!key) return '—'
  const s = mt.series.get(key)
  if (!s || !s.series?.length) return '—'
  const first = s.series[0]
  const pts = first.points || []
  if (!pts.length) return '—'
  return pts[pts.length - 1].value
}
const throughputText = computed(() => lastSeriesPointValue(tpKey.value))
const loss2hKey = computed(() => `train:${runId.value}:loss:2h`)
const val2hKey = computed(() => `train:${runId.value}:val_acc:2h`)
function lastPointVal(key: string | undefined) {
  if (!key) return '—'
  const s = mt.series.get(key)
  if (!s || !s.series.length) return '—'
  const first = s.series[0]
  const pts = first.points
  if (!pts || !pts.length) return '—'
  return pts[pts.length - 1].value
}
const loss2hText = computed(() => lastPointVal(loss2hKey.value))
const valAcc2hText = computed(() => lastPointVal(val2hKey.value))

const comboLoading = computed(() => mt.loading.has(`train:${runId.value}:loss:24h`) || mt.loading.has(`train:${runId.value}:val_acc:24h`))
const comboError = computed(() => mt.errors.get(`train:${runId.value}:loss:24h`)?.message || mt.errors.get(`train:${runId.value}:val_acc:24h`)?.message || false)

// Combined VM for loss & val_acc (24h)
const loss24Key = computed(() => `train:${runId.value}:loss:24h`)
const val24Key = computed(() => `train:${runId.value}:val_acc:24h`)
const lossValVm = computed<SeriesVM | null>(() => {
  const l = mt.series.get(loss24Key.value || '')
  const v = mt.series.get(val24Key.value || '')
  if (!l && !v) return null
  function mapSeries(s?: any, label?: string) {
    if (!s || !s.series?.length) return []
    return s.series.map((ss: any) => ({ label: label || ss.metric, data: (ss.points||[]).map((p: any) => ({ x: new Date(p.ts).getTime(), y: p.value })) }))
  }
  return { series: [ ...mapSeries(l, 'loss'), ...mapSeries(v, 'val_acc') ] }
})

// Logs Table VM
const logsLoading = ref(false)
const logsVm = ref<TableVM>({ columns: [
  { key: 'time', label: 'Time' },
  { key: 'level', label: 'Level' },
  { key: 'msg', label: 'Message' },
], rows: [] })
async function loadLogs(){
  if (!runId.value) return
  logsLoading.value = true
  try {
    const rows = await getTrainingLogs(runId.value)
    logsVm.value = { ...logsVm.value, rows: rows.map(r => ({ time: r.time, level: r.level, msg: r.msg })) }
  } finally { logsLoading.value = false }
}

// Utilization VM
const utilKey = computed(() => `util:${runId.value}:24h`)
const utilVm = computed<SeriesVM | null>(() => {
  const s = mt.series.get(utilKey.value || '')
  if (!s) return null
  return {
    range: s.range,
    series: (s.series || []).map(ss => ({ label: ss.metric, data: (ss.points||[]).map(p => ({ x: new Date(p.ts).getTime(), y: p.value })) }))
  }
})
const utilLoading = computed(() => mt.loading.has(utilKey.value || ''))

// Artifacts & Hyperparams
const artifacts = ref<TrainingArtifact[]>([])
const artifactsLoading = ref(false)
async function loadArtifacts(){
  if (!runId.value) return
  artifactsLoading.value = true
  try { artifacts.value = await listArtifacts(runId.value) } finally { artifactsLoading.value = false }
}

const hyperparams = ref<Record<string, unknown> | { key: string; value: unknown }[] | null>(null)
const paramsLoading = ref(false)
async function loadHyperparams(){
  if (!runId.value) return
  paramsLoading.value = true
  try {
    hyperparams.value = await getHyperparams(runId.value)
  } catch {
    hyperparams.value = []
  } finally { paramsLoading.value = false }
}
</script>
