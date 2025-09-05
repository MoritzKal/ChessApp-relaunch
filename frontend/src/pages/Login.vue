<template>
  <v-container class="d-flex align-center justify-center" style="min-height:70vh;">
    <v-card width="420" class="pa-6 chs-card">
      <v-card-title>Anmelden</v-card-title>
      <v-form @submit.prevent="login">
        <v-text-field v-model="username" label="Username" autofocus @keyup.enter="login" />
        <v-text-field v-model="password" label="Password" type="password" @keyup.enter="login" />
        <v-switch v-model="remember" label="Remember me" />
        <v-alert v-if="error" type="error" density="comfortable" class="mb-2">{{ error }}</v-alert>
        <v-btn class="mt-2" color="primary" block type="submit" :loading="loading">Anmelden</v-btn>
      </v-form>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'

const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const remember = ref(true)
const loading = ref(false)
const error = ref<string | null>(null)

async function login() {
  error.value = null
  loading.value = true
  try {
    const base = (import.meta as any).env.VITE_API_BASE || '/api'
    const { data } = await axios.post(base + '/auth/login', { username: username.value, password: password.value })
    const token = (data as any)?.token
    if (token && token.split('.').length === 3) {
      auth.setToken(token)
      const ok = await auth.ensureValid()
      if (ok) { router.push('/') } else { throw new Error('Ungültiger Token') }
      return
    }
    throw new Error('Unerwartete Antwort vom Server')
  } catch (e: any) {
    error.value = e?.message || 'Login fehlgeschlagen'
  } finally { loading.value = false }
}
</script>
