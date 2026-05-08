import { useMemo } from 'react'
import HuntPin from '../components/HuntPin'
import worldMap from '../assets/world_map.png'

const positions = [
  { x: 30, y: 45 },
  { x: 58, y: 55 },
  { x: 78, y: 35 },
  { x: 42, y: 68 },
  { x: 70, y: 72 },
  { x: 18, y: 34 },
  { x: 52, y: 28 },
  { x: 84, y: 58 },
  { x: 26, y: 76 },
  { x: 64, y: 40 },
]

const positionOffsets = [
  { x: 0, y: 0 },
  { x: 8, y: -6 },
  { x: -8, y: 6 },
  { x: 10, y: 8 },
  { x: -10, y: -8 },
  { x: 14, y: 0 },
  { x: -14, y: 0 },
  { x: 0, y: 12 },
  { x: 0, y: -12 },
]

const minPinSpacing = {
  x: 12,
  y: 14,
}

function clampPosition(position) {
  return {
    x: Math.min(88, Math.max(12, position.x)),
    y: Math.min(84, Math.max(25, position.y)),
  }
}

function isTooClose(candidate, placedPositions) {
  return placedPositions.some(
    (position) =>
      Math.abs(position.x - candidate.x) < minPinSpacing.x &&
      Math.abs(position.y - candidate.y) < minPinSpacing.y,
  )
}

function getAvailablePosition(index, placedPositions) {
  for (let positionIndex = 0; positionIndex < positions.length; positionIndex += 1) {
    const basePosition = positions[(index + positionIndex) % positions.length]

    for (const offset of positionOffsets) {
      const candidate = clampPosition({
        x: basePosition.x + offset.x,
        y: basePosition.y + offset.y,
      })

      if (!isTooClose(candidate, placedPositions)) {
        return candidate
      }
    }
  }

  return clampPosition(positions[index % positions.length])
}

function getPinType(hunt) {
  if (hunt.sourceType === 'DAILY_BOSS' || hunt.difficulty === 'BOSS') {
    return 'boss'
  }

  if (hunt.sourceType === 'REPEATABLE') {
    return 'normal'
  }

  if (hunt.sourceType === 'DAILY_BOUNTY' || hunt.sourceType === 'WEEKLY_CONTRACT') {
    return 'daily'
  }

  if (hunt.type === 'SOLO_HUNT') {
    return 'normal'
  }

  return 'daily'
}

function BoardPage({ hunts, loading, error, weather, onSelectHunt }) {
  const mappedHunts = useMemo(
    () => {
      const placedPositions = []

      return hunts.map((hunt, index) => {
        const position = getAvailablePosition(index, placedPositions)
        placedPositions.push(position)

        return {
          ...hunt,
          ...position,
          primaryBeastType: hunt.beasts?.[0]?.type ?? null,
          pinType: getPinType(hunt),
        }
      })
    },
    [hunts],
  )

  return (
    <main
      className="board-page"
      style={{ backgroundImage: `url(${worldMap})` }}
    >
      {weather && (
        <section className="board-weather-card" aria-label="Current weather">
          <strong>{weather.displayName}</strong>
          <span>{weather.city}{weather.country ? `, ${weather.country}` : ''}</span>
          <p>{weather.activeEffects[0] ?? 'No weather effects'}</p>
        </section>
      )}

      {loading && <p className="board-status">Loading hunts...</p>}
      {!loading && error && <p className="board-status board-status-error">{error}</p>}

      {!loading && !error && mappedHunts.map((hunt) => (
        <HuntPin
          key={hunt.id}
          x={hunt.x}
          y={hunt.y}
          type={hunt.pinType}
          beastType={hunt.primaryBeastType}
          onClick={() => onSelectHunt(hunt)}
        />
      ))}
    </main>
  )
}

export default BoardPage
