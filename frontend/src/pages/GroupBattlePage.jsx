import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { completeHunt } from '../api/huntApi'
import BattleScene from '../components/BattleScene'

function GroupBattlePage() {
  const navigate = useNavigate()
  const { huntId } = useParams()
  const [battleResult, setBattleResult] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      try {
        const response = await completeHunt(huntId, true)
        if (!cancelled) {
          setBattleResult(response.data)
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error.response?.data?.message ?? 'Could not start group battle')
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [huntId])

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

  if (!battleResult) {
    return (
      <main className="battle-route-message-page">
        <div className="battle-route-message-card">
          <p>Preparing group battle...</p>
        </div>
      </main>
    )
  }

  return <BattleScene key={JSON.stringify(battleResult)} battleResult={battleResult} onContinue={() => navigate('/')} />
}

export default GroupBattlePage
