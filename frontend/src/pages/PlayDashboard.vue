<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Latency P50" icon="mdi-timer-sand" endpoint="/v1/metrics/latency?p=50" /></div>
    <div class="is_small"><InfoMetricTile title="Latency P95" icon="mdi-timer-sand-complete" endpoint="/v1/metrics/latency?p=95" /></div>
    <div class="is_small"><InfoMetricTile title="Moves/sec" icon="mdi-swap-horizontal" endpoint="/v1/metrics/mps" /></div>
    <div class="is_small"><InfoMetricTile title="Online Games" icon="mdi-account-group-outline" endpoint="/v1/games/online_count" /></div>

    <!-- Row 2: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Requests/sec (24h)" icon="mdi-chart-areaspline" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Error Rate (24h)" icon="mdi-alert-circle-outline" /></div>

    <!-- Row 3: Large -->
    <div class="is_large"><PlaceholderLargeTile title="Recent Games" icon="mdi-chess-queen" /></div>
    <div class="is_large"><PlaceholderLargeTile title="Engine ELO Trend (30d)" icon="mdi-chart-line" /></div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import PlaceholderLargeTile from '@/components/panels/PlaceholderLargeTile.vue'
import { Endpoints as ep } from '@/lib/endpoints'
</script>
