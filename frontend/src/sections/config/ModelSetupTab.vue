<template>
  <v-form @submit.prevent="save">
    <div class="d-flex gap-4 flex-wrap">
      <v-select label="Preset" v-model="preset" :items="presets"/>
      <v-text-field label="Learning Rate" v-model.number="hp.lr" type="number" step="0.0001"/>
      <v-text-field label="Batch Size" v-model.number="hp.batchSize" type="number" step="1"/>
      <v-text-field label="Epochs" v-model.number="hp.epochs" type="number"/>
      <v-text-field label="Temperature" v-model.number="hp.temperature" type="number" step="0.1"/>
      <v-text-field label="Top-k" v-model.number="hp.topk" type="number"/>
    </div>
    <v-btn class="mt-3" color="primary" type="submit" :disabled="!plannedEnabled">Save Preset</v-btn>
  </v-form>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import api from '@/plugins/axios'
import { usePlanned } from '@/composables/usePlanned'
import type { HyperParams, ModelPreset } from '@/types/config'
const { plannedEnabled } = usePlanned()
const presets:ModelPreset[] = ['policy_tiny','policy_small','custom']
const preset = ref<ModelPreset>('policy_tiny')
const hp = ref<HyperParams>({ lr:0.001, batchSize:64, epochs:10, temperature:0.8, topk:5 })
async function save(){
  if(!plannedEnabled) return
  await api.post('/v1/config/model', { preset: preset.value, params: hp.value })
}
</script>
