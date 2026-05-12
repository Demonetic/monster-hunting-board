import { useMemo } from 'react'
import HuntPin from '../components/HuntPin'
import logoRound from '../assets/logo_round.png'
import weatherPanel from '../assets/weather_panel.png'
import worldMap from '../assets/world_map.png'
import { getHuntPinType } from '../assets/beastVisuals'
import { formatWeatherTemperature, getWeatherTitleLines } from '../constants/weatherPresentation'

const positions = [
  { x: 30, y: 50 },
  { x: 58, y: 60 },
  { x: 78, y: 40 },
  { x: 42, y: 73 },
  { x: 70, y: 77 },
  { x: 18, y: 39 },
  { x: 52, y: 33 },
  { x: 84, y: 63 },
  { x: 26, y: 81 },
  { x: 64, y: 45 },
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

const blockedZones = [
  { minX: 0, maxX: 31, minY: 0, maxY: 48 },
  { minX: 26, maxX: 74, minY: 0, maxY: 22 },
  { minX: 0, maxX: 100, minY: 88, maxY: 100 },
]

function clampPosition(position) {
  return {
    x: Math.min(88, Math.max(12, position.x)),
    y: Math.min(84, Math.max(31, position.y)),
  }
}

function isBlockedZone(position) {
  return blockedZones.some(
    (zone) =>
      position.x >= zone.minX &&
      position.x <= zone.maxX &&
      position.y >= zone.minY &&
      position.y <= zone.maxY,
  )
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

      if (!isBlockedZone(candidate) && !isTooClose(candidate, placedPositions)) {
        return candidate
      }
    }
  }

  const fallbackPosition = clampPosition(positions[index % positions.length])

  if (!isBlockedZone(fallbackPosition)) {
    return fallbackPosition
  }

  return clampPosition({ x: 50, y: 60 })
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
          pinType: getHuntPinType(hunt),
        }
      })
    },
    [hunts],
  )
  const weatherTitleLines = useMemo(
    () => getWeatherTitleLines(weather?.displayName),
    [weather?.displayName],
  )
  const weatherTemperature = useMemo(
    () => formatWeatherTemperature(weather?.temperatureCelsius),
    [weather?.temperatureCelsius],
  )

  return (
    <main
      className="board-page"
      style={{ backgroundImage: `url(${worldMap})` }}
    >
      <img className="board-logo-round" src={logoRound} alt="" aria-hidden="true" />

      {weather && (
        <section
          className="board-weather-card"
          aria-label="Current weather"
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
          <span>{weather.city}{weather.country ? `, ${weather.country}` : ''}</span>
          <p>{weather.activeEffects[0] ?? 'No weather effects'}</p>
        </section>
      )}

      {loading && <p className="board-status">Loading hunts...</p>}
      {!loading && error && <p className="board-status board-status-error">{error}</p>}

      {!loading && !error && mappedHunts.map((hunt) => (
        <HuntPin
          key={hunt.id}
          hunt={hunt}
          x={hunt.x}
          y={hunt.y}
          onClick={() => onSelectHunt(hunt)}
        />
      ))}
    </main>
  )
}

export default BoardPage
