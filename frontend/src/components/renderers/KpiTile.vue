<template>
  <SmallTile :title="vm.label" :icon="icon" @click="onClick" :style="clickable ? 'cursor:pointer' : ''">
    <div class="wrap" :aria-label="vm.ariaLabel || vm.label">
      <div class="value">
        <span class="num">{{ displayValue }}</span>
        <span v-if="vm.unit" class="unit">{{ vm.unit }}</span>
      </div>
      <div class="meta">
        <v-chip v-if="vm.badge" size="small" label>{{ vm.badge }}</v-chip>
        <div v-if="vm.delta" class="delta" :class="vm.delta.direction">
          <v-icon size="16" class="mr-1">{{ deltaIcon }}</v-icon>{{ fmtDelta }}
        </div>
      </div>
    </div>
  </SmallTile>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SmallTile from '@/components/tiles/SmallTile.vue'
import type { KpiVM } from '@/types/vm'
import { useRouter } from 'vue-router'

interface Props { vm: KpiVM; icon?: string; loading?: boolean }
const props = withDefaults(defineProps<Props>(), { loading: false })
const router = useRouter()

const displayValue = computed(() => props.vm.value)
const fmtDelta = computed(() => props.vm.delta ? `${props.vm.delta.value > 0 ? '+' : ''}${props.vm.delta.value}` : '')
const deltaIcon = computed(() => {
  if (!props.vm.delta) return 'mdi-minus'
  return props.vm.delta.direction === 'up' ? 'mdi-arrow-up' : props.vm.delta.direction === 'down' ? 'mdi-arrow-down' : 'mdi-minus'
})

const clickable = computed(() => !!props.vm.href)
function onClick(){ if (props.vm.href) router.push(props.vm.href) }
</script>

<style scoped>
.wrap{ display:flex; align-items:center; justify-content:space-between; height:100% }
.value{ display:flex; align-items:baseline; gap:8px }
.num{ font-size:2.2rem; font-weight:800; letter-spacing:.2px; color: var(--chs-brass) }
.unit{ opacity:.85; font-size:.95rem }
.meta{ display:flex; align-items:center; gap:10px }
.delta{ display:flex; align-items:center; font-weight:600 }
.delta.up{ color:#5FBF77 }
.delta.down{ color:#E57373 }
</style>

