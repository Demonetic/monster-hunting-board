import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import BattleScene from '../components/BattleScene'

function BattlePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const battleResult = location.state?.battleResult ?? null

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
