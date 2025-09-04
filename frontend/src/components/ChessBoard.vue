<!-- TODO: Minimal, but functional chess board (click-to-move). No external deps. -->
<template>
  <div class="chs_board" :class="[`ori-${orientation}`]">
    <div v-for="r in 8" :key="r" class="rank">
      <div
        v-for="f in 8"
        :key="f"
        class="sq"
        :class="squareClass(f, r)"
        @click="onSquareClick(f, r)"
      >
        <span v-if="pieceAt(f, r)" class="pc">{{ pieceGlyph(pieceAt(f, r)!) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, defineEmits, defineProps, defineExpose } from 'vue'

type Orientation = 'white' | 'black'

interface Props {
  initialFen?: string
  orientation?: Orientation
}
const props = withDefaults(defineProps<Props>(), {
  initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1',
  orientation: 'white',
})

const emit = defineEmits<{ (e: 'move', uci: string): void }>()

// Board is 8x8 array of piece codes, upper=white, lower=black
type Piece = 'P'|'N'|'B'|'R'|'Q'|'K'|'p'|'n'|'b'|'r'|'q'|'k'
type Board = (Piece | null)[][]
const board = ref<Board>(emptyBoard())
const sideToMove = ref<'w'|'b'>('w')

function emptyBoard(): Board { return Array.from({ length: 8 }, () => Array(8).fill(null)) }

function parseFen(fen: string) {
  try {
    const [bb, stm] = fen.split(' ')
    const rows = bb.split('/')
    const b = emptyBoard()
    for (let r = 0; r < 8; r++) {
      let c = 0
      for (const ch of rows[r]) {
        if (/[1-8]/.test(ch)) { c += Number(ch); continue }
        b[r][c] = ch as Piece
        c++
      }
    }
    board.value = b
    sideToMove.value = (stm as 'w'|'b') || 'w'
  } catch {}
}

function boardToFen(): string {
  const parts: string[] = []
  for (let r = 0; r < 8; r++) {
    let run = 0
    let row = ''
    for (let c = 0; c < 8; c++) {
      const p = board.value[r][c]
      if (!p) { run++; continue }
      if (run > 0) { row += String(run); run = 0 }
      row += p
    }
    if (run > 0) row += String(run)
    parts.push(row)
  }
  return parts.join('/') + ` ${sideToMove.value} - - 0 1`
}

function toIdx(file: number, rank: number): { r: number; c: number } {
  // file,rank are 1..8 in display space; orientation transforms to board indices
  const ori = props.orientation
  const r = ori === 'white' ? 8 - rank : rank - 1
  const c = ori === 'white' ? file - 1 : 8 - file
  return { r, c }
}

function idxToAlgAbs(r: number, c: number): string {
  const files = 'abcdefgh'
  const file = files[c]
  const rank = 8 - r
  return `${file}${rank}`
}

function pieceAt(file: number, rank: number): Piece | null {
  const { r, c } = toIdx(file, rank)
  return board.value[r][c]
}

function setPiece(file: number, rank: number, p: Piece | null) {
  const { r, c } = toIdx(file, rank)
  board.value[r][c] = p
}

function pieceGlyph(p: Piece): string {
  const map: Record<Piece, string> = {
    K: '♔', Q: '♕', R: '♖', B: '♗', N: '♘', P: '♙',
    k: '♚', q: '♛', r: '♜', b: '♝', n: '♞', p: '♟',
  }
  return map[p]
}

const selected = ref<{ f: number; r: number } | null>(null)

function onSquareClick(f: number, r: number) {
  if (selected.value == null) {
    const p = pieceAt(f, r)
    if (p) selected.value = { f, r }
    return
  }
  const from = selected.value
  selected.value = null
  if (!from) return
  if (from.f === f && from.r === r) return
  const p = pieceAt(from.f, from.r)
  if (!p) return
  // simple side-to-move enforcement by case (optional)
  const isWhite = p === p.toUpperCase()
  if ((sideToMove.value === 'w' && !isWhite) || (sideToMove.value === 'b' && isWhite)) return
  // apply move (no legality checks)
  setPiece(from.f, from.r, null)
  let place: Piece | null = p
  // naive promotion handling: auto-queen if pawn reaches last rank
  if ((p === 'P' && r === 8) || (p === 'p' && r === 1)) {
    place = isWhite ? 'Q' : 'q'
  }
  setPiece(f, r, place)
  const fromIdx = toIdx(from.f, from.r)
  const toIdxAbs = toIdx(f, r)
  const uFrom = idxToAlgAbs(fromIdx.r, fromIdx.c)
  const uTo = idxToAlgAbs(toIdxAbs.r, toIdxAbs.c)
  sideToMove.value = sideToMove.value === 'w' ? 'b' : 'w'
  emit('move', `${uFrom}${uTo}${place && (place === 'Q' || place === 'q') && (p === 'P' || p === 'p') ? (isWhite ? 'q' : 'q') : ''}`)
}

// Expose minimal API
function reset(fen?: string) { parseFen(fen || props.initialFen) }
function applyMove(uci: string) {
  const m = uci.trim()
  if (!/^[a-h][1-8][a-h][1-8][qrbn]?$/.test(m)) return
  const files = 'abcdefgh'
  const f1 = files.indexOf(m[0]) + 1
  const r1 = Number(m[1])
  const f2 = files.indexOf(m[2]) + 1
  const r2 = Number(m[3])
  const p = pieceAt(f1, r1)
  if (!p) return
  setPiece(f1, r1, null)
  let place: Piece | null = p
  if (m.length === 5) place = (sideToMove.value === 'w' ? m[4].toUpperCase() : m[4]) as Piece
  setPiece(f2, r2, place)
  sideToMove.value = sideToMove.value === 'w' ? 'b' : 'w'
}

defineExpose({ reset, applyMove, boardToFen })

watch(() => props.initialFen, (f) => parseFen(f), { immediate: true })

function squareClass(f: number, r: number) {
  const light = (f + r) % 2 === 0
  const sel = selected.value && selected.value.f === f && selected.value.r === r
  return { light, dark: !light, sel }
}
</script>

<style scoped>
.chs_board{ aspect-ratio: 1/1; width:100%; max-width: 520px; border: 1px solid rgba(212,175,55,.3); border-radius: 8px; overflow: hidden; }
.rank{ display: grid; grid-template-columns: repeat(8,1fr); height: 12.5%; }
.sq{ display:flex; align-items:center; justify-content:center; font-size: 2rem; cursor: pointer; user-select:none }
.sq.light{ background: #F6F1D1; color: #0D211D }
.sq.dark{ background: #CBA35C33; color: #0D211D }
.sq.sel{ outline: 3px solid #CBA35C; outline-offset: -3px }
.pc{ filter: drop-shadow(0 1px 0 #0004) }
</style>
