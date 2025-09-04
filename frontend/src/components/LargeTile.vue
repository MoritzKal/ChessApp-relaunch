<template>
  <div class="chs-tile chs-tile--large" :style="tileStyle">
    <div v-if="title" class="chs-tile__header">
      <v-icon v-if="icon" size="18" class="mr-2 gold">{{ icon }}</v-icon>
      <span>{{ title }}</span>
    </div>

    <div class="chs-tile__body">
      <slot />
    </div>

    <div v-if="$slots.footer" class="chs-tile__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title?: string
  icon?: string
  /* absolute + relative Größen erlaubt (z.B. 420, "40vh", "100%") */
  width?: number | string
  height?: number | string
  minWidth?: number | string
  maxWidth?: number | string
  minHeight?: number | string
  maxHeight?: number | string
}

const props = withDefaults(defineProps<Props>(), {
  minHeight: 340,
  maxHeight: 560,
  minWidth: 420,
  maxWidth: 'auto',
})

const toCss = (v: number | string | undefined) =>
  v === undefined ? undefined : (typeof v === 'number' ? `${v}px` : v)

const tileStyle = computed(() => ({
  width: toCss(props.width),
  height: toCss(props.height),
  minWidth: toCss(props.minWidth),
  maxWidth: toCss(props.maxWidth),
  minHeight: toCss(props.minHeight),
  maxHeight: toCss(props.maxHeight),
}))
</script>

<style scoped>
.chs-tile{
  background: var(--panel, #0E2A24);
  color: var(--text, #F6F1D1);
  border-radius: var(--radius, 14px);
  border: 1.5px solid var(--accent, #D4AF37);
  box-shadow: var(--shadow, 0 4px 14px rgba(0,0,0,.35));
  display: flex; flex-direction: column; overflow: hidden;
}
.chs-tile__header{
  color: var(--muted, #C6B07A);
  font-weight: 600;
  padding: 12px 14px 10px;
  border-bottom: 1px solid rgba(212,175,55,.18);
}
.chs-tile__body{ flex:1 1 auto; padding: 14px }
.chs-tile__footer{ padding:12px 14px; border-top:1px dashed rgba(212,175,55,.18) }
</style>
