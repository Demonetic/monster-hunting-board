import { useEffect, useMemo, useState } from 'react'
import {
  activateInventoryItem,
  discardInventoryItem,
  getCurrentHunter,
} from '../api/hunterApi'
import { getRole } from '../api/authStorage'
import buttonActivate from '../assets/button_activate.png'
import buttonClose from '../assets/button_close.png'
import buttonDiscard from '../assets/button_discard.png'
import buttonUse from '../assets/button_use.png'
import endurancePotionImage from '../assets/endurance_potion.png'
import expPotionImage from '../assets/exp_potion.png'
import healthPotionImage from '../assets/health_potion.png'
import useCurrentWeather from '../hooks/useCurrentWeather'
import inventoryPanelImage from '../assets/inventory_panel.png'
import PlayerSummary from './PlayerSummary'
import { getAppearanceCharacterImage } from '../constants/appearanceVisuals'

const itemTypeImages = {
  HEALTH_POTION: healthPotionImage,
  EXP_POTION: expPotionImage,
  ENDURANCE_POTION: endurancePotionImage,
}

const itemTypeDescriptions = {
  HEALTH_POTION: 'Restore health before the next fight.',
  EXP_POTION: 'Boost experience gain for the next hunt.',
  ENDURANCE_POTION: 'Reduce damage taken in the next hunt.',
}

