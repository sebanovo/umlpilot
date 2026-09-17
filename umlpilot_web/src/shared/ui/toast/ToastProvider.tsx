import { useCallback, useMemo, useState, type ReactNode } from 'react'
import * as ToastPrimitive from '@radix-ui/react-toast'
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react'
import { ToastContext, type ToastOptions, type ToastVariant } from './ToastContext'

interface ToastItem extends ToastOptions {
  id: string
  variant: ToastVariant
}

const DEFAULT_DURATION = 4000

const VARIANT_STYLES: Record<ToastVariant, { container: string; icon: ReactNode }> = {
  info: {
    container: 'border-gray-700 bg-gray-900 text-gray-100',
    icon: <Info className="w-5 h-5 text-blue-400" aria-hidden="true" />,
  },
  success: {
    container: 'border-green-800 bg-green-950/70 text-green-100',
    icon: <CheckCircle2 className="w-5 h-5 text-green-400" aria-hidden="true" />,
  },
  error: {
    container: 'border-red-800 bg-red-950/70 text-red-100',
    icon: <XCircle className="w-5 h-5 text-red-400" aria-hidden="true" />,
  },
  warning: {
    container: 'border-yellow-700/70 bg-yellow-950/60 text-yellow-100',
    icon: <AlertTriangle className="w-5 h-5 text-yellow-400" aria-hidden="true" />,
  },
}

let counter = 0
function nextId(): string {
  counter += 1
  return `toast-${Date.now()}-${counter}`
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id))
  }, [])

  const toast = useCallback((options: ToastOptions) => {
    const item: ToastItem = { variant: 'info', ...options, id: nextId() }
    setItems((prev) => [...prev.slice(-3), item])
  }, [])

  const value = useMemo(() => ({ toast, dismiss }), [toast, dismiss])

  return (
    <ToastContext.Provider value={value}>
      <ToastPrimitive.Provider swipeDirection="right" duration={DEFAULT_DURATION}>
        {children}
        {items.map((item) => {
          const styles = VARIANT_STYLES[item.variant]
          return (
            <ToastPrimitive.Root
              key={item.id}
              duration={item.duration ?? DEFAULT_DURATION}
              onOpenChange={(open) => {
                if (!open) dismiss(item.id)
              }}
              className={`ui-toast pointer-events-auto flex w-full items-start gap-3 rounded-xl border p-3.5 shadow-lg shadow-black/40 backdrop-blur ${styles.container}`}
            >
              <div className="mt-0.5 shrink-0">{styles.icon}</div>
              <div className="min-w-0 flex-1">
                <ToastPrimitive.Title className="text-sm font-semibold leading-snug">
                  {item.title}
                </ToastPrimitive.Title>
                {item.description && (
                  <ToastPrimitive.Description className="mt-0.5 text-xs leading-relaxed opacity-80">
                    {item.description}
                  </ToastPrimitive.Description>
                )}
              </div>
              <ToastPrimitive.Close
                aria-label="Cerrar notificación"
                className="shrink-0 rounded-md p-0.5 text-current/70 transition-colors hover:bg-white/10 hover:text-current cursor-pointer"
              >
                <X className="w-4 h-4" aria-hidden="true" />
              </ToastPrimitive.Close>
            </ToastPrimitive.Root>
          )
        })}
        <ToastPrimitive.Viewport
          className="pointer-events-none fixed bottom-4 right-4 z-[100] flex w-[min(92vw,380px)] list-none flex-col gap-2 outline-none"
        />
      </ToastPrimitive.Provider>
    </ToastContext.Provider>
  )
}
