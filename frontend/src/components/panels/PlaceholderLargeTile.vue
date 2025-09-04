<template>
  <LargeTile :title="title" :icon="icon">
    <div class="ph">
      <v-icon size="48" class="gold" v-if="icon">{{ icon }}</v-icon>
      <div class="msg">{{ message }}</div>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import LargeTile from '@/components/tiles/LargeTile.vue'

interface Props { title: string; icon?: string; message?: string }
withDefaults(defineProps<Props>(), { message: 'Placeholder – chart/table coming soon' })
</script>

<style scoped>
.ph{ height:100%; display:flex; gap: 12px; align-items:center; justify-content:center; opacity:.85 }
.msg{ color: rgba(237,237,237,.75) }
</style>

