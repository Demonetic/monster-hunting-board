import { useEffect, useMemo, useState } from 'react'
import { getCurrentHunter, updateAppearance } from '../api/hunterApi'
import { getRole } from '../api/authStorage'
import panelImage from '../assets/panel_new.png'
import buttonImage from '../assets/button_new.png'
import bardImage from '../assets/appearance_bard.png'
import mageImage from '../assets/appearance_mage.png'
import rangerImage from '../assets/appearance_ranger.png'
import knightImage from '../assets/appearance_knight.png'
import paladinImage from '../assets/appearance_paladin.png'
import hunterImage from '../assets/appearance_hunter.png'

const appearanceImages = {
  MAGE: mageImage,
  RANGER: rangerImage,
  KNIGHT: knightImage,
  PALADIN: paladinImage,
  HUNTER: hunterImage,
}

const appearanceOptions = ['MAGE', 'RANGER', 'KNIGHT', 'PALADIN', 'HUNTER']

function InventoryPanel({ onClose }) {
  const role = getRole()
  const isGameMaster = role === 'GAME_MASTER'
  const [hunter, setHunter] = useState(null)
  const [loading, setLoading] = useState(!isGameMaster)
  const [error, setError] = useState('')
  const [selectedAppearance, setSelectedAppearance] = useState('MAGE')
  const [successMessage, setSuccessMessage] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    if (isGameMaster) {
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
          setSelectedAppearance(response.data.appearance)
        }
      } catch (fetchError) {
        if (!cancelled) {
          setError(
            fetchError.response?.data?.message ?? 'Could not load hunter.',
          )
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

  const previewImage = useMemo(
    () => appearanceImages[selectedAppearance] ?? mageImage,
    [selectedAppearance],
  )

  const handleSaveAppearance = async () => {
    setSuccessMessage('')
    setError('')
    setIsSaving(true)

    try {
      const response = await updateAppearance(selectedAppearance)
      setHunter(response.data)
      setSelectedAppearance(response.data.appearance)
      setSuccessMessage('Appearance updated.')
    } catch (saveError) {
      setError(
        saveError.response?.data?.message ?? 'Could not update appearance.',
      )
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="game-overlay-layer">
      <section
        className="inventory-panel"
        style={{ backgroundImage: `url(${panelImage})` }}
        role="dialog"
        aria-modal="true"
        aria-label="Inventory panel"
      >
        <button
          type="button"
          className="inventory-panel-close"
          onClick={onClose}
        >
          <img src={buttonImage} alt="" />
          <span>Close</span>
        </button>

        {loading && <p className="inventory-state">Loading hunter...</p>}
        {!loading && error && <p className="inventory-state inventory-error">{error}</p>}

        {!loading && isGameMaster && (
          <div className="inventory-content inventory-content-game-master">
            <div className="inventory-visual">
              <img
                className="inventory-character-image"
                src={bardImage}
                alt="Game Master"
              />
            </div>

            <div className="inventory-details inventory-details-game-master">
              <div className="inventory-stats">
                <h2 className="inventory-title">GAMEMASTER</h2>
                <p><span>Role:</span> Guild Master</p>
                <p><span>Rank:</span> Hunt Keeper</p>
                <p><span>Duty:</span> Create and manage hunts</p>
                <p><span>Specialty:</span> Monster records and rewards</p>
                <p><span>Status:</span> Watching over the board</p>
              </div>
            </div>
          </div>
        )}

        {!loading && hunter && (
          <div className="inventory-content">
            <div className="inventory-visual">
              <img
                className="inventory-character-image"
                src={previewImage}
                alt={selectedAppearance}
              />
            </div>

            <div className="inventory-details">
              <div className="inventory-stats">
                <h2 className="inventory-title">{hunter.displayName}</h2>
                <p><span>Appearance:</span> {hunter.appearance}</p>
                <p><span>Level:</span> {hunter.level}</p>
                <p><span>EXP:</span> {hunter.exp}</p>
                <p><span>Gold:</span> {hunter.gold}</p>
                <p><span>Base HP:</span> {hunter.baseHp}</p>
                <p><span>Current HP:</span> {hunter.currentHp}</p>
              </div>

              <div className="inventory-editor">
                <h3>Edit Appearance</h3>
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
              </div>

              <div className="inventory-feedback">
                {successMessage && (
                  <p className="inventory-success">{successMessage}</p>
                )}
              </div>

              <div className="inventory-actions">
                <button
                  type="button"
                  className="inventory-save-button"
                  onClick={handleSaveAppearance}
                  disabled={isSaving}
                >
                  <img src={buttonImage} alt="" />
                  <span>{isSaving ? 'Saving' : 'Save'}</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}

export default InventoryPanel
