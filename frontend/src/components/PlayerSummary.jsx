import PassiveSkillSummary from './PassiveSkillSummary'
import WeatherEffectSummary from './WeatherEffectSummary'

function PlayerSummary({ hunter, weather, weatherLoading = false, className = '' }) {
  if (!hunter) {
    return null
  }

  return (
    <section className={`player-summary ${className}`.trim()}>
      <div className="player-summary-header">
        <strong className="player-summary-name">{hunter.displayName ?? 'Hunter'}</strong>
        <span className="player-summary-level">Level {hunter.level ?? 1}</span>
      </div>

      <PassiveSkillSummary
        className="player-summary-passive"
        appearanceName={hunter.appearanceDisplayName ?? hunter.appearance}
        passiveSkillName={hunter.passiveSkillName}
        passiveSkillDescription={hunter.passiveSkillDescription}
      />

      <WeatherEffectSummary
        className="player-summary-weather"
        weather={weather}
        loading={weatherLoading}
      />
    </section>
  )
}

export default PlayerSummary
