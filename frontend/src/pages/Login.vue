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
import api from '@/plugins/axios'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const remember = ref(true)

async function login() {
  const devToken = import.meta.env.VITE_DEV_STATIC_TOKEN
  try {
    if (devToken) auth.setToken(devToken)
    else {
      const { data } = await api.post('/v1/auth/login', {
        username: username.value,
        password: password.value
      })
      auth.setToken(data.accessToken)
    }
    router.push('/')
  } catch (e) {
    /* Snackbar via interceptor */
  }
}
</script>

