import { request } from './httpClient'

/** The latest message of a conversation, as sent in the list response. */
export type LastMessage = {
  content: string
  senderId: number
  createdAt: string
}

export type Conversation = {
  id: number
  listingId: number
  itemName: string
  buyerId: number
  buyerName: string
  sellerId: number
  sellerName: string
  createdAt: string
  /** Optional until the backend exposes it; missing or null means not sold. */
  listingStatus?: string | null
  /** Optional until the backend exposes it; missing or null means no messages yet. */
  lastMessage?: LastMessage | null
}

export type Message = {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  content: string
  createdAt: string
}

/** The other party in a conversation — whoever is not the current user. */
export type ConversationSummary = {
  id: number
  listingId: number
  itemName: string
  otherPartyName: string
  otherPartyId: number
  /** When the conversation last had activity: its last message, else when it was started. */
  activityAt: string
  sold: boolean
  /** Preview of the last message; `sender` is "You" or the other party's first name. */
  lastMessage: { sender: string; text: string } | null
}

export function getConversations(): Promise<Conversation[]> {
  return request<Conversation[]>('/api/conversations')
}

export function startConversation(listingId: number): Promise<Conversation> {
  return request<Conversation>(`/listings/${listingId}/conversation`, {
    method: 'POST',
  })
}

export function getConversation(id: number): Promise<Conversation> {
  return request<Conversation>(`/api/conversations/${id}`)
}

export function getMessages(id: number, after?: number): Promise<Message[]> {
  const query = after === undefined ? '' : `?after=${after}`
  return request<Message[]>(`/api/conversations/${id}/messages${query}`)
}

export function sendMessage(id: number, content: string): Promise<Message> {
  return request<Message>(`/api/conversations/${id}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

/** Maps a backend Conversation to a frontend summary, resolving the other party. */
export function toSummary(conversation: Conversation, currentStudentId: number): ConversationSummary {
  const isBuyer = conversation.buyerId === currentStudentId
  return {
    id: conversation.id,
    listingId: conversation.listingId,
    itemName: conversation.itemName,
    otherPartyId: isBuyer ? conversation.sellerId : conversation.buyerId,
    otherPartyName: isBuyer ? conversation.sellerName : conversation.buyerName,
    activityAt: activityAt(conversation),
    sold: conversation.listingStatus === 'SOLD',
    lastMessage: conversation.lastMessage
      ? {
          sender: conversation.lastMessage.senderId === currentStudentId
            ? 'You'
            : (isBuyer ? conversation.sellerName : conversation.buyerName).split(' ')[0],
          text: conversation.lastMessage.content,
        }
      : null,
  }
}

function activityAt(conversation: Conversation): string {
  return conversation.lastMessage?.createdAt ?? conversation.createdAt
}

/** Most recent activity first (last message, else creation time), without mutating the input. */
export function sortNewestFirst(conversations: Conversation[]): Conversation[] {
  return [...conversations].sort((a, b) => Date.parse(activityAt(b)) - Date.parse(activityAt(a)))
}
