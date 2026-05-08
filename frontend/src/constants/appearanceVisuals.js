import characterBard from '../assets/character_bard.png'
import characterHunter from '../assets/character_hunter.png'
import characterKnight from '../assets/character_knight.png'
import characterMage from '../assets/character_mage.png'
import characterPaladin from '../assets/character_paladin.png'
import characterRanger from '../assets/character_ranger.png'

export const appearanceCharacterImages = {
  BARD: characterBard,
  MAGE: characterMage,
  RANGER: characterRanger,
  KNIGHT: characterKnight,
  PALADIN: characterPaladin,
  HUNTER: characterHunter,
}

export function getAppearanceCharacterImage(appearance) {
  return appearanceCharacterImages[appearance] ?? characterHunter
}
