export function getWeatherTitleLines(displayName) {
  if (!displayName) {
    return []
  }

  return displayName
    .split('/')
    .map((line) => line.trim())
    .filter(Boolean)
}

export function formatWeatherTemperature(temperatureCelsius) {
  if (!Number.isFinite(temperatureCelsius)) {
    return ''
  }

  return `${Math.round(temperatureCelsius)}\u00B0`
}
