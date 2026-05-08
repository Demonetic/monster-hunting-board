import { Route, Routes } from 'react-router-dom'
import BattlePage from './pages/BattlePage'
import GamePage from './pages/GamePage'

function App() {
  return (
    <Routes>
      <Route path="/battle" element={<BattlePage />} />
      <Route path="*" element={<GamePage />} />
    </Routes>
  )
}

export default App
