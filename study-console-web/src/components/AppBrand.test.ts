import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AppBrand from './AppBrand.vue'

describe('AppBrand', () => {
  it('shows the complete product identity by default', () => {
    const wrapper = mount(AppBrand)

    expect(wrapper.text()).toContain('ContentGuard')
    expect(wrapper.text()).toContain('Risk Console')
  })

  it('hides product text in compact mode', () => {
    const wrapper = mount(AppBrand, { props: { compact: true } })

    expect(wrapper.text()).not.toContain('ContentGuard')
    expect(wrapper.find('svg').exists()).toBe(true)
  })
})
