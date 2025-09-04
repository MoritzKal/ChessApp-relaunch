<template>
  <div class="app-shell-v2">
    <!-- Head: über die volle Breite -->
    <header class="app-head">
      <v-app-bar flat :height="64">
        <v-btn icon class="gold" aria-label="Crown"><v-icon>mdi-crown</v-icon></v-btn>
        <v-toolbar-title>ChessApp</v-toolbar-title>
        <v-spacer />
        <v-btn icon variant="text" aria-label="Help"><v-icon>mdi-help-circle-outline</v-icon></v-btn>
      </v-app-bar>
    </header>

    <!-- Body: Sidebar dockt unter der App-Bar an, bleibt permanent offen -->
    <div class="app-body">
      <aside class="app-sidebar">
        <v-navigation-drawer
          :model-value="true"
          permanent
          width="240"
          class="chs-drawer"
        >
          <v-list nav density="comfortable">
            <v-list-item prepend-icon="mdi-view-dashboard" title="Overview" :to="{name:'overview'}" />
            <v-list-item prepend-icon="mdi-database" title="Data" :to="{name:'data'}" />
            <v-list-item prepend-icon="mdi-database-cog" title="Datasets" :to="{name:'datasets'}" />
            <v-list-item prepend-icon="mdi-robot" title="Training" :to="{name:'training'}" />
            <v-list-item prepend-icon="mdi-cube" title="Models" :to="{name:'models'}" />
            <v-list-item prepend-icon="mdi-clipboard-pulse" title="Evaluation" :to="{name:'evaluation'}" />
            <v-list-item prepend-icon="mdi-chess-queen" title="Play" :to="{name:'play'}" />
            <v-list-item prepend-icon="mdi-chart-areaspline" title="Observability" :to="{name:'observability'}" />
          </v-list>
        </v-navigation-drawer>
      </aside>

      <main class="app-main">
        <v-container fluid class="chs-container">
          <router-view />
        </v-container>
      </main>
    </div>
    <v-snackbar v-model="snackbar.show" :color="snackbar.color">
      {{ snackbar.text }}
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
// reines Layoutimport { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { storeToRefs } from 'pinia';

const { snackbar } = storeToRefs(useUiStore())
</script>

<style scoped>
/* Shell: Kopf oben, darunter 2-Spalten-Layout (Sidebar/Main) */
.app-shell-v2{
  --header-h: 64px;
  --sidebar-w: 240px;
  min-height: 100vh;
  display: grid;
  grid-template-rows: var(--header-h) 1fr;
  grid-template-columns: 1fr;
}
.app-head{
  position: sticky; top:0; z-index: 10;
  background: linear-gradient(180deg,#132A26,#0D211D);
  border-bottom: 1px solid rgba(212,175,55,.25);
}
.app-body{
  display: grid;
  grid-template-columns: var(--sidebar-w) 1fr;
  min-height: calc(100vh - var(--header-h));
}
.app-sidebar{
  position: sticky;
  top: var(--header-h);
  height: calc(100vh - var(--header-h));
  overflow: auto;
  background: #0F241F;
  border-right: 1px solid rgba(212,175,55,.18);
}
.chs-drawer{ background:#0F241F !important; color: var(--text, #F6F1D1) !important; }
.app-main{
  padding: 20px;
  min-width: 0; /* verhindert Zusammenschieben des Inhalts */
}
.v-toolbar-title{ font-weight:700; letter-spacing:.4px; color: var(--text,#F6F1D1) }
</style>
