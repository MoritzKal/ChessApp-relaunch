<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Rows" icon="mdi-format-list-numbered" :value="rowsText" /></div>
    <div class="is_small"><InfoMetricTile title="Size" icon="mdi-database" :value="sizeText" /></div>
    <div class="is_small"><InfoMetricTile title="Classes" icon="mdi-shape" :value="classesText" /></div>
    <div class="is_small"><InfoMetricTile title="Versions" icon="mdi-history" :value="versionsCount" /></div>

    <!-- Row 2: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Schema & Stats" icon="mdi-table" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Samples" icon="mdi-image-multiple-outline" /></div>

    <!-- Row 3: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Quality Issues" icon="mdi-alert-decagram-outline" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Ingest History" icon="mdi-timeline-text-outline" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import { useDatasetsStore } from '@/stores/datasets'
import { usePolling } from '@/composables/usePolling'

const route = useRoute()
const id = computed(() => route.params.id as string || 'sample')
const ds = useDatasetsStore()
const { startMany } = usePolling()

async function loadAll(){
  await Promise.all([ ds.fetchSummary(id.value), ds.fetchVersions(id.value) ])
  startMany([
    { key: `ds.summary:${id.value}`, intervalMs: 6000, run: () => ds.fetchSummary(id.value) },
    { key: `ds.versions:${id.value}`, intervalMs: 12000, run: () => ds.fetchVersions(id.value) },
  ])
}
onMounted(loadAll)
watch(id, loadAll)

const summaryRef = computed(() => ds.selectSummary(id.value).value)
const versionsRef = computed(() => ds.selectVersions(id.value).value)

const rowsText = computed(() => summaryRef.value?.rows ?? '—')
const sizeText = computed(() => fmtBytes(summaryRef.value?.sizeBytes as any))
const classesText = computed(() => summaryRef.value?.classes ?? '—')
const versionsCount = computed(() => (versionsRef.value?.length ?? 0))

function fmtBytes(v: number){
  if (!v && v !== 0) return '—'
  const units = ['B','KB','MB','GB','TB']
  let idx = 0; let n = v
  while(n >= 1024 && idx < units.length-1){ n/=1024; idx++ }
  return `${n.toFixed(1)} ${units[idx]}`
}
</script>
