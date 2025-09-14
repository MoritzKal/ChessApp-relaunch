<template>
  <DashboardGrid>
    <div class="is_large">
      <TableTile title="Spiel-Einstellungen" icon="mdi-tune" :vm="emptyVm">
        <template #cta>
          <ConfigPanel>
            <template #title><h3 class="title">Spiel-Einstellungen</h3></template>
            <div class="form">
              <div class="sect">
                <h4>KI-Optionen</h4>
                <v-text-field v-model.number="form.temperature" type="number" label="Temperature (0–1)" :min="0" :max="1" :step="0.05" density="compact" />
                <v-text-field v-model.number="form.topk" type="number" label="Top-K (1–20)" :min="1" :max="20" :step="1" density="compact" />
                <v-select v-model="form.sideToMove" :items="['auto','white','black']" label="Side to move" density="compact" />
                <v-textarea v-model="form.startFEN" label="Start FEN (optional)" auto-grow density="compact" />
              </div>
              <div class="sect">
                <h4>UI-Optionen</h4>
                <v-switch v-model="form.showTopK" inset label="Zeige Policy Top-K im Board" density="compact" />
                <v-switch v-model="form.useForTraining" inset label="Verwende dieses Spiel fürs Training" density="compact" />
              </div>
            </div>
            <template #footer>
              <v-btn color="primary" @click="save" prepend-icon="mdi-content-save">Speichern</v-btn>
            </template>
          </ConfigPanel>
        </template>
      </TableTile>
    </div>
  </DashboardGrid>
  
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import ConfigPanel from '@/components/ConfigPanel.vue'
import { usePlayGameStore } from '@/stores/usePlayGameStore'

const pg = usePlayGameStore()
const form = reactive({ ...pg.prefs })

const emptyVm = { columns: [], rows: [] }

function save(){
  pg.savePrefs({
    temperature: Number(form.temperature) || 0,
    topk: Math.max(1, Math.min(20, Number(form.topk) || 5)),
    sideToMove: (['auto','white','black'] as const).includes(form.sideToMove as any) ? (form.sideToMove as any) : 'auto',
    startFEN: form.startFEN || undefined,
    showTopK: !!form.showTopK,
    useForTraining: !!form.useForTraining,
  })
}
</script>

<style scoped>
.form{ display:grid; grid-template-columns: 1fr 1fr; gap:16px }
.sect{ display:flex; flex-direction:column; gap:8px }
@media (max-width: 900px){ .form{ grid-template-columns: 1fr } }
</style>