function InventoryPanel({ onClose }) {
  const role = getRole()
  const isGameMaster = role === 'GAME_MASTER'
  const [hunter, setHunter] = useState(null)
  const [loading, setLoading] = useState(!isGameMaster)
  const [error, setError] = useState('')
  const [inventoryActionItemId, setInventoryActionItemId] = useState(null)
  const [selectedItemId, setSelectedItemId] = useState(null)
  const { weather, weatherLoading } = useCurrentWeather(!isGameMaster)

  useEffect(() => {
    if (isGameMaster) {
      return
    }

    let cancelled = false

    const fetchHunter = async () => {
      setLoading(true)
      setError('')

      try {
        const hunterResponse = await getCurrentHunter()

        if (!cancelled) {
          setHunter(hunterResponse.data)
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

  const characterImage = useMemo(() => {
    if (isGameMaster) {
      return getAppearanceCharacterImage('BARD')
    }

    return getAppearanceCharacterImage(hunter?.appearance)
  }, [hunter?.appearance, isGameMaster])

  const backpackSlots = useMemo(() => {
    const capacity = hunter?.inventoryCapacity ?? 10
    const inventoryBySlot = new Map(
      (hunter?.inventory ?? []).map((item) => [item.slotIndex, item]),
    )

    return Array.from({ length: capacity }, (_, slotIndex) => ({
      slotIndex,
      item: inventoryBySlot.get(slotIndex) ?? null,
    }))
  }, [hunter])

  const selectedItem = useMemo(
    () => (hunter?.inventory ?? []).find((item) => item.id === selectedItemId) ?? null,
    [hunter, selectedItemId],
  )

  const handleUseItem = async (item) => {
    setError('')
    setInventoryActionItemId(item.id)

    try {
      const response = await activateInventoryItem(item.id)
      setHunter(response.data.hunter)
      setSelectedItemId(null)
    } catch (actionError) {
      setError(
        actionError.response?.data?.message ?? 'Could not use item.',
      )
    } finally {
      setInventoryActionItemId(null)
    }
  }

  const handleDiscardItem = async (item) => {
    const confirmed = window.confirm(
      `${item.displayName} will be destroyed with no refund. Continue?`,
    )

    if (!confirmed) {
      return
    }

    setError('')
    setInventoryActionItemId(item.id)

    try {
      const response = await discardInventoryItem(item.id)
      setHunter(response.data.hunter)
      setSelectedItemId(null)
    } catch (actionError) {
      setError(
        actionError.response?.data?.message ?? 'Could not discard item.',
      )
    } finally {
      setInventoryActionItemId(null)
    }
  }

  return (
    <div className="game-overlay-layer">
      <section
        className="inventory-panel"
        style={{ backgroundImage: `url(${inventoryPanelImage})` }}
        role="dialog"
        aria-modal="true"
        aria-label="Inventory panel"
      >
        <button
          type="button"
          className="inventory-panel-close"
          onClick={onClose}
          aria-label="Close inventory"
        >
          <img src={buttonClose} alt="" />
        </button>

        {loading && <p className="inventory-state inventory-state-overlay">Loading hunter...</p>}
        {!loading && error && <p className="inventory-state inventory-error inventory-state-overlay">{error}</p>}

        <div className="inventory-stage">
          {!isGameMaster && (
            <PlayerSummary
              className="inventory-player-summary"
              hunter={hunter}
              weather={weather}
              weatherLoading={weatherLoading}
              compact
            />
          )}

          <div className="inventory-character-stage">
            <img
              className="inventory-character-image"
              src={characterImage}
              alt={isGameMaster ? 'Gamemaster' : hunter?.appearance ?? 'Hunter'}
            />
          </div>

          {!isGameMaster && (
            <>
              <div className="inventory-slot-grid-overlay" aria-label="Inventory slots">
                {backpackSlots.map(({ slotIndex, item }) => {
                  const itemImage = item ? itemTypeImages[item.itemType] ?? null : null

                  return (
                    <button
                      key={slotIndex}
                      type="button"
                      className={`inventory-slot-hitbox ${item ? 'is-filled' : 'is-empty'} ${selectedItem?.id === item?.id ? 'is-selected' : ''}`.trim()}
                      onClick={() => item && setSelectedItemId(item.id)}
                      disabled={!item}
                      aria-label={item ? item.displayName : `Empty slot ${slotIndex + 1}`}
                    >
                      {itemImage && (
                        <img
                          className={`inventory-slot-potion inventory-slot-potion-${item.itemType.toLowerCase()}`}
                          src={itemImage}
                          alt={item.displayName}
                        />
                      )}
                    </button>
                  )
                })}
              </div>

              <div className="inventory-selection-stage">
                {selectedItem && (
                  <>
                    <div className="inventory-selection-preview">
                      <img
                        className="inventory-selection-preview-image"
                        src={itemTypeImages[selectedItem.itemType]}
                        alt={selectedItem.displayName}
                      />
                    </div>

                    <div className="inventory-selection-copy">
                      <strong>{selectedItem.displayName}</strong>
                      <p>{itemTypeDescriptions[selectedItem.itemType] ?? 'A useful hunt item.'}</p>
                    </div>

                    <div className="inventory-selection-actions">
                      <button
                        type="button"
                        className="inventory-slot-button"
                        onClick={() => handleUseItem(selectedItem)}
                        aria-label={
                          selectedItem.itemType === 'HEALTH_POTION' ? 'Use item' : 'Activate item'
                        }
                        disabled={
                          inventoryActionItemId === selectedItem.id ||
                          (selectedItem.itemType === 'HEALTH_POTION' && hunter.currentHp >= hunter.baseHp) ||
                          (selectedItem.itemType === 'EXP_POTION' && hunter.expPotionActive) ||
                          (selectedItem.itemType === 'ENDURANCE_POTION' && hunter.endurancePotionActive)
                        }
                      >
                        <img
                          src={selectedItem.itemType === 'HEALTH_POTION' ? buttonUse : buttonActivate}
                          alt=""
                        />
                      </button>

                      <button
                        type="button"
                        className="inventory-slot-button is-danger"
                        onClick={() => handleDiscardItem(selectedItem)}
                        aria-label="Discard item"
                        disabled={inventoryActionItemId === selectedItem.id}
                      >
                        <img src={buttonDiscard} alt="" />
                      </button>
                    </div>
                  </>
                )}
              </div>
            </>
          )}
        </div>
      </section>
    </div>
  )
}

export default InventoryPanel
