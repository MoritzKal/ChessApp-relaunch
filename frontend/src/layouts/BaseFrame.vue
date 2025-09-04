<template>
  <div class="chs_app_shell">
    <!-- Head (voller oberer Rand) -->
    <header class="chs_head">
      <v-app-bar flat :height="64">
        <v-btn icon class="gold" aria-label="Home" :to="'/'" title="Home">
          <v-icon>mdi-crown</v-icon>
        </v-btn>
        <v-toolbar-title>ChessApp</v-toolbar-title>
        <v-spacer />
        <v-btn icon variant="text" aria-label="Help" :to="'/help'"><v-icon>mdi-help-circle-outline</v-icon></v-btn>
      </v-app-bar>
    </header>

    <!-- Body: Sidebar dockt unter der App-Bar an und bleibt offen -->
    <div class="chs_body">
      <aside class="chs_sidebar">
        <v-navigation-drawer
          :model-value="true"
          permanent
          width="240"
          class="chs_drawer"
        >
          <v-list nav density="comfortable">
            <v-list-subheader class="text-uppercase" inset>Dashboards</v-list-subheader>
            <v-list-item prepend-icon="mdi-home" title="Overview" :to="'/'" />
            <v-list-item prepend-icon="mdi-robot" title="Training" :to="'/training'" />
            <v-list-item prepend-icon="mdi-database" title="Datasets" :to="'/datasets-overview'" />
            <v-list-item prepend-icon="mdi-chess-queen" title="Play" :to="'/play'" />

            <v-divider class="my-2 chs-divider" />

            <v-list-item prepend-icon="mdi-cube" title="Models" :to="'/models'" />
            <v-list-item prepend-icon="mdi-clipboard-pulse" title="Evaluation" :to="'/evaluation'" />
            <v-list-item prepend-icon="mdi-chart-areaspline" title="Observability" :to="'/observability'" />

            <v-divider class="my-2 chs-divider" />

            <v-list-subheader class="text-uppercase" inset>Configuration</v-list-subheader>
            <v-list-item prepend-icon="mdi-tune" title="Training" :to="'/config/training'" />
            <v-list-item prepend-icon="mdi-database-plus" title="Datasets" :to="'/datasets'" />
            <v-list-item prepend-icon="mdi-upload" title="Import Dataset" :to="'/datasets/import'" />
          </v-list>
        </v-navigation-drawer>
      </aside>

      <main class="chs_main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
// Layout-only
</script>

<style scoped>
.chs_app_shell{
  --header-h: 64px; --sidebar-w: 240px;
  min-height: 100vh;
  display: grid;
  grid-template-rows: var(--header-h) 1fr;
}
.chs_head{
  position: sticky; top: 0; z-index: 10;
  background: linear-gradient(180deg,#132A26,#0D211D);
  border-bottom: 1px solid rgba(212,175,55,.25);
}
.chs_body{
  display: grid;
  grid-template-columns: var(--sidebar-w) 1fr;
  min-height: calc(100vh - var(--header-h));
}
.chs_sidebar{
  position: sticky; top: var(--header-h);
  height: calc(100vh - var(--header-h));
  overflow: auto;
  background: #0F241F;
  border-right: 1px solid rgba(212,175,55,.18);
}
.chs_drawer{ background:#0F241F !important; color: var(--text, #F6F1D1) !important; }
.chs_main{ padding: 20px; min-width: 0; width: 100%; } /* verhindert Zusammenschieben und nutzt volle Breite */
.v-toolbar-title{ font-weight:700; letter-spacing:.4px; color: var(--text,#F6F1D1) }
</style>
