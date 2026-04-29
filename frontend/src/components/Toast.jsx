import { useEffect } from 'react'

function Toast({ message, type, onClose }) {
  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      onClose()
    }, 3000)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [message, onClose, type])

  return (
    <aside className={`toast toast-${type}`} role="status" aria-live="polite">
      <p className="toast-message">{message}</p>
      <button
        type="button"
        className="toast-close"
        onClick={onClose}
        aria-label="Close message"
      >
        x
      </button>
    </aside>
  )
}

export default Toast
