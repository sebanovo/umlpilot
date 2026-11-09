import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { AtSign, Bot, Send, Sparkles, UserRound } from 'lucide-react'
import type { MentionOption } from '../model/mentions'

interface ChatComposerProps {
  disabled?: boolean
  sending: boolean
  canMutate: boolean
  mentionOptions: MentionOption[]
  onSend: (content: string) => void
  onTyping: (typing: boolean) => void
}

export function ChatComposer({
  disabled,
  sending,
  canMutate,
  mentionOptions,
  onSend,
  onTyping,
}: ChatComposerProps) {
  const [input, setInput] = useState('')
  const [mentionOpen, setMentionOpen] = useState(false)
  const [mentionQuery, setMentionQuery] = useState('')
  const [mentionStart, setMentionStart] = useState<number | null>(null)
  const [activeIndex, setActiveIndex] = useState(0)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const typingTimer = useRef<number | null>(null)

  useEffect(() => () => {
    if (typingTimer.current) window.clearTimeout(typingTimer.current)
  }, [])

  const filteredMentions = useMemo(() => {
    const query = mentionQuery.trim().toLowerCase()
    if (!query) return mentionOptions
    return mentionOptions.filter((option) => option.label.toLowerCase().includes(query))
  }, [mentionOptions, mentionQuery])

  const notifyTyping = (typing: boolean) => {
    onTyping(typing)
    if (typingTimer.current) window.clearTimeout(typingTimer.current)
    if (typing) {
      typingTimer.current = window.setTimeout(() => onTyping(false), 1500)
    }
  }

  const closeMentions = () => {
    setMentionOpen(false)
    setMentionQuery('')
    setMentionStart(null)
    setActiveIndex(0)
  }

  const detectMention = (value: string, cursor: number) => {
    const before = value.slice(0, cursor)
    const match = before.match(/(?:^|\s)@([\p{L}\p{N} ]*)$/u)
    if (!match) {
      closeMentions()
      return
    }
    setMentionQuery(match[1] ?? '')
    setMentionStart(cursor - (match[1]?.length ?? 0) - 1)
    setMentionOpen(true)
    setActiveIndex(0)
  }

  const insertText = (text: string) => {
    const textarea = textareaRef.current
    const cursor = textarea?.selectionStart ?? input.length
    const start = mentionStart ?? cursor
    const next = input.slice(0, start) + text + input.slice(cursor)
    setInput(next)
    closeMentions()
    requestAnimationFrame(() => {
      textareaRef.current?.focus()
      const position = start + text.length
      textareaRef.current?.setSelectionRange(position, position)
    })
  }

  const insertMention = (option: MentionOption) => {
    insertText(option.assistant ? '@Asistente IA ' : `@${option.label} `)
  }

  const openMentionButton = () => {
    const textarea = textareaRef.current
    const cursor = textarea?.selectionStart ?? input.length
    const before = input.slice(0, cursor)
    const needsSpace = before.length > 0 && !/\s$/.test(before)
    const insertion = `${needsSpace ? ' ' : ''}@`
    const next = before + insertion + input.slice(cursor)
    setInput(next)
    const position = before.length + insertion.length
    requestAnimationFrame(() => {
      textareaRef.current?.focus()
      textareaRef.current?.setSelectionRange(position, position)
      detectMention(next, position)
    })
  }

  const submit = () => {
    const text = input.trim()
    if (!text || sending || disabled) return
    onSend(text)
    setInput('')
    closeMentions()
    notifyTyping(false)
  }

  const onChange = (value: string, cursor: number) => {
    setInput(value)
    detectMention(value, cursor)
    notifyTyping(value.length > 0)
  }

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (mentionOpen) {
      if (event.key === 'ArrowDown') {
        event.preventDefault()
        setActiveIndex((index) => (filteredMentions.length === 0 ? 0 : (index + 1) % filteredMentions.length))
        return
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault()
        setActiveIndex((index) => (filteredMentions.length === 0 ? 0
          : (index - 1 + filteredMentions.length) % filteredMentions.length))
        return
      }
      if ((event.key === 'Enter' || event.key === 'Tab') && filteredMentions.length > 0) {
        event.preventDefault()
        insertMention(filteredMentions[activeIndex] ?? filteredMentions[0])
        return
      }
      if (event.key === 'Escape') {
        event.preventDefault()
        closeMentions()
        return
      }
    }

    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      submit()
    }
  }

  const rows = Math.min(5, Math.max(1, Math.ceil(input.length / 72) + input.split('\n').length - 1))

  return (
    <div className="relative border-t border-gray-800 bg-gray-950/95 p-3">
      <div className="mx-auto flex max-w-4xl items-end gap-2">
        <div className="relative flex-1">
          {mentionOpen && (
            <div className="absolute bottom-full left-0 right-0 z-30 mb-2 overflow-hidden rounded-xl border border-gray-700 bg-gray-900 shadow-2xl">
              <div className="flex items-center justify-between border-b border-gray-800 px-3 py-2">
                <span className="text-[11px] font-medium uppercase tracking-wide text-gray-500">
                  Mencionar
                </span>
                <span className="text-[10px] text-gray-600">↑ ↓ para navegar · Enter para elegir</span>
              </div>
              <div className="max-h-56 overflow-y-auto p-1">
                {filteredMentions.length === 0 && (
                  <p className="px-3 py-2 text-xs text-gray-500">Sin coincidencias</p>
                )}
                {filteredMentions.map((option, index) => (
                  <button
                    key={option.id}
                    type="button"
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => insertMention(option)}
                    className={`flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-left transition-colors ${
                      index === activeIndex ? 'bg-blue-950/70 text-blue-100' : 'text-gray-300 hover:bg-gray-800'
                    }`}
                  >
                    <span className={`flex h-6 w-6 items-center justify-center rounded-full ${
                      option.assistant ? 'bg-blue-900 text-blue-300' : 'bg-gray-800 text-gray-400'
                    }`}>
                      {option.assistant ? <Bot className="h-3.5 w-3.5" /> : <UserRound className="h-3.5 w-3.5" />}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-xs font-medium">{option.label}</span>
                      <span className="block text-[10px] uppercase tracking-wide text-gray-500">{option.role}</span>
                    </span>
                    {option.assistant && (
                      <span className="rounded-full bg-blue-950 px-1.5 py-0.5 text-[10px] text-blue-300">IA</span>
                    )}
                  </button>
                ))}
              </div>
            </div>
          )}

          <textarea
            ref={textareaRef}
            value={input}
            rows={rows}
            disabled={disabled}
            onChange={(event) => onChange(event.target.value, event.target.selectionStart ?? event.target.value.length)}
            onKeyDown={onKeyDown}
            onBlur={() => window.setTimeout(closeMentions, 120)}
            placeholder="Escribe un mensaje… usa @asistente para preguntarle a la IA o @persona para mencionar a alguien"
            className="min-h-[52px] w-full resize-none rounded-2xl border border-gray-700 bg-gray-900 px-3 py-2.5 text-sm text-gray-100 placeholder-gray-500 outline-none transition-colors focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500/30 disabled:opacity-50"
          />
          <button
            type="button"
            onClick={openMentionButton}
            disabled={disabled}
            title="Mencionar a la IA o a un integrante"
            className="absolute bottom-2 right-2 flex h-7 w-7 items-center justify-center rounded-full text-gray-500 transition-colors hover:bg-gray-800 hover:text-emerald-300 disabled:opacity-40"
          >
            <AtSign className="h-4 w-4" />
          </button>
        </div>

        <button
          type="button"
          onClick={submit}
          disabled={!input.trim() || sending || disabled}
          className="flex h-[52px] w-[52px] shrink-0 items-center justify-center rounded-full bg-emerald-600 text-white shadow-lg shadow-emerald-950/30 transition-colors hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-40"
          title="Enviar mensaje"
        >
          {sending ? <Sparkles className="h-5 w-5 animate-pulse" /> : <Send className="h-5 w-5" />}
        </button>
      </div>

      <div className="mx-auto mt-1.5 flex max-w-4xl items-center justify-between gap-3 text-[11px] text-gray-500">
        <span>
          {canMutate
            ? 'La IA puede consultar y modificar el diagrama seleccionado.'
            : 'Modo lectura: puedes conversar, pero la IA no modificará el diagrama.'}
        </span>
        <span className="hidden sm:inline">Enter envía · Shift+Enter salto de línea</span>
      </div>
    </div>
  )
}
