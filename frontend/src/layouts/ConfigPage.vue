<template>
  <div class="chs_config">
    <header v-if="$slots.header" class="chs_config__header">
      <slot name="header" />
    </header>

    <div class="chs_config__grid">
      <aside v-if="$slots.aside" class="chs_config__aside">
        <slot name="aside" />
      </aside>
      <section class="chs_config__main">
        <slot />
      </section>
    </div>
  </div>
  
</template>

<script setup lang="ts">
// Layout-only – provides a two-column configurable workspace.
</script>

<style scoped>
.chs_config{ max-width: 1440px; margin: 0 auto; padding: 16px 20px; }
.chs_config__header{ margin-bottom: 12px; }
.chs_config__grid{
  display: grid; gap: 16px;
  grid-template-columns: 280px 1fr;
  align-items: start;
}
.chs_config__aside{ position: sticky; top: 84px; align-self: start; }
.chs_config__main{ min-width: 0; }

@media (max-width: 1200px){
  .chs_config__grid{ grid-template-columns: 1fr; }
  .chs_config__aside{ position: static; }
}
</style>

