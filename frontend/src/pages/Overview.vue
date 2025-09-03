<template>
  <div class="chs-grid">
    <!-- KPI Row -->
    <div v-for="k in kpis" :key="k.label" class="chs-col-span-3 sm:chs-col-span-1">
      <v-card class="chs-card ar-2x1">
        <div class="chs-kpi">
          <div class="label">{{ k.label }}</div>
          <div class="value">{{ k.value }}</div>
        </div>
      </v-card>
    </div>

    <!-- Training Loss -->
    <div class="chs-col-span-7 md:chs-col-span-6 sm:chs-col-span-1">
      <v-card class="chs-card ar-16x9">
        <v-card-title class="text-subtitle-1" style="color:#CBA35C">Training Loss</v-card-title>
        <v-divider class="chs-divider" />
        <div class="pa-4" style="height:100%;">
          <div style="height:100%; opacity:.9; border:1px dashed rgba(203,163,92,.25); border-radius:8px;"></div>
        </div>
      </v-card>
    </div>

    <!-- Recent Trainings -->
    <div class="chs-col-span-5 md:chs-col-span-6 sm:chs-col-span-1">
      <v-card class="chs-card ar-4x3">
        <v-card-title class="text-subtitle-1" style="color:#CBA35C">Recent Trainings</v-card-title>
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
    </div>

    <!-- Requests/sec -->
    <div class="chs-col-span-7 md:chs-col-span-6 sm:chs-col-span-1">
      <v-card class="chs-card ar-16x9">
        <v-card-title class="text-subtitle-1" style="color:#CBA35C">Requests/sec</v-card-title>
        <v-divider class="chs-divider" />
        <div class="pa-4" style="height:100%;">
          <div style="height:100%; border:1px dashed rgba(203,163,92,.25); border-radius:8px;"></div>
        </div>
      </v-card>
    </div>

    <!-- Promo / Mini-Board -->
    <div class="chs-col-span-5 md:chs-col-span-6 sm:chs-col-span-1">
      <v-card class="chs-card ar-1x1 d-flex align-center justify-center">
        <v-icon size="96" color="primary">mdi-crown</v-icon>
      </v-card>
    </div>
  </div>
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

