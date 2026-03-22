import { describe, expect, it } from 'vitest'
import { formatDateTimeEs } from './dateFormat'

describe('formatDateTimeEs', () => {
  it('returns dash for empty values', () => {
    expect(formatDateTimeEs(null)).toBe('-')
    expect(formatDateTimeEs(undefined)).toBe('-')
    expect(formatDateTimeEs('')).toBe('-')
  })

  it('returns dash for invalid date values', () => {
    expect(formatDateTimeEs('not-a-date')).toBe('-')
  })

  it('formats a valid date in Spanish with uppercase first letter', () => {
    const result = formatDateTimeEs('2026-03-22T21:00:00')
    expect(result).toContain(' de ')
    expect(result).toContain(' a las ')
    expect(result.charAt(0)).toBe(result.charAt(0).toUpperCase())
  })
})
