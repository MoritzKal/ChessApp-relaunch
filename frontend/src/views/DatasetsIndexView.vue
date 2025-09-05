<template>
  <DashboardGrid>
    <div class="is_large">
      <TableTile title="Datasets" icon="mdi-database" :vm="tableVm" :loading="loading">
        <template #tools>
          <div class="tools-row">
            <v-text-field v-model="q" density="compact" hide-details label="Search" prepend-inner-icon="mdi-magnify" style="max-width:280px" @keyup.enter="reload" />
            <v-select v-model="sort" :items="sortItems" density="compact" hide-details label="Sort" style="max-width:180px" />
            <v-spacer />
            <RouterLink to="/datasets/import"><v-btn size="small" color="primary" prepend-icon="mdi-upload">Neu importieren</v-btn></RouterLink>
          </div>
        </template>
        <template #cta>
          <RouterLink to="/datasets/import"><v-btn size="small" color="primary" prepend-icon="mdi-upload">Neu importieren</v-btn></RouterLink>
        </template>
        <template #item-actions="{ item }">
          <div class="actions">
            <RouterLink :to="`/datasets/${item.id}`">
              <v-btn size="x-small" variant="text" icon="mdi-open-in-new" :title="`Ansehen ${item.name}`" />
            </RouterLink>
            <a :href="exportHref(item.id)" target="_blank" rel="noopener">
              <v-btn size="x-small" variant="text" icon="mdi-export" :title="'Export'" />
            </a>
          </div>
        </template>
      </TableTile>
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import type { TableVM } from '@/types/vm'
import { listDatasets, datasetExportUrl } from '@/services/datasets'

const LIMIT = 20
const q = ref('')
const sort = ref<'size'|'rows'|''>('')
const sortItems = [
  { title: '—', value: '' },
  { title: 'size', value: 'size' },
  { title: 'rows', value: 'rows' },
]
const loading = ref(false)
const rows = ref<any[]>([])

const tableVm = computed<TableVM>(() => ({
  columns: [
    { key: 'name', label: 'Name' },
    { key: 'rows', label: 'Rows', align: 'end' },
    { key: 'sizeBytes', label: 'Size', align: 'end' },
    { key: 'versions', label: 'Versions', align: 'end' },
    { key: 'updatedAt', label: 'Updated' },
    { key: 'actions', label: 'Actions', align: 'end' },
  ],
  rows: rows.value,
}))

function fmtBytes(v: number){
  if (!v && v !== 0) return '—'
  const u = ['B','KB','MB','GB','TB']
  let i=0; let n=v
  while(n>=1024 && i<u.length-1){ n/=1024; i++ }
  return `${n.toFixed(1)} ${u[i]}`
}

async function reload(){
  loading.value = true
  try {
    const list = await listDatasets({ limit: LIMIT, offset: 0, sort: sort.value || undefined, q: q.value || undefined })
    rows.value = list.map((d: any) => ({
      id: d.id,
      name: d.name,
      rows: d.sizeRows,
      sizeBytes: fmtBytes((d as any).sizeBytes || 0),
      versions: (d as any).versions?.count ?? '—',
      updatedAt: (d as any).updatedAt || d.createdAt,
    }))
  } finally { loading.value = false }
}

onMounted(reload)
watch([q, sort], () => reload())

function exportHref(id: string){ return datasetExportUrl(id, { format: 'pgn', version: 'latest' }) }
</script>

<style scoped>
.tools-row{ display:flex; align-items:center; gap:8px }
.actions{ display:flex; gap:4px; justify-content:flex-end }
</style>

