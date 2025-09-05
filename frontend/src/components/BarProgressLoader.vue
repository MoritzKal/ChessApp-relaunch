<template>
  <div class="bar_wrap" v-if="active">
    <v-progress-linear
      :indeterminate="indeterminate || value==null"
      :model-value="value ?? 0"
      height="6"
      rounded
      striped
      color="primary"
    />
    <div class="label" v-if="text">{{ text }}</div>
  </div>
  <div v-else></div>
</template>

<script setup lang="ts">
interface Props {
  active?: boolean
  indeterminate?: boolean
  value?: number | null
  text?: string
}
withDefaults(defineProps<Props>(), { active: true, indeterminate: true, value: null, text: '' })
</script>

<style scoped>
.bar_wrap{ position: relative; padding: 6px 0 }
.label{ font-size: .85rem; opacity: .85; margin-top: 4px }
</style>

