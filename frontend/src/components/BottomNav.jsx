import buttonCharacter from '../assets/button_character.png'
import buttonHunts from '../assets/button_hunts.png'
import buttonManage from '../assets/button_manage.png'
import buttonLogout from '../assets/button_logout.png'
import buttonMap from '../assets/button_map.png'

function BottomNav({
  onBoard,
  onInventory,
  onHunts,
  onManage,
  onLogout,
  activeOverlay,
  role,
}) {
  const navButtons = [
    {
      key: 'board',
      active: (overlay) => overlay === null,
      className: 'bottom-nav-button bottom-nav-button-map',
      label: 'Map',
      ariaLabel: 'Map',
      image: buttonMap,
      onClick: 'onBoard',
    },
    {
      key: 'inventory',
      active: (overlay) => overlay === 'inventory',
      className: 'bottom-nav-button bottom-nav-button-character',
      label: 'Character',
      ariaLabel: 'Character',
      image: buttonCharacter,
      onClick: 'onInventory',
    },
    {
      key: 'hunts',
      active: (overlay) => overlay === 'hunts',
      className: 'bottom-nav-button bottom-nav-button-hunts',
      label: 'Hunts',
      ariaLabel: 'Hunts',
      image: buttonHunts,
      onClick: 'onHunts',
    },
    ...(role === 'GAME_MASTER'
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
