export interface MentionOption {
  id: string
  label: string
  role: string
  assistant?: boolean
}

const ASSISTANT_MENTION = /@(asistente|ai|ia)\b/i

export function mentionsAssistant(content: string): boolean {
  return ASSISTANT_MENTION.test(content)
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** Construye un regex que reconoce @Asistente IA y los nombres de los participantes. */
export function buildMentionRegex(labels: string[]): RegExp {
  const names = Array.from(new Set(['Asistente IA', 'Asistente', 'AI', 'IA', ...labels]
    .map((label) => label.trim())
    .filter(Boolean)))
    .sort((a, b) => b.length - a.length)
    .map(escapeRegExp)
  return new RegExp(`@(?:${names.join('|')})\\b`, 'gi')
}
