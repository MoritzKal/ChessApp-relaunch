<template>
  <LargeTile :title="title" :icon="icon">
    <div class="wrap">
      <div class="section">
        <div class="sec_title">Artifacts</div>
        <div v-if="loadingArtifacts" class="sk" />
        <v-data-table v-else :headers="artifactHeaders" :items="artifacts" density="comfortable" :items-per-page="5" class="chs-card" />
      </div>
      <div class="section">
        <div class="sec_title">Hyperparams</div>
        <div v-if="loadingParams" class="sk" />
        <v-data-table v-else :headers="paramHeaders" :items="paramsRows" density="comfortable" :items-per-page="5" class="chs-card" />
      </div>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LargeTile from '@/components/tiles/LargeTile.vue'

interface Artifact { name: string; sizeBytes?: number; downloadUrl?: string }
interface Props {
  title: string
  icon?: string
  artifacts?: Artifact[]
  params?: Record<string, unknown> | { key: string; value: unknown }[]
  loadingArtifacts?: boolean
  loadingParams?: boolean
}
const props = withDefaults(defineProps<Props>(), { artifacts: () => [], loadingArtifacts: false, loadingParams: false })

const artifactHeaders = [
  { key: 'name', title: 'Name' },
  { key: 'sizeBytes', title: 'Size', align: 'end' },
  { key: 'downloadUrl', title: 'Download' },
]

const paramHeaders = [
  { key: 'key', title: 'Key' },
  { key: 'value', title: 'Value' },
]

const paramsRows = computed(() => {
  const p = props.params
  if (!p) return []
  if (Array.isArray(p)) return p as any[]
  return Object.entries(p).map(([key, value]) => ({ key, value }))
})
</script>

<style scoped>
.wrap{ display:grid; grid-template-columns: 1fr 1fr; gap: 12px; height:100% }
.section{ display:flex; flex-direction:column }
.sec_title{ font-weight:600; opacity:.9; margin: 4px 6px 8px }
.sk{ flex:1; border-radius: 10px; background: linear-gradient(90deg, rgba(255,255,255,.05), rgba(255,255,255,.12), rgba(255,255,255,.05)); background-size:200% 100%; animation: sk 1.1s linear infinite; }
@media (max-width: 1280px){ .wrap{ grid-template-columns: 1fr } }
</style>

