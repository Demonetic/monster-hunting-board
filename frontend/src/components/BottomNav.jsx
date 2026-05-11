import buttonInventory from '../assets/button_inventory.png'
import buttonHunts from '../assets/button_hunts.png'
import buttonLogout from '../assets/button_logout.png'
import buttonManage from '../assets/button_manage.png'
import buttonMenu from '../assets/buttons_menu.png'
import buttonShop from '../assets/button_shop.png'
import buttonWorldMap from '../assets/button_worldmap.png'

function BottomNav({
  onBoard,
  onInventory,
  onMenu,
  onShop,
  onHunts,
  onManage,
  onLogout,
  activeOverlay,
  role,
}) {
  const isGameMaster = role === 'GAME_MASTER'

  const navButtons = [
    {
      key: 'board',
      active: (overlay) => overlay === null,
      className: 'bottom-nav-button bottom-nav-button-map',
      label: 'Map',
      ariaLabel: 'Map',
      image: buttonWorldMap,
      onClick: 'onBoard',
    },
    ...(!isGameMaster
      ? [
          {
            key: 'inventory',
            active: (overlay) => overlay === 'inventory',
            className: 'bottom-nav-button bottom-nav-button-character',
            label: 'Inventory',
            ariaLabel: 'Inventory',
            image: buttonInventory,
            onClick: 'onInventory',
          },
        ]
      : []),
    {
      key: 'menu',
      active: (overlay) => overlay === 'menu',
      className: 'bottom-nav-button bottom-nav-button-menu',
      label: 'Menu',
      ariaLabel: 'Menu',
      image: buttonMenu,
      onClick: 'onMenu',
    },
    ...(!isGameMaster
      ? [
          {
            key: 'shop',
            active: (overlay) => overlay === 'shop',
            className: 'bottom-nav-button bottom-nav-button-shop',
            label: 'Shop',
            ariaLabel: 'Shop',
            image: buttonShop,
            onClick: 'onShop',
          },
        ]
      : []),
    {
      key: 'hunts',
      active: (overlay) => overlay === 'hunts',
      className: 'bottom-nav-button bottom-nav-button-hunts',
      label: 'Hunts',
      ariaLabel: 'Hunts',
      image: buttonHunts,
      onClick: 'onHunts',
    },
    ...(isGameMaster
      ? [
          {
            key: 'manage',
            active: (overlay) => overlay === 'manage',
            className: 'bottom-nav-button bottom-nav-button-manage',
            label: 'Manage',
            ariaLabel: 'Manage',
            image: buttonManage,
            onClick: 'onManage',
          },
        ]
      : []),
    {
      key: 'logout',
      active: () => false,
      className: 'bottom-nav-button bottom-nav-button-logout',
      label: 'Logout',
      ariaLabel: 'Logout',
      image: buttonLogout,
      onClick: 'onLogout',
    },
  ]

  const handlers = {
    onBoard,
    onInventory,
    onMenu,
    onShop,
    onHunts,
    onManage,
    onLogout,
  }

  return (
    <nav className="bottom-nav" aria-label="Game navigation">
      {navButtons.map((button) => (
        <button
          key={button.key}
          type="button"
          className={`${button.className} ${button.active(activeOverlay) ? 'is-active' : ''}`.trim()}
          onClick={handlers[button.onClick]}
          aria-label={button.ariaLabel}
        >
          <img src={button.image} alt={button.label} />
        </button>
      ))}
    </nav>
  )
}

export default BottomNav
