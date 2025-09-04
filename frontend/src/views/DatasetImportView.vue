<template>
  <DashboardGrid>
    <div class="is_large">
      <TableTile title="Dataset Import" icon="mdi-upload" :vm="formVm" :loading="false">
        <template #cta>
          <div class="form_wrap">
            <v-alert v-if="error" type="error" density="comfortable" class="mb-2">{{ error }}</v-alert>

            <div v-if="!runId">
              <v-form v-model="formValid" @submit.prevent="onSubmit">
                <div class="row">
                  <v-text-field v-model="datasetName" label="datasetName" :rules="[req]" />
                  <v-text-field v-model="tags" label="tags (comma-separated)" />
                </div>
                <v-textarea v-model="note" label="note" rows="2" auto-grow />
                <v-file-input v-model="file" label="file (PGN/ZIP)" accept=".pgn,.zip" prepend-icon="mdi-paperclip" :rules="[reqFile]" />
                <div class="row">
                  <div class="spacer"></div>
                  <v-btn type="submit" color="primary" :disabled="!formValid || submitting" :loading="submitting" prepend-icon="mdi-upload">Hochladen</v-btn>
                </div>
              </v-form>
            </div>

            <div v-else>
              <div class="status_row">
                <div class="label">Status:</div>
                <div class="val">{{ status }}</div>
              </div>
              <div class="sk" v-if="status==='running'" />
              <div v-else-if="status==='success'" class="success">
                <div>Import abgeschlossen.</div>
                <div v-if="datasetId">
                  <RouterLink :to="`/datasets/${datasetId}`"><v-btn size="small" color="primary" prepend-icon="mdi-database">Zum Datensatz</v-btn></RouterLink>
                </div>
                <div v-else>
                  Der Datensatz ist unter /datasets sichtbar.
                </div>
              </div>
              <div v-else-if="status==='error'" class="err">Fehler beim Import.</div>
            </div>
          </div>
        </template>
      </TableTile>
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import type { TableVM } from '@/types/vm'
import { startIngest, getIngestRun } from '@/services/datasets'
import { usePolling } from '@/composables/usePolling'

const formVm = ref<TableVM>({ columns: [], rows: [] })
const datasetName = ref('')
const note = ref('')
const tags = ref('')
const file = ref<File | null>(null)
const formValid = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

const runId = ref<string | null>(null)
const status = ref<'running'|'success'|'error'|'idle'>('idle')
const datasetId = ref<string | null>(null)

const { start, stop } = usePolling({ autoPauseOnHidden: false })

const req = (v: any) => !!v || 'Required'
const reqFile = (_: any) => !!file.value || 'Required'

async function onSubmit(){
  error.value = null
  submitting.value = true
  try {
    if (!file.value || !datasetName.value) throw new Error('file and datasetName required')
    const resp = await startIngest(file.value, { datasetId: datasetName.value, note: note.value })
    runId.value = resp.runId
    datasetId.value = resp.datasetId || null
    status.value = 'running'
    // poll
    start({ key: `ingest:${resp.runId}`, intervalMs: 1500, run: async () => {
      const r = await getIngestRun(resp.runId)
      status.value = r.status
      if (r.datasetId) datasetId.value = r.datasetId
      if (r.status !== 'running') stop(`ingest:${resp.runId}`)
    } })
  } catch (e: any) {
    error.value = e?.message || 'Upload fehlgeschlagen'
  } finally { submitting.value = false }
}
</script>

<style scoped>
.form_wrap { padding: 8px; display:flex; flex-direction:column; gap:10px }
.row{ display:flex; gap:10px; align-items:center }
.row > *{ flex:1 }
.spacer{ flex:1 }
.status_row{ display:flex; gap:10px; align-items:center; margin-bottom:8px }
.label{ opacity:.8 }
.sk{ height: 160px; border-radius: 10px; background: linear-gradient(90deg, rgba(255,255,255,.05), rgba(255,255,255,.12), rgba(255,255,255,.05)); background-size:200% 100%; animation: sk 1.1s linear infinite; }
.success{ display:flex; flex-direction:column; gap:8px }
.err{ color: #ffb4a9 }
@keyframes sk{ to{ background-position: 200% 0 } }
</style>

