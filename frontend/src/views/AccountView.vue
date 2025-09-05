<template>
  <DashboardGrid>
    <!-- Row 1: KPIs -->
    <div class="is_small"><InfoMetricTile title="Datasets" icon="mdi-database" :endpoint="dsCountEp" value-key="count" /></div>
    <div class="is_small"><InfoMetricTile title="Trainings active" icon="mdi-robot" :endpoint="trCountEp" value-key="count" /></div>
    <div class="is_small"><InfoMetricTile title="Models active" icon="mdi-cube" :endpoint="mdCountEp" value-key="count" /></div>
    <div class="is_small"><InfoMetricTile title="Spiele insgesamt" icon="mdi-chess-queen" :value="gamesTotal" /></div>

    <!-- Row 2: Profile + Tokens -->
    <div class="is_large">
      <TableTile title="Profil" icon="mdi-account" :vm="profileVm" :loading="loading.profile" />
    </div>
    <div class="is_large">
      <TableTile title="API-Tokens" icon="mdi-key" :vm="tokensVm" :loading="loading.tokens">
        <template #tools>
          <v-btn size="small" prepend-icon="mdi-plus" @click="onCreateToken">Token generieren</v-btn>
        </template>
        <template #item-actions="{ item }">
          <v-btn size="x-small" color="error" variant="text" @click="onRevoke(item.id)" title="Revoke">Revoke</v-btn>
        </template>
        <template #cta>
          <div v-if="!tokens.length" style="opacity:.8">Keine Tokens vorhanden</div>
        </template>
      </TableTile>
    </div>

    <!-- Row 3: Prefs -->
    <div class="is_large">
      <TableTile title="Einstellungen (Prefs)" icon="mdi-cog" :vm="prefsVm">
        <template #tools><v-btn size="small" color="primary" @click="savePrefs" prepend-icon="mdi-content-save">Speichern</v-btn></template>
        <template #cta>
          <div class="prefs">
            <v-text-field v-model="prefs.theme" label="Theme" density="compact" />
            <v-text-field v-model="prefs.locale" label="Locale" density="compact" />
          </div>
        </template>
      </TableTile>
    </div>
  </DashboardGrid>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import DashboardGrid from '@/layouts/DashboardGrid.vue'
import InfoMetricTile from '@/components/panels/InfoMetricTile.vue'
import TableTile from '@/components/renderers/TableTile.vue'
import { Endpoints as ep } from '@/lib/endpoints'
import { recentGames } from '@/services/games'
import { getMe, listTokens, createToken, revokeToken, getPrefs as getUserPrefs, putPrefs as putUserPrefs } from '@/services/users'

const dsCountEp = ep.datasets.count()
const trCountEp = ep.training.count({ status: 'active' })
const mdCountEp = ep.models.count({ status: 'active' })

const gamesTotal = ref<number | string>('—')
onMounted(async () => {
  try { const list = await recentGames(1); gamesTotal.value = Array.isArray(list) ? list.length : 0 } catch { gamesTotal.value = 0 }
})

const loading = reactive({ profile: false, tokens: false })
const me = ref<any>(null)
const tokens = ref<any[]>([])
const prefs = reactive<any>(loadPrefs())

function loadPrefs(){ try { const raw = localStorage.getItem('chs_user_prefs_v1'); return raw ? JSON.parse(raw) : { theme: 'dark', locale: 'de' } } catch { return { theme: 'dark', locale: 'de' } } }
function savePrefsLocal(){ localStorage.setItem('chs_user_prefs_v1', JSON.stringify(prefs)) }

onMounted(async () => {
  try { loading.profile = true; me.value = await getMe() } catch { /* ignore */ } finally { loading.profile = false }
  try { const up = await getUserPrefs(); Object.assign(prefs, up) } catch { /* fallback to LS */ }
  try { loading.tokens = true; tokens.value = await listTokens() } catch { tokens.value = [] } finally { loading.tokens = false }
})

const profileVm = computed(() => ({
  columns: [ { key: 'key', label: 'Key' }, { key: 'val', label: 'Value' } ],
  rows: me.value ? [
    { key: 'username', val: me.value.username },
    { key: 'createdAt', val: me.value.createdAt || '—' },
    { key: 'roles', val: (me.value.roles || []).join(', ') },
  ] : []
}))

const tokensVm = computed(() => ({
  columns: [
    { key: 'id', label: 'ID' },
    { key: 'createdAt', label: 'Created' },
    { key: 'lastUsedAt', label: 'Last Used' },
    { key: 'actions', label: 'Actions', align: 'end' },
  ],
  rows: tokens.value,
}))

const prefsVm = computed(() => ({ columns: [], rows: [] }))

async function onCreateToken(){ try { const t = await createToken(); tokens.value = [t, ...tokens.value] } catch { /* ignore */ } }
async function onRevoke(id: string){ try { await revokeToken(id); tokens.value = tokens.value.filter(t => t.id !== id) } catch { /* ignore */ } }
async function savePrefs(){ savePrefsLocal(); try { await putUserPrefs(prefs) } catch { /* offline fallback only */ } }
</script>

<style scoped>
.prefs{ display:grid; grid-template-columns: 1fr 1fr; gap:12px }
@media (max-width: 900px){ .prefs{ grid-template-columns: 1fr } }
</style>

