<template>
  <div class="config-panel">
    <div class="config-header">
      <slot name="title">
        <h3 class="title">Konfiguration</h3>
      </slot>
      <div class="tools">
        <slot name="tools" />
      </div>
    </div>
    <div class="config-body">
      <slot />
    </div>
    <div class="config-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
// simple flexible panel for forms
</script>

<style scoped>
.config-panel{
  display:flex; flex-direction:column; gap:12px;
  min-height: 300px; height: 100%; width: 100%;
  padding: 12px; border: 1px solid rgba(212,175,55,.24); border-radius: 8px;
  background: rgba(15,36,31,.6);
}
.config-header{ display:flex; align-items:center; justify-content:space-between; gap:8px }
.config-body{ flex:1; display:flex; flex-direction:column; justify-content:space-between; gap:12px }
.config-footer{ display:flex; justify-content:flex-end; gap:8px }
.title{ margin:0; font-size: 16px; font-weight: 600 }
</style>
