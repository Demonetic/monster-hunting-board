import { useEffect, useMemo, useState } from 'react'
import { getCurrentHunter, getShop, purchaseShopItem } from '../api/hunterApi'
import buttonBuy from '../assets/button_buy.png'
import buttonClose from '../assets/button_close.png'
import endurancePotionImage from '../assets/endurance_potion.png'
import expPotionImage from '../assets/exp_potion.png'
import healthPotionImage from '../assets/health_potion.png'
import panelImage from '../assets/panel_information.png'

const SHOP_ITEM_ORDER = [
  {
    itemType: 'HEALTH_POTION',
    displayName: 'Health Potion',
    description: 'Restore health before the next fight.',
  },
  {
    itemType: 'EXP_POTION',
    displayName: 'EXP Potion',
    description: 'Boost experience gain for the next hunt.',
  },
  {
    itemType: 'ENDURANCE_POTION',
    displayName: 'Endurance Potion',
    description: 'Reduce damage taken in the next hunt.',
  },
]

const itemTypeImages = {
  HEALTH_POTION: healthPotionImage,
  EXP_POTION: expPotionImage,
  ENDURANCE_POTION: endurancePotionImage,
}

function ShopPanel({ onClose, role }) {
  const isGameMaster = role === 'GAME_MASTER'
  const [hunter, setHunter] = useState(null)
  const [shop, setShop] = useState(null)
  const [loading, setLoading] = useState(!isGameMaster)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [purchasingItemType, setPurchasingItemType] = useState('')

  useEffect(() => {
    if (isGameMaster) {
      setLoading(false)
      setError('')
      return
    }

    let cancelled = false

    const fetchShop = async () => {
      setLoading(true)
      setError('')

      try {
        const [hunterResponse, shopResponse] = await Promise.all([
          getCurrentHunter(),
          getShop(),
        ])

        if (!cancelled) {
          setHunter(hunterResponse.data)
          setShop(shopResponse.data)
        }
      } catch (fetchError) {
        if (!cancelled) {
          setError(
            fetchError.response?.data?.message ?? 'Could not load shop.',
          )
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    fetchShop()

    return () => {
      cancelled = true
    }
  }, [isGameMaster])

  const visibleItems = useMemo(() => {
    const itemsByType = new Map(
      (shop?.items ?? []).map((item) => [item.itemType, item]),
    )

    return SHOP_ITEM_ORDER.map((item) => ({
      ...item,
      ...(itemsByType.get(item.itemType) ?? {}),
    }))
  }, [shop])

  const handlePurchase = async (itemType) => {
    setMessage('')
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
      setMessage(response.data.message)
    } catch (purchaseError) {
      setError(
        purchaseError.response?.data?.message ?? 'Could not buy item.',
      )
    } finally {
      setPurchasingItemType('')
    }
  }

  return (
    <div className="game-overlay-layer">
      <section
        className="hunts-panel shop-panel"
        style={{ backgroundImage: `url(${panelImage})` }}
        role="dialog"
        aria-modal="true"
        aria-label="Shop panel"
      >
        <button
          type="button"
          className="hunts-panel-close"
          onClick={onClose}
          aria-label="Close shop"
        >
          <img src={buttonClose} alt="" />
        </button>

        <div className="hunts-panel-header shop-panel-header">
          <h2 className="hunts-panel-title">Shop</h2>

          {isGameMaster && (
            <p className="shop-panel-state">Shop is only available for hunters.</p>
          )}

          {!isGameMaster && hunter && (
            <p className="shop-panel-summary">
              Your Gold: {hunter.gold}
            </p>
          )}

          {message && <p className="shop-panel-message">{message}</p>}
          {error && <p className="shop-panel-error">{error}</p>}
        </div>

        <div className="hunts-list shop-panel-list">
          {loading && <p className="shop-panel-state">Loading shop...</p>}

          {!loading && !isGameMaster && visibleItems.length > 0 && hunter && (
            <div className="shop-panel-items">
              <div className="shop-panel-items-list">
                {visibleItems.map((item) => (
                  <div key={item.itemType} className="shop-panel-item">
                    <div className="shop-panel-item-icon" aria-hidden="true">
                      <img
                        src={itemTypeImages[item.itemType]}
                        alt=""
                      />
                    </div>

                    <div className="shop-panel-item-copy">
                      <strong>{item.displayName}</strong>
                      <p>{item.description}</p>
                      <span>{item.price ?? '-'} gold</span>
                    </div>

                    <button
                      type="button"
                      className="shop-panel-buy-button"
                      onClick={() => handlePurchase(item.itemType)}
                      aria-label={`Buy ${item.displayName}`}
                      disabled={
                        !item.price ||
                        purchasingItemType === item.itemType ||
                        hunter.inventory.length >= hunter.inventoryCapacity ||
                        hunter.gold < item.price
                      }
                    >
                      <img src={buttonBuy} alt="" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}

export default ShopPanel
