<template>
  <div class="chs_tile chs_tile__large" :style="tileStyle">
    <header v-if="title" class="chs_tile__header">
      <v-icon v-if="icon" size="18" class="mr-2 gold">{{ icon }}</v-icon>
      <span>{{ title }}</span>
    </header>

    <section class="chs_tile__body">
      <slot />
    </section>

    <footer v-if="$slots.footer" class="chs_tile__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type Size = number | string
interface Props {
  title?: string
  icon?: string
  width?: Size;  height?: Size;
  minWidth?: Size; maxWidth?: Size;
  minHeight?: Size; maxHeight?: Size;
}
const props = withDefaults(defineProps<Props>(), {
  minHeight: 340,
  maxHeight: 560,
  minWidth: 420,
  maxWidth: 'none',
})

const toCss = (v?: Size) => v === undefined ? undefined : (typeof v === 'number' ? `${v}px` : v)
const tileStyle = computed(() => ({
  width: toCss(props.width),   height: toCss(props.height),
  minWidth: toCss(props.minWidth), maxWidth: toCss(props.maxWidth),
  minHeight: toCss(props.minHeight), maxHeight: toCss(props.maxHeight),
}))
</script>

<style scoped>
/* Basis-Styling – analog SmallTile, mit sanftem Messing-Schimmer */
.chs_tile{
  position: relative;
  background: linear-gradient(180deg, var(--panel, #0E2A24), #0c1a18);
  color: var(--text, #F6F1D1);
  border-radius: var(--radius, 14px);
  border: 1px solid rgba(212,175,55,.35);
  box-shadow:
    0 10px 24px rgba(203,163,92,0.10),
    0 2px 10px rgba(0,0,0,0.35),
    inset 0 1px 0 rgba(255,255,255,0.04),
    inset 0 -1px 0 rgba(0,0,0,0.25);
  display: flex; flex-direction: column; overflow: hidden;
}
.chs_tile::before{
  content:"";
  position:absolute; inset:0; border-radius: inherit; padding:1px;
  background: linear-gradient(120deg, rgba(240,210,107,.20), rgba(240,210,107,.80), rgba(240,210,107,.20));
  background-size: 200% 200%;
  animation: brassShimmer 10s linear infinite;
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor; mask-composite: exclude;
  pointer-events: none;
}
@keyframes brassShimmer{ to { background-position: 200% 0; } }
.chs_tile__header{
  color: var(--muted, #C6B07A);
  font-weight: 600; padding: 12px 14px 10px;
  border-bottom: 1px solid rgba(212,175,55,.18);
}
.chs_tile__body{ flex:1 1 auto; padding: 14px }
.chs_tile__footer{ padding:12px 14px; border-top:1px dashed rgba(212,175,55,.18) }
</style>
