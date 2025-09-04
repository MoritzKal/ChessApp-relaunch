<template>
  <DashboardGrid>
    <!-- Row 1: KPIs (from stores) -->
    <div class="is_small"><InfoMetricTile title="Run Status" icon="mdi-progress-clock" :value="runStatus" /></div>
    <div class="is_small"><InfoMetricTile title="Throughput it/s" icon="mdi-speedometer" :value="throughputText" /></div>
    <div class="is_small"><InfoMetricTile title="Loss (2h)" icon="mdi-chart-line" :value="loss2hText" /></div>
    <div class="is_small"><InfoMetricTile title="Val-Acc (2h)" icon="mdi-chart-bell-curve" :value="valAcc2hText" /></div>

    <!-- Row 2: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Loss & Val-Acc (24h)" icon="mdi-chart-timeline-variant" :loading="comboLoading" :error="comboError" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Logs Stream" icon="mdi-console" message="Polling every 3s (TBD)"/></div>

    <!-- Row 3: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Resource Utilization" icon="mdi-chip" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Artifacts & Hyperparams" icon="mdi-file-table" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import { useTrainingStore } from '@/stores/training'
import { useMetricsStore } from '@/stores/metrics'
import { usePolling } from '@/composables/usePolling'

const route = useRoute()
const runId = computed(() => route.params.runId as string | undefined)
const tr = useTrainingStore()
const mt = useMetricsStore()
const { startMany } = usePolling()

async function loadAll() {
  if (!runId.value) return
  await Promise.all([
    tr.fetchRun(runId.value),
    mt.fetchThroughput(runId.value),
    mt.fetchTrainingSeries(runId.value, 'loss', '2h'),
    mt.fetchTrainingSeries(runId.value, 'val_acc', '2h'),
    mt.fetchTrainingSeries(runId.value, 'loss', '24h'),
    mt.fetchTrainingSeries(runId.value, 'val_acc', '24h'),
  ])
  startMany([
    { key: `t.run:${runId.value}`, intervalMs: 4000, run: () => tr.fetchRun(runId.value!) },
    { key: `t.tp:${runId.value}`, intervalMs: 4000, run: () => mt.fetchThroughput(runId.value!) },
    { key: `t.loss2h:${runId.value}`, intervalMs: 4000, run: () => mt.fetchTrainingSeries(runId.value!, 'loss', '2h') },
    { key: `t.val2h:${runId.value}`, intervalMs: 4000, run: () => mt.fetchTrainingSeries(runId.value!, 'val_acc', '2h') },
  ])
}

onMounted(loadAll)
watch(runId, loadAll)

// Selectors/texts
const runStatus = computed(() => runId.value ? (tr.selectRun(runId.value).value?.status ?? '—') : 'Pick a Run')
const tpKey = computed(() => `throughput:${runId.value}`)
const throughputText = computed(() => mt.scalars.get(tpKey.value || '')?.value ?? '—')
const loss2hKey = computed(() => `train:${runId.value}:loss:2h`)
const val2hKey = computed(() => `train:${runId.value}:val_acc:2h`)
const loss2hText = computed(() => mt.series.get(loss2hKey.value || '')?.series?.at(0)?.points?.at(-1)?.value ?? '—')
const valAcc2hText = computed(() => mt.series.get(val2hKey.value || '')?.series?.at(0)?.points?.at(-1)?.value ?? '—')

const comboLoading = computed(() => mt.loading.has(`train:${runId.value}:loss:24h`) || mt.loading.has(`train:${runId.value}:val_acc:24h`))
const comboError = computed(() => mt.errors.get(`train:${runId.value}:loss:24h`)?.message || mt.errors.get(`train:${runId.value}:val_acc:24h`)?.message || false)
</script>
