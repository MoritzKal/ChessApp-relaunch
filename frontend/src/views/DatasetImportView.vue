<template>
  <DashboardGrid>
    <div class="is_large">
      <TableTile title="Dataset Import" icon="mdi-upload" style="height: 80vh;" :vm="formVm" :loading="false">
        <template #cta>
          <div class="form_wrap">
            <v-tabs v-model="tab" density="comfortable">
              <v-tab value="upload">Datei-Upload</v-tab>
              <v-tab value="chesscom">Chess.com</v-tab>
            </v-tabs>

            <v-window v-model="tab">
              <!-- Datei-Upload Tab -->
              <v-window-item value="upload">
                <v-alert v-if="error && tab==='upload'" type="error" density="comfortable" class="mb-2">{{ error }}</v-alert>
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
                  <BarProgressLoader v-if="status==='running'" :indeterminate="true" :active="true" text="Import läuft…" />
                  <div v-else-if="status==='success'" class="success">
                    <div>Import abgeschlossen.</div>
                    <div v-if="datasetId">
                      <RouterLink :to="`/datasets/${datasetId}`"><v-btn size="small" color="primary" prepend-icon="mdi-database">Zum Datensatz</v-btn></RouterLink>
                    </div>
                    <div v-else>
                      <RouterLink to="/datasets"><v-btn size="small" variant="tonal" prepend-icon="mdi-database">Zu Datasets</v-btn></RouterLink>
                    </div>
                  </div>
                  <div v-else-if="status==='error'" class="err">Fehler beim Import.</div>
                </div>
              </v-window-item>

              <!-- Chess.com Tab -->
              <v-window-item value="chesscom">
                <v-alert v-if="chessError" type="error" density="comfortable" class="mb-2">{{ chessError }}</v-alert>
                <div class="row">
                  <v-text-field v-model="username" label="username" :rules="[req]" @update:model-value="(v:any)=>username=v.toLowerCase()" />
                  <v-btn @click="loadArchives" :loading="archivesLoading" :disabled="!username" prepend-icon="mdi-folder-download">Archive laden</v-btn>
                </div>
                <div class="row">
                  <v-btn variant="tonal" size="small" @click="selectAll" :disabled="months.length===0">Alles auswählen</v-btn>
                  <v-text-field v-model="fromMonth" label="From (YYYY-MM)" placeholder="YYYY-MM" />
                  <v-text-field v-model="toMonth" label="To (YYYY-MM)" placeholder="YYYY-MM" />
                  <v-btn variant="tonal" size="small" @click="selectRange" :disabled="months.length===0">Nur Range auswählen</v-btn>
                  <div class="spacer"></div>
                  <v-text-field v-model="dsIdChess" label="datasetId (optional)" :placeholder="`chesscom_${username||'user'}`" />
                  <v-text-field v-model="noteChess" label="note (optional)" />
                  <v-btn color="primary" :disabled="selected.size===0 || importing" :loading="importing" @click="startChessImport" prepend-icon="mdi-upload">Import starten</v-btn>
                </div>

                <div v-if="archivesLoading" class="sk" style="height:140px" />
                <div v-else>
                  <div class="months_list" v-if="months.length">
                    <div class="mrow head">
                      <div class="col c1">Select</div>
                      <div class="col c2">Month</div>
                    </div>
                    <div v-for="m in months" :key="m" class="mrow" @click="toggleSel(m)">
                      <div class="col c1">
                        <v-checkbox density="compact" hide-details :model-value="selected.has(m)" @click.stop="toggleSel(m)" />
                      </div>
                      <div class="col c2">{{ m }}</div>
                    </div>
                  </div>
                  <div v-else class="hint">Keine Archive geladen. Nutzername eingeben und "Archive laden" klicken.</div>
                </div>

                <div class="footer_row" v-if="months.length">
                  <div>{{ selected.size }} Monate ausgewählt</div>
                  <div class="spacer"></div>
                  <v-btn color="primary" :disabled="selected.size===0 || importing" :loading="importing" @click="startChessImport" prepend-icon="mdi-upload">Import starten</v-btn>
                </div>

                <div v-if="ingRunId && ingStatus!=='running'" class="success mt-3">
                  <div>Import abgeschlossen.</div>
                  <div v-if="ingDatasetId">
                    <RouterLink :to="`/datasets/${ingDatasetId}`"><v-btn size="small" color="primary" prepend-icon="mdi-database">Zum Datensatz</v-btn></RouterLink>
                  </div>
                </div>
                <div v-else-if="ingStatus==='running'" class="sk mt-2" style="height:80px" />
              </v-window-item>
            </v-window>
          </div>
        </template>
      </TableTile>
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import BarProgressLoader from '@/components/BarProgressLoader.vue'
import type { TableVM } from '@/types/vm'
import { startIngest, getIngestRun, getChesscomArchives, importChesscom as svcImportChesscom } from '@/services/datasets'
import { usePolling } from '@/composables/usePolling'
import { useUiStore } from '@/stores/ui'

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

