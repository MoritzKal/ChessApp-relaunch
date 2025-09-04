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
      <div v-if="loading" class="skeleton"/>
      <Line v-else :data="chartData" :options="chartOptions" :height="280" />
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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

const chartData = computed(() => {
  const vm = props.vm
  if (!vm) return { labels: [], datasets: [] }
  return {
    datasets: vm.series.map((s, i) => ({
      label: s.label,
      data: thin(s.data.map(p => ({ x: p.x, y: p.y })), props.maxPoints),
      borderColor: s.color || ['#F0D26B','#6BB3FF','#5FBF77','#E57373'][i%4],
      backgroundColor: (s.color || '#F0D26B') + '33',
      fill: s.type === 'area',
      pointRadius: 0,
      tension: 0.25,
      borderWidth: 2,
    }))
  }
})

const chartOptions = computed(() => ({
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
  animation: { duration: 120 }
}))
</script>

<style scoped>
.chart_wrap{ position:relative; height:100%; display:flex; flex-direction:column }
.toolbar{ display:flex; justify-content:flex-end; padding: 2px 6px }
.skeleton{ flex:1; border-radius: 10px; background: linear-gradient(90deg, rgba(255,255,255,.05), rgba(255,255,255,.12), rgba(255,255,255,.05)); background-size:200% 100%; animation: sk 1.1s linear infinite; }
@keyframes sk{ to{ background-position: 200% 0 } }
</style>
