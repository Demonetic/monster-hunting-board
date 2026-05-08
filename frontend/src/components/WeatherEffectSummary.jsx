function WeatherEffectSummary({
  weather,
  loading = false,
  className = '',
  title = 'Weather',
  fallbackText = 'Cloudy / Overcast: No weather effects',
}) {
  if (loading) {
    return (
      <section className={`weather-effect-summary ${className}`.trim()}>
        <span className="weather-effect-summary-label">{title}</span>
        <p className="weather-effect-summary-state">Loading weather...</p>
      </section>
    )
  }

  if (!weather) {
    return (
      <section className={`weather-effect-summary ${className}`.trim()}>
        <span className="weather-effect-summary-label">{title}</span>
        <p className="weather-effect-summary-state">{fallbackText}</p>
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
