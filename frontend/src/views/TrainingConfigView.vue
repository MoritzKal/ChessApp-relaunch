<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small">
      <InfoMetricTile
        title="Datasets total"
        icon="mdi-database-import"
        :endpoint="ep.datasets.count()"
        value-key="count"
      />
    </div>
    <div class="is_small">
      <InfoMetricTile
        title="Active trainings"
        icon="mdi-robot"
        :endpoint="ep.training.count({ status: 'active' })"
        value-key="count"
      />
    </div>
    <div class="is_small">
      <InfoMetricTile
        title="Models active"
        icon="mdi-cube"
        :endpoint="ep.models.count({ status: 'active' })"
        value-key="count"
        :formatter="fmtZeroAsDash"
      />
    </div>
    <div class="is_small">
      <InfoMetricTile
        title="System Health"
        icon="mdi-stethoscope"
        :endpoint="ep.health()"
        value-key="status"
        :formatter="(v:any) => String(v || 'TODO').toUpperCase()"
      />
    </div>

    <!-- Row 2: Large – Start Training -->
    <div class="is_large">
      <TableTile title="Start Training" icon="mdi-rocket-launch" :vm="formVm" :loading="false">
        <template #cta>
          <ConfigPanel>
            <v-form v-model="formValid" @submit.prevent="onSubmit">
              <div class="row">
                <v-select
                  label="Dataset"
                  v-model="form.datasetId"
                  :items="datasetItems"
                  item-title="label"
                  item-value="value"
                  :loading="datasetsLoading"
                  :rules="[req]"
                  clearable
                />
                <v-select
                  label="Version"
                  v-model="form.datasetVersion"
                  :items="versionItems"
                  :loading="versionsLoading"
                  :disabled="!form.datasetId"
                  :rules="[req]"
                  clearable
                />
                <v-select
                  label="Model"
                  v-model="form.modelId"
                  :items="modelItems"
                  item-title="label"
                  item-value="value"
                  :loading="modelsLoading"
                  :rules="[req]"
                  clearable
                />
              </div>

              <div class="row">
                <v-text-field
                  label="epochs"
                  type="number"
                  :min="1"
                  v-model.number="form.epochs"
                  :rules="[req, intMin1]"
                />
                <v-text-field
                  label="batchSize"
                  type="number"
                  :min="1"
                  v-model.number="form.batchSize"
                  :rules="[req, intMin1]"
                />
                <v-text-field
                  label="learningRate"
                  type="number"
                  step="0.0001"
                  :min="0.0000001"
                  v-model.number="form.learningRate"
                  :rules="[req, numGtZero]"
                />
                <v-text-field
                  label="optimizer"
                  v-model="form.optimizer"
                  :rules="[req]"
                />
                <v-text-field
                  label="seed"
                  type="number"
                  v-model.number="form.seed"
                  :rules="[req, intRule]"
                />
              </div>

              <v-textarea label="notes" v-model="form.notes" rows="2" auto-grow />

              <div class="row">
                <v-checkbox label="Use GPU" v-model="form.useGPU" />
                <v-select
                  label="priority"
                  v-model="form.priority"
                  :items="['low','normal','high']"
                  :rules="[req]"
                />
                <div class="spacer" />
                <v-btn
                  :loading="submitting"
                  :disabled="!formValid || submitting"
                  color="primary"
                  type="submit"
                  prepend-icon="mdi-rocket"
                >
                  Start
                </v-btn>
              </div>
            </v-form>
          </ConfigPanel>
        </template>

        <template #footer>
          <div class="row" style="justify-content:flex-end">
            <v-btn
              :loading="submitting"
              :disabled="!formValid || submitting"
              color="primary"
              type="button"
              prepend-icon="mdi-rocket"
              @click="onSubmit"
            >
              Start
            </v-btn>
          </div>
        </template>
      </TableTile>
    </div>

    <!-- Row 2: Large – Active Runs -->
    <div class="is_large">
      <TableTile title="Active Runs" icon="mdi-history" :vm="activeVm" :loading="activeLoading">
        <template #cta>
          <v-btn size="small" @click="loadActive">Refresh</v-btn>
        </template>
        <template #item-actions="{ item }">
          <div class="actions">
            <RouterLink :to="`/training/${item.runId}`">
              <v-btn size="x-small" variant="text" :title="`Open ${item.runId}`" icon>
                <v-icon>mdi-open-in-new</v-icon>
              </v-btn>
            </RouterLink>
            <v-btn size="x-small" variant="text" :title="'Pause'" icon @click="() => onControl(item.runId, 'pause')">
              <v-icon>mdi-pause</v-icon>
            </v-btn>
            <v-btn size="x-small" variant="text" :title="'Stop'" icon @click="() => onControl(item.runId, 'stop')">
              <v-icon>mdi-stop</v-icon>
            </v-btn>
          </div>
        </template>
      </TableTile>
    </div>

    <!-- Row 3: Large -->
    <div class="is_large">
      <TableTile title="Recent Runs" icon="mdi-timeline-text-outline" :vm="recentVm" :loading="recentLoading" />
    </div>
    <div class="is_large">
      <ChartTile title="Queue / Throughput" icon="mdi-chart-areaspline" :vm="null" :loading="false" />
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import { RouterLink } from 'vue-router'

