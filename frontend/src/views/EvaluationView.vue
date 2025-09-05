<template>
  <DashboardGrid>
    <!-- Row 1: Create Evaluation -->
    <div class="is_large">
      <TableTile title="Neue Evaluation" icon="mdi-clipboard-plus" :vm="emptyVm">
        <template #cta>
          <div class="form">
            <v-text-field v-model="form.baselineModelId" label="Baseline Model Id" density="compact" />
            <v-text-field v-model="form.candidateModelId" label="Candidate Model Id" density="compact" />
            <v-select v-model="form.suite" :items="['accuracy','topk','acpl']" label="Suite" density="compact" />
            <v-textarea v-model="form.notes" label="Notes" auto-grow density="compact" />
            <div class="actions"><v-btn color="primary" @click="startEval" prepend-icon="mdi-play">Start</v-btn></div>
          </div>
        </template>
      </TableTile>
    </div>

    <!-- Row 2: Recent Evaluations -->
    <div class="is_large">
      <TableTile title="Letzte Evaluations" icon="mdi-history" :vm="listVm" :loading="listLoading">
        <template #item-actions="{ item }">
          <v-btn size="x-small" variant="text" @click="selectEval(item.id)">Open</v-btn>
        </template>
      </TableTile>
    </div>

    <!-- Row 3: Result Chart -->
    <div class="is_large">
      <ChartTile title="A/B Ergebnis (Top-k/ACPL)" icon="mdi-chart-line" :vm="chartVm" :loading="chartLoading" />
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'
import { createEvaluation, listEvaluations, getEvaluation } from '@/services/evaluations'

const emptyVm = { columns: [], rows: [] }
const form = ref({ baselineModelId: '', candidateModelId: '', suite: 'accuracy', notes: '' })
const list = ref<any[]>([])
const listLoading = ref(false)
const sel = ref<any>(null)
const chartVm = ref<any>(null)
const chartLoading = ref(false)

onMounted(async () => { await refreshList() })

async function startEval(){ try { const r = await createEvaluation(form.value as any); await refreshList(); if (r?.evaluationId) await selectEval(r.evaluationId) } catch { /* ignore */ } }
async function refreshList(){ try { listLoading.value = true; list.value = await listEvaluations(20) } catch { list.value = [] } finally { listLoading.value = false } }
async function selectEval(id: string){ try { chartLoading.value = true; sel.value = await getEvaluation(id); chartVm.value = toChartVm(sel.value) } catch { chartVm.value = { series: [] } } finally { chartLoading.value = false } }

const listVm = computed(() => ({
  columns: [ { key: 'id', label: 'ID' }, { key: 'baseline', label: 'Baseline' }, { key: 'candidate', label: 'Candidate' }, { key: 'status', label: 'Status' }, { key: 'createdAt', label: 'Created' }, { key: 'actions', label: 'Actions', align: 'end' } ],
  rows: list.value,
}))

function toChartVm(resp: any){
  // Expect resp.series? fallback to empty
  const series = Array.isArray(resp?.series) ? resp.series : []
  return { series: series.map((s: any) => ({ label: s.name || s.metric || 'series', data: (s.points||s.data||[]).map((p: any) => ({ x: p.x || p.ts || 0, y: p.y || p.value || 0 })) })) }
}
</script>

<style scoped>
.form{ display:grid; grid-template-columns: 1fr 1fr; gap:12px }
.actions{ grid-column: 1 / -1; display:flex; justify-content:flex-end }
@media (max-width: 900px){ .form{ grid-template-columns: 1fr } }
</style>

