<template>
  <v-container class="py-6" fluid>
    <!-- KPI Row -->
    <v-row dense>
      <v-col v-for="k in kpis" :key="k.label" cols="12" sm="6" md="3">
        <v-card class="chs-card">
          <div class="chs-kpi">
            <div class="label">{{ k.label }}</div>
            <div class="value">{{ k.value }}</div>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- Charts & Table -->
    <v-row dense class="mt-2">
      <v-col cols="12" md="7">
        <v-card class="chs-card" min-height="280">
          <v-card-title class="text-subtitle-1" style="color:#EBD6A2">Training Loss</v-card-title>
          <v-divider class="chs-divider" />
          <div class="pa-4">
            <!-- Chart placeholder; später via vue-echarts/chart.js -->
            <div style="height:220px; opacity:.9; border:1px dashed rgba(203,163,92,.25); border-radius:8px;"></div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" md="5">
        <v-card class="chs-card" min-height="280">
          <v-card-title class="text-subtitle-1" style="color:#EBD6A2">Recent Trainings</v-card-title>
          <v-divider class="chs-divider" />
          <v-table density="comfortable">
            <thead>
              <tr><th>Run ID</th><th>Epoch</th><th>Val-Acc</th><th>Status</th></tr>
            </thead>
            <tbody>
              <tr v-for="t in trainings" :key="t.runId">
                <td>{{ t.runId }}</td>
                <td>{{ t.epoch }}</td>
                <td>{{ t.acc }}</td>
                <td>{{ t.status }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </v-col>
    </v-row>

    <v-row dense class="mt-2">
      <v-col cols="12" md="7">
        <v-card class="chs-card" min-height="260">
          <v-card-title class="text-subtitle-1" style="color:#EBD6A2">Requests/sec</v-card-title>
          <v-divider class="chs-divider" />
          <div class="pa-4">
            <div style="height:200px; border:1px dashed rgba(203,163,92,.25); border-radius:8px;"></div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" md="5">
        <v-card class="chs-card" min-height="260">
          <v-card-title class="text-subtitle-1" style="color:#EBD6A2">Promo / Mini-Board</v-card-title>
          <v-divider class="chs-divider" />
          <div class="pa-4 d-flex align-center justify-center" style="height:200px;">
            <v-icon size="96" color="primary">mdi-crown</v-icon>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
const kpis = [
  { label: 'Games Imported', value: '1,248' },
  { label: 'Active Training', value: '1' },
  { label: 'Best Val-Acc', value: '0.41' },
  { label: 'Prod Model', value: 'v1.2' },
]
const trainings = [
  { runId:'20240403-1', epoch:10, acc:0.38, status:'running' },
  { runId:'20240402-2', epoch:15, acc:0.40, status:'completed' },
  { runId:'20240402-1', epoch:20, acc:0.37, status:'failed' }
]
</script>

