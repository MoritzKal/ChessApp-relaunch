<template>
  <div class="d-flex gap-4 flex-wrap">
    <v-text-field label="Games" v-model.number="games" type="number"/>
    <v-select label="Time Control" v-model="tc" :items="['1+0','3+2','5+0','10+0']"/>
    <v-text-field label="Concurrency" v-model.number="conc" type="number"/>
  </div>
  <v-btn class="mt-2" color="primary" @click="start" :disabled="!plannedEnabled">Start Self-Play</v-btn>
</template>
<script setup lang="ts">
import api from '@/plugins/axios'
import { usePlanned } from '@/composables/usePlanned'
const { plannedEnabled } = usePlanned()
const games = $ref(100)
const tc = $ref('3+2')
const conc = $ref(2)
async function start(){
  if(!plannedEnabled) return
  await api.post('/v1/selfplay', { games, timeControl:tc, concurrency:conc })
}
</script>
