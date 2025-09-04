<template>
  <DashboardGrid>
    <!-- Row 1: KPIs (via stores) -->
    <div class="is_small"><InfoMetricTile title="Datasets" icon="mdi-database-import" :value="datasetsCountText" /></div>
    <div class="is_small"><InfoMetricTile title="Trainings (active)" icon="mdi-robot" :value="trainingActiveText" /></div>
    <div class="is_small"><InfoMetricTile title="Active Models" icon="mdi-cube" value="—" /></div>
    <div class="is_small"><InfoMetricTile title="System Health" icon="mdi-stethoscope" :value="healthText" /></div>

    <!-- Row 2: Large (Loss 7d, Requests/sec 7d) with loading/error states -->
    <div class="is_large"><PlaceholderLargeTile title="Loss (7d)" icon="mdi-chart-line" :loading="lossLoading" :error="lossError" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Requests/sec (7d)" icon="mdi-chart-areaspline" :loading="rpsLoading" :error="rpsError" /></div>

    <!-- Row 3: Large (Recent Trainings, Top Datasets) → simple list placeholder -->
    <div class="is_large"><ListLargeTile title="Recent Trainings" icon="mdi-history" :items="recentTrainingItems" :loading="recentLoading" :error="recentError" /></div>
    <div class="is_large"><ListLargeTile title="Top Datasets (by size)" icon="mdi-table" :items="topDatasetItems" :loading="topDsLoading" :error="topDsError" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import ListLargeTile from '@/components/panels/ListLargeTile.vue'
import { useDatasetsStore } from '@/stores/datasets'
import { useTrainingStore } from '@/stores/training'
import { useMetricsStore } from '@/stores/metrics'
import { usePolling } from '@/composables/usePolling'

const ds = useDatasetsStore()
const tr = useTrainingStore()
const mt = useMetricsStore()
const { startMany, stop } = usePolling()

onMounted(async () => {
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
  startMany([ ...ds.pollTargets.value, ...tr.pollTargets.value, ...mt.pollTargets.value ])
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

// Recent trainings list
const recentKey = 'l:20|o:0'
const recentList = tr.selectRuns(20,0)
const recentLoading = computed(() => tr.loading.has(recentKey))
const recentError = computed(() => tr.errors.get(recentKey)?.message || false)
const recentTrainingItems = computed(() => recentList.value.map(r => `${r.runId} • ${r.status}`))

// Top datasets list by size
const topKey = 'l:20|o:0|s:size'
const topList = ds.selectTop('size', 20)
const topDsLoading = computed(() => ds.loading.has(topKey))
const topDsError = computed(() => ds.errors.get(topKey)?.message || false)
const topDatasetItems = computed(() => topList.value.map(d => `${d.name} • ${Intl.NumberFormat('de-DE').format(d.sizeRows)} rows`))
</script>

<style scoped>
.placeholder{ height:100%; display:flex; align-items:center; justify-content:center; opacity:.6 }
</style>
