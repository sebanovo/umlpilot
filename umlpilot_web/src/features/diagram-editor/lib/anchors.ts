export type AnchorSide = 'top' | 'right' | 'bottom' | 'left'

export interface EdgeAnchor {
  side: AnchorSide
  percent: number
}

export type AnchorEnd = 'source' | 'target'

const clampPercent = (value: number) => Math.max(0, Math.min(100, value))

export function sideFromPosition(position: string | undefined, fallback: AnchorSide): AnchorSide {
  if (position === 'top' || position === 'right' || position === 'bottom' || position === 'left') {
    return position
  }
  return fallback
}

export function parseEdgeAnchor(handle: string | null | undefined, fallbackSide: AnchorSide): EdgeAnchor {
  if (!handle) return { side: fallbackSide, percent: 50 }

  const raw = handle.replace(/^[st]-/, '')
  let side: AnchorSide | null = null
  let rest = raw

  if (raw.startsWith('top-')) {
    side = 'top'
    rest = raw.slice(4)
  } else if (raw.startsWith('right-')) {
    side = 'right'
    rest = raw.slice(6)
  } else if (raw.startsWith('bottom-')) {
    side = 'bottom'
    rest = raw.slice(7)
  } else if (raw.startsWith('left-')) {
    side = 'left'
    rest = raw.slice(5)
  } else if (raw === 'top' || raw === 'right' || raw === 'bottom' || raw === 'left') {
    side = raw
    rest = ''
  }

  if (!side) return { side: fallbackSide, percent: 50 }
  if (!rest || rest === 'center') return { side, percent: 50 }
  if (rest === 'left' || rest === 'top') return { side, percent: 10 }
  if (rest === 'right' || rest === 'bottom') return { side, percent: 90 }

  const value = Number(rest)
  return { side, percent: Number.isFinite(value) ? clampPercent(value) : 50 }
}

export function edgeHandleFor(anchor: EdgeAnchor, type: AnchorEnd): string {
  return `${type === 'source' ? 's' : 't'}-${anchor.side}-center`
}

export function anchorStorageValue(anchor: EdgeAnchor, type: AnchorEnd): string {
  return `${type === 'source' ? 's' : 't'}-${anchor.side}-${Math.round(clampPercent(anchor.percent))}`
}

export function clampAnchorPercent(percent: number): number {
  return clampPercent(percent)
}
