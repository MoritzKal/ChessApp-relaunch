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
          primary: '#D4AF37',   // Gold
          secondary: '#C6B07A', // Blassgold
          onSurface: '#F6F1D1'
        }
      }
    }
  }
})

