import { useEffect, useMemo, useState } from 'react'
import { getAppearanceOptions } from '../api/authApi'
import { getCurrentUsername, getRole } from '../api/authStorage'
import { getCurrentHunter, updateAppearance, updateLocation } from '../api/hunterApi'
import buttonClose from '../assets/button_close.png'
import buttonSave from '../assets/button_save.png'
import useCurrentWeather from '../hooks/useCurrentWeather'
import panelImage from '../assets/panel_information.png'
import AppearanceOptionSelector from './AppearanceOptionSelector'
import PassiveSkillSummary from './PassiveSkillSummary'
import WeatherEffectSummary from './WeatherEffectSummary'

function formatToggleState(value) {
  return value ? 'Active' : 'Inactive'
}

function MenuPanel({ onClose, showToast, onLocationUpdated }) {
  const username = getCurrentUsername()
  const role = getRole()
  const isGameMaster = role === 'GAME_MASTER'
  const [hunter, setHunter] = useState(null)
  const [loading, setLoading] = useState(() => !isGameMaster)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [selectedAppearance, setSelectedAppearance] = useState('MAGE')
  const [isSavingAppearance, setIsSavingAppearance] = useState(false)
  const [selectedCity, setSelectedCity] = useState('Stockholm')
  const [isSavingLocation, setIsSavingLocation] = useState(false)
  const [appearanceOptions, setAppearanceOptions] = useState([])
  const [previewAppearance, setPreviewAppearance] = useState('MAGE')
  const { weather, refreshWeather } = useCurrentWeather(!isGameMaster)

  useEffect(() => {
    if (isGameMaster) {
      return undefined
    }

    let cancelled = false

    const fetchHunter = async () => {
      setLoading(true)
      setError('')

      try {
        const [hunterResponse, appearanceOptionsResponse] = await Promise.allSettled([
          getCurrentHunter(),
          getAppearanceOptions(),
        ])

        if (!cancelled) {
          if (hunterResponse.status !== 'fulfilled') {
            throw hunterResponse.reason
          }

          const hunterData = hunterResponse.value.data
          setHunter(hunterData)
          setSelectedAppearance(hunterData.appearance ?? 'MAGE')
          setPreviewAppearance(hunterData.appearance ?? 'MAGE')
          setSelectedCity(hunterData.city ?? 'Stockholm')
          setAppearanceOptions(
            appearanceOptionsResponse.status === 'fulfilled'
              ? appearanceOptionsResponse.value.data ?? []
              : [],
          )
        }
      } catch (fetchError) {
        if (!cancelled) {
          setError(fetchError.response?.data?.message ?? 'Could not load player menu.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    fetchHunter()

    return () => {
      cancelled = true
    }
  }, [isGameMaster])

  const selectedAppearanceOption = useMemo(
    () => appearanceOptions.find((option) => option.appearance === previewAppearance)
      ?? appearanceOptions.find((option) => option.appearance === selectedAppearance)
      ?? null,
    [appearanceOptions, previewAppearance, selectedAppearance],
  )

  const infoRows = useMemo(() => {
    if (isGameMaster) {
      return [
        { label: 'Username', value: username || '-' },
        { label: 'Role', value: 'Game Master' },
      ]
    }

    if (!hunter) {
      return []
    }

    return [
      { label: 'Name', value: (hunter.displayName ?? hunter.name ?? username) || '-' },
      { label: 'Username', value: username || '-' },
      { label: 'Role', value: role === 'HUNTER' ? 'Hunter' : role || '-' },
      { label: 'Level', value: hunter.level ?? '-' },
      { label: 'Appearance', value: hunter.appearanceDisplayName ?? hunter.appearance ?? '-' },
      { label: 'Gold', value: hunter.gold ?? 0 },
      { label: 'EXP', value: hunter.exp ?? 0 },
      { label: 'HP', value: `${hunter.currentHp ?? 0} / ${hunter.baseHp ?? 0}` },
      { label: 'City', value: hunter.city ?? 'Stockholm' },
      {
        label: 'Inventory',
        value: `${hunter.inventory?.length ?? 0} / ${hunter.inventoryCapacity ?? 0}`,
      },
      { label: 'EXP Potion', value: formatToggleState(hunter.expPotionActive) },
      {
        label: 'Endurance Potion',
        value: formatToggleState(hunter.endurancePotionActive),
      },
    ]
  }, [hunter, isGameMaster, role, username])

  const handleSaveAppearance = async () => {
    if (!hunter || selectedAppearance === hunter.appearance || isSavingAppearance) {
      return
    }

    setMessage('')
    setError('')
    setIsSavingAppearance(true)

    try {
      const response = await updateAppearance(selectedAppearance)
      setHunter(response.data)
      setSelectedAppearance(response.data.appearance ?? selectedAppearance)
      setPreviewAppearance(response.data.appearance ?? selectedAppearance)
      setMessage('Appearance updated')
      showToast?.('Appearance updated', 'success')
    } catch (saveError) {
      const nextError = saveError.response?.data?.message ?? 'Could not update appearance.'
      setError(nextError)
      showToast?.(nextError, 'error')
    } finally {
      setIsSavingAppearance(false)
    }
  }

  const handleSaveLocation = async () => {
    if (!hunter || isSavingLocation || selectedCity.trim() === (hunter.city ?? '').trim()) {
      return
    }

    setMessage('')
    setError('')
    setIsSavingLocation(true)

    try {
      const response = await updateLocation(selectedCity)
      setHunter(response.data)
      setSelectedCity(response.data.city ?? selectedCity)
      await refreshWeather()
      setMessage('Location updated')
      showToast?.('Location updated', 'success')
      onLocationUpdated?.()
    } catch (saveError) {
      const nextError = saveError.response?.data?.message ?? 'Could not update location.'
      setError(nextError)
      showToast?.(nextError, 'error')
    } finally {
      setIsSavingLocation(false)
    }
  }

  return (
    <div className="game-overlay-layer">
      <section
        className="hunts-panel menu-panel"
        style={{ backgroundImage: `url(${panelImage})` }}
        role="dialog"
        aria-modal="true"
        aria-label="Player menu"
      >
        <button
          type="button"
          className="hunts-panel-close"
          onClick={onClose}
          aria-label="Close player menu"
        >
          <img src={buttonClose} alt="" />
        </button>

        <div className="hunts-panel-header menu-panel-header">
          <h2 className="hunts-panel-title">Menu</h2>
          {message && <p className="menu-panel-message">{message}</p>}
          {error && <p className="menu-panel-error">{error}</p>}
          {loading && <p className="menu-panel-state">Loading player info...</p>}
          {isGameMaster && !loading && (
            <p className="menu-panel-state">Hunter stats are only available for hunters.</p>
          )}
        </div>

        {!loading && (
          <div className="hunts-list menu-panel-list">
            <div className="menu-panel-content">
              <div className="menu-panel-info-grid">
                {infoRows.map((row) => (
                  <div key={row.label} className="menu-panel-info-row">
                    <span className="menu-panel-info-label">{row.label}</span>
                    <span className="menu-panel-info-value">{row.value}</span>
                  </div>
                ))}
              </div>

              {!isGameMaster && hunter && (
                <>
                  <WeatherEffectSummary className="menu-panel-weather" weather={weather} />

                  <PassiveSkillSummary
                    className="menu-panel-passive"
                    appearanceName={hunter.appearanceDisplayName ?? hunter.appearance}
                    passiveSkillName={hunter.passiveSkillName}
                    passiveSkillDescription={hunter.passiveSkillDescription}
                    title="Current passive"
                  />

                  <div className="menu-panel-settings-grid">
                    <div className="menu-panel-setting-block">
                      <div className="menu-panel-setting-heading">Change appearance</div>
                      <AppearanceOptionSelector
                        className="menu-panel-appearance-selector"
                        options={appearanceOptions}
                        value={selectedAppearance}
                        previewValue={previewAppearance}
                        onChange={(appearance) => {
                          setSelectedAppearance(appearance)
                          setPreviewAppearance(appearance)
                        }}
                        onPreviewChange={setPreviewAppearance}
                      />

                      {selectedAppearanceOption && (
                        <PassiveSkillSummary
                          className="menu-panel-passive menu-panel-passive-preview"
                          appearanceName={selectedAppearanceOption.displayName}
                          passiveSkillName={selectedAppearanceOption.passiveSkillName}
                          passiveSkillDescription={selectedAppearanceOption.passiveSkillDescription}
                          title="Selected passive"
                        />
                      )}

                      <button
                        type="button"
                        className="menu-panel-save-button"
                        onClick={handleSaveAppearance}
                        disabled={isSavingAppearance || selectedAppearance === hunter.appearance}
                        aria-label="Save appearance"
                      >
                        <img src={buttonSave} alt="" />
                      </button>
                    </div>

                    <div className="menu-panel-appearance">
                      <label className="menu-panel-appearance-field">
                        <span>Location</span>
                        <input
                          value={selectedCity}
                          onChange={(event) => setSelectedCity(event.target.value)}
                          placeholder="Stockholm"
                        />
                      </label>

                      <button
                        type="button"
                        className="menu-panel-save-button"
                        onClick={handleSaveLocation}
                        disabled={isSavingLocation || selectedCity.trim() === (hunter.city ?? '').trim()}
                        aria-label="Save location"
                      >
                        <img src={buttonSave} alt="" />
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  )
}

export default MenuPanel
