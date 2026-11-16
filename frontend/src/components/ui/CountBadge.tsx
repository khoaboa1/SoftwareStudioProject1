/**
 * Small count pill next to a label. Screen readers hear ", N open reports" (or the given noun) instead of a bare
 * number. Renders nothing for zero or a missing count.
 */
export function CountBadge({
  count,
  noun,
}: {
  count: number | null | undefined
  noun: string
}) {
  if (!count) return null
  return (
    <>
      <span
        aria-hidden="true"
        className="ml-1.5 inline-flex min-w-5 items-center justify-center rounded-full bg-accent-700 px-1.5 text-xs font-semibold leading-5 text-white"
      >
        {count}
      </span>
      <span className="sr-only">{`, ${count} ${noun}`}</span>
    </>
  )
}
