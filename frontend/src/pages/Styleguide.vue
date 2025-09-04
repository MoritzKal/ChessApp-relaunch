<template>
  <v-container class="d-flex flex-column ga-6">
    <v-card>
      <v-card-title>Theme Tokens</v-card-title>
      <v-card-text>
        <div class="d-flex flex-wrap ga-4">
          <TokenSwatch name="primary" hex="#CBA35C"/>
          <TokenSwatch name="secondary" hex="#8B5E3C"/>
          <TokenSwatch name="surface" hex="#0F1F1C"/>
          <TokenSwatch name="surface-variant" hex="#15322D"/>
          <TokenSwatch name="on-surface" hex="#EDEDED" text="#1A1A1A"/>
          <TokenSwatch name="outline" hex="#2C3A36"/>
          <TokenSwatch name="info" hex="#6BB3FF"/>
          <TokenSwatch name="success" hex="#5FBF77"/>
          <TokenSwatch name="warning" hex="#F2C14E"/>
          <TokenSwatch name="error" hex="#E57373"/>
        </div>
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-title>Buttons</v-card-title>
      <v-card-text class="d-flex flex-wrap ga-4">
        <v-btn>Primary</v-btn>
        <v-btn variant="flat">Flat</v-btn>
        <v-btn variant="outlined">Outlined</v-btn>
        <v-btn color="secondary">Secondary</v-btn>
        <v-btn color="warning">Warning</v-btn>
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-title>Inputs</v-card-title>
      <v-card-text class="d-flex flex-wrap ga-4">
        <v-text-field label="Text Field" />
        <v-select :items="['A','B','C']" label="Select"/>
        <v-autocomplete :items="['Alpha','Beta','Gamma']" label="Autocomplete"/>
        <v-switch label="Switch" />
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-title>Tabs</v-card-title>
      <v-card-text>
        <v-tabs v-model="tab">
          <v-tab value="one">One</v-tab>
          <v-tab value="two">Two</v-tab>
        </v-tabs>
        <v-window v-model="tab" class="mt-4">
          <v-window-item value="one"><div class="chs-section">Tab One Content</div></v-window-item>
          <v-window-item value="two"><div class="chs-section">Tab Two Content</div></v-window-item>
        </v-window>
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-title>Table</v-card-title>
      <v-card-text>
        <v-data-table :items="rows" :headers="headers" items-per-page="5" />
      </v-card-text>
    </v-card>

    <v-card>
      <v-card-title>Snackbar</v-card-title>
      <v-card-text>
        <v-btn @click="snack=true">Show Snackbar</v-btn>
        <v-snackbar v-model="snack" color="success">It works 🎉</v-snackbar>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'


const tab = ref('one')
const snack = ref(false)
const headers = [
  { title:'Name', key:'name' }, { title:'Value', key:'value' }
]
const rows = [
  { name:'Example A', value:42 },
  { name:'Example B', value:1337 },
  { name:'Example C', value:7 }
]
</script>

<script lang="ts">
import { defineComponent } from 'vue'
export default defineComponent({
  name: 'Styleguide',
  components: {
    TokenSwatch: defineComponent({
      props: { name: String, hex: String, text: { type: String, default: '#EDEDED' } },
      template: `
        <div style="width:130px">
          <div :style="{ background: hex, color: text, borderRadius:'10px', padding:'12px', border:'1px solid rgba(203,163,92,0.25)' }">
            <div class="text-caption">{{ name }}</div>
            <div class="mono" style="font-size:12px">{{ hex }}</div>
          </div>
        </div>`
    })
  }
})
</script>
