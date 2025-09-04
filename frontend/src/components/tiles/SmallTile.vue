<template>
  <div class="chs_tile chs_tile__small" :style="tileStyle">
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
  minHeight: 180,
  maxHeight: 280,
  minWidth: 220,
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
.chs_tile{
  background: var(--panel, #0E2A24);
  color: var(--text, #F6F1D1);
  border-radius: var(--radius, 14px);
  border: 2px solid var(--accent, #D4AF37); /* goldener Rand */
  box-shadow: var(--shadow, 0 4px 14px rgba(0,0,0,.35));
  display: flex; flex-direction: column; overflow: hidden;
}
.chs_tile__header{
  color: var(--muted, #C6B07A);
  font-weight: 600; padding: 10px 12px 8px;
  border-bottom: 1px solid rgba(212,175,55,.18);
}
.chs_tile__body{ flex:1 1 auto; padding: 12px }
.chs_tile__footer{ padding:10px 12px; border-top:1px dashed rgba(212,175,55,.18) }
</style>
