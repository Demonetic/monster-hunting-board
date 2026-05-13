import { useEffect, useMemo, useState } from 'react'
import { createBeast, deleteBeast, getAllBeasts } from '../api/beastApi'
import { createHunt, deleteHunt, getAllHunts, updateHunt } from '../api/huntApi'
import panelImage from '../assets/panel_information.png'
import buttonCancel from '../assets/button_cancel.png'
import buttonClose from '../assets/button_close.png'
import buttonCreateBeast from '../assets/button_create_beast.png'
import buttonCreateHunt from '../assets/button_create_hunt.png'
import buttonDelete from '../assets/button_delete.png'
import buttonUpdate from '../assets/button_update.png'
import { getBeastDisplayName, getBeastImage } from '../assets/beastVisuals'
import BeastSelector from './BeastSelector'

const huntTypeOptions = [
  { value: 'HUNT', label: 'Group Hunt' },
  { value: 'SOLO_HUNT', label: 'Solo Hunt' },
]

const difficultyOptions = ['EASY', 'MEDIUM', 'HARD', 'BOSS']
const statusOptions = ['SCHEDULED', 'ACTIVE']
const beastTypeOptions = ['UNKNOWN', 'DRAGON', 'PHOENIX', 'GRIFFIN', 'PEGASUS', 'CHIMERA', 'BASILISK']

const initialHuntForm = {
  id: null,
  title: '',
  type: 'HUNT',
  difficulty: 'MEDIUM',
  status: 'SCHEDULED',
  startTime: '',
  maxPartySize: '4',
  beastIds: [],
  rewardExp: '100',
  rewardGold: '75',
}

const initialBeastForm = {
  name: '',
  type: 'UNKNOWN',
  hp: '100',
  attackPower: '20',
  rewardExp: '50',
  rewardGold: '25',
}

function toDateTimeLocalValue(value) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const offset = date.getTimezoneOffset()
  const normalized = new Date(date.getTime() - offset * 60000)
  return normalized.toISOString().slice(0, 16)
}

function formatType(type) {
  return type === 'HUNT' ? 'Group Hunt' : 'Solo Hunt'
}

