import { useCallback, useEffect, useRef, useState } from 'react'
import { getCurrentWeather } from '../api/weatherApi'

export const WEATHER_REFRESH_INTERVAL_MS = 15 * 60 * 1000

function useCurrentWeather(enabled = true) {
  const [weather, setWeather] = useState(null)
  const [weatherLoading, setWeatherLoading] = useState(false)
  const isMountedRef = useRef(false)

  const refreshWeather = useCallback(async ({ markLoading = true } = {}) => {
    if (!enabled) {
      return null
    }

    if (markLoading && isMountedRef.current) {
      setWeatherLoading(true)
    }

    try {
      const response = await getCurrentWeather()

      if (isMountedRef.current) {
        setWeather(response.data)
      }

      return response.data
    } catch {
      if (isMountedRef.current) {
        setWeather(null)
      }

      return null
    } finally {
      if (markLoading && isMountedRef.current) {
        setWeatherLoading(false)
      }
    }
  }, [enabled])

  useEffect(() => {
    isMountedRef.current = true

    if (!enabled) {
      return () => {
        isMountedRef.current = false
      }
    }

    const initialTimeoutId = window.setTimeout(() => {
      void refreshWeather()
    }, 0)

    const intervalId = window.setInterval(() => {
      void refreshWeather({ markLoading: false })
    }, WEATHER_REFRESH_INTERVAL_MS)

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void refreshWeather({ markLoading: false })
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      isMountedRef.current = false
      window.clearTimeout(initialTimeoutId)
      window.clearInterval(intervalId)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [enabled, refreshWeather])

  return {
    weather: enabled ? weather : null,
    weatherLoading: enabled ? weatherLoading : false,
    refreshWeather,
  }
}

export default useCurrentWeather
