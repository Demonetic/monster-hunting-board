import { useEffect, useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import BattleScene from '../components/BattleScene'

function BattlePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const battleResult = useMemo(() => {
    const stateBattleResult = location.state?.battleResult

    if (!stateBattleResult) {
      return null
    }

    return {
      ...stateBattleResult,
      difficulty: stateBattleResult.difficulty ?? location.state?.difficulty ?? null,
    }
  }, [location.state])

  useEffect(() => {
    if (!battleResult) {
      navigate('/', { replace: true })
    }
  }, [battleResult, navigate])

  if (!battleResult) {
    return null
  }

  return <BattleScene key={JSON.stringify(battleResult)} battleResult={battleResult} onContinue={() => navigate('/')} />
}

export default BattlePage
