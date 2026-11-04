const API_URL = ''

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  try {
    const res = await fetch(`${API_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return null
    const data = await res.json()
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    return data.accessToken
  } catch {
    return null
  }
}

/** Cierra la sesión local y navega al login (redirección de página completa). */
function redirectToLogin() {
  if (typeof window === 'undefined') return
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

async function authFetch(path: string, options: RequestInit = {}, isRetry = false): Promise<unknown> {
  const token = localStorage.getItem('accessToken')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers as Record<string, string>,
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${API_URL}${path}`, { ...options, headers })

  if ((res.status === 401 || res.status === 403) && !isRetry) {
    const newToken = await refreshAccessToken()
    if (newToken) {
      return authFetch(path, options, true)
    }
  }
  if (res.status === 401) {
    redirectToLogin()
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.error || `Error ${res.status}`)
  }
  if (res.status === 204) return null
  return res.json()
}

async function authUpload(path: string, formData: FormData, isRetry = false): Promise<unknown> {
  const token = localStorage.getItem('accessToken')
  const headers: Record<string, string> = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${API_URL}${path}`, { method: 'POST', headers, body: formData })

  if ((res.status === 401 || res.status === 403) && !isRetry) {
    const newToken = await refreshAccessToken()
    if (newToken) {
      return authUpload(path, formData, true)
    }
  }
  if (res.status === 401) {
    redirectToLogin()
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.error || `Error ${res.status}`)
  }
  return res.json()
}

export interface DownloadedFile {
  blob: Blob
  filename: string
}

async function authDownload(path: string, isRetry = false): Promise<DownloadedFile> {
  const token = localStorage.getItem('accessToken')
  const headers: Record<string, string> = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${API_URL}${path}`, { headers })

  if ((res.status === 401 || res.status === 403) && !isRetry) {
    const newToken = await refreshAccessToken()
    if (newToken) {
      return authDownload(path, true)
    }
  }
  if (res.status === 401) {
    redirectToLogin()
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.error || `Error ${res.status}`)
  }

  const disposition = res.headers.get('Content-Disposition') || ''
  const match = disposition.match(/filename="?([^";]+)"?/i)
  return { blob: await res.blob(), filename: match ? match[1] : 'download' }
}

export interface Project {
  id: string
  name: string
  description: string
  creatorId: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface Collaborator {
  userId: string
  email: string
  firstName: string
  lastName: string
  role: string
  invitationStatus: string
}

export interface Diagram {
  id: string
  projectId: string
  name: string
  type: string
  description: string
  canvasData: string
  version: number
  isLocked: boolean
  lockedByUserId: string | null
  createdAt: string
  updatedAt: string
}

export const projectsApi = {
  list: () => authFetch('/api/v1/projects') as Promise<Project[]>,
  get: (id: string) => authFetch(`/api/v1/projects/${id}`) as Promise<Project>,
  create: (data: { name: string; description?: string }) =>
    authFetch('/api/v1/projects', { method: 'POST', body: JSON.stringify(data) }) as Promise<Project>,
  update: (id: string, data: { name?: string; description?: string }) =>
    authFetch(`/api/v1/projects/${id}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<Project>,
  delete: (id: string) => authFetch(`/api/v1/projects/${id}`, { method: 'DELETE' }),
  getCollaborators: (id: string) => authFetch(`/api/v1/projects/${id}/collaborators`) as Promise<Collaborator[]>,
  inviteCollaborator: (id: string, data: { email: string; role: string }) =>
    authFetch(`/api/v1/projects/${id}/collaborators`, { method: 'POST', body: JSON.stringify(data) }) as Promise<Collaborator>,
  updateCollaboratorRole: (id: string, userId: string, role: 'editor' | 'viewer') =>
    authFetch(`/api/v1/projects/${id}/collaborators/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({ role }),
    }),
  removeCollaborator: (id: string, userId: string) =>
    authFetch(`/api/v1/projects/${id}/collaborators/${userId}`, { method: 'DELETE' }),
}

export const diagramsApi = {
  list: (projectId: string) => authFetch(`/api/v1/projects/${projectId}/diagrams`) as Promise<Diagram[]>,
  get: (projectId: string, id: string) => authFetch(`/api/v1/projects/${projectId}/diagrams/${id}`) as Promise<Diagram>,
  create: (projectId: string, data: { name: string; type: string; description?: string }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams`, { method: 'POST', body: JSON.stringify(data) }) as Promise<Diagram>,
  update: (projectId: string, id: string, data: { name?: string; description?: string; canvasData?: string }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${id}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<Diagram>,
  delete: (projectId: string, id: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${id}`, { method: 'DELETE' }),
  lock: (projectId: string, id: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${id}/lock`, { method: 'POST' }) as Promise<Diagram>,
  unlock: (projectId: string, id: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${id}/unlock`, { method: 'POST' }) as Promise<Diagram>,
}

