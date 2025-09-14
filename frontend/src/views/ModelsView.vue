<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Active Model" icon="mdi-cube" :value="activeName" /></div>
    <div class="is_small"><InfoMetricTile title="Version" icon="mdi-pound" :value="activeVersion" /></div>
    <div class="is_small"><InfoMetricTile title="Loaded At" icon="mdi-clock-outline" :value="loadedAt" /></div>
    <div class="is_small"><InfoMetricTile title="Total Models" icon="mdi-counter" :endpoint="modelsCountEp" value-key="count" /></div>

    <!-- Row 2: Models table -->
    <div class="is_large">
      <TableTile title="Models" icon="mdi-view-list" :vm="modelsVm" :loading="loading">
        <template #item-actions="{ item }">
          <v-btn size="x-small" variant="text" @click="loadForServing(item)" title="Load for Serving">Load</v-btn>
          <v-btn size="x-small" variant="text" @click="promote(item)" title="Promote to prod">Promote</v-btn>
        </template>
      </TableTile>
    </div>

    <!-- Row 3: Metrics -->
    <div class="is_large"><ChartTile title="Error Rate (24h)" icon="mdi-alert-circle-outline" :vm="errVm" :loading="errLoading" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import { Endpoints as ep } from '@/lib/endpoints'
import { listModels, listModelVersions, loadModel } from '@/services/models'
import { listTrainingRuns, getTraining } from '@/services/training'
import { api } from '@/lib/api'
import { useMetricsStore } from '@/stores/metrics'

const modelsCountEp = ep.models.count({})
const activeName = ref<string | number>('—')
const activeVersion = ref<string | number>('—')
const loadedAt = ref<string | number>('—')
const rows = ref<any[]>([])
const loading = ref(false)

const mt = useMetricsStore()
onMounted(async () => {
  try { loading.value = true; rows.value = await listModels() } catch { rows.value = [] } finally { loading.value = false }
  // Metrics
  await mt.fetchErrorRate('24h')
})

const errVm = mt.selectSeriesVm('error_rate:24h')
const errLoading = computed(() => mt.loading.has('error_rate:24h'))

const modelsVm = computed(() => ({
  columns: [
    { key: 'modelId', label: 'Name' },
    { key: 'displayName', label: 'Display' },
    { key: 'tags', label: 'Tags' },
    { key: 'actions', label: 'Actions', align: 'end' },
  ],
  rows: rows.value.map(r => ({ ...r, tags: Array.isArray(r.tags) ? r.tags.join(',') : '' })),
}))

async function loadForServing(item: any){
  try {
    // Try to find the most recent successful training and reuse its weights uri
    let artifactUri: string | undefined
    try {
      const runs = await listTrainingRuns({ status: 'finished', limit: 10 })
      if (runs && runs.length) {
        const last = runs[0]
        const full = await getTraining(last.runId)
        artifactUri = (full as any)?.artifactUris?.weights
      }
    } catch { /* tolerate */ }
    const versions = await listModelVersions(item.modelId).catch(() => [])
    const ver = versions?.[0]?.modelVersion
    await loadModel({ modelId: item.modelId, modelVersion: ver, artifactUri })
  } catch { /* ignore errors; UI shows no blocking */ }
}
async function promote(item: any){
  try {
    const versions = await listModelVersions(item.modelId).catch(() => [])
    const ver = versions?.[0]?.modelVersion || 'v1'
    let runId: string | undefined
    let artifactUri: string | undefined
    try {
      const runs = await listTrainingRuns({ status: 'finished', limit: 10 })
      if (runs && runs.length) {
        const last = runs[0]
        runId = last.runId
        const full = await getTraining(last.runId)
        artifactUri = (full as any)?.artifactUris?.weights
      }
    } catch {}
    await api.post(ep.models.promote(), { modelId: item.modelId, modelVersion: ver, runId, artifactUri })
  } catch { /* ignore */ }
}
</script>