function ManagePanel({ onClose, onHuntsChanged, onBeastsChanged, showToast }) {
  const [activeTab, setActiveTab] = useState('HUNTS')
  const [hunts, setHunts] = useState([])
  const [beasts, setBeasts] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [huntFormMode, setHuntFormMode] = useState(null)
  const [beastFormOpen, setBeastFormOpen] = useState(false)
  const [huntForm, setHuntForm] = useState(initialHuntForm)
  const [beastForm, setBeastForm] = useState(initialBeastForm)
  const [isSubmittingHunt, setIsSubmittingHunt] = useState(false)
  const [isSubmittingBeast, setIsSubmittingBeast] = useState(false)

  const refreshData = async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const [huntsResponse, beastsResponse] = await Promise.all([
        getAllHunts(),
        getAllBeasts(),
      ])
      setHunts(huntsResponse.data)
      setBeasts(beastsResponse.data)
    } catch (error) {
      setErrorMessage(error.response?.data?.message ?? 'Could not load management data.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    let cancelled = false

    const loadInitialData = async () => {
      try {
        const [huntsResponse, beastsResponse] = await Promise.all([
          getAllHunts(),
          getAllBeasts(),
        ])

        if (!cancelled) {
          setHunts(huntsResponse.data)
          setBeasts(beastsResponse.data)
          setErrorMessage('')
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error.response?.data?.message ?? 'Could not load management data.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadInitialData()

    return () => {
      cancelled = true
    }
  }, [])

  const beastOptions = useMemo(
    () =>
      beasts.map((beast) => ({
        id: beast.id,
        label: `${getBeastDisplayName(beast)} (${beast.type})`,
      })),
    [beasts],
  )

  const resetHuntForm = () => {
    setHuntForm(initialHuntForm)
    setHuntFormMode(null)
  }

  const resetBeastForm = () => {
    setBeastForm(initialBeastForm)
    setBeastFormOpen(false)
  }

  const handleHuntFieldChange = (event) => {
    const { name, value } = event.target

    setHuntForm((current) => {
      const next = { ...current, [name]: value }

      if (name === 'type' && value === 'SOLO_HUNT') {
        next.status = 'ACTIVE'
        next.startTime = ''
        next.maxPartySize = ''
      }

      if (name === 'type' && value === 'HUNT' && !next.maxPartySize) {
        next.maxPartySize = '4'
      }

      return next
    })
  }

  const handleToggleHuntBeast = (beastId) => {
    setHuntForm((current) => ({
      ...current,
      beastIds: [beastId],
    }))
  }

  const handleBeastFieldChange = (event) => {
    const { name, value } = event.target
    setBeastForm((current) => ({ ...current, [name]: value }))
  }

  const openCreateHunt = () => {
    setHuntForm({
      ...initialHuntForm,
      beastIds: beastOptions[0] ? [beastOptions[0].id] : [],
    })
    setHuntFormMode('create')
  }

  const openUpdateHunt = (hunt) => {
    setHuntForm({
      id: hunt.id,
      title: hunt.title,
      type: hunt.type,
      difficulty: hunt.difficulty,
      status: hunt.status === 'ACTIVE' ? 'ACTIVE' : 'SCHEDULED',
      startTime: toDateTimeLocalValue(hunt.startTime),
      maxPartySize: hunt.maxPartySize ? String(hunt.maxPartySize) : '',
      beastIds: hunt.beasts?.map((beast) => beast.id) ?? [],
      rewardExp: String(hunt.rewardExp),
      rewardGold: String(hunt.rewardGold),
    })
    setHuntFormMode('update')
  }

  const buildHuntPayload = () => {
    const isSolo = huntForm.type === 'SOLO_HUNT'

    return {
      title: huntForm.title.trim(),
      type: huntForm.type,
      difficulty: huntForm.difficulty,
      status: isSolo ? 'ACTIVE' : huntForm.status,
      startTime: isSolo || !huntForm.startTime ? null : `${huntForm.startTime}:00`,
      maxPartySize: isSolo ? null : Number(huntForm.maxPartySize),
      beastIds: huntForm.beastIds,
      rewardExp: Number(huntForm.rewardExp),
      rewardGold: Number(huntForm.rewardGold),
    }
  }

  const handleSubmitHunt = async (event) => {
    event.preventDefault()
    setIsSubmittingHunt(true)

    try {
      const payload = buildHuntPayload()

      if (huntFormMode === 'create') {
        await createHunt(payload)
        showToast?.('Hunt created', 'success')
      }

      if (huntFormMode === 'update' && huntForm.id !== null) {
        await updateHunt(huntForm.id, {
          title: payload.title,
          difficulty: payload.difficulty,
          status: payload.status,
          startTime: payload.startTime,
          maxPartySize: payload.maxPartySize,
          beastIds: payload.beastIds,
          rewardExp: payload.rewardExp,
          rewardGold: payload.rewardGold,
        })
        showToast?.('Hunt updated', 'success')
      }

      await refreshData()
      await onHuntsChanged?.()
      resetHuntForm()
    } catch (error) {
      showToast?.(error.response?.data?.message ?? 'Hunt save failed', 'error')
    } finally {
      setIsSubmittingHunt(false)
    }
  }

  const handleDeleteHunt = async (huntId) => {
    const confirmed = window.confirm('Delete this hunt?')
    if (!confirmed) {
      return
    }

    try {
      await deleteHunt(huntId)
      showToast?.('Hunt deleted', 'success')
      await refreshData()
      await onHuntsChanged?.()
      if (huntForm.id === huntId) {
        resetHuntForm()
      }
    } catch (error) {
      showToast?.(error.response?.data?.message ?? 'Delete failed', 'error')
    }
  }

  const handleSubmitBeast = async (event) => {
    event.preventDefault()
    setIsSubmittingBeast(true)

    try {
      await createBeast({
        name: beastForm.name.trim(),
        type: beastForm.type,
        hp: Number(beastForm.hp),
        attackPower: Number(beastForm.attackPower),
        rewardExp: Number(beastForm.rewardExp),
        rewardGold: Number(beastForm.rewardGold),
      })
      showToast?.('Beast created', 'success')
      await refreshData()
      await onBeastsChanged?.()
      resetBeastForm()
    } catch (error) {
      showToast?.(error.response?.data?.message ?? 'Beast save failed', 'error')
    } finally {
      setIsSubmittingBeast(false)
    }
  }

  const handleDeleteBeast = async (beastId) => {
    const confirmed = window.confirm('Delete this beast?')
    if (!confirmed) {
      return
    }

    try {
      await deleteBeast(beastId)
      showToast?.('Beast deleted', 'success')
      await refreshData()
      await onBeastsChanged?.()
    } catch (error) {
      showToast?.(error.response?.data?.message ?? 'Delete failed', 'error')
    }
  }

  return (
    <div className="game-overlay-layer">
      <section
        className="manage-panel"
        style={{ backgroundImage: `url(${panelImage})` }}
        role="dialog"
        aria-modal="true"
        aria-label="Manage panel"
      >
        <button type="button" className="manage-panel-close" onClick={onClose}>
          <img src={buttonClose} alt="" />
        </button>

        <div className="manage-panel-content">
          <div className="hunts-panel-header manage-panel-header">
            <h2 className="hunts-panel-title manage-panel-title">Manage</h2>

            <div className="hunts-filter-row manage-filter-row">
              {['HUNTS', 'BEASTS'].map((tab) => (
                <button
                  key={tab}
                  type="button"
                  className={`hunts-filter-button ${activeTab === tab ? 'is-active' : ''}`}
                  onClick={() => setActiveTab(tab)}
                >
                  {tab.charAt(0) + tab.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </div>

          {errorMessage && <p className="manage-panel-error">{errorMessage}</p>}

          {huntFormMode && (
            <div className="manage-modal-layer">
              <form className="manage-form manage-form-hunt manage-form-overlay" onSubmit={handleSubmitHunt}>
                <h4>{huntFormMode === 'create' ? 'Create Hunt' : 'Update Hunt'}</h4>

                <label className="manage-field">
                  <span>Title</span>
                  <input name="title" value={huntForm.title} onChange={handleHuntFieldChange} required />
                </label>

                <div className="manage-form-row">
                  <label className="manage-field">
                    <span>Type</span>
                    <select
                      name="type"
                      value={huntForm.type}
                      onChange={handleHuntFieldChange}
                      disabled={huntFormMode === 'update'}
                    >
                      {huntTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="manage-field">
                    <span>Difficulty</span>
                    <select name="difficulty" value={huntForm.difficulty} onChange={handleHuntFieldChange}>
                      {difficultyOptions.map((option) => (
                        <option key={option} value={option}>
                          {option}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                {huntForm.type === 'HUNT' && (
                  <div className="manage-form-row">
                    <label className="manage-field">
                      <span>Status</span>
                      <select name="status" value={huntForm.status} onChange={handleHuntFieldChange}>
                        {statusOptions.map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    </label>

                    <label className="manage-field">
                      <span>Party Size</span>
                      <input
                        type="number"
                        min="1"
                        name="maxPartySize"
                        value={huntForm.maxPartySize}
                        onChange={handleHuntFieldChange}
                        required
                      />
                    </label>
                  </div>
                )}

                {huntForm.type === 'HUNT' && (
                  <label className="manage-field">
                    <span>Start Time</span>
                    <input
                      type="datetime-local"
                      name="startTime"
                      value={huntForm.startTime}
                      onChange={handleHuntFieldChange}
                      required={huntForm.type === 'HUNT'}
                    />
                  </label>
                )}

                <label className="manage-field manage-field-beasts">
                  <span>Beasts</span>
                  <BeastSelector
                    options={beastOptions}
                    selectedIds={huntForm.beastIds}
                    onToggle={handleToggleHuntBeast}
                    className="manage-beast-selector"
                  />
                </label>

                <div className="manage-form-row">
                  <label className="manage-field">
                    <span>Reward EXP</span>
                    <input
                      type="number"
                      min="0"
                      name="rewardExp"
                      value={huntForm.rewardExp}
                      onChange={handleHuntFieldChange}
                      required
                    />
                  </label>

                  <label className="manage-field">
                    <span>Reward Gold</span>
                    <input
                      type="number"
                      min="0"
                      name="rewardGold"
                      value={huntForm.rewardGold}
                      onChange={handleHuntFieldChange}
                      required
                    />
                  </label>
                </div>

                <div className="manage-form-actions">
                  <button
                    type="submit"
                    className="manage-inline-button"
                    disabled={isSubmittingHunt}
                    aria-label={huntFormMode === 'create' ? 'Create hunt' : 'Update hunt'}
                  >
                    <img
                      src={huntFormMode === 'create' ? buttonCreateHunt : buttonUpdate}
                      alt=""
                    />
                  </button>
                  <button type="button" className="manage-inline-button" onClick={resetHuntForm} aria-label="Cancel">
                    <img src={buttonCancel} alt="" />
                  </button>
                </div>
              </form>
            </div>
          )}

          <section className="manage-panel-section manage-panel-stage">
            {activeTab === 'HUNTS' ? (
              <>
                <div className="manage-panel-section-header">
                  <h3>Hunts Management</h3>
                  <button type="button" className="manage-panel-button" onClick={openCreateHunt}>
                    <img src={buttonCreateHunt} alt="" />
                  </button>
                </div>

                <div className="manage-list">
                  {isLoading ? (
                    <p className="manage-panel-message">Loading hunts...</p>
                  ) : hunts.length === 0 ? (
                    <p className="hunts-empty">No hunts found.</p>
                  ) : (
                    hunts.map((hunt) => (
                      <div key={hunt.id} className="manage-list-row">
                        <div className="manage-list-copy">
                          <h4>{hunt.title}</h4>
                          <p>{formatType(hunt.type)}</p>
                          <p>Hunt Difficulty: {hunt.difficulty}</p>
                          <p>Beast Templates: {hunt.beasts?.map((beast) => getBeastDisplayName(beast)).join(', ') || 'Unknown'}</p>
                        </div>
                        <div className="manage-list-actions">
                          <button type="button" className="manage-inline-button" onClick={() => openUpdateHunt(hunt)} aria-label="Update hunt">
                            <img src={buttonUpdate} alt="" />
                          </button>
                          <button type="button" className="manage-inline-button is-danger" onClick={() => handleDeleteHunt(hunt.id)} aria-label="Delete hunt">
                            <img src={buttonDelete} alt="" />
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            ) : (
              <>
                <div className="manage-panel-section-header">
                  <h3>Beast Management</h3>
                  <button type="button" className="manage-panel-button" onClick={() => setBeastFormOpen(true)}>
                    <img src={buttonCreateBeast} alt="" />
                  </button>
                </div>

                <div className="manage-list">
                  {isLoading ? (
                    <p className="manage-panel-message">Loading beasts...</p>
                  ) : beasts.length === 0 ? (
                    <p className="hunts-empty">No beasts found.</p>
                  ) : (
                    beasts.map((beast) => (
                      <div key={beast.id} className="manage-list-row">
                        <div className="manage-list-copy manage-list-copy-beast">
                          <img className="manage-beast-icon" src={getBeastImage(beast)} alt={beast.type} />
                          <div>
                            <h4>{getBeastDisplayName(beast)}</h4>
                            <p>{beast.type}</p>
                          </div>
                        </div>
                        <div className="manage-list-actions">
                          <button type="button" className="manage-inline-button is-danger" onClick={() => handleDeleteBeast(beast.id)} aria-label="Delete beast">
                            <img src={buttonDelete} alt="" />
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </>
            )}
          </section>

          {beastFormOpen && (
            <div className="manage-modal-layer">
              <form className="manage-form manage-form-overlay" onSubmit={handleSubmitBeast}>
                <h4>Create Beast</h4>

                <div className="manage-form-row">
                  <label className="manage-field">
                    <span>Beast Name</span>
                    <input
                      name="name"
                      value={beastForm.name}
                      onChange={handleBeastFieldChange}
                      maxLength="80"
                      placeholder="Crystal Fox"
                      required
                    />
                  </label>

                  <label className="manage-field">
                    <span>Type</span>
                    <select name="type" value={beastForm.type} onChange={handleBeastFieldChange}>
                      {beastTypeOptions.map((option) => (
                        <option key={option} value={option}>
                          {option}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <div className="manage-form-row">
                  <label className="manage-field">
                    <span>HP</span>
                    <input type="number" min="1" name="hp" value={beastForm.hp} onChange={handleBeastFieldChange} required />
                  </label>

                  <label className="manage-field">
                    <span>Attack</span>
                    <input
                      type="number"
                      min="1"
                      name="attackPower"
                      value={beastForm.attackPower}
                      onChange={handleBeastFieldChange}
                      required
                    />
                  </label>
                </div>

                <div className="manage-form-row">
                  <label className="manage-field">
                    <span>Reward EXP</span>
                    <input
                      type="number"
                      min="0"
                      name="rewardExp"
                      value={beastForm.rewardExp}
                      onChange={handleBeastFieldChange}
                      required
                    />
                  </label>

                  <label className="manage-field">
                    <span>Reward Gold</span>
                    <input
                      type="number"
                      min="0"
                      name="rewardGold"
                      value={beastForm.rewardGold}
                      onChange={handleBeastFieldChange}
                      required
                    />
                  </label>
                </div>

                <div className="manage-form-actions">
                  <button type="submit" className="manage-inline-button" disabled={isSubmittingBeast} aria-label="Create beast">
                    <img src={buttonCreateBeast} alt="" />
                  </button>
                  <button type="button" className="manage-inline-button" onClick={resetBeastForm} aria-label="Cancel">
                    <img src={buttonCancel} alt="" />
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}

export default ManagePanel