const ui = useUiStore()

async function onSubmit(){
  error.value = null
  submitting.value = true
  try {
    if (!file.value || !datasetName.value) throw new Error('file and datasetName required')
    const resp = await startIngest(file.value, { datasetId: datasetName.value, note: note.value })
    runId.value = resp.runId
    datasetId.value = resp.datasetId || null
    status.value = 'running'
    ui.notify(`Import gestartet (runId: ${resp.runId})`, 'info')
    // poll
    start({ key: `ingest:${resp.runId}`, intervalMs: 1500, run: async () => {
      const r = await getIngestRun(resp.runId)
      status.value = r.status
      if (r.datasetId) datasetId.value = r.datasetId
      if (r.status !== 'running') {
        stop(`ingest:${resp.runId}`)
        if (r.status === 'success') ui.notify('Import abgeschlossen', 'success')
        else if (r.status === 'error') ui.notify('Import fehlgeschlagen', 'error')
      }
    } })
  } catch (e: any) {
    error.value = e?.message || 'Upload fehlgeschlagen'
    ui.notify(error.value, 'error')
  } finally { submitting.value = false }
}

// Chess.com tab state
const tab = ref<'upload'|'chesscom'>('upload')
const username = ref('')
const archivesLoading = ref(false)
const chessError = ref<string | null>(null)
const monthsRef = ref<string[]>([])
const months = computed(() => monthsRef.value)
const selected = ref<Set<string>>(new Set())
const fromMonth = ref('')
const toMonth = ref('')
const dsIdChess = ref('')
const noteChess = ref('')
const importing = ref(false)
const ingRunId = ref<string | null>(null)
const ingStatus = ref<'running'|'success'|'error'|'idle'>('idle')
const ingDatasetId = ref<string | null>(null)

async function loadArchives(){
  chessError.value = null
  archivesLoading.value = true
  selected.value = new Set()
  try {
    if (!username.value) throw new Error('username required')
    const resp = await getChesscomArchives(username.value.trim().toLowerCase())
    monthsRef.value = resp.months || []
  } catch (e: any) {
    const code = e?.response?.status
    if (code === 404) chessError.value = 'Nutzer nicht gefunden.'
    else if (code === 429) chessError.value = 'Rate limit erreicht. Bitte später erneut versuchen.'
    else chessError.value = e?.message || 'Fehler beim Laden der Archive'
  } finally { archivesLoading.value = false }
}

function toggleSel(m: string){ if (selected.value.has(m)) selected.value.delete(m); else selected.value.add(m) }
function selectAll(){ selected.value = new Set(months.value) }
function inRange(m: string, a: string, b: string){
  if (!a && !b) return true
  const x = m
  const lo = a || '0000-00'
  const hi = b || '9999-99'
  return x >= lo && x <= hi
}
function selectRange(){ selected.value = new Set(months.value.filter(m => inRange(m, fromMonth.value, toMonth.value))) }

async function startChessImport(){
  importing.value = true
  chessError.value = null
  try {
    const u = username.value.trim().toLowerCase()
    const monthsArr = Array.from(selected.value)
    const body = { user: u, months: monthsArr, datasetId: dsIdChess.value || `chesscom_${u}`, note: noteChess.value || undefined }
    const resp = await svcImportChesscom(body)
    ingRunId.value = resp.runId
    ingStatus.value = 'running'
    ui.notify(`Chess.com Import gestartet (runId: ${resp.runId})`, 'info')
    // poll here directly to avoid store dependency
    const done = await (async () => {
      // simple loop
      // eslint-disable-next-line no-constant-condition
      while (true) {
        const r = await getIngestRun(resp.runId)
        if (r.status !== 'running') return r
        await new Promise((res) => setTimeout(res, 1200))
      }
    })()
    ingStatus.value = done.status as any
    if (done.datasetId) ingDatasetId.value = done.datasetId
    if (done.status === 'success') ui.notify('Chess.com Import abgeschlossen', 'success')
    else if (done.status === 'error') ui.notify('Chess.com Import fehlgeschlagen', 'error')
  } catch (e: any) {
    chessError.value = e?.message || 'Fehler beim Import'
    ui.notify(chessError.value, 'error')
  } finally { importing.value = false }
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
/* chess.com months list */
.months_list{ display:flex; flex-direction:column; border: 1px solid rgba(212,175,55,.25); border-radius:8px; overflow:hidden }
.mrow{ display:grid; grid-template-columns: 120px 1fr; align-items:center; padding:6px 8px; cursor:pointer }
.mrow:nth-child(odd){ background: rgba(203,163,92,.08) }
.mrow.head{ background: transparent; font-weight:600; cursor: default }
.col.c1{ display:flex; align-items:center }
.footer_row{ display:flex; align-items:center; gap:8px; margin-top:10px }
.hint{ opacity:.85 }
</style>
