<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Rows" icon="mdi-format-list-numbered" :endpoint="rowsEp" :valueKey="'rows'" /></div>
    <div class="is_small"><InfoMetricTile title="Size" icon="mdi-database" :endpoint="summaryEp" :valueKey="'sizeBytes'" :formatter="fmtBytes" /></div>
    <div class="is_small"><InfoMetricTile title="Classes" icon="mdi-shape" :endpoint="summaryEp" :valueKey="'classes'" /></div>
    <div class="is_small"><InfoMetricTile title="Versions" icon="mdi-history" :endpoint="versionsEp" :valueKey="'count'" /></div>

    <!-- Row 2: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Schema & Stats" icon="mdi-table" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Samples" icon="mdi-image-multiple-outline" /></div>

    <!-- Row 3: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Quality Issues" icon="mdi-alert-decagram-outline" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Ingest History" icon="mdi-timeline-text-outline" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import { Endpoints as ep } from '@/lib/endpoints'

const route = useRoute()
const id = computed(() => route.params.id as string || 'sample')

const summaryEp = computed(() => ep.datasets.summary(id.value))
const rowsEp = summaryEp
const versionsEp = computed(() => ep.datasets.versions(id.value))

function fmtBytes(v: number){
  if (!v && v !== 0) return '—'
  const units = ['B','KB','MB','GB','TB']
  let idx = 0; let n = v
  while(n >= 1024 && idx < units.length-1){ n/=1024; idx++ }
  return `${n.toFixed(1)} ${units[idx]}`
}
</script>
