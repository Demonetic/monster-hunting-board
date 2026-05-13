import { useEffect, useMemo, useRef, useState } from 'react'
import characterBard from '../assets/character_bard.png'
import characterHunter from '../assets/character_hunter.png'
import characterKnight from '../assets/character_knight.png'
import characterMage from '../assets/character_mage.png'
import characterPaladin from '../assets/character_paladin.png'
import characterRanger from '../assets/character_ranger.png'
import { getBattleArenaBackground, getBattleArenaDifficulty } from '../assets/battleArenaBackgrounds'
import { getBeastImage } from '../assets/beastVisuals'
import { formatWeatherTemperature, getWeatherTitleLines } from '../constants/weatherPresentation'
import weatherPanel from '../assets/weather_panel.png'
import BattleCombatant from './BattleCombatant'
import BattleResultOverlay from './BattleResultOverlay'

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
    const normalizedMessage = turn.message.toLowerCase()
    entries.push({
      text: turn.message,
      variant: normalizedMessage.includes('takes damage') ? 'damage' : 'status',
    })
  }

  return entries.slice(0, 2)
}

function getCombatantIdFromTurnValue(value, side, fallbackName, currentHunterId) {
  if (value) {
    return value
  }

  if (side === 'beast') {
    return 'beast'
  }

  if (side === 'hunter' && currentHunterId) {
    return `hunter-${currentHunterId}`
  }

  if (side === 'hunter' && fallbackName) {
    return `hunter-name-${fallbackName}`
  }

  return side ?? 'combatant'
}

function buildInitialHunterCombatants(battleResult) {
  const participants = battleResult?.battleParticipants

  if (Array.isArray(participants) && participants.length > 0) {
    return participants.map((participant) => ({
      id: `hunter-${participant.hunterId}`,
      name: participant.hunterName,
      image: hunterImages[participant.hunterAppearance] ?? characterHunter,
      currentHp: participant.initialHp,
      maxHp: participant.initialMaxHp,
      side: 'hunter',
      isCurrentHunter: participant.hunterId === battleResult.currentHunterId,
    }))
  }

  return [{
    id: `hunter-${battleResult.currentHunterId ?? 'current'}`,
    name: battleResult.hunterName,
    image: hunterImages[battleResult.hunterAppearance] ?? characterHunter,
    currentHp: battleResult.initialHunterHp,
    maxHp: battleResult.initialHunterMaxHp,
    side: 'hunter',
    isCurrentHunter: true,
  }]
}

function getHunterPlacementStyle(index, count) {
  if (count <= 1) {
    return {}
  }

  if (count <= 5) {
    const verticalOffsets = [10, 21, 2, 30, -6]
    const horizontalOffsets = ['0px', '34px', '68px', '18px', '88px']
    const verticalOffset = verticalOffsets[index] ?? index * 7

    return {
      bottom: `calc(10% + ${verticalOffset}%)`,
      left: `calc(5% + ${horizontalOffsets[index] ?? `${index * 24}px`})`,
      zIndex: 100 - verticalOffset,
    }
  }

  const safeCount = Math.min(count, 10)
  const step = safeCount >= 9 ? 4.5 : 5.1
  const bottom = 5 + index * step
  const left = 1.5 + (index % 2) * 2.1 + Math.floor(index / 2) * 0.55
  const spriteWidth = 'min(136px, 10.5vw)'
  const uiWidth = '122px'
  const uiOffset = safeCount >= 9 ? '74%' : '72%'
  const uiBottom = safeCount >= 9 ? '46%' : '48%'

  return {
    bottom: `${bottom}%`,
    left: `${left}%`,
    zIndex: 200 - index,
    '--hunter-sprite-width': spriteWidth,
    '--hunter-ui-width': uiWidth,
    '--hunter-ui-offset': uiOffset,
    '--hunter-ui-bottom': uiBottom,
  }
}

