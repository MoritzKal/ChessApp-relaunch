import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HelpView from '@/views/HelpView.vue'

describe('HelpView', () => {
  it('renders help sections', () => {
    const wrapper = mount(HelpView)
    expect(wrapper.text()).toContain('Hilfe & Beispiele')
    expect(wrapper.text()).toContain('Mein erstes Dataset')
  })
})

