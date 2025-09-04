<template>
  <LargeTile :title="title" :icon="icon">
    <div class="chart_wrap">
      <div class="toolbar">
        <v-btn-toggle v-model="localRange" density="compact" divided>
          <v-btn value="24h" size="small">24h</v-btn>
          <v-btn value="7d" size="small">7d</v-btn>
          <v-btn value="30d" size="small">30d</v-btn>
        </v-btn-toggle>
      </div>
      <div class="canvas">
        <Line ref="chartRef" :data="dataRef" :options="optionsRef" :dataset-id-key="'label'" :height="280" />
        <div v-if="loading" class="skeleton overlay"/>
      </div>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { computed, ref, watch, shallowRef, markRaw, onBeforeUnmount } from 'vue'
import LargeTile from '@/components/tiles/LargeTile.vue'
import type { SeriesVM } from '@/types/vm'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS, LineElement, PointElement, LinearScale, TimeScale, Tooltip, Legend, Filler, CategoryScale, Decimation
} from 'chart.js'
import 'chartjs-adapter-date-fns'

ChartJS.register(LineElement, PointElement, LinearScale, TimeScale, Tooltip, Legend, Filler, CategoryScale, Decimation)

interface Props {
  title: string
  icon?: string
  vm: SeriesVM | null
  range?: string
  loading?: boolean
  maxPoints?: number
}
const props = withDefaults(defineProps<Props>(), { loading: false, range: '7d', maxPoints: 1500 })
const emit = defineEmits<{ (e:'update:range', val: string): void }>()

const localRange = ref(props.range!)
watch(localRange, v => emit('update:range', v))
watch(() => props.range, v => { if (v) localRange.value = v })

function thin(data: { x:number;y:number }[], limit: number){
  if (!data || data.length <= limit) return data
  const step = Math.ceil(data.length / limit)
  const res: {x:number;y:number}[] = []
  for (let i=0; i<data.length; i+=step) res.push(data[i])
  return res
}

const dataRef = shallowRef<any>({ datasets: [] })
const optionsRef = shallowRef<any>(null)

function buildOptions(heavy = false){
  return markRaw({
    responsive: true,
    maintainAspectRatio: false,
    interaction: { intersect: false, mode: 'nearest' as const },
    scales: {
      x: { type: 'time' as const, time: { tooltipFormat: 'dd.MM HH:mm' }, ticks: { color: '#CBA35C99' }, grid: { color: '#ffffff12' } },
      y: { type: 'linear' as const, ticks: { color: '#CBA35C99' }, grid: { color: '#ffffff12' } }
    },
    plugins: {
      legend: { labels: { color: '#F6F1D1' } },
      tooltip: { enabled: true },
      decimation: { enabled: true, algorithm: 'lttb', samples: 1200 }
    },
    parsing: false,
    normalized: true,
    animation: { duration: heavy ? 0 : 120 }
  })
}

watch(() => props.vm, (vm) => {
  if (!vm) { dataRef.value = markRaw({ datasets: [] }); optionsRef.value = buildOptions(false); return }
  const colors = ['#F0D26B','#6BB3FF','#5FBF77','#E57373']
  let total = 0
  const datasets = vm.series.map((s, i) => {
    const pts = thin(s.data.map(p => ({ x: p.x, y: p.y })), props.maxPoints)
    total += pts.length
    return markRaw({
      label: s.label,
      data: pts,
      borderColor: s.color || colors[i%colors.length],
      backgroundColor: (s.color || colors[i%colors.length]) + '33',
      fill: s.type === 'area',
      pointRadius: 0,
      tension: 0.25,
      borderWidth: 2,
    })
  })
  const heavy = total > 5000
  if (heavy) {
    datasets.forEach(d => { d.backgroundColor = d.backgroundColor.replace('33','00'); d.tension = 0; })
  }
  dataRef.value = markRaw({ datasets })
  optionsRef.value = buildOptions(heavy)
}, { immediate: true })

const chartRef = ref<any>(null)
onBeforeUnmount(() => { try { chartRef.value?.chart?.destroy?.() } catch { /* noop */ } })
</script>

<style scoped>
.chart_wrap{ position:relative; height:100%; display:flex; flex-direction:column }
.toolbar{ display:flex; justify-content:flex-end; padding: 2px 6px }
.canvas{ position:relative; flex:1; min-height: 260px }
.skeleton{ position:relative; border-radius: 10px; background: linear-gradient(90deg, rgba(255,255,255,.05), rgba(255,255,255,.12), rgba(255,255,255,.05)); background-size:200% 100%; animation: sk 1.1s linear infinite; }
.overlay{ position:absolute; inset:6px 6px 0 0; pointer-events:none }
@keyframes sk{ to{ background-position: 200% 0 } }
</style>
