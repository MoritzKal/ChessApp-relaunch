<template>
  <LargeTile :title="title" :icon="icon">
    <div class="wrap">
      <div class="canvas">
        <Doughnut ref="chartRef" :data="dataRef" :options="optionsRef" :height="260" />
        <div v-if="loading" class="skeleton overlay" />
      </div>
      <div class="legend">
        <div v-for="(s, i) in segments" :key="i" class="lg_item">
          <span class="dot" :style="{ background: colors[i%colors.length] }" />
          <span class="lbl">{{ s.label }}</span>
          <span class="val">{{ fmtPct(s.value) }}</span>
        </div>
      </div>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { shallowRef, markRaw, ref, computed, watch } from 'vue'
import LargeTile from '@/components/tiles/LargeTile.vue'
import { Doughnut } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend)

interface Segment { label: string; value: number }
interface Props { title: string; icon?: string; segments: Segment[]; loading?: boolean }
const props = withDefaults(defineProps<Props>(), { segments: () => [], loading: false })

const colors = ['#6BB3FF', '#E57373', '#F0D26B', '#5FBF77']
const dataRef = shallowRef<any>({ labels: [], datasets: [] })
const optionsRef = shallowRef<any>(markRaw({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  cutout: '60%'
}))

function buildData(){
  const labels = props.segments.map(s => s.label)
  const values = props.segments.map(s => Math.max(0, Number(s.value) || 0))
  dataRef.value = markRaw({ labels, datasets: [{ data: values, backgroundColor: colors.slice(0, values.length) }] })
}

watch(() => props.segments, buildData, { immediate: true })

const chartRef = ref<any>(null)
function fmtPct(v: number){ return `${(Math.max(0, Number(v) || 0) * 100).toFixed(1)}%` }
</script>

<style scoped>
.wrap{ display:flex; height:100%; gap: 12px }
.canvas{ position:relative; flex:1; min-height: 260px }
.legend{ width: 200px; display:flex; flex-direction:column; justify-content:center; gap:8px; padding-right:8px }
.lg_item{ display:flex; align-items:center; gap:8px; justify-content:space-between }
.dot{ width:10px; height:10px; border-radius:50% }
.lbl{ opacity:.8 }
.val{ color:#F6F1D1; font-weight:600 }
.skeleton{ position:relative; border-radius: 10px; background: linear-gradient(90deg, rgba(255,255,255,.05), rgba(255,255,255,.12), rgba(255,255,255,.05)); background-size:200% 100%; animation: sk 1.1s linear infinite; }
.overlay{ position:absolute; inset:6px 6px 0 0; pointer-events:none }
@keyframes sk{ to{ background-position: 200% 0 } }
</style>