export interface UmlElementApi {
  id: string; diagramId: string; name: string; visibility: string; stereotype?: string; elementType: string; positionX: number; positionY: number; width: number; height: number
}
export interface UmlAttributeApi { id: string; elementId: string; name: string; dataType: string; visibility: string; alias?: string; stereotype?: string; defaultValue?: string; isStatic: boolean; isFinal: boolean; orderIndex: number }
export interface UmlMethodApi { id: string; elementId: string; name: string; returnType: string; visibility: string; alias?: string; stereotype?: string; defaultValue?: string; isStatic: boolean; isAbstract: boolean; isFinal: boolean; isConstructor: boolean; orderIndex: number; parameters: UmlParameterApi[] }
export interface UmlParameterApi { id: string; methodId: string; name: string; dataType: string; direction?: string; notes?: string; orderIndex: number }
export interface UmlRelationshipApi { id: string; diagramId: string; sourceElementId: string; targetElementId: string; relationshipType: string; name?: string; direction: string; associationClassElementId?: string | null; sourceHandle?: string | null; targetHandle?: string | null }

export interface MultiplicityApi {
  id: string
  relationshipId: string
  end: string
  min: number | null
  max: number | null
  isOrdered: boolean
  isUnique: boolean
}

export const elementsApi = {
  list: (projectId: string, diagramId: string) => authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements`) as Promise<UmlElementApi[]>,
  create: (projectId: string, diagramId: string, data: { name: string; elementType: string; positionX?: number; positionY?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlElementApi>,
  update: (projectId: string, diagramId: string, elementId: string, data: Record<string, unknown>) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<UmlElementApi>,
  delete: (projectId: string, diagramId: string, elementId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}`, { method: 'DELETE' }),
  deleteAll: (projectId: string, diagramId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements`, { method: 'DELETE' }),
  getAttributes: (projectId: string, diagramId: string, elementId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/attributes`) as Promise<UmlAttributeApi[]>,
  addAttribute: (projectId: string, diagramId: string, elementId: string, data: { name: string; dataType: string; visibility?: string; alias?: string; stereotype?: string; defaultValue?: string; orderIndex?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/attributes`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlAttributeApi>,
  deleteAttribute: (projectId: string, diagramId: string, attributeId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/attributes/${attributeId}`, { method: 'DELETE' }),
  getMethods: (projectId: string, diagramId: string, elementId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/methods`) as Promise<UmlMethodApi[]>,
  addMethod: (projectId: string, diagramId: string, elementId: string, data: { name: string; returnType: string; visibility?: string; alias?: string; stereotype?: string; defaultValue?: string; orderIndex?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/methods`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlMethodApi>,
  deleteMethod: (projectId: string, diagramId: string, methodId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/methods/${methodId}`, { method: 'DELETE' }),
  addParameter: (projectId: string, diagramId: string, methodId: string, data: { name: string; dataType: string; direction?: string; notes?: string; orderIndex?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/methods/${methodId}/parameters`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlParameterApi>,
  deleteParameter: (projectId: string, diagramId: string, parameterId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/parameters/${parameterId}`, { method: 'DELETE' }),
}

export const relationshipsApi = {
  list: (projectId: string, diagramId: string) => authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships`) as Promise<UmlRelationshipApi[]>,
  create: (projectId: string, diagramId: string, data: { sourceElementId: string; targetElementId: string; relationshipType: string; associationClassElementId?: string; sourceHandle?: string; targetHandle?: string }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlRelationshipApi>,
  update: (projectId: string, diagramId: string, relationshipId: string, data: { name?: string; type?: string; direction?: string; associationClassElementId?: string; sourceElementId?: string; targetElementId?: string; sourceHandle?: string; targetHandle?: string }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships/${relationshipId}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<UmlRelationshipApi>,
  delete: (projectId: string, diagramId: string, relationshipId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships/${relationshipId}`, { method: 'DELETE' }),
  getMultiplicities: (projectId: string, diagramId: string, relationshipId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships/${relationshipId}/multiplicities`) as Promise<MultiplicityApi[]>,
  updateMultiplicity: (projectId: string, diagramId: string, multiplicityId: string, data: { min?: number | null; max?: number | null }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships/multiplicities/${multiplicityId}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<MultiplicityApi>,
}

export interface AiChatAction { action: string; entity: string; id: string; result: string }
export interface AiChatTurn { role: 'user' | 'assistant'; content: string }
export interface AiChatResult { reply: string; actions: AiChatAction[] }

export const aiChatApi = {
  chat: (projectId: string, diagramId: string, message: string, history: AiChatTurn[]) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/ai/chat`, {
      method: 'POST',
      body: JSON.stringify({ message, history }),
    }) as Promise<AiChatResult>,
  transcribe: (projectId: string, diagramId: string, audio: Blob, filename: string) => {
    const form = new FormData()
    form.append('file', audio, filename)
    return authUpload(`/api/v1/projects/${projectId}/diagrams/${diagramId}/ai/transcribe`, form) as Promise<{ text: string }>
  },
  diagramFromImage: (projectId: string, diagramId: string, image: File, instructions?: string) => {
    const form = new FormData()
    form.append('file', image, image.name)
    if (instructions) form.append('instructions', instructions)
    return authUpload(`/api/v1/projects/${projectId}/diagrams/${diagramId}/ai/diagram-from-image`, form) as Promise<AiChatResult>
  },
  diagramFromText: (projectId: string, diagramId: string, prompt: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/ai/diagram-from-text`, {
      method: 'POST',
      body: JSON.stringify({ prompt }),
    }) as Promise<AiChatResult>,
}

export const backendGenerationApi = {
  download: (projectId: string, diagramId: string) =>
    authDownload(`/api/v1/projects/${projectId}/diagrams/${diagramId}/generate/backend`),
}

export interface ImportXmiResult {
  elements: number
  attributes: number
  methods: number
  parameters: number
  literals: number
  relationships: number
  warnings: string[]
}

export const xmiImportApi = {
  importXmi: (projectId: string, diagramId: string, file: File) => {
    const form = new FormData()
    form.append('file', file, file.name)
    return authUpload(
      `/api/v1/projects/${projectId}/diagrams/${diagramId}/import/xmi`,
      form,
    ) as Promise<ImportXmiResult>
  },
}

export interface UserSearchResult {
  id: string
  email: string
  firstName: string
  lastName: string
}

export const usersApi = {
  search: (query: string) =>
    authFetch(`/api/v1/users/search?q=${encodeURIComponent(query)}`) as Promise<UserSearchResult[]>,
}

export interface AppNotification {
  id: string
  type: string
  title: string
  message: string
  projectId: string | null
  role: string | null
  read: boolean
  readAt: string | null
  createdAt: string
  invitationStatus: 'pending' | 'accepted' | 'rejected' | null
}

export const notificationsApi = {
  list: (unreadOnly = false) =>
    authFetch(`/api/v1/notifications?unreadOnly=${unreadOnly}`) as Promise<AppNotification[]>,
  unreadCount: () => authFetch('/api/v1/notifications/unread-count') as Promise<{ count: number }>,
  markAsRead: (id: string) => authFetch(`/api/v1/notifications/${id}/read`, { method: 'PUT' }),
  markAllAsRead: () => authFetch('/api/v1/notifications/read-all', { method: 'PUT' }),
  respond: (id: string, action: 'accept' | 'reject') =>
    authFetch(`/api/v1/notifications/${id}/respond`, {
      method: 'POST',
      body: JSON.stringify({ action }),
    }),
}

/** Fetch sin Authorization: usado por las vistas públicas (enlaces compartidos). */
async function publicFetch(path: string): Promise<unknown> {
  const res = await fetch(`${API_URL}${path}`)
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.error || `Error ${res.status}`)
  }
  return res.json()
}

export interface UmlLiteralApi {
  id: string
  elementId: string
  name: string
  value: string | null
  orderIndex: number
}

export type SharedLinkRole = 'viewer' | 'editor'

export interface PublicDiagramView {
  projectId: string
  diagramId: string
  linkRole: SharedLinkRole
  projectName: string
  diagramName: string
  diagramType: string
  elements: UmlElementApi[]
  relationships: UmlRelationshipApi[]
  attributes: Record<string, UmlAttributeApi[]>
  methods: Record<string, UmlMethodApi[]>
  literals: Record<string, UmlLiteralApi[]>
  multiplicities: Record<string, MultiplicityApi[]>
}

export interface ShareLinkState {
  enabled: boolean
  token: string | null
  url: string | null
  role: SharedLinkRole
}

export interface ShareJoinResult {
  projectId: string
  diagramId: string
  role: 'owner' | 'editor' | 'viewer'
}

export const diagramShareApi = {
  get: (projectId: string, diagramId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/share`) as Promise<ShareLinkState>,
  enable: (projectId: string, diagramId: string, role: SharedLinkRole) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/share`, {
      method: 'POST',
      body: JSON.stringify({ role }),
    }) as Promise<ShareLinkState>,
  disable: (projectId: string, diagramId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/share`, { method: 'DELETE' }),
}

export const publicDiagramApi = {
  get: (token: string) =>
    publicFetch(`/api/v1/public/diagrams/${encodeURIComponent(token)}`) as Promise<PublicDiagramView>,
  join: (token: string) =>
    authFetch(`/api/v1/public/diagrams/${encodeURIComponent(token)}/join`, {
      method: 'POST',
    }) as Promise<ShareJoinResult>,
}
