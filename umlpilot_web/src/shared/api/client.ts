const API_URL = import.meta.env.VITE_API_URL

async function authFetch(path: string, options: RequestInit = {}) {
  const token = localStorage.getItem('accessToken')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers as Record<string, string>,
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${API_URL}${path}`, { ...options, headers })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.error || `Error ${res.status}`)
  }
  if (res.status === 204) return null
  return res.json()
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
export interface UmlAttributeApi { id: string; elementId: string; name: string; dataType: string; visibility: string; defaultValue?: string; isStatic: boolean; isFinal: boolean; orderIndex: number }
export interface UmlMethodApi { id: string; elementId: string; name: string; returnType: string; visibility: string; isStatic: boolean; isAbstract: boolean; isFinal: boolean; isConstructor: boolean; orderIndex: number; parameters: { id: string; methodId: string; name: string; dataType: string; orderIndex: number }[] }
export interface UmlRelationshipApi { id: string; diagramId: string; sourceElementId: string; targetElementId: string; relationshipType: string; name?: string; direction: string }

export const elementsApi = {
  list: (projectId: string, diagramId: string) => authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements`) as Promise<UmlElementApi[]>,
  create: (projectId: string, diagramId: string, data: { name: string; elementType: string; positionX?: number; positionY?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlElementApi>,
  update: (projectId: string, diagramId: string, elementId: string, data: Record<string, unknown>) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}`, { method: 'PUT', body: JSON.stringify(data) }) as Promise<UmlElementApi>,
  delete: (projectId: string, diagramId: string, elementId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}`, { method: 'DELETE' }),
  getAttributes: (projectId: string, diagramId: string, elementId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/attributes`) as Promise<UmlAttributeApi[]>,
  addAttribute: (projectId: string, diagramId: string, elementId: string, data: { name: string; dataType: string; visibility?: string; orderIndex?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/attributes`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlAttributeApi>,
  deleteAttribute: (projectId: string, diagramId: string, attributeId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/attributes/${attributeId}`, { method: 'DELETE' }),
  getMethods: (projectId: string, diagramId: string, elementId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/methods`) as Promise<UmlMethodApi[]>,
  addMethod: (projectId: string, diagramId: string, elementId: string, data: { name: string; returnType: string; visibility?: string; orderIndex?: number }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/${elementId}/methods`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlMethodApi>,
  deleteMethod: (projectId: string, diagramId: string, methodId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/elements/methods/${methodId}`, { method: 'DELETE' }),
}

export const relationshipsApi = {
  list: (projectId: string, diagramId: string) => authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships`) as Promise<UmlRelationshipApi[]>,
  create: (projectId: string, diagramId: string, data: { sourceElementId: string; targetElementId: string; relationshipType: string }) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships`, { method: 'POST', body: JSON.stringify(data) }) as Promise<UmlRelationshipApi>,
  delete: (projectId: string, diagramId: string, relationshipId: string) =>
    authFetch(`/api/v1/projects/${projectId}/diagrams/${diagramId}/relationships/${relationshipId}`, { method: 'DELETE' }),
}
