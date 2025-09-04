<template>
  <ConfigPage>
    <template #header>
      <h2 class="mono" style="font-weight:600">Configuration Workbench</h2>
    </template>

    <v-tabs v-model="tab" class="mb-4">
      <v-tab value="model">Model Setup</v-tab>
      <v-tab value="datasets">Datasets</v-tab>
      <v-tab value="workflow">Workflow</v-tab>
      <v-tab value="selfplay">Self-Play</v-tab>
      <v-tab value="export">Export</v-tab>
    </v-tabs>
    <v-window v-model="tab">
      <v-window-item value="model"><ModelSetupTab/></v-window-item>
      <v-window-item value="datasets"><DatasetSelectionTab/></v-window-item>
      <v-window-item value="workflow"><WorkflowTab/></v-window-item>
      <v-window-item value="selfplay"><SelfPlayTab/></v-window-item>
      <v-window-item value="export"><ExportTab/></v-window-item>
    </v-window>
    <v-alert
      v-if="!plannedEnabled"
      density="compact"
      type="info"
      variant="tonal"
      class="mt-4"
    >
      Backend-Endpunkte für Konfiguration/Workflow sind (noch) nicht aktiv. Buttons sind daher deaktiviert.
    </v-alert>
  </ConfigPage>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import ModelSetupTab from '@/sections/config/ModelSetupTab.vue'
import DatasetSelectionTab from '@/sections/config/DatasetSelectionTab.vue'
import WorkflowTab from '@/sections/config/WorkflowTab.vue'
import SelfPlayTab from '@/sections/config/SelfPlayTab.vue'
import ExportTab from '@/sections/config/ExportTab.vue'
import { usePlanned } from '@/composables/usePlanned'
import ConfigPage from '@/layouts/ConfigPage.vue'
const tab = ref('model')
const { plannedEnabled } = usePlanned()
</script>
