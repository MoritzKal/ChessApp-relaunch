<template>
  <LargeTile :title="title" :icon="icon">
    <div class="ph" :class="{ loading, error: !!error }">
      <template v-if="loading">
        <v-progress-circular indeterminate size="28" color="primary" class="mr-2" />
        <div class="msg">Loading…</div>
      </template>
      <template v-else-if="error">
        <v-icon class="mr-2" color="error">mdi-alert</v-icon>
        <div class="msg">{{ errorMessage }}</div>
      </template>
      <template v-else>
        <v-icon size="48" class="gold" v-if="icon">{{ icon }}</v-icon>
        <div class="msg">{{ message }}</div>
      </template>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LargeTile from '@/components/tiles/LargeTile.vue'

interface Props { title: string; icon?: string; message?: string; loading?: boolean; error?: string | boolean }
const props = withDefaults(defineProps<Props>(), { message: 'Placeholder – chart/table coming soon', loading: false, error: false })
const errorMessage = computed(() => typeof props.error === 'string' ? props.error : 'Error loading data')
</script>

<style scoped>
.ph{ height:100%; display:flex; gap: 12px; align-items:center; justify-content:center; opacity:.92 }
.msg{ color: rgba(237,237,237,.75) }
.error .msg{ color: #E57373 }
</style>
