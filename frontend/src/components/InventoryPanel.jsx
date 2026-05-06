import { useEffect, useMemo, useState } from 'react'
import {
  getCurrentHunter,
  getShop,
  purchaseShopItem,
  activateInventoryItem,
  discardInventoryItem,
  updateAppearance,
} from '../api/hunterApi'
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
  const [shop, setShop] = useState(null)
  const [shopMessage, setShopMessage] = useState('')
  const [purchasingItemType, setPurchasingItemType] = useState('')
  const [inventoryActionItemId, setInventoryActionItemId] = useState(null)

  useEffect(() => {
    if (isGameMaster) {
      return
    }

    let cancelled = false

    const fetchHunter = async () => {
      setLoading(true)
      setError('')

      try {
        const [hunterResponse, shopResponse] = await Promise.all([
          getCurrentHunter(),
          getShop(),
        ])

        if (!cancelled) {
          setHunter(hunterResponse.data)
          setSelectedAppearance(hunterResponse.data.appearance)
          setShop(shopResponse.data)
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

  const handlePurchase = async (itemType) => {
    setShopMessage('')
    setSuccessMessage('')
    setError('')
    setPurchasingItemType(itemType)

    try {
      const response = await purchaseShopItem(itemType)
      setHunter(response.data.hunter)
      setShop((currentShop) => {
        if (!currentShop) {
          return currentShop
        }

        return {
          ...currentShop,
          hunterGold: response.data.remainingGold,
          inventorySize: response.data.inventorySize,
        }
      })
      setShopMessage(response.data.message)
    } catch (purchaseError) {
      setError(
        purchaseError.response?.data?.message ?? 'Could not buy item.',
      )
    } finally {
      setPurchasingItemType('')
    }
  }

  const syncHunterFromInventoryAction = (updatedHunter) => {
    setHunter(updatedHunter)
    setShop((currentShop) => {
      if (!currentShop) {
        return currentShop
      }

      return {
        ...currentShop,
        hunterGold: updatedHunter.gold,
        inventorySize: updatedHunter.inventory.length,
      }
    })
  }

  const handleUseItem = async (item) => {
    setShopMessage('')
    setSuccessMessage('')
    setError('')
    setInventoryActionItemId(item.id)

    try {
      const response = await activateInventoryItem(item.id)
      syncHunterFromInventoryAction(response.data.hunter)
      setShopMessage(response.data.message)
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

    setShopMessage('')
    setSuccessMessage('')
    setError('')
    setInventoryActionItemId(item.id)

    try {
      const response = await discardInventoryItem(item.id)
      syncHunterFromInventoryAction(response.data.hunter)
      setShopMessage(response.data.message)
    } catch (actionError) {
      setError(
        actionError.response?.data?.message ?? 'Could not discard item.',
      )
    } finally {
      setInventoryActionItemId(null)
    }
  }

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
                <p><span>Backpack:</span> {hunter.inventory.length} / {hunter.inventoryCapacity}</p>
                <p><span>EXP Potion:</span> {hunter.expPotionActive ? 'Active for next hunt' : 'Inactive'}</p>
                <p><span>Endurance:</span> {hunter.endurancePotionActive ? 'Active for next hunt' : 'Inactive'}</p>
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
                {shopMessage && (
                  <p className="inventory-success">{shopMessage}</p>
                )}
              </div>

              <div className="inventory-backpack">
                <h3>Backpack</h3>
                <div className="inventory-slot-grid">
                  {backpackSlots.map(({ slotIndex, item }) => (
                    <div
                      key={slotIndex}
                      className={`inventory-slot ${item ? 'is-filled' : 'is-empty'}`}
                    >
                      <span className="inventory-slot-number">{slotIndex + 1}</span>
                      {item ? (
                        <>
                          <strong>{item.displayName}</strong>
                          <p>{item.description}</p>
                          <div className="inventory-slot-actions">
                            <button
                              type="button"
                              className="inventory-slot-button"
                              onClick={() => handleUseItem(item)}
                              disabled={
                                inventoryActionItemId === item.id ||
                                (item.itemType === 'HEALTH_POTION' && hunter.currentHp >= hunter.baseHp) ||
                                (item.itemType === 'EXP_POTION' && hunter.expPotionActive) ||
                                (item.itemType === 'ENDURANCE_POTION' && hunter.endurancePotionActive)
                              }
                            >
                              {inventoryActionItemId === item.id
                                ? 'Working...'
                                : item.itemType === 'HEALTH_POTION'
                                  ? 'Use'
                                  : 'Activate'}
                            </button>
                            <button
                              type="button"
                              className="inventory-slot-button is-danger"
                              onClick={() => handleDiscardItem(item)}
                              disabled={inventoryActionItemId === item.id}
                            >
                              Discard
                            </button>
                          </div>
                        </>
                      ) : (
                        <p>Empty</p>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {shop && (
                <div className="inventory-shop">
                  <div className="inventory-shop-header">
                    <h3>Shop</h3>
                    <p>Gold: {shop.hunterGold}</p>
                  </div>

                  <div className="inventory-shop-list">
                    {shop.items.map((item) => (
                      <div key={item.itemType} className="inventory-shop-item">
                        <div className="inventory-shop-copy">
                          <strong>{item.displayName}</strong>
                          <p>{item.description}</p>
                          <span>{item.price} gold</span>
                        </div>

                        <button
                          type="button"
                          className="inventory-shop-button"
                          onClick={() => handlePurchase(item.itemType)}
                          disabled={
                            purchasingItemType === item.itemType ||
                            hunter.inventory.length >= hunter.inventoryCapacity ||
                            hunter.gold < item.price
                          }
                        >
                          {purchasingItemType === item.itemType ? 'Buying...' : 'Buy'}
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

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
