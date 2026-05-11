import basiliskImage from './beast_basilisk.png'
import chimeraImage from './beast_chimera.png'
import dragonImage from './beast_dragon.png'
import griffinImage from './beast_griffin.png'
import pegasusImage from './beast_pegasus.png'
import phoenixImage from './beast_phoenix.png'
import unknownBeastImage from './beast_unknown.png'
import bossBasiliskPin from './pin_boss_basilisk.png'
import bossChimeraPin from './pin_boss_chimera.png'
import bossDragonPin from './pin_boss_dragon.png'
import bossGriffinPin from './pin_boss_griffin.png'
import bossPegasusPin from './pin_boss_pegasus.png'
import bossPhoenixPin from './pin_boss_phoenix.png'
import bossUnknownPin from './pin_boss_unknown.png'
import dailyBasiliskPin from './pin_daily_basilisk.png'
import dailyChimeraPin from './pin_daily_chimera.png'
import dailyDragonPin from './pin_daily_dragon.png'
import dailyGriffinPin from './pin_daily_griffin.png'
import dailyPegasusPin from './pin_daily_pegasus.png'
import dailyPhoenixPin from './pin_daily_phoenix.png'
import dailyUnknownPin from './pin_daily_unknown.png'
import normalBasiliskPin from './pin_normal_basilisk.png'
import normalChimeraPin from './pin_normal_chimera.png'
import normalDragonPin from './pin_normal_dragon.png'
import normalGriffinPin from './pin_normal_griffin.png'
import normalPegasusPin from './pin_normal_pegasus.png'
import normalPhoenixPin from './pin_normal_phoenix.png'
import normalUnknownPin from './pin_normal_unknown.png'

export const beastImages = {
  BASILISK: basiliskImage,
  CHIMERA: chimeraImage,
  DRAGON: dragonImage,
  GRIFFIN: griffinImage,
  PEGASUS: pegasusImage,
  PHOENIX: phoenixImage,
}

const pinImagesByCategory = {
  boss: {
    BASILISK: bossBasiliskPin,
    CHIMERA: bossChimeraPin,
    DRAGON: bossDragonPin,
    GRIFFIN: bossGriffinPin,
    PEGASUS: bossPegasusPin,
    PHOENIX: bossPhoenixPin,
    UNKNOWN: bossUnknownPin,
  },
  daily: {
    BASILISK: dailyBasiliskPin,
    CHIMERA: dailyChimeraPin,
    DRAGON: dailyDragonPin,
    GRIFFIN: dailyGriffinPin,
    PEGASUS: dailyPegasusPin,
    PHOENIX: dailyPhoenixPin,
    UNKNOWN: dailyUnknownPin,
  },
  normal: {
    BASILISK: normalBasiliskPin,
    CHIMERA: normalChimeraPin,
    DRAGON: normalDragonPin,
    GRIFFIN: normalGriffinPin,
    PEGASUS: normalPegasusPin,
    PHOENIX: normalPhoenixPin,
    UNKNOWN: normalUnknownPin,
  },
}

function normalizeBeastImageKey(beastOrKey) {
  if (!beastOrKey) {
    return null
  }

  if (typeof beastOrKey === 'string') {
    return beastOrKey.trim().toUpperCase() || null
  }

  if (typeof beastOrKey === 'object') {
    const rawKey = beastOrKey.imageKey ?? beastOrKey.type ?? beastOrKey.beastType ?? null
    if (typeof rawKey === 'string') {
      return rawKey.trim().toUpperCase() || null
    }
  }

  return null
}

function normalizePinCategory(category) {
  return pinImagesByCategory[category] ? category : 'daily'
}

function formatBeastTypeLabel(beastType) {
  if (!beastType) {
    return 'Unknown Beast'
  }

  return beastType
    .toLowerCase()
    .split('_')
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
}

export function getBeastImage(beastOrKey) {
  const beastImageKey = normalizeBeastImageKey(beastOrKey)
  return beastImages[beastImageKey] ?? unknownBeastImage
}

export function getBeastDisplayName(beastOrKey) {
  if (typeof beastOrKey === 'object' && beastOrKey?.name?.trim()) {
    return beastOrKey.name.trim()
  }

  return formatBeastTypeLabel(normalizeBeastImageKey(beastOrKey))
}

export function getPinImage(category, beastOrKey) {
  const normalizedCategory = normalizePinCategory(category)
  const categoryImages = pinImagesByCategory[normalizedCategory]
  const beastImageKey = normalizeBeastImageKey(beastOrKey)
  return categoryImages[beastImageKey] ?? categoryImages.UNKNOWN
}

export function getHuntPinType(hunt) {
  if (hunt?.sourceType === 'DAILY_BOSS' || hunt?.difficulty === 'BOSS') {
    return 'boss'
  }

  if (hunt?.sourceType === 'DAILY_BOUNTY' || hunt?.sourceType === 'WEEKLY_CONTRACT') {
    return 'daily'
  }

  if (hunt?.sourceType === 'REPEATABLE' || hunt?.type === 'SOLO_HUNT' || hunt?.sourceType === 'MANUAL' || !hunt?.sourceType) {
    return 'normal'
  }

  return 'daily'
}

export function getHuntPinImage(hunt) {
  const pinType = getHuntPinType(hunt)
  const primaryBeast = hunt?.beasts?.[0] ?? hunt?.beast ?? hunt?.primaryBeast ?? hunt?.primaryBeastType ?? hunt?.beastType ?? null
  return getPinImage(pinType, primaryBeast)
}
