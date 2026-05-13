import { useCallback, useEffect, useRef, useState } from 'react'
import { clearRole, clearToken, getRole, isAuthenticated } from '../api/authStorage'
import { getAllHunts } from '../api/huntApi'
import AuthModal from '../components/AuthModal'
import BottomNav from '../components/BottomNav'
import ChatBox from '../components/ChatBox'
import HuntModal from '../components/HuntModal'
import HuntsPanel from '../components/HuntsPanel'
import InventoryPanel from '../components/InventoryPanel'
import ManagePanel from '../components/ManagePanel'
import MenuPanel from '../components/MenuPanel'
import ShopPanel from '../components/ShopPanel'
import Toast from '../components/Toast'
import useCurrentWeather from '../hooks/useCurrentWeather'
import BoardPage from './BoardPage'

function GamePage() {
  const isMountedRef = useRef(true)
  const [authenticated, setAuthenticated] = useState(isAuthenticated())
  const [role, setRole] = useState(getRole())
  const [activeOverlay, setActiveOverlay] = useState(null)
  const [hunts, setHunts] = useState([])
  const [huntsLoading, setHuntsLoading] = useState(false)
  const [huntsError, setHuntsError] = useState('')
  const [selectedHuntId, setSelectedHuntId] = useState(null)
  const [globalChatOpen, setGlobalChatOpen] = useState(false)
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

  const fetchHunts = useCallback(async ({ showLoading = true } = {}) => {
    if (showLoading) {
      setHuntsLoading(true)
    }
    setHuntsError('')

    try {
      const response = await getAllHunts()

      if (isMountedRef.current) {
        setHunts(response.data)
      }
    } catch (error) {
      if (isMountedRef.current) {
        setHuntsError(error.response?.data?.message ?? 'Could not load hunts.')
      }
    } finally {
      if (isMountedRef.current && showLoading) {
        setHuntsLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    return () => {
      isMountedRef.current = false
    }
  }, [])

  useEffect(() => {
    if (!authenticated) {
      return undefined
    }

    const timeoutId = window.setTimeout(() => {
      fetchHunts()
    }, 0)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [authenticated, fetchHunts])

  useEffect(() => {
    if (!authenticated) {
      return undefined
    }

    const refreshBoardHunts = () => {
      fetchHunts({ showLoading: false })
    }

    const intervalId = window.setInterval(refreshBoardHunts, 5000)
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        refreshBoardHunts()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    return () => {
      window.clearInterval(intervalId)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [authenticated, fetchHunts])

  const handleAuthenticated = () => {
    setAuthenticated(true)
    setRole(getRole())
  }

  const handleLogout = () => {
    clearRole()
    clearToken()
    setActiveOverlay(null)
    setGlobalChatOpen(false)
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
      <BoardPage
        hunts={hunts}
        loading={authenticated && huntsLoading}
        error={authenticated ? huntsError : ''}
        weather={weather}
        onSelectHunt={handleSelectHunt}
        showChatButton={authenticated && role === 'HUNTER'}
        isChatOpen={globalChatOpen}
        onToggleChat={() => setGlobalChatOpen((current) => !current)}
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

      {authenticated && role === 'HUNTER' && (
        <ChatBox
          title="Global Chat"
          collapsible
          collapsed={!globalChatOpen}
          onCollapsedChange={(nextCollapsed) => setGlobalChatOpen(!nextCollapsed)}
          className="global-chat-panel"
        />
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
