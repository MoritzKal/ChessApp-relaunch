<template>
  <v-row>
    <v-col cols="12" md="6">
      <v-autocomplete
        v-model="selected"
        :items="datasets"
        item-title="name"
        item-value="id"
        label="Datasets"
        multiple chips
      />
      <div class="d-flex gap-4">
        <v-text-field label="Train %" v-model.number="split.train" type="number"/>
        <v-text-field label="Val %" v-model.number="split.val" type="number"/>
        <v-text-field label="Test %" v-model.number="split.test" type="number"/>
      </div>
      <v-btn color="primary" class="mt-2" @click="apply" :disabled="!plannedEnabled">Apply Selection</v-btn>
    </v-col>
    <v-col cols="12" md="6">
      <v-alert type="info" variant="tonal">Verteilungen & Größen folgen (Charts)</v-alert>
    </v-col>
  </v-row>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/plugins/axios'
import { usePlanned } from '@/composables/usePlanned'
const { plannedEnabled } = usePlanned()
const datasets = ref<{id:string; name:string}[]>([])
const selected = ref<string[]>([])
const split = ref({ train:80, val:10, test:10 })
onMounted(async ()=>{
  try { const { data } = await api.get('/v1/datasets'); datasets.value = data.items||data } catch {}
})
async function apply(){
  if(!plannedEnabled) return
  await api.post('/v1/config/datasets', { ids:selected.value, split: split.value })
}
</script>
