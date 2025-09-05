<template>
  <v-dialog v-model="model" width="960" scrollable :scrim="false">
    <v-card class="chs-card" style="max-height: 80vh; display:flex; flex-direction:column;">
      <v-card-title class="d-flex align-center justify-space-between">
        <div class="d-flex align-center gap-2"><v-icon>mdi-console</v-icon><span>Debug Konsole</span></div>
        <div class="d-flex align-center gap-2">
          <v-btn size="small" icon title="Copy to clipboard" @click="copy"><v-icon>mdi-content-copy</v-icon></v-btn>
          <v-btn size="small" icon title="Close" @click="() => emit('update:modelValue', false)"><v-icon>mdi-close</v-icon></v-btn>
        </div>
      </v-card-title>
      <v-card-text style="display:flex; flex-direction:column; gap:8px;">
        <v-tabs v-model="tab" density="compact">
          <v-tab v-for="s in sources" :key="s" :value="s">{{ s }}</v-tab>
        </v-tabs>
        <div class="logwrap">
          <pre ref="pre" class="logarea">{{ lines.join('\n') }}</pre>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { getAppLogs, type LogLine } from '@/services/logs'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits(['update:modelValue'])
const model = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })

const sources = ['api','ml','serve','obs-proxy']
const tab = ref('api')
const pre = ref<HTMLElement | null>(null)
const lines = ref<string[]>([])
let timer: number | null = null

async function refresh(){
  try {
    const list = await getAppLogs(tab.value, { range: '15m', limit: 500, direction: 'backward' })
    lines.value = (list || []).map(l => `[${l.time}] ${l.level?.padEnd(5)} ${l.msg}`)
    requestAnimationFrame(() => { if (pre.value) pre.value.scrollTop = pre.value.scrollHeight })
  } catch {}
}

function start(){ if (timer) stop(); refresh(); timer = window.setInterval(refresh, 3000) }
function stop(){ if (timer) { window.clearInterval(timer); timer = null } }

function copy(){ try { navigator.clipboard.writeText(lines.value.join('\n')) } catch {}
}

watch(tab, () => start())
watch(model, (v) => { if (v) start(); else stop() })

onMounted(() => { if (model.value) start() })
onBeforeUnmount(() => stop())
</script>

<style scoped>
.logwrap{ flex:1; overflow:auto; border:1px solid rgba(212,175,55,.24); border-radius:8px; background: #0B1F1A; padding:8px; }
.logarea{ font-family: var(--font-mono, 'JetBrains Mono', monospace); font-size: 12px; margin:0; white-space:pre; color:#DAE9E2; min-height: 360px; }
</style>
