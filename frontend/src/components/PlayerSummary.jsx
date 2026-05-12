import PassiveSkillSummary from './PassiveSkillSummary'
import WeatherEffectSummary from './WeatherEffectSummary'

function PlayerSummary({
  hunter,
  weather,
  weatherLoading = false,
  className = '',
  compact = false,
}) {
  if (!hunter) {
    return null
  }

  return (
    <section className={`player-summary ${compact ? 'is-compact' : ''} ${className}`.trim()}>
      <div className={`player-summary-header ${compact ? 'is-compact' : ''}`.trim()}>
        <strong className="player-summary-name">{hunter.displayName ?? 'Hunter'}</strong>
        <span className="player-summary-level">Level {hunter.level ?? 1}</span>
        <span className="player-summary-hp">
          {hunter.currentHp ?? 0}/{hunter.baseHp ?? 0} HP
        </span>
      </div>

      <PassiveSkillSummary
        className="player-summary-passive"
        appearanceName={hunter.appearanceDisplayName ?? hunter.appearance}
        passiveSkillName={hunter.passiveSkillName}
        passiveSkillDescription={hunter.passiveSkillDescription}
        compact={compact}
      />

      <WeatherEffectSummary
        className="player-summary-weather"
        weather={weather}
        loading={weatherLoading}
        compact={compact}
      />
    </section>
  )
}

export default PlayerSummary
