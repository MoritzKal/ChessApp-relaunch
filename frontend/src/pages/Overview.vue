<template>
  <div class="chs-grid">
    <!-- Reihe 1 -->
    <SmallTile class="is-small" title="Games Imported">
      <div class="kpi"><span class="kpi-v">1,248</span></div>
    </SmallTile>

    <SmallTile class="is-small" title="Active Training">
      <div class="kpi"><span class="kpi-v">1</span></div>
    </SmallTile>

    <SmallTile class="is-small" title="Best Val-Acc">
      <div class="kpi"><span class="kpi-v">0.41</span></div>
    </SmallTile>

    <SmallTile class="is-small" title="Prod Model">
      <div class="kpi"><span class="kpi-v">v1.2</span></div>
    </SmallTile>

    <!-- Reihe 2 -->
    <LargeTile class="is-large" title="Training Loss">
      <!-- Chart-Komponente später einhängen -->
      <div style="height:100%;display:flex;align-items:center;justify-content:center;opacity:.6">
        Chart Placeholder
      </div>
    </LargeTile>

    <LargeTile class="is-large" title="Recent Trainings">
      <div class="list">
        <div>20240403-1 • Epoch 10 • Val 0.38</div>
        <div>20240402-2 • Epoch 15 • Val 0.40</div>
        <div>20240402-1 • Epoch 20 • Val 0.37</div>
      </div>
    </LargeTile>

    <!-- Reihe 3 -->
    <LargeTile class="is-large" title="Requests/sec">
      <div style="height:100%;display:flex;align-items:center;justify-content:center;opacity:.6">
        Chart Placeholder
      </div>
    </LargeTile>

    <LargeTile class="is-large" title="Board">
      <div style="height:100%;display:flex;align-items:center;justify-content:center;">
        <v-icon size="96" class="gold">mdi-chess-queen</v-icon>
      </div>
    </LargeTile>
  </div>
</template>

<script setup lang="ts">
import SmallTile from '@/components/SmallTile.vue'
import LargeTile from '@/components/LargeTile.vue'
</script>

<style scoped>
.kpi{ display:flex; align-items:center; height:100% }
.kpi-v{ color: var(--accent, #D4AF37); font-weight:800; font-size: 2rem }
.list > div{ padding: 6px 0; border-bottom: 1px dotted rgba(212,175,55,.18) }
.list > div:last-child{ border-bottom:0 }
</style>
