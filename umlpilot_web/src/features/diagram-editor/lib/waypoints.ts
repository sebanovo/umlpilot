export interface EdgeWaypoint {
  x: number
  y: number
}

export function parseWaypoints(value: string | null | undefined): EdgeWaypoint[] {
  if (!value) return []
  try {
    const parsed = JSON.parse(value) as unknown
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((point): point is { x: number; y: number } =>
        !!point && typeof point === 'object' && Number.isFinite((point as { x?: unknown }).x) && Number.isFinite((point as { y?: unknown }).y))
      .map((point) => ({ x: Math.round(point.x), y: Math.round(point.y) }))
  } catch {
    return []
  }
}

export function serializeWaypoints(points: EdgeWaypoint[]): string {
  return JSON.stringify(points.map((point) => ({ x: Math.round(point.x), y: Math.round(point.y) })))
}
