import { getToken } from './authStorage'

const FRAME_TERMINATOR = '\u0000'
const RECONNECT_DELAY_MS = 2500

function toWebSocketUrl(value) {
  if (value.startsWith('ws://') || value.startsWith('wss://')) {
    return value
  }

  if (value.startsWith('http://')) {
    return `ws://${value.slice('http://'.length)}`
  }

  if (value.startsWith('https://')) {
    return `wss://${value.slice('https://'.length)}`
  }

  if (value.startsWith('/')) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}${value}`
  }

  return value
}

function getWebSocketUrl() {
  if (import.meta.env.VITE_WS_URL) {
    return toWebSocketUrl(import.meta.env.VITE_WS_URL)
  }

  const isLocalViteDev =
    ['localhost', '127.0.0.1'].includes(window.location.hostname) &&
    window.location.port === '5173'

  if (isLocalViteDev) {
    return 'ws://localhost:8080/ws'
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

function buildFrame(command, headers = {}, body = '') {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`)
  return `${command}\n${headerLines.join('\n')}\n\n${body}${FRAME_TERMINATOR}`
}

function parseFrame(rawFrame) {
  const [head = '', body = ''] = rawFrame.split('\n\n')
  const [command = '', ...headerLines] = head.split('\n')
  const headers = {}

  headerLines.forEach((line) => {
    const separatorIndex = line.indexOf(':')

    if (separatorIndex > -1) {
      headers[line.slice(0, separatorIndex)] = line.slice(separatorIndex + 1)
    }
  })

  return {
    command,
    headers,
    body,
  }
}

export function createChatSocketClient({
  subscriptions,
  onMessage,
  onError,
  onStatusChange,
}) {
  let socket = null
  let connected = false
  let manuallyClosed = false
  let reconnectTimeoutId = null

  const sendFrame = (command, headers = {}, body = '') => {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return false
    }

    socket.send(buildFrame(command, headers, body))
    return true
  }

  const connect = () => {
    const token = getToken()

    if (!token) {
      onError?.('Chat requires login')
      return
    }

    const socketUrl = getWebSocketUrl()
    socket = new WebSocket(socketUrl, ['v12.stomp', 'v11.stomp', 'v10.stomp'])
    onStatusChange?.('connecting')

    socket.addEventListener('open', () => {
      sendFrame('CONNECT', {
        'accept-version': '1.2',
        'heart-beat': '10000,10000',
        Authorization: `Bearer ${token}`,
      })
    })

    socket.addEventListener('message', (event) => {
      const frames = String(event.data)
        .split(FRAME_TERMINATOR)
        .map((frame) => frame.trim())
        .filter(Boolean)

      frames.forEach((rawFrame) => {
        const frame = parseFrame(rawFrame)

        if (frame.command === 'CONNECTED') {
          connected = true
          onStatusChange?.('connected')
          subscriptions.forEach((destination, index) => {
            sendFrame('SUBSCRIBE', {
              id: `chat-${index}`,
              destination,
            })
          })
          sendFrame('SUBSCRIBE', {
            id: 'chat-errors',
            destination: '/user/queue/chat/errors',
          })
          return
        }

        if (frame.command === 'MESSAGE') {
          try {
            const payload = JSON.parse(frame.body)

            if (frame.headers.destination === '/user/queue/chat/errors') {
              onError?.(payload.message ?? 'Chat error')
            } else {
              onMessage?.(payload)
            }
          } catch {
            onError?.('Could not read chat message')
          }
          return
        }

        if (frame.command === 'ERROR') {
          onError?.(frame.body || 'Chat connection error')
        }
      })
    })

    socket.addEventListener('close', (event) => {
      connected = false
      onStatusChange?.('disconnected')

      if (!manuallyClosed && event.code !== 1000) {
        onError?.(
          `Chat disconnected (${socketUrl}) code=${event.code} reason=${event.reason || 'none'} readyState=${socket?.readyState ?? 'unknown'}`,
        )
      }

      if (!manuallyClosed) {
        reconnectTimeoutId = window.setTimeout(connect, RECONNECT_DELAY_MS)
      }
    })

    socket.addEventListener('error', () => {
      onError?.(
        `Chat connection failed (${socketUrl}) readyState=${socket?.readyState ?? 'unknown'}`,
      )
    })
  }

  connect()

  return {
    send(destination, body) {
      return sendFrame('SEND', {
        destination,
        'content-type': 'application/json',
      }, JSON.stringify(body))
    },
    isConnected() {
      return connected
    },
    disconnect() {
      manuallyClosed = true

      if (reconnectTimeoutId) {
        window.clearTimeout(reconnectTimeoutId)
      }

      if (socket && socket.readyState === WebSocket.OPEN) {
        sendFrame('DISCONNECT')
      }

      socket?.close()
      socket = null
      connected = false
      onStatusChange?.('disconnected')
    },
  }
}
