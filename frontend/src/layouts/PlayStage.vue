<template>
  <div class="chs_play">
    <div class="chs_play__stage">
      <slot />
    </div>
    <aside v-if="$slots.sidebar" class="chs_play__sidebar">
      <slot name="sidebar" />
    </aside>
  </div>
</template>

<script setup lang="ts">
// Centers a chessboard or game area with an optional sidebar.
</script>

<style scoped>
.chs_play{
  display: grid; gap: 16px; padding: 16px 20px;
  grid-template-columns: 1fr minmax(280px, 360px);
  max-width: 1600px; margin: 0 auto;
}
.chs_play__stage{ aspect-ratio: 1 / 1; width: 100%; max-width: 900px; justify-self: center; }

@media (max-width: 1200px){
  .chs_play{ grid-template-columns: 1fr; }
  .chs_play__stage{ max-width: 100%; }
}
</style>

