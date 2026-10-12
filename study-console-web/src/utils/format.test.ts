import { describe, expect, it } from 'vitest'

import { compactId, formatDateTime } from './format'

describe('format utilities', () => {
  it('compacts long identifiers while retaining both ends', () => {
    expect(compactId('dt_1234567890abcdefghijklmnopqrstuvwxyz')).toBe('dt_123456789…tuvwxyz')
  })

  it('keeps short identifiers unchanged', () => {
    expect(compactId('dt_short')).toBe('dt_short')
  })

  it('returns a fallback for missing date values', () => {
    expect(formatDateTime()).toBe('-')
  })
})
