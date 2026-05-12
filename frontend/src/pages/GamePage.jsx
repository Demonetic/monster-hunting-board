import { useEffect, useState } from 'react'
import { clearRole, clearToken, getRole, isAuthenticated } from '../api/authStorage'
import { getAllHunts } from '../api/huntApi'
import AuthModal from '../components/AuthModal'
import BottomNav from '../components/BottomNav'
import HuntModal from '../components/HuntModal'
import HuntsPanel from '../components/HuntsPanel'
import InventoryPanel from '../components/InventoryPanel'
import ManagePanel from '../components/ManagePanel'
import MenuPanel from '../components/MenuPanel'
import ShopPanel from '../components/ShopPanel'
import Toast from '../components/Toast'
import useCurrentWeather from '../hooks/useCurrentWeather'
import titleImage from '../assets/monster_hunter_board.png'
import BoardPage from './BoardPage'

function GamePage() {
  const [authenticated, setAuthenticated] = useState(isAuthenticated())
  const [role, setRole] = useState(getRole())
  const [activeOverlay, setActiveOverlay] = useState(null)
  const [hunts, setHunts] = useState([])
  const [huntsLoading, setHuntsLoading] = useState(false)
  const [huntsError, setHuntsError] = useState('')
  const [selectedHuntId, setSelectedHuntId] = useState(null)
  const [toast, setToast] = useState(null)
  const {
    weather,
    refreshWeather,
  } = useCurrentWeather(authenticated && role === 'HUNTER')

  const selectedHunt = selectedHuntId === null
    ? null
    : hunts.find((hunt) => hunt.id === selectedHuntId) ?? null

  const showToast = (message, type) => {
    setToast({
      id: Date.now(),
      message,
      type,
    })
  }

  const fetchHunts = async () => {
    setHuntsLoading(true)
    setHuntsError('')

    try {
      const response = await getAllHunts()
      setHunts(response.data)
    } catch (error) {
      setHuntsError(error.response?.data?.message ?? 'Could not load hunts.')
    } finally {
      setHuntsLoading(false)
    }
  }

  useEffect(() => {
    if (!authenticated) {
      return
    }

    let cancelled = false

    ;(async () => {
      setHuntsLoading(true)
      setHuntsError('')

      try {
        const response = await getAllHunts()

        if (!cancelled) {
          setHunts(response.data)
        }
      } catch (error) {
        if (!cancelled) {
          setHuntsError(
            error.response?.data?.message ?? 'Could not load hunts.',
          )
        }
      } finally {
        if (!cancelled) {
          setHuntsLoading(false)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [authenticated])

  const handleAuthenticated = () => {
    setAuthenticated(true)
    setRole(getRole())
  }

  const handleLogout = () => {
    clearRole()
    clearToken()
    setActiveOverlay(null)
    setSelectedHuntId(null)
    setAuthenticated(false)
    setRole('')
    setHunts([])
    setHuntsLoading(false)
    setHuntsError('')
    showToast('Logged out', 'success')
  }

  const handleSelectHunt = (hunt) => {
    setSelectedHuntId(hunt.id)
    setActiveOverlay(null)
  }

  return (
    <div className="game-page-shell">
      {!authenticated && (
        <div className="game-title" aria-hidden="true">
          <img src={titleImage} alt="" />
        </div>
      )}

      <BoardPage
        hunts={hunts}
        loading={authenticated && huntsLoading}
        error={authenticated ? huntsError : ''}
        weather={weather}
        onSelectHunt={handleSelectHunt}
      />

      {selectedHunt && (
        <HuntModal
          key={selectedHunt.id}
          hunt={selectedHunt}
          onClose={() => setSelectedHuntId(null)}
          onHuntChanged={fetchHunts}
          weather={weather}
          role={role}
          showToast={showToast}
        />
      )}

      {authenticated && activeOverlay === 'inventory' && (
        <InventoryPanel onClose={() => setActiveOverlay(null)} />
      )}

      {authenticated && activeOverlay === 'menu' && (
        <MenuPanel
          onClose={() => setActiveOverlay(null)}
          showToast={showToast}
          onLocationUpdated={refreshWeather}
        />
      )}

      {authenticated && activeOverlay === 'shop' && (
        <ShopPanel onClose={() => setActiveOverlay(null)} role={role} />
      )}

      {authenticated && activeOverlay === 'hunts' && (
        <HuntsPanel
          hunts={hunts}
          onClose={() => setActiveOverlay(null)}
          onSelectHunt={handleSelectHunt}
        />
      )}

      {authenticated && role === 'GAME_MASTER' && activeOverlay === 'manage' && (
        <ManagePanel
          onClose={() => setActiveOverlay(null)}
          onHuntsChanged={fetchHunts}
          onBeastsChanged={() => {}}
          showToast={showToast}
        />
      )}

      {authenticated ? (
        <BottomNav
          activeOverlay={activeOverlay}
          onBoard={() => setActiveOverlay(null)}
          onInventory={() => setActiveOverlay('inventory')}
          onMenu={() => setActiveOverlay('menu')}
          onShop={() => setActiveOverlay('shop')}
          onHunts={() => setActiveOverlay('hunts')}
          onManage={() => setActiveOverlay('manage')}
          onLogout={handleLogout}
          role={role}
        />
      ) : (
        <AuthModal onAuthSuccess={handleAuthenticated} showToast={showToast} />
      )}

      {toast && (
        <Toast
          key={toast.id}
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  )
}

export default GamePage
