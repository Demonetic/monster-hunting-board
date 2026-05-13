import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getRecentGlobalMessages,
  getRecentLobbyMessages,
} from '../api/chatApi'
import { createChatSocketClient } from '../api/chatSocket'

const MAX_MESSAGE_LENGTH = 250

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
  collapsed: controlledCollapsed,
  onCollapsedChange = null,
  disabled = false,
  className = '',
}) {
  const messageListRef = useRef(null)
  const chatClientRef = useRef(null)
  const shouldAutoScrollRef = useRef(true)
  const [internalCollapsed, setInternalCollapsed] = useState(initiallyCollapsed)
  const [messages, setMessages] = useState([])
  const [messageText, setMessageText] = useState('')
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [connectionStatus, setConnectionStatus] = useState('disconnected')

  const isLobbyChat = mode === 'LOBBY'
  const collapsed = controlledCollapsed ?? internalCollapsed
  const canLoad = !collapsed && !disabled && (!isLobbyChat || lobbyId)
  const topicDestination = isLobbyChat
    ? `/topic/chat/lobby/${lobbyId}`
    : '/topic/chat/global'
  const sendDestination = isLobbyChat
    ? `/app/chat/lobby/${lobbyId}`
    : '/app/chat/global'

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
    if (!canLoad) {
      return undefined
    }

    const initialLoadId = window.setTimeout(() => {
      loadMessages({ showLoading: true })
    }, 0)

    return () => {
      window.clearTimeout(initialLoadId)
    }
  }, [canLoad, loadMessages])

  useEffect(() => {
    if (!canLoad) {
      return undefined
    }

    const chatClient = createChatSocketClient({
      subscriptions: [topicDestination],
      onMessage: (message) => {
        setMessages((currentMessages) => {
          if (currentMessages.some((currentMessage) => currentMessage.id === message.id)) {
            return currentMessages
          }

          return [...currentMessages, message].slice(-50)
        })
        setSending(false)
        setErrorMessage('')
      },
      onError: (message) => {
        setSending(false)
        setErrorMessage(message)
      },
      onStatusChange: setConnectionStatus,
    })
    chatClientRef.current = chatClient

    return () => {
      chatClient.disconnect()
      chatClientRef.current = null
    }
  }, [canLoad, topicDestination])

  useEffect(() => {
    const listElement = messageListRef.current

    if (listElement && shouldAutoScrollRef.current) {
      listElement.scrollTop = listElement.scrollHeight
    }
  }, [messages])

  const handleMessageListScroll = () => {
    const listElement = messageListRef.current

    if (!listElement) {
      return
    }

    const distanceFromBottom = listElement.scrollHeight - listElement.scrollTop - listElement.clientHeight
    shouldAutoScrollRef.current = distanceFromBottom < 32
  }

  const trimmedMessage = messageText.trim()
  const canSend =
    trimmedMessage.length > 0 &&
    trimmedMessage.length <= MAX_MESSAGE_LENGTH &&
    !sending &&
    !disabled &&
    connectionStatus === 'connected'

  const handleSend = async () => {
    if (!canSend) {
      return
    }

    setSending(true)
    shouldAutoScrollRef.current = true

    try {
      const chatClient = chatClientRef.current

      if (!chatClient?.send(sendDestination, { message: trimmedMessage })) {
        throw new Error('Chat is reconnecting')
      }

      setMessageText('')
      setErrorMessage('')
    } catch (error) {
      setSending(false)
      setErrorMessage(error.message ?? 'Could not send message')
    }
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSend()
    }
  }

  const updateCollapsed = (nextCollapsed) => {
    if (controlledCollapsed === undefined) {
      setInternalCollapsed(nextCollapsed)
    }

    onCollapsedChange?.(nextCollapsed)
  }

  return (
    <section className={`chat-box ${collapsed ? 'is-collapsed' : ''} ${className}`.trim()}>
      <header className="chat-box-header">
        <h2>{title}</h2>
        {collapsible && (
          <button
            type="button"
            className="chat-box-toggle"
            onClick={() => updateCollapsed(!collapsed)}
            aria-expanded={!collapsed}
          >
            {collapsed ? 'Open' : 'Close'}
          </button>
        )}
      </header>

      {!collapsed && (
        <>
          <div className="chat-box-messages" ref={messageListRef} onScroll={handleMessageListScroll}>
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
