<template>
  <v-checkbox v-for="s in steps" :key="s" v-model="active" :label="s" :value="s"/>
  <v-btn class="mt-2" color="primary" @click="run" :disabled="!plannedEnabled">Run Workflow</v-btn>
</template>
<script setup lang="ts">
import api from '@/plugins/axios'
import { usePlanned } from '@/composables/usePlanned'
const { plannedEnabled } = usePlanned()
const steps = ['Ingest','Build Datasets','Train','Evaluate','Promote']
const active = $ref<string[]>(['Ingest','Build Datasets','Train'])
async function run(){
  if(!plannedEnabled) return
  await api.post('/v1/workflows', { steps: active })
}
</script>
