function WeatherEffectSummary({
  weather,
  loading = false,
  className = '',
  title = 'Weather',
  fallbackText = 'Cloudy / Overcast: No weather effects',
  compact = false,
}) {
  if (loading) {
    return (
      <section className={`weather-effect-summary ${compact ? 'is-compact' : ''} ${className}`.trim()}>
        <span className="weather-effect-summary-label">{compact ? `${title}:` : title}</span>
        <p className="weather-effect-summary-state">Loading weather...</p>
      </section>
    )
  }

  if (!weather) {
    return (
      <section className={`weather-effect-summary ${compact ? 'is-compact' : ''} ${className}`.trim()}>
        <span className="weather-effect-summary-label">{compact ? `${title}:` : title}</span>
        <p className="weather-effect-summary-state">{fallbackText}</p>
      </section>
    )
  }

  if (compact) {
    const compactEffects = weather.activeEffects?.length > 0
      ? weather.activeEffects.join(' • ')
      : 'No weather effects'

    return (
      <section className={`weather-effect-summary is-compact ${className}`.trim()}>
        <p className="weather-effect-summary-inline">
          <span className="weather-effect-summary-label">{title}:</span>{' '}
          <strong className="weather-effect-summary-heading">
            {weather.city}, {weather.displayName}
          </strong>
          {weather.country && (
            <>
              <span className="weather-effect-summary-separator"> • </span>
              <span className="weather-effect-summary-subtle">{weather.country}</span>
            </>
          )}
        </p>
        <p className="weather-effect-summary-state">{compactEffects}</p>
      </section>
    )
  }

  return (
    <section className={`weather-effect-summary ${className}`.trim()}>
      <span className="weather-effect-summary-label">{title}</span>
      <strong className="weather-effect-summary-heading">
        {weather.city}: {weather.displayName}
      </strong>
      {weather.country && <span className="weather-effect-summary-subtle">{weather.country}</span>}
      {weather.activeEffects?.length > 0 ? (
        <ul className="weather-effect-summary-list">
          {weather.activeEffects.map((effect) => (
            <li key={effect}>{effect}</li>
          ))}
        </ul>
      ) : (
        <p className="weather-effect-summary-state">No weather effects</p>
      )}
    </section>
  )
}

export default WeatherEffectSummary