function buildInitialDefeatedCombatants(initialCombatants) {
  const defeatedCombatants = {}

  if ((initialCombatants?.beast?.currentHp ?? 0) <= 0) {
    defeatedCombatants.beast = true
  }

  for (const hunter of initialCombatants?.hunters ?? []) {
    if ((hunter.currentHp ?? 0) <= 0) {
      defeatedCombatants[hunter.id] = true
    }
  }

  return defeatedCombatants
}

function BattleScene({ battleResult, onContinue }) {
  const weather = battleResult?.weather ?? null
  const turns = useMemo(() => battleResult?.turns ?? [], [battleResult])
  const arenaBackgroundImage = useMemo(
    () => getBattleArenaBackground(getBattleArenaDifficulty(battleResult)),
    [battleResult],
  )
  const beastLabel = battleResult?.beastName ?? battleResult?.beastType ?? 'Unknown Beast'
  const beastSprite = useMemo(
    () => getBeastImage({ type: battleResult?.beastType, imageKey: battleResult?.beastImageKey }),
    [battleResult?.beastImageKey, battleResult?.beastType],
  )
  const initialHunters = useMemo(() => buildInitialHunterCombatants(battleResult), [battleResult])
  const initialCombatants = useMemo(() => ({
    beast: {
      id: 'beast',
      name: beastLabel,
      image: beastSprite,
      currentHp: battleResult?.initialBeastHp ?? 0,
      maxHp: battleResult?.initialBeastMaxHp ?? 0,
      side: 'beast',
    },
    hunters: initialHunters,
  }), [battleResult, beastLabel, beastSprite, initialHunters])
  const isGroupBattle = initialHunters.length > 1
  const weatherTitleLines = useMemo(
    () => getWeatherTitleLines(weather?.displayName),
    [weather?.displayName],
  )
  const weatherTemperature = useMemo(
    () => formatWeatherTemperature(weather?.temperatureCelsius),
    [weather?.temperatureCelsius],
  )

  const [phase, setPhase] = useState('intro')
  const [playedTurnIndex, setPlayedTurnIndex] = useState(-1)
  const [hunters, setHunters] = useState(initialCombatants.hunters)
  const [beastHp, setBeastHp] = useState(initialCombatants.beast.currentHp)
  const [actingCombatantId, setActingCombatantId] = useState('')
  const [damagedCombatantId, setDamagedCombatantId] = useState('')
  const [floatingTextsByCombatant, setFloatingTextsByCombatant] = useState({})
  const [defeatedCombatants, setDefeatedCombatants] = useState(() =>
    buildInitialDefeatedCombatants(initialCombatants),
  )
  const defeatedCombatantsRef = useRef(defeatedCombatants)

  useEffect(() => {
    defeatedCombatantsRef.current = defeatedCombatants
  }, [defeatedCombatants])

  const markCombatantDefeated = (combatantId) => {
    if (!combatantId) {
      return
    }

    setDefeatedCombatants((current) => (
      current[combatantId]
        ? current
        : { ...current, [combatantId]: true }
    ))
  }

  useEffect(() => {
    if (!battleResult || phase !== 'intro') {
      return undefined
    }

    const timeoutId = window.setTimeout(() => {
      setPhase('playing')
    }, INTRO_DELAY_MS)

    return () => window.clearTimeout(timeoutId)
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

      return () => window.clearTimeout(timeoutId)
    }

    const nextTurn = turns[nextTurnIndex]
    const attackerId = getCombatantIdFromTurnValue(
      nextTurn.attackerCombatantId,
      nextTurn.attackerSide,
      nextTurn.attacker,
      battleResult.currentHunterId,
    )
    const targetId = getCombatantIdFromTurnValue(
      nextTurn.targetCombatantId,
      nextTurn.targetSide,
      nextTurn.target,
      battleResult.currentHunterId,
    )

    const startTimeoutId = window.setTimeout(() => {
      if (!defeatedCombatantsRef.current[attackerId]) {
        setActingCombatantId(attackerId)
      }
    }, 0)

    const impactTimeoutId = window.setTimeout(() => {
      setDamagedCombatantId(targetId)

      if (targetId === 'beast') {
        setBeastHp(nextTurn.targetHpAfter)
        if (nextTurn.targetHpAfter <= 0) {
          markCombatantDefeated(targetId)
        }
      } else if (targetId.startsWith('hunter-')) {
        setHunters((current) => current.map((hunter) => (
          hunter.id === targetId
            ? { ...hunter, currentHp: nextTurn.targetHpAfter }
            : hunter
        )))
        if (nextTurn.targetHpAfter <= 0) {
          markCombatantDefeated(targetId)
        }
      }

      const nextFloatingTexts = buildFloatingEntries(nextTurn).map((entry, index) => ({
        id: `${nextTurn.turnNumber}-${targetId}-${entry.text}-${index}`,
        text: entry.text,
        variant: entry.variant,
      }))

      setFloatingTextsByCombatant(nextFloatingTexts.length > 0 ? { [targetId]: nextFloatingTexts } : {})
    }, IMPACT_DELAY_MS)

    const resolveTimeoutId = window.setTimeout(() => {
      setActingCombatantId('')
      setDamagedCombatantId('')
      setFloatingTextsByCombatant({})
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
      className={`battle-page ${isGroupBattle ? 'is-group-battle' : ''} ${initialHunters.length >= 6 ? 'is-large-group-battle' : ''}`.trim()}
      style={{ backgroundImage: `url(${arenaBackgroundImage})` }}
    >
      <div className="battle-stage">
        {!isGroupBattle && weather?.displayName && (
          <div
            className="battle-weather-badge"
            style={{ backgroundImage: `url(${weatherPanel})` }}
          >
            {weatherTemperature && (
              <span className="weather-panel-temperature" aria-hidden="true">
                {weatherTemperature}
              </span>
            )}
            <strong className="weather-panel-title">
              {weatherTitleLines.map((line) => (
                <span key={line}>{line}</span>
              ))}
            </strong>
            <span>{weather.activeEffects?.[0] ?? 'No weather effects'}</span>
          </div>
        )}

        {phase === 'intro' && (
          <p className="battle-intro-banner">
            {!isGroupBattle && weather?.displayName
              ? `Battle begins under ${weather.displayName}`
              : 'Battle begins...'}
          </p>
        )}

        <div className="battle-hunter-group">
          {hunters.map((hunter, index) => (
            <BattleCombatant
              key={hunter.id}
              side="hunter"
              className={hunter.isCurrentHunter ? 'is-current-hunter' : ''}
              style={getHunterPlacementStyle(index, hunters.length)}
              name={hunter.name}
              image={hunter.image}
              currentHp={hunter.currentHp}
              maxHp={hunter.maxHp}
              isDefeated={Boolean(defeatedCombatants[hunter.id])}
              isActing={actingCombatantId === hunter.id && !defeatedCombatants[hunter.id]}
              isDamaged={damagedCombatantId === hunter.id}
              floatingTexts={floatingTextsByCombatant[hunter.id] ?? []}
            />
          ))}
        </div>

        <BattleCombatant
          side="beast"
          name={initialCombatants.beast.name}
          image={initialCombatants.beast.image}
          currentHp={beastHp}
          maxHp={initialCombatants.beast.maxHp}
          isDefeated={Boolean(defeatedCombatants.beast)}
          isActing={actingCombatantId === 'beast' && !defeatedCombatants.beast}
          isDamaged={damagedCombatantId === 'beast'}
          floatingTexts={floatingTextsByCombatant.beast ?? []}
        />
      </div>

      {phase === 'finished' && (
        <BattleResultOverlay
          result={battleResult}
          onContinue={onContinue}
        />
      )}
    </main>
  )
}

export default BattleScene
