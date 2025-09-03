import { describe, it, expect } from 'vitest'
describe('planned endpoints flag', ()=>{
  it('is disabled by default', ()=>{
    expect(import.meta.env.VITE_ENABLE_CONFIG_ENDPOINTS).not.toBe('true')
  })
})
