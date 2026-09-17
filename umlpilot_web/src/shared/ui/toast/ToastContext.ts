import { createContext } from 'react'

export type ToastVariant = 'info' | 'success' | 'error' | 'warning'

export interface ToastOptions {
  /** Título corto y claro de la notificación. */
  title: string
  /** Detalle opcional (causa o siguiente paso). */
  description?: string
  variant?: ToastVariant
  duration?: number
}

export interface ToastContextValue {
  toast: (options: ToastOptions) => void
  dismiss: (id: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)
