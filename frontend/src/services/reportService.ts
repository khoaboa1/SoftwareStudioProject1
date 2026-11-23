import { request } from './httpClient'

export type ReportReason =
  'SCAM' | 'PROHIBITED_ITEM' | 'SPAM' | 'OFFENSIVE' | 'OTHER'

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  SCAM: 'Scam or fraud',
  PROHIBITED_ITEM: 'Prohibited item',
  SPAM: 'Spam',
  OFFENSIVE: 'Offensive content',
  OTHER: 'Other',
}

export type Report = {
  id: number
  listingId: number
  reason: ReportReason
  details: string | null
  status: 'OPEN' | 'DISMISSED'
  createdAt: string
}

export function reportListing(
  listingId: number,
  input: { reason: ReportReason; details?: string }
): Promise<Report> {
  return request<Report>(`/listings/${listingId}/reports`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}
