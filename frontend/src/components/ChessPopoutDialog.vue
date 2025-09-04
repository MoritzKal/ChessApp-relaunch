<template>
  <v-dialog v-model="model" max-width="700">
    <v-card class="chs-card">
      <v-card-title class="d-flex align-center justify-space-between">
        <div>Chessboard</div>
        <v-btn icon variant="text" @click="close"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>
      <v-card-text>
        <div class="board_wrap">
          <ChessBoard ref="boardRef" :initial-fen="fen" :orientation="orientation" @move="onUserMove" />
        </div>
        <div class="board_tools">
          <v-btn size="small" variant="tonal" @click="onNewGame" prepend-icon="mdi-restart">Neu starten</v-btn>
          <v-btn size="small" variant="tonal" @click="rotate" prepend-icon="mdi-axis-z-rotate-clockwise">Board drehen 180°</v-btn>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ChessBoard from '@/components/ChessBoard.vue'
import { usePlayGameStore } from '@/stores/usePlayGameStore'

interface Props { modelValue: boolean }
const props = defineProps<Props>()
const emit = defineEmits<{ (e:'update:modelValue', v:boolean): void }>()
const model = ref<boolean>(props.modelValue)
watch(() => props.modelValue, (v)=> model.value = v)
watch(model, (v)=> emit('update:modelValue', v))

const pg = usePlayGameStore()
const boardRef = ref<InstanceType<typeof ChessBoard> | null>(null)
const fen = computed(() => pg.fen)
const orientation = ref<'white'|'black'>('white')

function close(){ model.value = false }
function rotate(){ orientation.value = orientation.value === 'white' ? 'black' : 'white' }
function onNewGame(){ pg.newGame(); boardRef.value?.reset(pg.fen) }
function onUserMove(uci: string){
  const curFen = boardRef.value?.boardToFen() || pg.fen
  pg.makeUserMove(uci, curFen)
}
</script>

<style scoped>
.board_wrap{ display:flex; justify-content:center; }
.board_tools{ display:flex; align-items:center; gap:8px; justify-content:space-between; margin-top:8px }
</style>