import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import ConfigPanel from '@/components/ConfigPanel.vue'
import ChartTile from '@/components/renderers/ChartTile.vue'

import { Endpoints as ep } from '@/lib/endpoints'
import { listDatasets, getDatasetVersions } from '@/services/datasets'
import { listModels } from '@/services/models'
import { createTraining, listTrainingRuns, controlTrainingRun } from '@/services/training'

import type { TableVM } from '@/types/vm'
import { useTrainingStore } from '@/stores/training'
import { usePolling } from '@/composables/usePolling'

const fmtZeroAsDash = (v: any) => {
  const n = Number(v) || 0
  return n === 0 ? '—' : String(n)
}

// --- Form state (flach, passend zum API-Contract) ---
const LOCAL_KEY = 'chs_training_form_v2'
const formValid = ref(false)
const submitting = ref(false)
const formError = ref<string | null>(null)
const form = ref({
  datasetId: '',
  datasetVersion: '',
  modelId: '',
  // flach:
  epochs: 10,
  batchSize: 32,
  learningRate: 0.001,
  optimizer: 'adam',
  seed: 42,
  // resources/priority unverschachtelt
  priority: 'normal' as 'low' | 'normal' | 'high',
  useGPU: true,
  // nur UI
  notes: ''
})

// validation rules
const req = (v: any) => (!!v || v === 0) || 'Required'
const intRule = (v: any) => (v === undefined || v === null || Number.isInteger(Number(v))) || 'Must be integer'
const intMin1 = (v: any) => (Number(v) >= 1) || 'Must be ≥ 1'
const numGtZero = (v: any) => (Number(v) > 0) || 'Must be > 0'

// persist last values
onMounted(() => {
  try {
    const raw = localStorage.getItem(LOCAL_KEY)
    if (raw) {
      const saved = JSON.parse(raw)
      form.value = { ...form.value, ...saved }
    }
  } catch { /* ignore */ }
})
watch(form, (val) => {
  try { localStorage.setItem(LOCAL_KEY, JSON.stringify(val)) } catch { /* ignore */ }
}, { deep: true })

// datasets/models for selects
const datasetsLoading = ref(false)
const versionsLoading = ref(false)
const modelsLoading = ref(false)

const datasets = ref<{ value: string; label: string }[]>([])
const datasetItems = computed(() => datasets.value)
const versionItems = ref<string[]>([])
const models = ref<{ value: string; label: string }[]>([])
const modelItems = computed(() => models.value)

function modelLabel(m: any) {
  const name = m?.displayName ?? m?.name ?? m?.modelId ?? m?.id ?? 'model'
  const ver  = m?.version ?? m?.latestVersion
  const stg  = m?.stage
  return [name, ver && `v${ver}`, stg && `(${stg})`].filter(Boolean).join(' ')
}

