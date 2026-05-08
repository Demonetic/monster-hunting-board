import { useEffect, useMemo, useState } from 'react'
import { getCurrentUsername, getRole } from '../api/authStorage'
import { getCurrentHunter, updateAppearance } from '../api/hunterApi'
import buttonClose from '../assets/button_close.png'
import buttonSave from '../assets/button_save.png'
import panelImage from '../assets/panel_information.png'

const appearanceOptions = ['BARD', 'MAGE', 'RANGER', 'KNIGHT', 'PALADIN', 'HUNTER']

function formatToggleState(value) {
  return value ? 'Active' : 'Inactive'
}

function MenuPanel({ onClose, showToast }) {
  const username = getCurrentUsername()
  const role = getRole()
  const isGameMaster = role === 'GAME_MASTER'
  const [hunter, setHunter] = useState(null)
  const [loading, setLoading] = useState(!isGameMaster)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [selectedAppearance, setSelectedAppearance] = useState('MAGE')
  const [isSavingAppearance, setIsSavingAppearance] = useState(false)

  useEffect(() => {
    if (isGameMaster) {
      setLoading(false)
      setError('')
      return
    }

    let cancelled = false

    const fetchHunter = async () => {
      setLoading(true)
      setError('')

      try {
        const response = await getCurrentHunter()

        if (!cancelled) {
          setHunter(response.data)
          setSelectedAppearance(response.data.appearance ?? 'MAGE')
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
      { label: 'Gold', value: hunter.gold ?? 0 },
      { label: 'EXP', value: hunter.exp ?? 0 },
      { label: 'HP', value: `${hunter.currentHp ?? 0} / ${hunter.baseHp ?? 0}` },
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
                <div className="menu-panel-appearance">
                  <label className="menu-panel-appearance-field">
                    <span>Appearance</span>
                    <select
                      value={selectedAppearance}
                      onChange={(event) => setSelectedAppearance(event.target.value)}
                    >
                      {appearanceOptions.map((appearance) => (
                        <option key={appearance} value={appearance}>
                          {appearance}
                        </option>
                      ))}
                    </select>
                  </label>

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
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  )
}

export default MenuPanel
