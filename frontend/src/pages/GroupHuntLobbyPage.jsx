import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { getBattleArenaBackground, getBattleArenaDifficulty } from '../assets/battleArenaBackgrounds'
import { getGroupLobby } from '../api/huntApi'
import panelBattleResult from '../assets/panel_battle_result.png'
import ChatBox from '../components/ChatBox'

const LOBBY_OPEN_MS = 10 * 60 * 1000

function formatCountdown(msRemaining) {
  const safeMs = Math.max(0, msRemaining)
  const totalSeconds = Math.floor(safeMs / 1000)
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

function GroupHuntLobbyPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { huntId } = useParams()
  const [lobby, setLobby] = useState(null)
  const [now, setNow] = useState(0)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let cancelled = false

    const loadLobby = async () => {
      try {
        const response = await getGroupLobby(huntId)
        if (!cancelled) {
          setLobby({
            ...response.data,
            difficulty: response.data?.difficulty ?? location.state?.difficulty ?? null,
          })
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
  }, [huntId, location.state])

  useEffect(() => {
    const initialTickId = window.setTimeout(() => {
      setNow(Date.now())
    }, 0)

    const tickId = window.setInterval(() => {
      setNow(Date.now())
    }, 1000)

    return () => {
      window.clearTimeout(initialTickId)
      window.clearInterval(tickId)
    }
  }, [])

  const startTimeMs = lobby?.startTime ? new Date(lobby.startTime).getTime() : null
  const arenaBackgroundImage = getBattleArenaBackground(getBattleArenaDifficulty(lobby))
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
    <main className="group-lobby-page" style={{ backgroundImage: `url(${arenaBackgroundImage})` }}>
      <section className="group-lobby-layout">
        <div
          className="group-lobby-panel battle-result-card"
          style={{ backgroundImage: `url(${panelBattleResult})` }}
        >
          <div className="group-lobby-panel-content battle-result-card-content">
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
            </div>
          </div>
        </div>

        {lobby.joined && !hasStarted && (
          <ChatBox
            mode="LOBBY"
            lobbyId={huntId}
            title="Lobby Chat"
            className="lobby-chat-panel"
            disabled={!isLobbyOpen}
          />
        )}
      </section>
    </main>
  )
}

export default GroupHuntLobbyPage
