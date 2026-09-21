type LogoProps = {
  className?: string
}

/** Simple geometric monogram mark for Handoff — no real brand logo exists yet. */
export function Logo({ className }: LogoProps) {
  return (
    <svg
      viewBox="0 0 40 40"
      role="img"
      aria-label="Handoff"
      className={className}
    >
      <rect width="40" height="40" rx="10" className="fill-accent-500" />
      <path
        d="M12 10v20M28 10v20M12 20h16"
        stroke="white"
        strokeWidth="3.5"
        strokeLinecap="round"
      />
    </svg>
  )
}
