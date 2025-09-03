<template>
  <v-checkbox v-model="inc.datasets" label="Datasets"/>
  <v-checkbox v-model="inc.models" label="Models"/>
  <v-checkbox v-model="inc.reports" label="Reports"/>
  <v-btn class="mt-2" color="primary" @click="doExport" :disabled="!plannedEnabled">Export Snapshot</v-btn>
  <div v-if="uri" class="mt-2">Export URI: <code>{{ uri }}</code></div>
</template>
<script setup lang="ts">
import api from '@/plugins/axios'
import { usePlanned } from '@/composables/usePlanned'
const { plannedEnabled } = usePlanned()
const inc = $ref({ datasets:true, models:true, reports:false })
const uri = $ref<string|null>(null)
async function doExport(){
  if(!plannedEnabled) return
  const { data } = await api.post('/v1/export/state', { include: inc })
  uri.value = data?.reportUri || data?.uri || null
}
</script>
