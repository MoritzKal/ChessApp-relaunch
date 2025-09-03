import 'vuetify/styles'
import { createVuetify } from 'vuetify'

const colors = {
  primary: '#CBA35C',       // Brass
  secondary: '#8B5E3C',     // Walnut
  background: '#0B0E0C',    // Page bg (very dark, near-black-green)
  surface: '#0F1F1C',       // Card/nav base (deep green)
  'surface-variant': '#15322D',
  'on-surface': '#EDEDED',
  'on-surface-variant': '#CFCFCF',
  info: '#6BB3FF',
  success: '#5FBF77',
  warning: '#F2C14E',
  error: '#E57373',
  outline: '#2C3A36'
}

const variables = {
  'radius-sm': '10px',
  'radius-md': '14px',
  'radius-lg': '20px',
  'border-color': '#2C3A36',
  'overlay-multiplier': 1,
  'shadow-key-umbra-opacity': 0.22,
  'shadow-key-penumbra-opacity': 0.16,
  'shadow-key-ambient-opacity': 0.14
}

export default createVuetify({
  theme: {
    defaultTheme: 'chsDark',
    themes: {
      chsDark: { dark: true, colors, variables }
    }
  },
  defaults: {
    VCard: { rounded: 'lg', elevation: 2, class: 'chs-card' },
    VBtn: { rounded: 'lg', variant: 'tonal', color: 'primary' },
    VTextField: { variant: 'outlined', density: 'comfortable' },
    VSelect: { variant: 'outlined', density: 'comfortable' },
    VAutocomplete: { variant: 'outlined', density: 'comfortable' },
    VSwitch: { color: 'primary' },
    VTabs: { color: 'primary' },
    VDataTable: { density: 'comfortable' },
    VNavigationDrawer: { width: 280, class: 'chs-nav' },
    VAppBar: { flat: true, color: 'transparent' },
    VDivider: { class: 'chs-divider' },
    VSnackbar: { timeout: 3000 }
  }
})
