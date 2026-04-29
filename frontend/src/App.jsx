import { Route, Routes } from 'react-router-dom'
import GamePage from './pages/GamePage'

function App() {
  return (
    <Routes>
      <Route path="*" element={<GamePage />} />
    </Routes>
  )
}

export default App
