<template>
  <LargeTile :title="title" :icon="icon">
    <div class="wrap">
      <div v-if="loading" class="loading"><v-progress-circular indeterminate size="24" class="mr-2"/>Loading…</div>
      <div v-else-if="error" class="error"><v-icon color="error" class="mr-1">mdi-alert</v-icon>{{ errorMessage }}</div>
      <div v-else-if="!items || items.length === 0" class="empty">No data</div>
      <ul v-else class="list">
        <li v-for="(it, idx) in items" :key="idx">{{ it }}</li>
      </ul>
    </div>
  </LargeTile>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LargeTile from '@/components/tiles/LargeTile.vue'

interface Props { title: string; icon?: string; items?: string[]; loading?: boolean; error?: string | boolean }
const props = withDefaults(defineProps<Props>(), { items: () => [], loading: false, error: false })
const errorMessage = computed(() => typeof props.error === 'string' ? props.error : 'Error')
</script>

<style scoped>
.wrap{ height:100%; display:flex; padding: 8px 10px; }
.list{ list-style: none; padding: 0; margin: 0; width: 100%; }
.list > li{ padding: 6px 0; border-bottom: 1px dotted rgba(212,175,55,.18) }
.list > li:last-child{ border-bottom: 0 }
.loading, .error, .empty{ margin:auto; opacity:.9 }
</style>