async function loadDatasets () {
  datasetsLoading.value = true
  try {
    const list = await listDatasets({ limit: 50 })
    datasets.value = (list || []).map((d: any) => ({ value: d.id, label: String(d.name ?? d.id) }))
  } finally { datasetsLoading.value = false }
}

async function loadVersions () {
  if (!form.value.datasetId) { versionItems.value = []; return }
  versionsLoading.value = true
  try {
    const vers = await getDatasetVersions(form.value.datasetId)
    versionItems.value = (vers || []).map((v: any) => v?.version ?? v?.name ?? String(v))
  } finally { versionsLoading.value = false }
}

async function loadModels () {
  modelsLoading.value = true
  try {
    const list = await listModels().catch(() => [] as any[])
    models.value = (list || []).map((m: any) => ({
      value: m?.modelId ?? m?.id ?? (m?.name ? `${m.name}${m?.version ? `:${m.version}` : ''}` : ''),
      label: modelLabel(m)
    }))
  } finally { modelsLoading.value = false }
}

watch(() => form.value.datasetId, () => { form.value.datasetVersion = ''; void loadVersions() })

async function onSubmit () {
  formError.value = null
  submitting.value = true
  try {
    // minimal client validation
    if (!form.value.datasetId || !form.value.datasetVersion || !form.value.modelId) {
      throw new Error('Dataset, Version and Model are required')
    }
    const payload = {
      datasetId: form.value.datasetId,
      datasetVersion: form.value.datasetVersion,
      modelId: form.value.modelId,
      epochs: form.value.epochs,
      batchSize: form.value.batchSize,
      learningRate: form.value.learningRate,
      optimizer: form.value.optimizer,
      seed: form.value.seed,
      priority: form.value.priority,
      useGPU: form.value.useGPU
    }
    await createTraining(payload)
    await Promise.all([loadActive(), loadRecent()])
  } catch (e: any) {
    formError.value = e?.message || 'Failed to start training'
  } finally { submitting.value = false }
}

// --- Active Runs (local list w/ actions) ---
const activeLoading = ref(false)
const activeRows = ref<{ runId: string; status: string; progress?: number; updatedAt: string }[]>([])

async function loadActive () {
  activeLoading.value = true
  try {
    const rows = await listTrainingRuns({ status: 'active', limit: 50 })
    console.log('Active runs:', rows)
    activeRows.value = rows.map((r: any) => ({
      runId: r.runId,
      status: r.status,
      progress: r.progress,
      updatedAt: r.finishedAt || r.updatedAt || ''
    }))
  } finally { activeLoading.value = false }
}

async function onControl (runId: string, action: 'pause' | 'stop') {
  await controlTrainingRun(runId, action)
  await loadActive()
}

const activeVm = computed<TableVM>(() => ({
  columns: [
    { key: 'runId', label: 'Run ID' },
    { key: 'status', label: 'Status' },
    { key: 'progress', label: 'Progress' },
    { key: 'updatedAt', label: 'Updated' },
    { key: 'actions', label: 'Actions', align: 'end' }
  ],
  rows: activeRows.value
}))

// --- Recent Runs (reuse store shape) ---
const tr = useTrainingStore()
const recentKey = 'l:50|o:0'
const recentLoading = computed(() => tr.loading.has(recentKey))
const recentVm = tr.selectRunsTableVm(50, 0)
async function loadRecent () { await tr.fetchRuns(50, 0) }

// initial load + polling
const { startMany } = usePolling()
onMounted(async () => {
  await Promise.all([loadDatasets(), loadModels(), loadActive(), loadRecent()])
  startMany([
    { key: 'training.active', intervalMs: 5000, run: loadActive },
    { key: 'training.recent', intervalMs: 8000, run: loadRecent }
  ])
})

// VM for form container (empty -> shows slot)
const formVm = ref<TableVM>({ columns: [], rows: [] })
</script>

<style scoped>
.form_wrap { padding: 8px 8px 2px 8px; display: flex; flex-direction: column; gap: 8px }
.row { display: flex; gap: 12px; align-items: center }
.row > * { flex: 1 }
.spacer { flex: 1 }
.actions { display: flex; gap: 4px; justify-content: flex-end }
</style>
