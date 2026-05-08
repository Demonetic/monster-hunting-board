import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import battleArenaImage from '../assets/battle_arena.png'
import characterBard from '../assets/character_bard.png'
import characterHunter from '../assets/character_hunter.png'
import characterKnight from '../assets/character_knight.png'
import characterMage from '../assets/character_mage.png'
import characterPaladin from '../assets/character_paladin.png'
import characterRanger from '../assets/character_ranger.png'
import { getBeastImage } from '../assets/beastVisuals'
import BattleResultOverlay from '../components/BattleResultOverlay'
import BattleSprite from '../components/BattleSprite'
import FloatingCombatText from '../components/FloatingCombatText'
import HPBar from '../components/HPBar'

const hunterImages = {
  BARD: characterBard,
  HUNTER: characterHunter,
  KNIGHT: characterKnight,
  MAGE: characterMage,
  PALADIN: characterPaladin,
  RANGER: characterRanger,
}

const INTRO_DELAY_MS = 850
const IMPACT_DELAY_MS = 480
const TURN_TOTAL_MS = 1280

function buildFloatingEntries(turn) {
  const entries = []

  if (turn.missed) {
    entries.push({ text: 'Miss!', variant: 'status' })
    return entries
  }

  if (turn.damage > 0) {
    entries.push({ text: `-${turn.damage} HP`, variant: 'damage' })
  }

  if (turn.criticalHit) {
    entries.push({ text: 'Critical hit!', variant: 'critical' })
  }

  if (turn.message) {
    entries.push({ text: turn.message, variant: 'status' })
  }

  return entries.slice(0, 2)
}

function BattlePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const battleResult = location.state?.battleResult ?? null
  const weather = battleResult?.weather ?? location.state?.weatherEffect ?? null
  const turns = useMemo(() => battleResult?.turns ?? [], [battleResult])

  const [phase, setPhase] = useState('intro')
  const [playedTurnIndex, setPlayedTurnIndex] = useState(-1)
  const [hunterHp, setHunterHp] = useState(battleResult?.initialHunterHp ?? 0)
  const [beastHp, setBeastHp] = useState(battleResult?.initialBeastHp ?? 0)
  const [actingSide, setActingSide] = useState('')
  const [damagedSide, setDamagedSide] = useState('')
  const [floatingTexts, setFloatingTexts] = useState([])

  const hunterSprite = useMemo(
    () => hunterImages[battleResult?.hunterAppearance] ?? characterHunter,
    [battleResult?.hunterAppearance],
  )

  const beastSprite = useMemo(
    () => getBeastImage(battleResult?.beastType) ?? null,
    [battleResult?.beastType],
  )

  useEffect(() => {
    if (!battleResult) {
      navigate('/', { replace: true })
    }
  }, [battleResult, navigate])

  useEffect(() => {
    if (!battleResult || phase !== 'intro') {
      return undefined
    }

    const timeoutId = window.setTimeout(() => {
      setPhase('playing')
    }, INTRO_DELAY_MS)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [battleResult, phase])

  useEffect(() => {
    if (!battleResult || phase !== 'playing') {
      return undefined
    }

    const nextTurnIndex = playedTurnIndex + 1
    if (nextTurnIndex >= turns.length) {
      const timeoutId = window.setTimeout(() => {
        setPhase('finished')
      }, 420)

      return () => {
        window.clearTimeout(timeoutId)
      }
    }

    const nextTurn = turns[nextTurnIndex]

    const startTimeoutId = window.setTimeout(() => {
      setActingSide(nextTurn.attackerSide)
    }, 0)

    const impactTimeoutId = window.setTimeout(() => {
      setDamagedSide(nextTurn.targetSide)

      if (nextTurn.targetSide === 'hunter') {
        setHunterHp(nextTurn.targetHpAfter)
      }

      if (nextTurn.targetSide === 'beast') {
        setBeastHp(nextTurn.targetHpAfter)
      }

      const nextFloatingTexts = buildFloatingEntries(nextTurn).map((entry, index) => ({
        id: `${nextTurn.turnNumber}-${entry.text}-${index}`,
        side: nextTurn.targetSide,
        text: entry.text,
        variant: entry.variant,
      }))
      setFloatingTexts(nextFloatingTexts)
    }, IMPACT_DELAY_MS)

    const resolveTimeoutId = window.setTimeout(() => {
      setActingSide('')
      setDamagedSide('')
      setFloatingTexts([])
      setPlayedTurnIndex(nextTurnIndex)
    }, TURN_TOTAL_MS)

    return () => {
      window.clearTimeout(startTimeoutId)
      window.clearTimeout(impactTimeoutId)
      window.clearTimeout(resolveTimeoutId)
    }
  }, [battleResult, phase, playedTurnIndex, turns])

  if (!battleResult) {
    return null
  }

  return (
    <main
      className="battle-page"
      style={{ backgroundImage: `url(${battleArenaImage})` }}
    >
      <div className="battle-stage">
        <div className="battle-info-row">
          <HPBar
            side="hunter"
            name={battleResult.hunterName}
            currentHp={hunterHp}
            maxHp={battleResult.initialHunterMaxHp}
          />

          <HPBar
            side="beast"
            name={battleResult.beastType}
            currentHp={beastHp}
            maxHp={battleResult.initialBeastMaxHp}
          />
        </div>

        {weather?.displayName && (
          <div className="battle-weather-badge">
            <strong>{weather.displayName}</strong>
            <span>{weather.activeEffects?.[0] ?? 'No weather effects'}</span>
          </div>
        )}

        {phase === 'intro' && (
          <p className="battle-intro-banner">
            {weather?.displayName ? `Battle begins under ${weather.displayName}` : 'Battle begins...'}
          </p>
        )}

        {floatingTexts.map((entry, index) => (
          <FloatingCombatText
            key={entry.id}
            side={entry.side}
            text={entry.text}
            variant={entry.variant}
            stackIndex={index}
          />
        ))}

        <BattleSprite
          side="hunter"
          name={battleResult.hunterName}
          image={hunterSprite}
          isActing={actingSide === 'hunter'}
          isDamaged={damagedSide === 'hunter'}
        />

        <BattleSprite
          side="beast"
          name={battleResult.beastType}
          image={beastSprite}
          isActing={actingSide === 'beast'}
          isDamaged={damagedSide === 'beast'}
        />
      </div>

      {phase === 'finished' && (
        <BattleResultOverlay
          result={battleResult}
          weatherEffect={weather}
          onContinue={() => navigate('/')}
        />
      )}
    </main>
  )
}

export default BattlePage
