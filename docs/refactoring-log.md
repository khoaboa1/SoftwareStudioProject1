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

---

## SSP1-98 — Extract shared university email validation policy

- **Date:** 2026-10-26
- **Code smell:** `AuthService` and `VerificationService` each owned the same university email regex. Four validation checks repeated its null handling and matching logic.
- **Mechanic:** Extract Class/Policy. Move the regex and null check into a stateless Spring `UniversityEmailValidator` component, constructor-inject it into both services, and delegate all four checks to `isValid(String email)`.
- **Implementation commit:** [5cf1836 — extract shared university email validation policy](https://github.com/khoaboa1/SoftwareStudioProject1/commit/5cf183670b55e98124f9b26b9a721c81ab78440c)

### Before

Both services declared this policy independently:

```java
private static final Pattern EDU_EMAIL_PATTERN =
    Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);
```

For example, signup checked the pattern directly:

```java
if (email == null || !EDU_EMAIL_PATTERN.matcher(email).matches()) {
  throw new InvalidSignupException("Use a valid .edu school email");
}
```

### After

One component owns the rule:

```java
@Component
public class UniversityEmailValidator {
  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  public boolean isValid(String email) {
    return email != null && EDU_EMAIL_PATTERN.matcher(email).matches();
  }
}
```

Services delegate validation while retaining their existing exceptions and messages:

```java
if (!universityEmailValidator.isValid(email)) {
  throw new InvalidSignupException("Use a valid .edu school email");
}
```

### Compatibility

- Signup, PIN verification, PIN resend, and direct PIN generation use the shared policy. Each retains its original exception type, message, and validation order.
- The exact case-insensitive regex is preserved. Input is neither trimmed nor normalized; uppercase domains, subdomains, plus addressing, and previously accepted permissive forms retain their behavior.
- Repository lookups, PIN expiry, email-sender fallback, and sessions retain their existing behavior. HTTP endpoints, response shapes/statuses, and the database schema are unchanged; no frontend integration changes are needed.
- All existing test files and test cases are retained. Only the four constructor calls in `VerificationServicePinDispatchTest` were updated to pass the validator as the third argument.

### Verification

Executed under Temurin JDK 21.0.13:

- **New coverage:** 170 test invocations across `UniversityEmailValidatorTest`, `AuthServiceEmailValidationTest`, `VerificationServiceEmailValidationTest`, and `AuthEmailValidationControllerTest`. Shared cases cover accepted and rejected forms through the policy and every flow. Tests also check exact errors, validation order, rejection before repository/sender interactions, PIN state, success responses, and session creation.
- **Focused run:** 187 tests passed, including the existing authentication, verification, and sender-dispatch tests.
- **Full backend suite:** 315 tests across 24 classes passed; zero failures, errors, or skips.
- **Whitespace check:** `git diff --check` passed.

From `backend/`, with `JAVA_HOME` pointing to JDK 21:

```bash
./mvnw -q -Dtest=UniversityEmailValidatorTest,AuthServiceEmailValidationTest,VerificationServiceEmailValidationTest,AuthEmailValidationControllerTest,VerificationServicePinDispatchTest,AuthControllerTest,VerificationServiceTest test
./mvnw -q test
```

## SSP1-96 — Consolidate repeated async request state and error mapping

**Commits:**
- [`5dfdf0e`](https://github.com/khoaboa1/SoftwareStudioProject1/commit/5dfdf0e7e55b5b0852b7b4be65c9dce31a016708) migrates the four views onto the hook. This is the main refactoring commit.
- [`d248121`](https://github.com/khoaboa1/SoftwareStudioProject1/commit/d2481218d668030b499fbbe8c8e06c3bb23b40b8) adds the `useAsyncResource` hook.
- [`24630d1`](https://github.com/khoaboa1/SoftwareStudioProject1/commit/24630d1ebe8a4049830b17a4a0ecad4d97b9cc05) extracts `getErrorMessage` / `hasApiStatus`.

### Code smell

**Duplicated Code.** Four views that load data on mount each hand-rolled the same request lifecycle:
- a local `'loading' | 'error' | 'ready'` state union
- `setState` calls for each transition
- a `let cancelled = false` guard against responses arriving after unmount or after a newer request

| View (before, `develop` @ `0a8c11b`) | State union | Lifecycle effect |
|---|---|---|
| `components/MyListings.tsx` | lines 10–13 | lines 21–42 |
| `pages/ListingDetailPage.tsx` | lines 10–14 | lines 20–39 |
| `pages/ProfilePage.tsx` | lines 11–14 | lines 19, 42–60 |
| `pages/FeedPage.tsx` | lines 40–43 | lines 50–51, 75–94 |

The API-error-to-message check `error instanceof ApiError ? error.message : '<fallback>'` was copied **11 times across 8 files**:
- the four views above
- `LoginPage`, `SignupPage` and `ProfileSetupPage`
- `PinVerificationStep`

`ApiError` was also imported from three different modules.

### Mechanic

**Extract Function** came first: `frontend/src/lib/apiError.ts`.
- `getErrorMessage(error, fallback)` replaces all 11 copies, with every fallback string unchanged.
- `hasApiStatus(error, status)` replaces the 404/401 status checks in the pages and in `getMyProfileOrNull` / `getCurrentStudent`.

**Extract Hook** came next: `frontend/src/lib/useAsyncResource.ts`. It owns:
- the loading → ready/error transitions
- ignoring stale and post-unmount responses
- `reload()`
- `setData()` for local updates

Page-specific rules stay in the pages:
- **ListingDetailPage** passes `enabled: validId`, so an invalid id never calls the API. At render time it maps a 404 to "Listing not found".
- **ProfilePage** passes its existing `redirectOnAuthError` as `onError`. It returns `true` after navigating to `/login` (401) or `/profile-setup` (404), which leaves the state as loading.
- **FeedPage** passes an `onSuccess` that jumps to the last page when the requested page is out of range, again leaving the state as loading so the empty state never flashes. Retry and post-delete refresh call `reload()` instead of bumping a local `reloadToken`.
- **MyListings** keeps its seller filter inside `load`.

The hook stores the raw error, and each view picks its own fallback message at render time.

### Before

`MyListings.tsx`. The other three views repeated this shape, with their own state union and fallback:

```tsx
type ListingsState =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; listings: Listing[] }

const [state, setState] = useState<ListingsState>({ status: 'loading' })

useEffect(() => {
  let cancelled = false
  getListings()
    .then((listings) => {
      if (!cancelled) {
        setState({ status: 'ready', listings: listings.filter((listing) => listing.sellerId === sellerId) })
      }
    })
    .catch((error: unknown) => {
      if (!cancelled) {
        setState({
          status: 'error',
          error: error instanceof ApiError ? error.message : 'Could not load your listings.',
        })
      }
    })
  return () => {
    cancelled = true
  }
}, [sellerId])
```

### After

```tsx
const { state } = useAsyncResource(
  () => getListings().then((listings) => listings.filter((listing) => listing.sellerId === sellerId)),
  [sellerId],
)
// render: getErrorMessage(state.error, 'Could not load your listings.')
```

The shared hook signature is:

```ts
export function useAsyncResource<T>(
  load: () => Promise<T>,
  deps: DependencyList,
  options?: {
    enabled?: boolean
    onSuccess?: (data: T) => boolean | void   // true = handled (e.g. navigated); stay loading
    onError?: (error: unknown) => boolean | void
  },
): { state: AsyncState<T>; reload: () => void; setData: (data: T) => void }
```

The 4 views lost 141 lines and gained 56.

### Behavior preserved: how it was verified

1. **Existing tests unchanged.** No existing test file was edited; `git diff develop -- 'frontend/src/**/*.test.*'` lists only the two new test files. Every existing test passes against the refactored views, including:
   - the profile 401/404 redirects
   - the detail page's 404 and "invalid id without calling the API" cases
   - the feed's out-of-range jump, Retry, and reload after delete
   - MyListings' API and fallback errors
2. **New focused tests.**
   - `apiError.test.ts` (4 tests): API message, network-error fallback, non-Error fallback, status matching.
   - `useAsyncResource.test.tsx` (12 tests):
     - success
     - API failure
     - network failure
     - a response after unmount is ignored
     - a response from a superseded request is ignored
     - loading on dependency change
     - `reload`
     - disabled (`enabled: false`)
     - `onSuccess`/`onError` "handled" keeps loading
     - the latest `onError` closure is used
     - `setData`
3. **Full suite.**
   - Tests: 190/190 pass, up from 174.
   - `tsc -b`: clean.
   - `oxlint`: unchanged, one existing warning.
   - `npm run build`: succeeds.
4. **Browser run.** Headless Chromium drove the Vite dev server against the Spring Boot backend running on in-memory H2. All 15 checks passed, with no uncaught page errors:
   - Feed: renders; `?q=` filters; `?page=99` jumps back to the last page.
   - Feed with a failing search API: shows "We couldn't load listings" with Retry, and Retry loads the listings.
   - Listing detail: renders; a missing id (404) and `/listings/abc` both show "Listing not found", and `/listings/abc` makes no API call.
   - Profile: renders with "My listings (3)"; editing the major and saving shows the update.
   - Logged out: `/profile` redirects to `/login`.

**One accepted difference:** if the route id changes while `ListingDetailPage` stays mounted, the page now shows the loading skeleton until the new listing arrives. Before, it kept showing the previous listing. The feed already behaved this way, and no test or requirement relied on the old behavior.

**Out of scope:**
- `auth-context.tsx` keeps its own cancellation guard, because it bootstraps the session rather than loading page data.
- **Remaining smell, left for a follow-up ticket:** form *submit* state is still duplicated. The `'idle' | 'submitting' | 'error'` union and its `submitting → try → error` transitions appear in six places:
  - `LoginPage`
  - `SignupPage`
  - `ProfileSetupPage`
  - the `ProfilePage` save
  - the `FeedPage` listing form
  - `PinVerificationStep`

  These now share `getErrorMessage`, but their state handling is still copied. This ticket covers load-on-mount requests only; a `useSubmitState` hook would be the matching extraction for submits.

---
