<template>
  <SmallTile :title="title" :icon="icon">
    <div class="kpi">
      <div v-if="loading" class="skeleton" />
      <div v-else-if="error" class="err">—</div>
      <div v-else class="val">{{ display }}</div>
    </div>
  </SmallTile>
  
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import SmallTile from '@/components/tiles/SmallTile.vue'
import { apiGet } from '@/lib/api'

interface Props {
  title: string
  icon?: string
  endpoint?: string
  value?: number | string
  valueKey?: string
  formatter?: (v: any) => string
}
const props = withDefaults(defineProps<Props>(), { valueKey: 'value' })

const loading = ref(false)
const error = ref<null | string>(null)
const data = ref<any>(null)

async function load() {
  if (!props.endpoint) return
  loading.value = true; error.value = null
  try {
    data.value = await apiGet<any>(props.endpoint)
  } catch (e: any) {
    error.value = e?.message || 'error'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.endpoint, () => load())

const raw = computed(() => {
  if (props.value !== undefined) return props.value
  if (!data.value) return null
  const k = props.valueKey!
  return typeof data.value === 'object' && k in data.value ? (data.value as any)[k] : data.value
})

const display = computed(() => {
  const v = raw.value
  if (props.formatter) return props.formatter(v)
  if (typeof v === 'number') {
    if (v >= 1_000_000) return (v/1_000_000).toFixed(1) + 'M'
    if (v >= 1_000) return (v/1_000).toFixed(1) + 'k'
    return String(v)
  }
  return v ?? '—'
})
</script>

<style scoped>
.kpi { height:100%; display:flex; align-items:center; }
.val { font-size: 2.2rem; font-weight: 800; color: var(--chs-brass); letter-spacing:.3px }
.skeleton { height: 28px; width: 52%; border-radius: 8px; background: linear-gradient(90deg, rgba(255,255,255,.06), rgba(255,255,255,.12), rgba(255,255,255,.06)); background-size: 200% 100%; animation: sk 1.1s linear infinite; }
@keyframes sk { to { background-position: 200% 0; } }
.err{ opacity:.6 }
</style>

