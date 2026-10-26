# Refactoring Log

Entries for User Story 1 — Refactor Toward Clean Code. Each entry records the smell, the refactoring mechanic, before/after code, and the commit that applied it.

---

## SSP1-95 — Extract duplicated PIN verification step from Login and Signup pages

**Commit:** [`4d7191a`](https://github.com/khoaboa1/SoftwareStudioProject1/commit/4d7191a2c8da78f208393d88babd21b502f28ebf)

### Code smell

**Duplicated Code**, which leads to **Shotgun Surgery**.

`LoginPage.tsx` and `SignupPage.tsx` each had a near-identical copy of the 6-digit PIN verification step, about 100 lines per file. The copied parts were:
- the `pin` and `resendStatus` state
- the 6-digit validation
- `handleResendPin` and its 4-second reset timer
- the digit-stripping PIN field
- the "Didn't receive it? / Resend PIN" row

| Duplicated piece | `LoginPage.tsx` (before) | `SignupPage.tsx` (before) |
|---|---|---|
| PIN validation + verify submit | lines 64–87 | lines 64–89 |
| `handleResendPin` | lines 89–102 | lines 91–104 |
| PIN form JSX (alerts, field, button, resend row) | lines 127–163 | lines 147–183 |

Changing the PIN length, the resend behavior or any of the copy meant editing both files and keeping them in sync by hand. The copies had already started to drift:
- The helper text and error fallbacks were worded differently.
- Login verified through `authFacade`, while Signup called `verifyPin` directly.
- `SignupPage` had no tests, so its copy was unprotected.

### Mechanic

**Extract Component.** The shared step now lives in `frontend/src/components/PinVerificationStep.tsx`. It owns all PIN, resend, loading and error state. Each page passes in only what really differs:
- the copy: `title`, `subtitle`, `helperText`, `submitLabel` and `verifyErrorFallback`
- its `footer`
- an `onVerify(pin)` callback for what happens on success. Login signs in through `authFacade` and navigates to `/feed`. Signup verifies the device and shows the completion card.

### Before

This block appeared in **both** pages. The Signup copy is shown here; the Login copy differs only in the strings and in what happens after `verifyPin`.

```tsx
const [pin, setPin] = useState('')
const [resendStatus, setResendStatus] = useState<'idle' | 'sending' | 'sent'>('idle')

async function handleVerifyPin(event: FormEvent<HTMLFormElement>) {
  event.preventDefault()

  if (!pin.trim() || pin.trim().length !== 6) {
    setErrors({ pin: 'Enter the 6-digit PIN' })
    return
  }

  setStatus('submitting')
  setServerError(null)

  try {
    await verifyPin({ email, pin: pin.trim(), deviceId: getOrCreateDeviceId() })
    setStatus('idle')
    setStep('complete')
  } catch (error) {
    setServerError(
      error instanceof ApiError ? error.message : 'Failed to verify PIN. Please try again.',
    )
    setStatus('error')
  }
}

async function handleResendPin() {
  setResendStatus('sending')
  setServerError(null)
  try {
    await resendPin({ email })
    setResendStatus('sent')
    setTimeout(() => setResendStatus('idle'), 4000)
  } catch (error) {
    setServerError(error instanceof ApiError ? error.message : 'Failed to resend PIN.')
    setResendStatus('idle')
  }
}

// ...and ~40 lines of identical JSX: error/success alerts, the PIN TextField
// with digit stripping, the submit Button, and the "Resend PIN" row.
```

### After

Each page now renders the step with only its own differences (`SignupPage.tsx`):

```tsx
if (step === 'verify_pin') {
  return (
    <PinVerificationStep
      email={email}
      title="Verify your school email"
      subtitle={`We generated a 6-digit PIN for ${email}`}
      helperText="Check your university email (or the backend console during dev/demo)"
      submitLabel="Verify PIN & Activate Account"
      verifyErrorFallback="Failed to verify PIN. Please try again."
      onVerify={async (pin) => {
        await verifyPin({ email, pin, deviceId: getOrCreateDeviceId() })
        setStatus('idle')
        setStep('complete')
      }}
      footer={/* "Entered the wrong email? Start over" */}
    />
  )
}
```

The validation, resend and form markup exist once, in `PinVerificationStep`:

```tsx
async function handleVerifyPin(event: FormEvent<HTMLFormElement>) {
  event.preventDefault()

  if (!pin.trim() || pin.trim().length !== 6) {
    setPinError('Enter the 6-digit PIN')
    return
  }

  setStatus('submitting')
  setServerError(null)

  try {
    await onVerify(pin.trim())
  } catch (error) {
    setServerError(error instanceof ApiError ? error.message : verifyErrorFallback)
    setStatus('error')
  }
}
```

**Result:**
- `LoginPage.tsx` went from 214 to 142 lines and `SignupPage.tsx` from 255 to 182.
- The PIN logic went from 2 copies to 1.
- Every string a user sees is unchanged.

### Behavior preserved: how it was verified

1. **Characterization tests first.**
   - New `SignupPage.test.tsx` (9 tests): PIN step entry, short-PIN validation, digit stripping, verify success, API and fallback errors, resend success and error, Start over.
   - 4 new `LoginPage.test.tsx` tests: short-PIN validation, resend, fallback error, Back to login clears the password.
   - All 17 page tests passed against the **original** pages and pass unchanged against the refactored pages.
2. **Boundary tests.** `PinVerificationStep.test.tsx` (6 tests) covers:
   - rendering the copy it is given
   - validation
   - passing a trimmed PIN, with a loading state while pending
   - ApiError and fallback messages
   - resend, with the `Sending...` state while pending
3. **Full suite.**
   - Tests: 174/174 pass, up from 155.
   - `oxlint`: unchanged, one existing warning.
   - `tsc -b`: clean.
4. **In the browser, against the real Spring Boot backend and Postgres.** PINs were read from the backend console log. These flows were checked:
   - signup → PIN validation → resend (new PIN issued) → wrong PIN ("Incorrect verification PIN") → correct PIN → "Account Verified!"
   - login with an unverified account → PIN step showing the backend's prompt → Back to login (password cleared) → log in again → verify → `/feed`, which redirects to profile setup because the account has no profile

**One accepted difference:** the step is now its own component, so it starts fresh each time it is shown. If you leave the PIN step ("Back to login" / "Start over") and return, the PIN field is empty and no earlier resend or error message remains. Before, the old PIN was still in the field. No existing test or requirement relied on the old behavior.

**Left as-is on purpose, to keep this a pure refactor:**
- the resend timer is still not cleared when the step unmounts
- Signup still calls `verifyPin` directly rather than going through `authFacade`

Both are now one-place fixes for a follow-up.
