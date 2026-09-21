const EDU_EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.edu$/i

export function validateEmail(value: string): string | undefined {
  if (!value.trim()) return 'Email is required'
  if (!EDU_EMAIL_PATTERN.test(value)) {
    return 'Use your .edu school email'
  }
  return undefined
}

export function validatePassword(value: string): string | undefined {
  if (!value) return 'Password is required'
  if (value.length < 8) return 'Password must be at least 8 characters'
  return undefined
}

export function validateName(value: string): string | undefined {
  if (!value.trim()) return 'Full name is required'
  return undefined
}

export function validateConfirmPassword(
  value: string,
  password: string,
): string | undefined {
  if (!value) return 'Confirm your password'
  if (value !== password) return 'Passwords do not match'
  return undefined
}
