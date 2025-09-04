<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Latency P50" icon="mdi-timer-sand" :value="latP50" /></div>
    <div class="is_small"><InfoMetricTile title="Latency P95" icon="mdi-timer-sand-complete" :value="latP95" /></div>
    <div class="is_small"><InfoMetricTile title="Moves/sec" icon="mdi-swap-horizontal" :value="mps" /></div>
    <div class="is_small"><InfoMetricTile title="Online Games" icon="mdi-account-group-outline" :value="online" /></div>

    <!-- Row 2: Large -->
    <div class="is_large"><ChartTile title="Requests/sec (24h)" icon="mdi-chart-areaspline" :vm="rpsVm" :loading="rpsLoading" /></div>
    <div class="is_large"><ChartTile title="Error Rate (24h)" icon="mdi-alert-circle-outline" :vm="errVm" :loading="errLoading" /></div>

    <!-- Row 3: Large -->
    <div class="is_large"><TableTile title="Recent Games" icon="mdi-chess-queen" :vm="recentVm" :loading="recentLoading" /></div>
    <div class="is_large"><ChartTile title="Engine ELO Trend (30d)" icon="mdi-chart-line" :vm="eloVm" :loading="eloLoading" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import { useMetricsStore } from '@/stores/metrics'
import { useGamesStore } from '@/stores/games'
import { usePolling } from '@/composables/usePolling'

const mt = useMetricsStore()
const gm = useGamesStore()
const { startMany } = usePolling()

onMounted(async () => {
  await Promise.all([
    mt.fetchLatencyP50(), mt.fetchLatencyP95(), mt.fetchMps(),
    mt.fetchRps('24h'), mt.fetchErrorRate('24h'), mt.fetchElo('30d'),
    gm.fetchOnline(), gm.fetchRecent(50),
  ])
  startMany([
    { key: 'lat.p50', intervalMs: 4000, run: mt.fetchLatencyP50 },
    { key: 'lat.p95', intervalMs: 4000, run: mt.fetchLatencyP95 },
    { key: 'mps', intervalMs: 4000, run: mt.fetchMps },
    { key: 'gm.online', intervalMs: 2000, run: gm.fetchOnline },
  ])
})

const latP50 = computed(() => mt.scalars.get('latency:p50')?.value ?? '—')
const latP95 = computed(() => mt.scalars.get('latency:p95')?.value ?? '—')
const mps = computed(() => mt.scalars.get('mps')?.value ?? '—')
const online = computed(() => gm.online ?? '—')

const rpsLoading = computed(() => mt.loading.has('rps:24h'))
const rpsError = computed(() => mt.errors.get('rps:24h')?.message || false)
const errLoading = computed(() => mt.loading.has('error_rate:24h'))
const errError = computed(() => mt.errors.get('error_rate:24h')?.message || false)
const eloLoading = computed(() => mt.loading.has('elo:30d'))
const eloError = computed(() => mt.errors.get('elo:30d')?.message || false)
const rpsVm = mt.selectSeriesVm('rps:24h')
const errVm = mt.selectSeriesVm('error_rate:24h')
const eloVm = mt.selectSeriesVm('elo:30d')

const recentKey = 'recent:50'
const recentList = computed(() => gm.lists.get(recentKey) || [])
const recentLoading = computed(() => gm.loading.has(recentKey))
const recentError = computed(() => gm.errors.get(recentKey)?.message || false)
const recentVm = computed(() => ({
  columns: [
    { key: 'id', label: 'ID' },
    { key: 'white', label: 'White' },
    { key: 'black', label: 'Black' },
    { key: 'result', label: 'Result' },
    { key: 'mvs', label: 'Moves', align: 'end' },
    { key: 'endedAt', label: 'Ended' },
  ],
  rows: recentList.value.map(g => ({ id: g.id, white: g.white, black: g.black, result: g.result, mvs: g.moves, endedAt: g.endedAt }))
}))
</script>
