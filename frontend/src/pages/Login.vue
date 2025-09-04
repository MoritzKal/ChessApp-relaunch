<template>
  <v-container class="d-flex align-center justify-center" style="min-height:70vh;">
    <v-card width="420" class="pa-6 chs-card">
      <v-card-title>Sign in</v-card-title>
      <v-text-field v-model="username" label="Username" />
      <v-text-field v-model="password" label="Password" type="password" />
      <v-switch v-model="remember" label="Remember me" />
      <v-btn class="mt-2" color="primary" block @click="login">Login</v-btn>
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

async function login() {
  try {
    // Try open /v1/auth/token (dev convenience)
    const base = (import.meta as any).env.VITE_API_BASE || '/api'
    const { data } = await axios.get(base + '/v1/auth/token', {
      params: { user: username.value || 'user1', roles: 'USER', scope: 'api.read', ttl: 3600 }
    })
    const token = (data as any)?.token
    if (token && token.split('.').length === 3) {
      auth.setToken(token)
      router.push('/');
      return
    }
  } catch {}
  // Fallback to env dev token if it looks like a JWT
  const devToken = (import.meta as any).env.VITE_DEV_STATIC_TOKEN
  if (devToken && String(devToken).split('.').length === 3) {
    auth.setToken(devToken)
    router.push('/')
    return
  }
  alert('Login failed. Please configure a valid dev token or auth endpoint.')
}
</script>
