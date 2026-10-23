import { describe, expect, it } from 'vitest'
import { isBrowseActive, parseBrowseParams, toSearchParams } from './browseParams'

const parse = (query: string) => parseBrowseParams(new URLSearchParams(query))

describe('browseParams', () => {
  it('parses a full URL', () => {
    expect(parse('q=desk&category=furniture&minPrice=10&maxPrice=50&sort=price_asc')).toEqual({
      q: 'desk', category: 'FURNITURE', minPrice: 10, maxPrice: 50, sort: 'price_asc', page: 1,
    })
  })

  it('defaults when empty', () => {
    expect(parse('')).toEqual({ q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest', page: 1 })
  })

  it('ignores invalid values', () => {
    expect(parse('category=banana&minPrice=abc&maxPrice=-5&sort=xyz')).toEqual({
      q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest', page: 1,
    })
  })

  it('drops both prices when min exceeds max', () => {
    expect(parse('minPrice=50&maxPrice=10')).toEqual({
      q: '', category: null, minPrice: null, maxPrice: null, sort: 'newest', page: 1,
    })
  })

  it('reports each browse value as active', () => {
    expect(isBrowseActive(parse(''))).toBe(false)
    for (const query of ['q=desk', 'category=decor', 'minPrice=0', 'maxPrice=50', 'sort=price_desc']) {
      expect(isBrowseActive(parse(query))).toBe(true)
    }
  })

  it('maps every sort to API params', () => {
    expect(toSearchParams(parse(''))).toEqual({ sortBy: 'createdAt', sortOrder: 'desc', page: 1, limit: 24 })
    expect(toSearchParams(parse('sort=price_asc'))).toEqual({ sortBy: 'price', sortOrder: 'asc', page: 1, limit: 24 })
    expect(toSearchParams(parse('sort=price_desc'))).toEqual({ sortBy: 'price', sortOrder: 'desc', page: 1, limit: 24 })
    expect(toSearchParams(parse('q=desk&category=furniture&minPrice=10&maxPrice=50&sort=price_asc'))).toEqual({
      q: 'desk', category: 'FURNITURE', minPrice: 10, maxPrice: 50,
      sortBy: 'price', sortOrder: 'asc', page: 1, limit: 24,
    })
  })

  it('parses the page for the search API', () => {
    expect(parse('page=3').page).toBe(3)
    expect(toSearchParams(parse('page=3'))).toEqual({ sortBy: 'createdAt', sortOrder: 'desc', page: 3, limit: 24 })
  })

  it.each(['page=0', 'page=-2', 'page=abc', 'page=1.5'])('treats invalid %s as page 1', (query) => {
    expect(parse(query).page).toBe(1)
  })

  it('does not count the page as a browse filter', () => {
    expect(isBrowseActive(parse('page=4'))).toBe(false)
  })
})
