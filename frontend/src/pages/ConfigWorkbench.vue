<template>
  <v-container>
    <v-tabs v-model="tab" class="mb-4">
      <v-tab value="model">Model Setup</v-tab>
      <v-tab value="datasets">Datasets</v-tab>
      <v-tab value="workflow">Workflow</v-tab>
      <v-tab value="selfplay">Self-Play</v-tab>
      <v-tab value="export">Export</v-tab>
    </v-tabs>
    <v-window v-model="tab">
      <v-window-item value="model"><v-alert type="info">Planned: POST /v1/config/model</v-alert></v-window-item>
      <v-window-item value="datasets"><v-alert type="info">Planned: POST /v1/config/datasets</v-alert></v-window-item>
      <v-window-item value="workflow"><v-alert type="info">Planned: POST /v1/workflows</v-alert></v-window-item>
      <v-window-item value="selfplay"><v-alert type="info">Planned: POST /v1/selfplay</v-alert></v-window-item>
      <v-window-item value="export"><v-alert type="info">Planned: POST /v1/export/state</v-alert></v-window-item>
    </v-window>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tab = ref('model')
</script>

