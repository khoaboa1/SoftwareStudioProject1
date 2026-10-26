import { CaretLeft, CaretRight } from '@phosphor-icons/react'

type PaginationProps = {
  currentPage: number
  totalPages: number
  totalItems: number
  onPageChange: (page: number) => void
}

export function Pagination({ currentPage, totalPages, totalItems, onPageChange }: PaginationProps) {
  if (totalItems === 0) return null

  return (
    <nav aria-label="Pagination" className="mt-8 flex items-center justify-between gap-3">
      <button type="button" disabled={currentPage <= 1} onClick={() => onPageChange(currentPage - 1)} className="inline-flex items-center gap-1 text-sm text-zinc-700 disabled:cursor-not-allowed disabled:opacity-50">
        <CaretLeft aria-hidden="true" /> Previous
      </button>
      <span className="text-sm text-zinc-600">Page {currentPage} of {totalPages} · {totalItems} {totalItems === 1 ? 'result' : 'results'}</span>
      <button type="button" disabled={currentPage >= totalPages} onClick={() => onPageChange(currentPage + 1)} className="inline-flex items-center gap-1 text-sm text-zinc-700 disabled:cursor-not-allowed disabled:opacity-50">
        Next <CaretRight aria-hidden="true" />
      </button>
    </nav>
  )
}
