import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MessageComposer } from './MessageComposer'

// Contract: <MessageComposer onSend={(content) => void} sending={boolean} />
// A textarea labelled "Message" and a "Send" button. Max 1000 characters.

function setup(sending = false) {
  const onSend = vi.fn()
  render(<MessageComposer onSend={onSend} sending={sending} />)
  return {
    onSend,
    user: userEvent.setup(),
    box: screen.getByRole('textbox', { name: 'Message' }),
    send: screen.getByRole('button', { name: 'Send' }),
  }
}

describe('MessageComposer', () => {
  it('sends trimmed text on Enter and clears the box', async () => {
    const { onSend, user, box } = setup()
    await user.type(box, '  Is it still available?  {Enter}')

    expect(onSend).toHaveBeenCalledWith('Is it still available?')
    expect(box).toHaveValue('')
  })

  it('sends with the Send button', async () => {
    const { onSend, user, box, send } = setup()
    await user.type(box, 'Hello')
    await user.click(send)

    expect(onSend).toHaveBeenCalledWith('Hello')
  })

  it('adds a new line on Shift+Enter without sending', async () => {
    const { onSend, user, box } = setup()
    await user.type(box, 'Line one{Shift>}{Enter}{/Shift}Line two')

    expect(onSend).not.toHaveBeenCalled()
    expect(box).toHaveValue('Line one\nLine two')
  })

  it('disables Send while the box is empty or only spaces', async () => {
    const { onSend, user, box, send } = setup()
    expect(send).toBeDisabled()

    await user.type(box, '   {Enter}')
    expect(send).toBeDisabled()
    expect(onSend).not.toHaveBeenCalled()
  })

  it('disables Send while a message is being sent', async () => {
    const { onSend, user, box, send } = setup(true)
    await user.type(box, 'Hello{Enter}')

    expect(send).toBeDisabled()
    expect(onSend).not.toHaveBeenCalled()
  })

  it('blocks messages over 1000 characters', async () => {
    const { onSend, user, box, send } = setup()
    await user.click(box)
    await user.paste('a'.repeat(1001))

    expect(send).toBeDisabled()
    expect(screen.getByText(/at most 1000 characters/i)).toBeInTheDocument()
    await user.keyboard('{Enter}')
    expect(onSend).not.toHaveBeenCalled()
  })

  it('allows exactly 1000 characters', async () => {
    const { onSend, user, box, send } = setup()
    await user.click(box)
    await user.paste('a'.repeat(1000))

    expect(send).toBeEnabled()
    await user.click(send)
    expect(onSend).toHaveBeenCalledWith('a'.repeat(1000))
  })
})
