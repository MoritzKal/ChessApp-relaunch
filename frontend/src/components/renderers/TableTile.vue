<template>
  <LargeTile v-bind="$attrs" :title="title" :icon="icon">
    <div class="tbl_wrap">
      <div class="tools">
        <v-btn size="small" variant="text" @click="copyCsv" prepend-icon="mdi-content-copy">CSV</v-btn>
        <slot name="tools"></slot>
      </div>
      <div v-if="loading" class="sk"/>
      <v-data-table v-else :headers="headers" :items="rows" density="comfortable" :items-per-page="pageSize" class="chs-card">
        <template #no-data>
          <div class="empty">
            <div>No data</div>
            <slot name="cta"></slot>
          </div>
        </template>
        <!-- Render actions column only; keep default cell rendering intact -->
        <template #item.actions="{ item }">
          <slot name="item-actions" :item="item" />
        </template>
      </v-data-table>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LargeTile from '@/components/tiles/LargeTile.vue'
import type { TableVM } from '@/types/vm'

interface Props { title: string; icon?: string; vm: TableVM | null; pageSize?: number; loading?: boolean }
const props = withDefaults(defineProps<Props>(), { pageSize: 10, loading: false })

const headers = computed(() => (props.vm?.columns || []).map(c => ({ key: c.key, title: c.label, align: c.align || 'start' })))
const rows = computed(() => props.vm?.rows || [])

function copyCsv(){
  const cols = props.vm?.columns || []
  const rows = props.vm?.rows || []
  const header = cols.map(c => c.label).join(',')
  const lines = rows.map(r => cols.map(c => JSON.stringify((r as any)[c.key] ?? '')).join(','))
  const csv = [header, ...lines].join('\n')
  navigator.clipboard.writeText(csv)
}
</script>

<style scoped>
.tbl_wrap{ display:flex; flex-direction:column; height:100% }
.tools{ display:flex; justify-content:flex-end; gap:8px; padding: 4px 8px }
.sk{ flex:1; border-radius: 10px; background: linear-gradient(90deg, rgba(255,255,255,.05), rgba(255,255,255,.12), rgba(255,255,255,.05)); background-size:200% 100%; animation: sk 1.1s linear infinite; }
.empty{ padding: 24px; opacity: .85 }
</style>
