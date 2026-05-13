import battleArenaBeach from './battle_arena_beach.png'
import battleArenaForest from './battle_arena_forest.png'
import battleArenaLava from './battle_arena_lava.png'
import battleArenaSnow from './battle_arena_snow.png'

const battleArenaBackgroundsByDifficulty = {
  EASY: battleArenaForest,
  MEDIUM: battleArenaBeach,
  HARD: battleArenaSnow,
  BOSS: battleArenaLava,
}

function normalizeDifficulty(value) {
  return typeof value === 'string' ? value.trim().toUpperCase() : ''
}

export function getBattleArenaBackground(difficulty) {
  const normalizedDifficulty = normalizeDifficulty(difficulty)
  return battleArenaBackgroundsByDifficulty[normalizedDifficulty] ?? battleArenaForest
}

export function getBattleArenaDifficulty(source) {
  if (typeof source === 'string') {
    return normalizeDifficulty(source)
  }

  return normalizeDifficulty(
    source?.difficulty
      ?? source?.huntDifficulty
      ?? source?.battleDifficulty,
  )
}
