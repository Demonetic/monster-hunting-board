import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getRecentGlobalMessages,
  getRecentLobbyMessages,
  sendGlobalMessage,
  sendLobbyMessage,
} from '../api/chatApi'

const MAX_MESSAGE_LENGTH = 250
const POLL_INTERVAL_MS = 3000

function formatChatTime(value) {
  if (!value) {
    return ''
  }

  return new Date(value).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function ChatBox({
  mode = 'GLOBAL',
  lobbyId = null,
  title = 'Global Chat',
  collapsible = false,
  initiallyCollapsed = false,
  disabled = false,
  className = '',
}) {
  const messageListRef = useRef(null)
  const [collapsed, setCollapsed] = useState(initiallyCollapsed)
  const [messages, setMessages] = useState([])
  const [messageText, setMessageText] = useState('')
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const isLobbyChat = mode === 'LOBBY'
  const canLoad = !collapsed && !disabled && (!isLobbyChat || lobbyId)

  const loadMessages = useCallback(async ({ showLoading = false } = {}) => {
    if (!canLoad) {
      return
    }

    if (showLoading) {
      setLoading(true)
    }

    try {
      const response = isLobbyChat
        ? await getRecentLobbyMessages(lobbyId)
        : await getRecentGlobalMessages()

      setMessages(Array.isArray(response.data) ? response.data : [])
      setErrorMessage('')
    } catch (error) {
      setErrorMessage(error.response?.data?.message ?? 'Could not load chat')
    } finally {
      if (showLoading) {
        setLoading(false)
      }
    }
  }, [canLoad, isLobbyChat, lobbyId])

  useEffect(() => {
    loadMessages({ showLoading: true })

    if (!canLoad) {
      return undefined
    }

    const intervalId = window.setInterval(() => {
      loadMessages()
    }, POLL_INTERVAL_MS)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [canLoad, loadMessages])

  useEffect(() => {
    const listElement = messageListRef.current

    if (listElement) {
      listElement.scrollTop = listElement.scrollHeight
    }
  }, [messages])

  const trimmedMessage = messageText.trim()
  const canSend = trimmedMessage.length > 0 && trimmedMessage.length <= MAX_MESSAGE_LENGTH && !sending && !disabled

  const handleSend = async () => {
    if (!canSend) {
      return
    }

    setSending(true)

    try {
      const response = isLobbyChat
        ? await sendLobbyMessage(lobbyId, trimmedMessage)
        : await sendGlobalMessage(trimmedMessage)

      setMessages((currentMessages) => [...currentMessages, response.data].slice(-50))
      setMessageText('')
      setErrorMessage('')
    } catch (error) {
      setErrorMessage(error.response?.data?.message ?? 'Could not send message')
    } finally {
      setSending(false)
    }
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSend()
    }
  }

  return (
    <section className={`chat-box ${collapsed ? 'is-collapsed' : ''} ${className}`.trim()}>
      <header className="chat-box-header">
        <h2>{title}</h2>
        {collapsible && (
          <button
            type="button"
            className="chat-box-toggle"
            onClick={() => setCollapsed((current) => !current)}
            aria-expanded={!collapsed}
          >
            {collapsed ? 'Open' : 'Close'}
          </button>
        )}
      </header>

      {!collapsed && (
        <>
          <div className="chat-box-messages" ref={messageListRef}>
            {loading && <p className="chat-box-muted">Loading chat...</p>}
            {!loading && messages.length === 0 && (
              <p className="chat-box-muted">No messages yet.</p>
            )}
            {messages.map((message) => (
              <article className="chat-message" key={message.id}>
                <div className="chat-message-meta">
                  <span>{message.senderDisplayName}</span>
                  <time dateTime={message.createdAt}>{formatChatTime(message.createdAt)}</time>
                </div>
                <p>{message.messageText}</p>
              </article>
            ))}
          </div>

          {errorMessage && <p className="chat-box-error">{errorMessage}</p>}

          <div className="chat-box-input-row">
            <input
              type="text"
              value={messageText}
              maxLength={MAX_MESSAGE_LENGTH}
              onChange={(event) => setMessageText(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={disabled ? 'Chat closed' : 'Message'}
              disabled={disabled || sending}
            />
            <button type="button" onClick={handleSend} disabled={!canSend}>
              {sending ? '...' : 'Send'}
            </button>
          </div>
          <p className="chat-box-count">{trimmedMessage.length}/{MAX_MESSAGE_LENGTH}</p>
        </>
      )}
    </section>
  )
}

export default ChatBox
