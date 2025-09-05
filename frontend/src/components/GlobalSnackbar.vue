<template>
  <v-snackbar v-model="snack.show" :color="snack.color" timeout="2500" location="bottom right">
    {{ snack.text }}
  </v-snackbar>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
const ui = useUiStore()
const { snackbar: snack } = storeToRefs(ui)
</script>

