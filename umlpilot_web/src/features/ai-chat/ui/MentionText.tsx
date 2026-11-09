import { buildMentionRegex } from '../model/mentions'

interface MentionTextProps {
  content: string
  labels: string[]
}

/** Renderiza el contenido resaltando las menciones @asistente y @persona. */
export function MentionText({ content, labels }: MentionTextProps) {
  const regex = buildMentionRegex(labels)
  const parts: Array<{ text: string; mention: boolean }> = []
  let lastIndex = 0

  for (const match of content.matchAll(regex)) {
    const index = match.index ?? 0
    if (index > lastIndex) {
      parts.push({ text: content.slice(lastIndex, index), mention: false })
    }
    parts.push({ text: match[0], mention: true })
    lastIndex = index + match[0].length
  }
  if (lastIndex < content.length) {
    parts.push({ text: content.slice(lastIndex), mention: false })
  }

  return (
    <>
      {parts.map((part, index) => part.mention ? (
        <span
          key={index}
          className="rounded bg-blue-500/20 px-1 font-medium text-blue-200"
        >
          {part.text}
        </span>
      ) : (
        <span key={index}>{part.text}</span>
      ))}
    </>
  )
}
