<template>
  <DashboardGrid>
    <!-- Row 1: KPIs (via stores) -->
    <div class="is_small"><InfoMetricTile title="Datasets" icon="mdi-database-import" :value="datasetsCountText" /></div>
    <div class="is_small"><InfoMetricTile title="Trainings (active)" icon="mdi-robot" :value="trainingActiveText" /></div>
    <div class="is_small"><InfoMetricTile title="Active Models" icon="mdi-cube" value="—" /></div>
    <div class="is_small"><InfoMetricTile title="System Health" icon="mdi-stethoscope" :value="healthText" /></div>

    <!-- Row 2: Large (Loss 7d, Requests/sec 7d) with loading/error states -->
    <div class="is_large"><ChartTile title="Loss (7d)" icon="mdi-chart-line" :vm="lossVm" :loading="lossLoading" /></div>
    <div class="is_large"><ChartTile title="Requests/sec (7d)" icon="mdi-chart-areaspline" :vm="rpsVm" :loading="rpsLoading" /></div>

    <!-- Row 3: Large (Recent Trainings, Top Datasets) → tables -->
    <div class="is_large"><TableTile title="Recent Trainings" icon="mdi-history" :vm="recentRunsVm" :loading="recentLoading" /></div>
    <div class="is_large"><TableTile title="Top Datasets (by size)" icon="mdi-table" :vm="topDatasetsVm" :loading="topDsLoading" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import ListLargeTile from '@/components/panels/ListLargeTile.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import { useDatasetsStore } from '@/stores/datasets'
import { useTrainingStore } from '@/stores/training'
import { useMetricsStore } from '@/stores/metrics'
import { usePolling } from '@/composables/usePolling'
import { useAuthStore } from '@/stores/auth'

const ds = useDatasetsStore()
const tr = useTrainingStore()
const mt = useMetricsStore()
const { startMany, stop } = usePolling()
const auth = useAuthStore()

onMounted(async () => {
  if (!auth.isAuthed) return
  // Initial load for KPIs
  await Promise.all([
    ds.fetchCount(),
    tr.fetchCount('active'),
    mt.fetchHealth(),
    mt.fetchLoss('7d'),
    mt.fetchRps('7d'),
    tr.fetchRuns(20,0),
    ds.fetchTop('size', 20),
  ])
  // Start declared polling targets
  const polls = [
    ...(((ds.pollTargets as unknown) as any).value || []),
    ...(((tr.pollTargets as unknown) as any).value || []),
    ...(((mt.pollTargets as unknown) as any).value || []),
  ]
  startMany(polls)
})

// KPI selectors
const datasetsCountText = computed(() => ds.counts ?? '—')
const trainingActiveText = computed(() => tr.countsByStatus.get('active') ?? '—')
const healthText = computed(() => mt.health?.status?.toUpperCase?.() ?? '—')

// Series loading/error
const lossKey = 'loss:7d'
const rpsKey = 'rps:7d'
const lossLoading = computed(() => mt.loading.has(lossKey))
const rpsLoading = computed(() => mt.loading.has(rpsKey))
const lossError = computed(() => mt.errors.get(lossKey)?.message || false)
const rpsError = computed(() => mt.errors.get(rpsKey)?.message || false)
const lossVm = mt.selectSeriesVm(lossKey)
const rpsVm = mt.selectSeriesVm(rpsKey)

// Recent trainings list
const recentKey = 'l:20|o:0'
const recentList = tr.selectRuns(20,0)
const recentLoading = computed(() => tr.loading.has(recentKey))
const recentError = computed(() => tr.errors.get(recentKey)?.message || false)
const recentRunsVm = tr.selectRunsTableVm(20,0)

// Top datasets list by size
const topKey = 'l:20|o:0|s:size'
const topList = ds.selectTop('size', 20)
const topDsLoading = computed(() => ds.loading.has(topKey))
const topDsError = computed(() => ds.errors.get(topKey)?.message || false)
const topDatasetsVm = ds.selectTopTableVm('size', 20)
</script>

<style scoped>
.placeholder{ height:100%; display:flex; align-items:center; justify-content:center; opacity:.6 }
</style>
