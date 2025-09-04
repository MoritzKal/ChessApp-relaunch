<template>
  <DashboardGrid>
    <!-- Reihe 1: KPIs -->
    <div class="is_small">
      <SmallTile title="Games Imported" icon="mdi-database-import">
        <div class="kpi"><span class="kpi_v">1,248</span></div>
      </SmallTile>
    </div>

    <div class="is_small">
      <SmallTile title="Active Training" icon="mdi-robot">
        <div class="kpi"><span class="kpi_v">1</span></div>
      </SmallTile>
    </div>

    <div class="is_small">
      <SmallTile title="Best Val-Acc" icon="mdi-chart-line">
        <div class="kpi"><span class="kpi_v">0.41</span></div>
      </SmallTile>
    </div>

    <div class="is_small">
      <SmallTile title="Prod Model" icon="mdi-cube">
        <div class="kpi"><span class="kpi_v">v1.2</span></div>
      </SmallTile>
    </div>

    <!-- Reihe 2 -->
    <div class="is_large">
      <LargeTile title="Training Loss" icon="mdi-chart-areaspline">
        <!-- Chart-Placeholder: später echtes Chart einklinken -->
        <div class="placeholder">Chart (Training Loss)</div>
      </LargeTile>
    </div>

    <div class="is_large">
      <LargeTile title="Recent Trainings" icon="mdi-history">
        <div class="list">
          <div>20240403-1 • Epoch 10 • Val 0.38</div>
          <div>20240402-2 • Epoch 15 • Val 0.40</div>
          <div>20240402-1 • Epoch 20 • Val 0.37</div>
        </div>
      </LargeTile>
    </div>

    <!-- Reihe 3 -->
    <div class="is_large">
      <LargeTile title="Requests/sec" icon="mdi-timer-sand">
        <div class="placeholder">Chart (Requests/sec)</div>
      </LargeTile>
    </div>

    <div class="is_large">
      <LargeTile title="Board" icon="mdi-chess-queen">
        <div class="board_ph">
          <v-icon size="96" class="gold">mdi-chess-queen</v-icon>
        </div>
      </LargeTile>
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import SmallTile from '@/components/tiles/SmallTile.vue'
import LargeTile from '@/components/tiles/LargeTile.vue'
</script>

<style scoped>
.kpi{ display:flex; align-items:center; height:100% }
.kpi_v{ color: var(--accent, #D4AF37); font-weight:800; font-size: 2rem }
.list > div{ padding: 6px 0; border-bottom: 1px dotted rgba(212,175,55,.18) }
.list > div:last-child{ border-bottom:0 }
.placeholder{ height:100%; display:flex; align-items:center; justify-content:center; opacity:.6 }
.board_ph{ height:100%; display:flex; align-items:center; justify-content:center }
</style>
