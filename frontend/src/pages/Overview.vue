<template>
  <DashboardGrid>
    <!-- Row 1: KPIs (Datasets, Trainings, Active Models, System Health) -->
    <div class="is_small"><InfoMetricTile title="Datasets" icon="mdi-database-import" :endpoint="ep.datasets.count()" /></div>
    <div class="is_small"><InfoMetricTile title="Trainings" icon="mdi-robot" :endpoint="ep.training.count({ status: 'active,total' })" /></div>
    <div class="is_small"><InfoMetricTile title="Active Models" icon="mdi-cube" :endpoint="ep.models.count({ status: 'active' })" /></div>
    <div class="is_small"><InfoMetricTile title="System Health" icon="mdi-stethoscope" :endpoint="ep.metrics.health()" /></div>

    <!-- Row 2: Large (Loss 7d, Requests/sec 7d) -->
    <div class="is_large"><PlaceholderLargeTile title="Loss (7d)" icon="mdi-chart-line" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Requests/sec (7d)" icon="mdi-chart-areaspline" /></div>

    <!-- Row 3: Large (Recent Trainings, Top Datasets) -->
    <div class="is_large"><PlaceholderLargeTile title="Recent Trainings" icon="mdi-history" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Top Datasets" icon="mdi-table" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import { Endpoints as ep } from '@/lib/endpoints'
</script>

<style scoped>
.placeholder{ height:100%; display:flex; align-items:center; justify-content:center; opacity:.6 }
</style>
