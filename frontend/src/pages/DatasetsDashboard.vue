<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Rows" icon="mdi-format-list-numbered" :value="rowsText" /></div>
    <div class="is_small"><InfoMetricTile title="Size" icon="mdi-database" :value="sizeText" /></div>
    <div class="is_small"><InfoMetricTile title="Classes" icon="mdi-shape" :value="classesText" /></div>
    <div class="is_small"><InfoMetricTile title="Versions" icon="mdi-history" :value="versionsCount" /></div>

    <!-- Row 2: Large -->
    <div class="is_large">
      <TableTile title="Schema & Stats" icon="mdi-table" :vm="schemaVm" :loading="schemaLoading" />
    </div>
    <div class="is_large">
      <TableTile title="Samples" icon="mdi-image-multiple-outline" :vm="sampleVm" :loading="sampleLoading">
        <template #tools>
          <v-btn v-if="nextCursor" size="small" @click="loadMoreSamples">Load more</v-btn>
        </template>
      </TableTile>
    </div>

    <!-- Row 3: Large -->
    <div class="is_large"><DonutTile title="Quality Issues" icon="mdi-alert-decagram-outline" :segments="qualitySegs" :loading="qualityLoading" /></div>
    <div class="is_large"><TableTile title="Ingest History" icon="mdi-timeline-text-outline" :vm="ingestVm" :loading="ingestLoading" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, watch, computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import DonutTile from '@/components/renderers/DonutTile.vue'
import { useDatasetsStore } from '@/stores/datasets'
import { usePolling } from '@/composables/usePolling'
import { getDatasetSchema, getDatasetSample, getDatasetQuality, getIngestHistory, type DatasetSchemaRow, type DatasetSampleResponse, type IngestEvent } from '@/services/datasets'
import type { TableVM } from '@/types/vm'

const route = useRoute()
const id = computed(() => route.params.id as string || 'sample')
const ds = useDatasetsStore()
const { startMany, stop } = usePolling()

async function loadAll(){
  // stop previous timers when switching datasets
  stop()
  await Promise.all([ ds.fetchSummary(id.value), ds.fetchVersions(id.value), loadSchema(), loadSamples(true), loadQuality(), loadIngest() ])
  startMany([
    { key: `ds.summary:${id.value}`, intervalMs: 6000, run: () => ds.fetchSummary(id.value) },
    { key: `ds.versions:${id.value}`, intervalMs: 12000, run: () => ds.fetchVersions(id.value) },
    { key: `ds.schema:${id.value}`, intervalMs: 30000, run: () => loadSchema() },
    { key: `ds.quality:${id.value}`, intervalMs: 60000, run: () => loadQuality() },
    { key: `ds.ingest:${id.value}`, intervalMs: 30000, run: () => loadIngest() },
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

// Schema VM
const schemaLoading = ref(false)
const schemaVm = ref<TableVM>({ columns: [
  { key: 'name', label: 'Name' },
  { key: 'dtype', label: 'Type' },
  { key: 'nullPct', label: 'Null %', align: 'end' },
  { key: 'uniquePct', label: 'Unique %', align: 'end' },
  { key: 'min', label: 'Min', align: 'end' },
  { key: 'max', label: 'Max', align: 'end' },
], rows: [] })
async function loadSchema(){
  schemaLoading.value = true
  try {
    const rows = await getDatasetSchema(id.value)
    schemaVm.value = { ...schemaVm.value, rows: rows.map(r => ({
      name: r.name, dtype: r.dtype,
      nullPct: pct(r.nullPct), uniquePct: pct(r.uniquePct),
      min: r.min ?? '—', max: r.max ?? '—'
    })) }
  } finally { schemaLoading.value = false }
}

// Sample VM (pageable)
const sampleLoading = ref(false)
const sampleVm = ref<TableVM>({ columns: [], rows: [] })
const nextCursor = ref<string | null | undefined>(null)
async function loadSamples(reset = false){
  sampleLoading.value = true
  try {
    const resp = await getDatasetSample(id.value, { limit: 25, cursor: reset ? undefined : nextCursor.value || undefined })
    nextCursor.value = resp.nextCursor
    const rows = resp.rows || []
    // derive columns dynamically
    const keys = Array.from(new Set(rows.flatMap(r => Object.keys(r))))
    sampleVm.value = {
      columns: keys.map(k => ({ key: k, label: k })),
      rows: reset ? rows : [...(sampleVm.value.rows || []), ...rows]
    }
  } finally { sampleLoading.value = false }
}
function loadMoreSamples(){ void loadSamples(false) }

// Quality donut
const qualityLoading = ref(false)
const qualitySegs = ref<{ label: string; value: number }[]>([])
async function loadQuality(){
  qualityLoading.value = true
  try {
    const q = await getDatasetQuality(id.value)
    function norm(v: number){ const n = Number(v) || 0; return n > 1 ? n/100 : n }
    qualitySegs.value = [
      { label: 'Missing', value: norm(q.missingPct) },
      { label: 'Outliers', value: norm(q.outlierPct) },
      { label: 'Duplicates', value: norm(q.duplicatePct) },
    ]
  } finally { qualityLoading.value = false }
}

// Ingest history
const ingestLoading = ref(false)
const ingestVm = ref<TableVM>({ columns: [
  { key: 'at', label: 'At' },
  { key: 'user', label: 'User' },
  { key: 'note', label: 'Note' },
  { key: 'version', label: 'Version' },
], rows: [] })
async function loadIngest(){
  ingestLoading.value = true
  try {
    const rows = await getIngestHistory(id.value)
    ingestVm.value = {
      ...ingestVm.value,
      rows: rows.map(r => ({
        at: r.at,
        user: r.user,
        note: r.note,
        version: r.version
      }))
    }
  } finally { ingestLoading.value = false }
}

function pct(v?: number){ if (v === undefined || v === null) return '—'; const n = (Number(v) || 0); const x = n > 1 ? n : n*100; return `${x.toFixed(1)}%` }
</script>
