<template>
  <v-app theme="chsDark">
    <v-navigation-drawer class="chs-nav" color="surface" rail expand-on-hover app>
      <v-list nav density="comfortable">
        <v-list-item to="/" prepend-icon="mdi-view-dashboard" title="Overview" />
        <v-list-item to="/config" prepend-icon="mdi-tune" title="Configuration" />
        <v-list-item to="/styleguide" prepend-icon="mdi-palette" title="Styleguide" />
        <v-list-item to="/obs" prepend-icon="mdi-chart-areaspline" title="Observability" />
      </v-list>
    </v-navigation-drawer>
    <v-container>
      <v-app-bar app class="chs-appbar">
        <v-container class="d-flex align-center">
          <v-icon class="mr-3" color="primary">mdi-crown</v-icon>
          <span class="text-h6">
            <p class="header">ChessApp</p>
          </span>
          <v-spacer />
          <v-btn icon="mdi-help-circle-outline" variant="text" />
        </v-container>
      </v-app-bar>
    </v-container>
    <v-container>
      <v-main class="chs-main">
        <v-container fluid class="chs-container">
          <router-view />
        </v-container>
      </v-main>
    </v-container>
    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import theme from '@/plugins/theme'

const { snackbar } = storeToRefs(useUiStore())
</script>
<style>



</style>
