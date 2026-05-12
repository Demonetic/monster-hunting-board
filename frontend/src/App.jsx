import { Route, Routes } from 'react-router-dom'
import BattlePage from './pages/BattlePage'
import GamePage from './pages/GamePage'
import GroupBattlePage from './pages/GroupBattlePage'
import GroupHuntLobbyPage from './pages/GroupHuntLobbyPage'

function App() {
  return (
    <>
      <Routes>
        <Route path="/battle" element={<BattlePage />} />
        <Route path="/battle/group/:huntId" element={<GroupBattlePage />} />
        <Route path="/hunts/:huntId/lobby" element={<GroupHuntLobbyPage />} />
        <Route path="*" element={<GamePage />} />
      </Routes>
      <div className="orientation-guard" aria-hidden="true">
        <div className="orientation-guard-card">
          <strong>Rotate your device</strong>
          <span>This game needs landscape on mobile.</span>
        </div>
      </div>
    </>
  )
}

export default App
