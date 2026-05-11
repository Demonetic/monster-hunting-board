import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import battleArenaImage from '../assets/battle_arena.png'
import { getGroupLobby } from '../api/huntApi'

const LOBBY_OPEN_MS = 10 * 60 * 1000

function formatCountdown(msRemaining) {
  const safeMs = Math.max(0, msRemaining)
  const totalSeconds = Math.floor(safeMs / 1000)
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

function GroupHuntLobbyPage() {
  const navigate = useNavigate()
  const { huntId } = useParams()
  const [lobby, setLobby] = useState(null)
  const [now, setNow] = useState(Date.now())
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let cancelled = false

    const loadLobby = async () => {
      try {
        const response = await getGroupLobby(huntId)
        if (!cancelled) {
          setLobby(response.data)
          setErrorMessage('')
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error.response?.data?.message ?? 'Could not load lobby')
        }
      }
    }

    loadLobby()
    const refreshId = window.setInterval(loadLobby, 5000)

    return () => {
      cancelled = true
      window.clearInterval(refreshId)
    }
  }, [huntId])

  useEffect(() => {
    const tickId = window.setInterval(() => {
      setNow(Date.now())
    }, 1000)

    return () => window.clearInterval(tickId)
  }, [])

  const startTimeMs = useMemo(
    () => (lobby?.startTime ? new Date(lobby.startTime).getTime() : null),
    [lobby?.startTime],
  )
  const msUntilStart = startTimeMs ? startTimeMs - now : null
  const isLobbyOpen = startTimeMs !== null && msUntilStart <= LOBBY_OPEN_MS
  const hasStarted = lobby?.status === 'ACTIVE' || (startTimeMs !== null && msUntilStart <= 0)

  useEffect(() => {
    if (hasStarted && lobby?.joined) {
      navigate(`/battle/group/${huntId}`, { replace: true })
    }
  }, [hasStarted, huntId, lobby?.joined, navigate])

  if (errorMessage) {
    return (
      <main className="battle-route-message-page">
        <div className="battle-route-message-card">
          <p>{errorMessage}</p>
          <button type="button" onClick={() => navigate('/')}>Back</button>
        </div>
      </main>
    )
  }

  if (!lobby) {
    return (
      <main className="battle-route-message-page">
        <div className="battle-route-message-card">
          <p>Opening lobby...</p>
        </div>
      </main>
    )
  }

  return (
    <main className="group-lobby-page" style={{ backgroundImage: `url(${battleArenaImage})` }}>
      <section className="group-lobby-card">
        <p className="group-lobby-kicker">{lobby.huntTitle}</p>
        <h1 className="group-lobby-title">{lobby.beastName}</h1>

        {!isLobbyOpen && (
          <p className="group-lobby-state">
            Lobby opens 10 minutes before the hunt starts.
          </p>
        )}

        {isLobbyOpen && !hasStarted && (
          <>
            <p className="group-lobby-state">Countdown to hunt start</p>
            <p className="group-lobby-timer">{formatCountdown(msUntilStart)}</p>
          </>
        )}

        {hasStarted && (
          <>
            <p className="group-lobby-state">The hunt has started.</p>
            <button
              type="button"
              className="group-lobby-enter-button"
              onClick={() => navigate(`/battle/group/${huntId}`)}
            >
              Enter Battle
            </button>
          </>
        )}

        <div className="group-lobby-party">
          <p>
            <span>Party:</span> {lobby.currentPartySize} / {lobby.maxPartySize ?? '?'}
          </p>
          {lobby.participants?.length > 0 && (
            <ul className="group-lobby-party-list">
              {lobby.participants.map((participant) => (
                <li key={participant.hunterId}>
                  {participant.hunterName} <span>{participant.hunterAppearance}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

      </section>
    </main>
  )
}

export default GroupHuntLobbyPage
