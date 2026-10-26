import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Button } from './Button'

describe('Button', () => {
  it('renders its children', () => {
    render(<Button>Sign up</Button>)

    expect(screen.getByRole('button', { name: 'Sign up' })).toBeInTheDocument()
  })

  it('calls onClick when clicked', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()

    render(<Button onClick={onClick}>Submit</Button>)

    await user.click(screen.getByRole('button', { name: 'Submit' }))

    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('shows loading state', () => {
    render(<Button loading>Submit</Button>)

    expect(screen.getByRole('button', { name: 'Please wait…' })).toBeDisabled()
  })
})
