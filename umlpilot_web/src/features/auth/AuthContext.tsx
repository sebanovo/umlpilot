import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'

interface User {
  id: number
  email: string
  firstName: string
  lastName: string
  role: string
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
}

interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

const API_URL = import.meta.env.VITE_API_URL

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    accessToken: null,
    refreshToken: null,
    isAuthenticated: false,
    isLoading: true,
  })

  useEffect(() => {
    const storedToken = localStorage.getItem('accessToken')
    const storedRefresh = localStorage.getItem('refreshToken')
    const storedUser = localStorage.getItem('user')

    if (storedToken && storedRefresh && storedUser) {
      setState({
        user: JSON.parse(storedUser),
        accessToken: storedToken,
        refreshToken: storedRefresh,
        isAuthenticated: true,
        isLoading: false,
      })
    } else {
      setState(prev => ({ ...prev, isLoading: false }))
    }
  }, [])

  const login = async (email: string, password: string) => {
    const res = await fetch(`${API_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })

    if (!res.ok) {
      const data = await res.json()
      throw new Error(data.error || 'Error al iniciar sesión')
    }

    const data = await res.json()
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data.user))

    setState({
      user: data.user,
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      isAuthenticated: true,
      isLoading: false,
    })
  }

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    const res = await fetch(`${API_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, firstName, lastName }),
    })

    if (!res.ok) {
      const data = await res.json()
      throw new Error(data.error || 'Error al registrarse')
    }

    const data = await res.json()
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data.user))

    setState({
      user: data.user,
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      isAuthenticated: true,
      isLoading: false,
    })
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    setState({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
    })
  }

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider')
  }
  return context
}
