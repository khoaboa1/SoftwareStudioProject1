/** The option id for an @ suggestion, shared by the list and the input's aria-activedescendant. */
export function suggestionOptionId(listId: string, index: number): string {
  return `${listId}-option-${index}`
}
