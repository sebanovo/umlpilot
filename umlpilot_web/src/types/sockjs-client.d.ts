declare module 'sockjs-client' {
  import { WebSocket } from 'ws'
  export default class SockJS implements WebSocket {
    constructor(url: string, protocols?: string | string[], options?: any)
    binaryType: string
    readyState: number
    url: string
    onopen: ((event: any) => void) | null
    onclose: ((event: any) => void) | null
    onmessage: ((event: any) => void) | null
    onerror: ((event: any) => void) | null
    close(code?: number, reason?: string): void
    send(data: any): void
    addEventListener(type: string, listener: (event: any) => void): void
    removeEventListener(type: string, listener: (event: any) => void): void
    dispatchEvent(event: any): boolean
  }
}
