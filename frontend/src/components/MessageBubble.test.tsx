import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MessageBubble } from './MessageBubble'

// Contract: <MessageBubble content createdAt mine status? onRetry? />
// Renders an <li data-mine="true|false">. status: 'sent' (default) | 'sending' | 'failed'.

const base = { content: 'Is it still available?', createdAt: '2026-11-13T10:00:00Z' }

function renderBubble(props: Partial<React.ComponentProps<typeof MessageBubble>> = {}) {
  return render(<ul><MessageBubble {...base} mine={false} {...props} /></ul>)
}

describe('MessageBubble', () => {
  it('shows the text and a machine-readable time', () => {
    renderBubble()

    expect(screen.getByText('Is it still available?')).toBeInTheDocument()
    expect(screen.getByRole('listitem').querySelector('time')).toHaveAttribute('dateTime', base.createdAt)
  })

  it('marks whose message it is', () => {
    const { rerender } = renderBubble({ mine: true })
    expect(screen.getByRole('listitem')).toHaveAttribute('data-mine', 'true')

    rerender(<ul><MessageBubble {...base} mine={false} /></ul>)
    expect(screen.getByRole('listitem')).toHaveAttribute('data-mine', 'false')
  })

  it('shows a sending message as busy', () => {
    renderBubble({ mine: true, status: 'sending' })
    expect(screen.getByRole('listitem')).toHaveAttribute('aria-busy', 'true')
  })

  it('shows a failed message with a Retry button', async () => {
    const onRetry = vi.fn()
    renderBubble({ mine: true, status: 'failed', onRetry })

    expect(screen.getByText(/Not sent/)).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Retry' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  it('shows no status for a sent message', () => {
    renderBubble({ mine: true })

    expect(screen.queryByText(/Not sent/)).not.toBeInTheDocument()
    expect(screen.getByRole('listitem')).not.toHaveAttribute('aria-busy', 'true')
  })
})
