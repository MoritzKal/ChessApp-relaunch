<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Run Status" icon="mdi-progress-clock" :value="statusText" /></div>
    <div class="is_small"><InfoMetricTile title="Throughput it/s" icon="mdi-speedometer" :endpoint="tpEndpoint" :valueKey="'value'" /></div>
    <div class="is_small"><InfoMetricTile title="Loss" icon="mdi-chart-line" :endpoint="loss2hEndpoint" :valueKey="'value'" /></div>
    <div class="is_small"><InfoMetricTile title="Val-Acc" icon="mdi-chart-bell-curve" :endpoint="valAcc2hEndpoint" :valueKey="'value'" /></div>

    <!-- Row 2: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Loss & Val-Acc (24h)" icon="mdi-chart-timeline-variant" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Logs Stream" icon="mdi-console" message="Polling every 3s (TBD)"/></div>

    <!-- Row 3: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Resource Utilization" icon="mdi-chip" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Artifacts & Hyperparams" icon="mdi-file-table" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'

const route = useRoute()
const runId = computed(() => route.params.runId as string | undefined)

const statusText = computed(() => runId.value ? `Run ${runId.value}` : 'Pick a Run')
const tpEndpoint = computed(() => runId.value ? `/v1/metrics/throughput?runId=${runId.value}` : undefined)
const loss2hEndpoint = computed(() => runId.value ? `/v1/metrics/training/${runId.value}?m=loss&range=2h` : undefined)
const valAcc2hEndpoint = computed(() => runId.value ? `/v1/metrics/training/${runId.value}?m=val_acc&range=2h` : undefined)
</script>

