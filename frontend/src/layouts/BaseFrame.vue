<template>
  <v-app>
    <v-navigation-drawer class="chs-nav" rail expand-on-hover app>
      <v-list nav density="comfortable">
        <v-list-item to="/" prepend-icon="mdi-view-dashboard" title="Overview" />
        <v-list-item to="/config" prepend-icon="mdi-tune" title="Configuration" />
        <v-list-item to="/obs" prepend-icon="mdi-chart-areaspline" title="Observability" />
      </v-list>
    </v-navigation-drawer>

    <v-app-bar flat color="transparent">
      <v-container class="d-flex align-center">
        <v-icon class="mr-3" color="primary">mdi-crown</v-icon>
        <span class="text-h6">ChessApp</span>
        <v-spacer />
        <v-btn icon="mdi-help-circle-outline" variant="text" />
      </v-container>
    </v-app-bar>

    <v-main>
      <v-container class="py-6">
        <router-view />
      </v-container>
    </v-main>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'

const { snackbar } = storeToRefs(useUiStore())
</script>

