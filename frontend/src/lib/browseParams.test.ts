import { describe, expect, it } from 'vitest'
import { isBrowseActive, parseBrowseParams, toSearchParams } from './browseParams'

const parse = (query: string) => parseBrowseParams(new URLSearchParams(query))

describe('browseParams', () => {
  it('parses a full URL', () => {
    expect(parse('q=desk&category=furniture&minPrice=10&maxPrice=50&sort=price_asc')).toEqual({
      q: 'desk', category: 'FURNITURE', minPrice: 10, maxPrice: 50, sort: 'price_asc',
    })
  })

  it('defaults when empty', () => {
    expect(parse('')).toEqual({ q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest' })
  })

  it('ignores invalid values', () => {
    expect(parse('category=banana&minPrice=abc&maxPrice=-5&sort=xyz')).toEqual({
      q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest',
    })
  })

  it('drops both prices when min exceeds max', () => {
    expect(parse('minPrice=50&maxPrice=10')).toEqual({
      q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest',
    })
  })

  it('reports each browse value as active', () => {
    expect(isBrowseActive(parse(''))).toBe(false)
    for (const query of ['q=desk', 'category=decor', 'minPrice=0', 'maxPrice=50', 'sort=price_desc']) {
      expect(isBrowseActive(parse(query))).toBe(true)
    }
  })

  it('maps every sort to API params', () => {
    expect(toSearchParams(parse(''))).toEqual({ sortBy: 'createdAt', sortOrder: 'desc', limit: 100 })
    expect(toSearchParams(parse('sort=price_asc'))).toEqual({ sortBy: 'price', sortOrder: 'asc', limit: 100 })
    expect(toSearchParams(parse('sort=price_desc'))).toEqual({ sortBy: 'price', sortOrder: 'desc', limit: 100 })
    expect(toSearchParams(parse('q=desk&category=furniture&minPrice=10&maxPrice=50&sort=price_asc'))).toEqual({
      q: 'desk', category: 'FURNITURE', minPrice: 10, maxPrice: 50,
      sortBy: 'price', sortOrder: 'asc', limit: 100,
    })
  })
})
