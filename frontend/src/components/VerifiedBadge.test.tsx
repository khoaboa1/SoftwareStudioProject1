import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { VerifiedBadge } from './VerifiedBadge'

describe('VerifiedBadge', () => {
  it('shows the Verified Student text exactly once', () => {
    render(<VerifiedBadge />)

    expect(screen.getAllByText('Verified Student')).toHaveLength(1)
  })

  it('hides the decorative icon from assistive technology', () => {
    const { container } = render(<VerifiedBadge />)
    const icon = container.querySelector('svg')

    expect(icon).not.toBeNull()
    expect(icon).toHaveAttribute('aria-hidden', 'true')
  })
})
