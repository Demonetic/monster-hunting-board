import basiliskImage from './beast_basilisk.png'
import chimeraImage from './beast_chimera.png'
import dragonImage from './beast_dragon.png'
import griffinImage from './beast_griffin.png'
import pegasusImage from './beast_pegasus.png'
import phoenixImage from './beast_phoenix.png'
import bossBasiliskPin from './pin_boss_basilisk.png'
import bossChimeraPin from './pin_boss_chimera.png'
import bossDragonPin from './pin_boss_dragon.png'
import bossGriffinPin from './pin_boss_griffin.png'
import bossPegasusPin from './pin_boss_pegasus.png'
import bossPhoenixPin from './pin_boss_phoenix.png'
import dailyBasiliskPin from './pin_daily_basilisk.png'
import dailyChimeraPin from './pin_daily_chimera.png'
import dailyDragonPin from './pin_daily_dragon.png'
import dailyGriffinPin from './pin_daily_griffin.png'
import dailyPegasusPin from './pin_daily_pegasus.png'
import dailyPhoenixPin from './pin_daily_phoenix.png'
import normalBasiliskPin from './pin_normal_basilisk.png'
import normalChimeraPin from './pin_normal_chimera.png'
import normalDragonPin from './pin_normal_dragon.png'
import normalGriffinPin from './pin_normal_griffin.png'
import normalPegasusPin from './pin_normal_pegasus.png'
import normalPhoenixPin from './pin_normal_phoenix.png'

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
  },
  daily: {
    BASILISK: dailyBasiliskPin,
    CHIMERA: dailyChimeraPin,
    DRAGON: dailyDragonPin,
    GRIFFIN: dailyGriffinPin,
    PEGASUS: dailyPegasusPin,
    PHOENIX: dailyPhoenixPin,
  },
  normal: {
    BASILISK: normalBasiliskPin,
    CHIMERA: normalChimeraPin,
    DRAGON: normalDragonPin,
    GRIFFIN: normalGriffinPin,
    PEGASUS: normalPegasusPin,
    PHOENIX: normalPhoenixPin,
  },
}

export function getBeastImage(beastType) {
  return beastImages[beastType] ?? null
}

export function getPinImage(category, beastType) {
  const normalizedCategory = pinImagesByCategory[category] ? category : 'daily'
  const categoryImages = pinImagesByCategory[normalizedCategory]
  return categoryImages[beastType] ?? categoryImages.DRAGON
}
