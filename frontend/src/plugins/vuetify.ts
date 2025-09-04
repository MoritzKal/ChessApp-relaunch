import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

export default createVuetify({
  theme: {
    defaultTheme: 'chess',
    themes: {
      chess: {
        dark: true,
        colors: {
          background: '#0E2A24',
          surface: '#13362E',
          primary: '#F0D26B',   // brighter gold
          secondary: '#F7E1A1', // soft highlight
          onSurface: '#F6F1D1'
        }
      }
    }
  }
})
