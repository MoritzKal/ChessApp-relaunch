<template>
  <DashboardGrid>
    <div class="is_large">
      <TableTile title="User Management" icon="mdi-account-cog" :vm="vm" :loading="loading">
        <template #tools>
          <v-chip color="info" size="small">Admin only</v-chip>
        </template>
        <template #item-actions="{ item }">
          <div class="d-flex ga-2">
            <v-btn size="x-small" variant="text" @click="toggleAdmin(item)" :title="item.roles.includes('ADMIN') ? 'Remove admin' : 'Make admin'">
              <v-icon>{{ item.roles.includes('ADMIN') ? 'mdi-shield-off' : 'mdi-shield-check' }}</v-icon>
            </v-btn>
          </div>
        </template>
      </TableTile>
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import { listUsers, updateUserRoles, type AdminUser } from '@/services/adminUsers'

const loading = ref(false)
const items = ref<AdminUser[]>([])

const vm = computed(() => ({
  columns: [
    { key: 'username', label: 'Username' },
    { key: 'roles', label: 'Roles', formatter: (r:any) => (r||[]).join(', ') },
    { key: 'createdAt', label: 'Created' },
    { key: 'actions', label: 'Actions', align: 'end' },
  ],
  rows: items.value
}))

async function load(){ loading.value = true; try { items.value = await listUsers() } finally { loading.value = false } }
async function toggleAdmin(user: AdminUser){
  const has = user.roles.includes('ADMIN')
  const roles = has ? user.roles.filter(r => r !== 'ADMIN') : [...user.roles, 'ADMIN']
  const u = await updateUserRoles(user.id, roles)
  const idx = items.value.findIndex(i => i.id === u.id)
  if (idx >= 0) items.value[idx] = u
}

onMounted(load)
</script>
