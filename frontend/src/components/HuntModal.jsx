import { useEffect, useState } from 'react'
import { getCurrentUsername } from '../api/authStorage'
import { getCurrentHunter } from '../api/hunterApi'
import { getAllBeasts } from '../api/beastApi'
import { completeHunt, deleteHunt, joinHunt, startSoloHunt, updateHunt } from '../api/huntApi'
import BattleResultPopup from './BattleResultPopup'
import panelParchment from '../assets/panel_new.png'
import dragonIcon from '../assets/icon_dragon.png'
import phoenixIcon from '../assets/icon_phoenix.png'
import griffinIcon from '../assets/icon_griffin.png'
import chimeraIcon from '../assets/icon_chimera.png'
import basiliskIcon from '../assets/icon_basilisk.png'
import buttonImage from '../assets/button_new.png'

const HUNT_PROGRESS_KEY = 'huntProgress'

const monsterImages = {
  dragon: dragonIcon,
  phoenix: phoenixIcon,
  griffin: griffinIcon,
  chimera: chimeraIcon,
  basilisk: basiliskIcon,
}

function getFirstBeast(hunt) {
  return hunt.beasts?.[0] ?? null
}

function getMonsterImage(hunt) {
  const firstBeast = getFirstBeast(hunt)

  if (!firstBeast) {
    return null
  }

  const beastKey = firstBeast.type.toLowerCase()
  return monsterImages[beastKey] ?? null
}

function formatType(type) {
  return {
    HUNT: 'Group Hunt',
    SOLO_HUNT: 'Solo Hunt',
  }[type] ?? type
}

function formatStartTime(startTime) {
  if (!startTime) {
    return 'Available anytime'
  }

  return new Date(startTime).toLocaleString()
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

function buildUpdateFormState(hunt) {
  return {
    title: hunt.title,
    difficulty: hunt.difficulty,
    status: hunt.status === 'ACTIVE' ? 'ACTIVE' : 'SCHEDULED',
    startTime: toDateTimeLocalValue(hunt.startTime),
    maxPartySize: hunt.maxPartySize ? String(hunt.maxPartySize) : '',
    beastIds: hunt.beasts?.map((beast) => beast.id) ?? [],
    rewardExp: String(hunt.rewardExp),
    rewardGold: String(hunt.rewardGold),
  }
}

function buildHuntSignature(hunt) {
  return JSON.stringify({
    title: hunt.title,
    type: hunt.type,
    difficulty: hunt.difficulty,
    status: hunt.status,
    startTime: hunt.startTime,
    maxPartySize: hunt.maxPartySize,
    rewardExp: hunt.rewardExp,
    rewardGold: hunt.rewardGold,
    beasts: (hunt.beasts ?? []).map((beast) => beast.id),
  })
}

function getFriendlyActionError(error) {
  const backendMessage = error.response?.data?.message

  if (!backendMessage) {
    return 'Action failed'
  }

  const normalizedMessage = backendMessage.toLowerCase()

  if (
    normalizedMessage.includes('already joined') ||
    normalizedMessage.includes('duplicate entry')
  ) {
    return 'Already joined'
  }

  return backendMessage
}

function isAlreadyJoinedError(error) {
  const backendMessage = error.response?.data?.message?.toLowerCase() ?? ''

  return (
    backendMessage.includes('already joined') ||
    backendMessage.includes('duplicate entry')
  )
}

function isAlreadyCompletedError(error) {
  const backendMessage = error.response?.data?.message?.toLowerCase() ?? ''

  return backendMessage.includes('already been completed')
}

function isAlreadyStartedError(error) {
  const backendMessage = error.response?.data?.message?.toLowerCase() ?? ''

  return backendMessage.includes('already started')
}

function readHuntProgress(username) {
  if (!username) {
    return {}
  }

  try {
    const stored = localStorage.getItem(`${HUNT_PROGRESS_KEY}:${username}`)
    return stored ? JSON.parse(stored) : {}
  } catch {
    return {}
  }
}

function writeHuntProgress(username, progress) {
  if (!username) {
    return
  }

  localStorage.setItem(`${HUNT_PROGRESS_KEY}:${username}`, JSON.stringify(progress))
}

function getActionConfig({
  hunt,
  role,
  isFull,
  isGroupHunt,
  isJoined,
  isCompleted,
  isInProgress,
  isSubmitting,
}) {
  if (role === 'GAME_MASTER') {
    return {
      primary: {
        action: 'update',
        disabled: isSubmitting,
        label: 'Update Hunt',
      },
      secondary: {
        action: 'delete',
        disabled: isSubmitting,
        label: isSubmitting ? 'Deleting...' : 'Delete Hunt',
      },
    }
  }

  if (hunt.status === 'COMPLETED' || isCompleted) {
    return {
      primary: {
        disabled: true,
        label: 'Completed',
      },
    }
  }

  if (hunt.type === 'SOLO_HUNT') {
    return {
      primary: {
        action: 'solo',
        disabled: isSubmitting,
        label: isSubmitting ? 'Fighting...' : 'Start Hunt',
      },
    }
  }

  if (!isGroupHunt) {
    return null
  }

  if (isFull && !isJoined) {
    return {
      primary: {
        disabled: true,
        label: 'Hunt Full',
      },
    }
  }

  if (!isJoined) {
    if (isInProgress) {
      return {
        primary: {
          disabled: true,
          label: 'In Progress',
        },
      }
    }

    return {
      primary: {
        action: 'join',
        disabled: isSubmitting,
        label: isSubmitting ? 'Joining...' : 'Join Hunt',
      },
    }
  }

  if (hunt.status === 'ACTIVE') {
    return {
      primary: {
        action: 'complete',
        disabled: isSubmitting,
        label: isSubmitting ? 'Fighting...' : 'Complete Hunt',
      },
    }
  }

  return {
    primary: {
      disabled: true,
      label: 'Joined',
    },
  }
}

function HuntModal({ hunt, onClose, onHuntChanged, role, showToast }) {
  const firstBeast = getFirstBeast(hunt)
  const monsterImage = getMonsterImage(hunt)
  const username = getCurrentUsername()
  const huntSignature = buildHuntSignature(hunt)
  const [actionMessage, setActionMessage] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionStateLabel, setActionStateLabel] = useState('')
  const [huntResult, setHuntResult] = useState(null)
  const [isUpdateFormOpen, setIsUpdateFormOpen] = useState(false)
  const [isSavingUpdate, setIsSavingUpdate] = useState(false)
  const [isLoadingBeasts, setIsLoadingBeasts] = useState(false)
  const [availableBeasts, setAvailableBeasts] = useState([])
  const [updateForm, setUpdateForm] = useState(() => buildUpdateFormState(hunt))
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [progressByHuntId, setProgressByHuntId] = useState(() => readHuntProgress(username))
  const [hunterState, setHunterState] = useState(null)

  const setHuntProgress = (updater) => {
    setProgressByHuntId((current) => {
      const nextValue = updater(current)
      writeHuntProgress(username, nextValue)
      return nextValue
    })
  }

  const storedHuntProgress = progressByHuntId[hunt.id]
  const huntProgress =
    storedHuntProgress?.signature === huntSignature
      ? storedHuntProgress
      : { joined: false, completed: false, inProgress: false, signature: huntSignature }
  const isGroupHunt = hunt.type === 'HUNT'
  const isJoined = huntProgress.joined
  const isCompleted = huntProgress.completed
  const isInProgress = huntProgress.inProgress || hunt.status === 'ACTIVE'
  const isFull =
    isGroupHunt &&
    hunt.maxPartySize !== null &&
    hunt.maxPartySize !== undefined &&
    hunt.currentPartySize >= hunt.maxPartySize

  const actionConfig = getActionConfig({
    hunt,
    role,
    isFull,
    isGroupHunt,
    isJoined,
    isCompleted,
    isInProgress,
    isSubmitting,
  })

  useEffect(() => {
    if (role !== 'HUNTER') {
      return undefined
    }

    let cancelled = false

    ;(async () => {
      try {
        const response = await getCurrentHunter()
        if (!cancelled) {
          setHunterState(response.data)
        }
      } catch {
        if (!cancelled) {
          setHunterState(null)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [role])

  const syncAfterAction = async () => {
    await onHuntChanged?.()

    if (role === 'HUNTER') {
      try {
        const response = await getCurrentHunter()
        setHunterState(response.data)
      } catch {
        setHunterState(null)
      }
    }
  }

  const getBattleOutcomeMessage = (won) => (won ? 'Victory!' : 'Defeat...')

  const primaryAction = actionConfig?.primary ?? null
  const secondaryAction = actionConfig?.secondary ?? null

  const handleJoin = async () => {
    const response = await joinHunt(hunt.id)

    setHuntProgress((current) => ({
      ...current,
      [hunt.id]: {
        joined: true,
        completed: false,
        inProgress: false,
        signature: huntSignature,
      },
    }))

    const message = response.data?.message ?? 'Joined hunt'
    setActionMessage(message)
    showToast?.('Joined hunt', 'success')
    await syncAfterAction()
  }

  const handleSoloStart = async () => {
    const response = await startSoloHunt(hunt.id, true)

    setHuntProgress((current) => ({
      ...current,
      [hunt.id]: {
        joined: true,
        completed: true,
        inProgress: false,
        signature: huntSignature,
      },
    }))

    setHuntResult(response.data)
    setHunterState((current) => current
      ? {
          ...current,
          currentHp: response.data.newCurrentHp,
          baseHp: response.data.newBaseHp,
          exp: response.data.newExp,
          gold: response.data.newGold,
          level: response.data.newLevel,
          expPotionActive: false,
          endurancePotionActive: false,
        }
      : current)
    const outcomeMessage = getBattleOutcomeMessage(response.data.won)
    showToast?.(outcomeMessage, response.data.won ? 'success' : 'error')
    await syncAfterAction()
  }

  const handleComplete = async () => {
    const response = await completeHunt(hunt.id, true)

    setHuntProgress((current) => ({
      ...current,
      [hunt.id]: {
        joined: true,
        completed: true,
        inProgress: true,
        signature: huntSignature,
      },
    }))

    setHuntResult(response.data)
    setHunterState((current) => current
      ? {
          ...current,
          currentHp: response.data.newCurrentHp,
          baseHp: response.data.newBaseHp,
          exp: response.data.newExp,
          gold: response.data.newGold,
          level: response.data.newLevel,
          expPotionActive: false,
          endurancePotionActive: false,
        }
      : current)
    const outcomeMessage = getBattleOutcomeMessage(response.data.won)
    showToast?.(outcomeMessage, response.data.won ? 'success' : 'error')
    await syncAfterAction()
  }

  const handleDelete = async () => {
    const confirmed = window.confirm('Delete this hunt?')

    if (!confirmed) {
      return
    }

    await deleteHunt(hunt.id)
    showToast?.('Hunt deleted', 'success')
    await syncAfterAction()
    onClose()
  }

  const openUpdateForm = async () => {
    setActionMessage('')
    setActionError('')
    setUpdateForm(buildUpdateFormState(hunt))
    setIsSavingUpdate(false)
    setIsUpdateFormOpen(true)

    if (availableBeasts.length > 0) {
      return
    }

    setIsLoadingBeasts(true)

    try {
      const response = await getAllBeasts()
      setAvailableBeasts(response.data)
    } catch (error) {
      const message = error.response?.data?.message ?? 'Could not load beasts'
      setActionError(message)
      showToast?.(message, 'error')
    } finally {
      setIsLoadingBeasts(false)
    }
  }

  const handleAction = async (action) => {
    if (!action || isSubmitting) {
      return
    }

    if (action === 'update') {
      await openUpdateForm()
      return
    }

    setActionMessage('')
    setActionError('')
    setActionStateLabel(
      action === 'delete'
        ? secondaryAction?.label ?? ''
        : primaryAction?.label ?? '',
    )
    setHuntResult(null)
    setIsSubmitting(true)

    try {
      if (action === 'join') {
        await handleJoin()
      }

      if (action === 'solo') {
        await handleSoloStart()
      }

      if (action === 'complete') {
        await handleComplete()
      }

      if (action === 'delete') {
        await handleDelete()
      }
    } catch (error) {
      if (action === 'join' && isAlreadyJoinedError(error)) {
        setHuntProgress((current) => ({
          ...current,
          [hunt.id]: {
            joined: true,
            completed: current[hunt.id]?.completed ?? false,
            inProgress: current[hunt.id]?.inProgress ?? hunt.status === 'ACTIVE',
            signature: huntSignature,
          },
        }))
        setActionMessage('Already joined')
        showToast?.('Already joined', 'error')
        return
      }

      if (action === 'join' && isAlreadyStartedError(error)) {
        setHuntProgress((current) => ({
          ...current,
          [hunt.id]: {
            joined: false,
            completed: current[hunt.id]?.completed ?? false,
            inProgress: true,
            signature: huntSignature,
          },
        }))
        setActionMessage('In progress')
        showToast?.('Hunt has already started', 'error')
        return
      }

      if (action === 'complete' && isAlreadyCompletedError(error)) {
        setHuntProgress((current) => ({
          ...current,
          [hunt.id]: {
            joined: true,
            completed: true,
            inProgress: true,
            signature: huntSignature,
          },
        }))
        setActionMessage('Already completed')
        showToast?.('Already completed', 'error')
        return
      }

      const message = getFriendlyActionError(error)
      setActionError(message)
      showToast?.(message, 'error')
    } finally {
      setActionStateLabel('')
      setIsSubmitting(false)
    }
  }

  const handleUpdateFieldChange = (event) => {
    const { name, value } = event.target
    setUpdateForm((current) => ({ ...current, [name]: value }))
  }

  const handleUpdateBeastSelectionChange = (event) => {
    const selectedValues = Array.from(
      event.target.selectedOptions,
      (option) => Number(option.value),
    )
    setUpdateForm((current) => ({ ...current, beastIds: selectedValues }))
  }

  const handleSubmitUpdate = async (event) => {
    event.preventDefault()
    setIsSavingUpdate(true)
    setActionError('')

    try {
      await updateHunt(hunt.id, {
        title: updateForm.title.trim(),
        difficulty: updateForm.difficulty,
        status: hunt.type === 'SOLO_HUNT' ? undefined : updateForm.status,
        startTime:
          hunt.type === 'SOLO_HUNT' || !updateForm.startTime
            ? null
            : `${updateForm.startTime}:00`,
        maxPartySize:
          hunt.type === 'SOLO_HUNT' || !updateForm.maxPartySize
            ? null
            : Number(updateForm.maxPartySize),
        beastIds: updateForm.beastIds,
        rewardExp: Number(updateForm.rewardExp),
        rewardGold: Number(updateForm.rewardGold),
      })
      showToast?.('Hunt updated', 'success')
      await syncAfterAction()
      setIsUpdateFormOpen(false)
      setActionMessage('Hunt updated')
    } catch (error) {
      const message = error.response?.data?.message ?? 'Update failed'
      setActionError(message)
      showToast?.(message, 'error')
    } finally {
      setIsSavingUpdate(false)
    }
  }

  return (
    <div className="hunt-modal-overlay" onClick={onClose}>
      <section
        className="hunt-modal-frame"
        style={{ backgroundImage: `url(${panelParchment})` }}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="hunt-modal-content-wrap">
          {monsterImage && (
            <img
              className="hunt-modal-monster"
              src={monsterImage}
              alt={firstBeast?.type ?? 'monster'}
            />
          )}

          <div className="hunt-modal-content">
            <h2 className="hunt-modal-title">{hunt.title}</h2>

            <div className="hunt-modal-info">
              <p>
                <span>Type:</span> {formatType(hunt.type)}
              </p>
              <p>
                <span>Difficulty:</span> {hunt.difficulty}
              </p>
              <p>
                <span>Status:</span> {hunt.status}
              </p>
              <p>
                <span>Start Time:</span> {formatStartTime(hunt.startTime)}
              </p>
              {hunt.maxPartySize !== null && hunt.maxPartySize !== undefined && (
                <p>
                  <span>Party Size:</span> {hunt.currentPartySize} / {hunt.maxPartySize}
                </p>
              )}
              {(hunt.maxPartySize === null || hunt.maxPartySize === undefined) && (
                <p>
                  <span>Current Party:</span> {hunt.currentPartySize}
                </p>
              )}
              <p>
                <span>Reward EXP:</span> {hunt.rewardExp}
              </p>
              <p>
                <span>Reward Gold:</span> {hunt.rewardGold}
              </p>
              <p>
                <span>Primary Beast:</span> {firstBeast?.type ?? 'Unknown'}
              </p>
              {role === 'HUNTER' && hunterState && (
                <>
                  <p>
                    <span>Your HP:</span> {hunterState.currentHp} / {hunterState.baseHp}
                  </p>
                  <p>
                    <span>EXP Potion:</span> {hunterState.expPotionActive ? 'Ready for this hunt' : 'Inactive'}
                  </p>
                  <p>
                    <span>Endurance Potion:</span> {hunterState.endurancePotionActive ? 'Ready for this hunt' : 'Inactive'}
                  </p>
                </>
              )}
            </div>

            {isSubmitting && <p className="hunt-action-state">{actionStateLabel}</p>}
            {actionMessage && <p className="hunt-action-message">{actionMessage}</p>}
            {actionError && <p className="hunt-action-error">{actionError}</p>}

          </div>
        </div>

        <div className="hunt-modal-actions">
          {primaryAction && (
            <button
              type="button"
              className={`hunt-action-button ${isSubmitting ? 'is-loading' : ''}`.trim()}
              onClick={() => handleAction(primaryAction.action)}
              disabled={primaryAction.disabled}
            >
              <img src={buttonImage} alt="" />
              <span>{primaryAction.label}</span>
            </button>
          )}

          {secondaryAction && (
            <button
              type="button"
              className={`hunt-action-button hunt-action-button-secondary ${isSubmitting ? 'is-loading' : ''}`.trim()}
              onClick={() => handleAction(secondaryAction.action)}
              disabled={secondaryAction.disabled}
            >
              <img src={buttonImage} alt="" />
              <span>{secondaryAction.label}</span>
            </button>
          )}

          <button
            type="button"
            className="hunt-modal-close"
            onClick={onClose}
            aria-label="Close hunt details"
          >
            <img src={buttonImage} alt="" />
            <span>Close</span>
          </button>
        </div>

        {isUpdateFormOpen && (
          <div className="hunt-update-overlay">
            <form className="hunt-update-form" onSubmit={handleSubmitUpdate}>
              <h3 className="hunt-update-title">Update Hunt</h3>

              <label className="hunt-update-field">
                <span>Title</span>
                <input
                  name="title"
                  value={updateForm.title}
                  onChange={handleUpdateFieldChange}
                  required
                />
              </label>

              <div className="hunt-update-row">
                <label className="hunt-update-field">
                  <span>Type</span>
                  <input value={formatType(hunt.type)} readOnly />
                </label>

                <label className="hunt-update-field">
                  <span>Difficulty</span>
                  <select
                    name="difficulty"
                    value={updateForm.difficulty}
                    onChange={handleUpdateFieldChange}
                  >
                    {['EASY', 'MEDIUM', 'HARD', 'BOSS'].map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {hunt.type === 'HUNT' && (
                <div className="hunt-update-row">
                  <label className="hunt-update-field">
                    <span>Status</span>
                    <select
                      name="status"
                      value={updateForm.status}
                      onChange={handleUpdateFieldChange}
                    >
                      {['SCHEDULED', 'ACTIVE'].map((option) => (
                        <option key={option} value={option}>
                          {option}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label className="hunt-update-field">
                    <span>Party Size</span>
                    <input
                      type="number"
                      min="1"
                      name="maxPartySize"
                      value={updateForm.maxPartySize}
                      onChange={handleUpdateFieldChange}
                      required
                    />
                  </label>
                </div>
              )}

              {hunt.type === 'HUNT' && (
                <label className="hunt-update-field">
                  <span>Start Time</span>
                  <input
                    type="datetime-local"
                    name="startTime"
                    value={updateForm.startTime}
                    onChange={handleUpdateFieldChange}
                    required={hunt.type === 'HUNT'}
                  />
                </label>
              )}

              <label className="hunt-update-field">
                <span>Beasts</span>
                <select
                  multiple
                  value={updateForm.beastIds.map(String)}
                  onChange={handleUpdateBeastSelectionChange}
                  size={Math.min(4, Math.max(availableBeasts.length, 2))}
                  disabled={isLoadingBeasts}
                >
                  {availableBeasts.map((beast) => (
                    <option key={beast.id} value={beast.id}>
                      {`${beast.type} (${beast.difficulty})`}
                    </option>
                  ))}
                </select>
              </label>

              <div className="hunt-update-row">
                <label className="hunt-update-field">
                  <span>Reward EXP</span>
                  <input
                    type="number"
                    min="0"
                    name="rewardExp"
                    value={updateForm.rewardExp}
                    onChange={handleUpdateFieldChange}
                    required
                  />
                </label>

                <label className="hunt-update-field">
                  <span>Reward Gold</span>
                  <input
                    type="number"
                    min="0"
                    name="rewardGold"
                    value={updateForm.rewardGold}
                    onChange={handleUpdateFieldChange}
                    required
                  />
                </label>
              </div>

              <div className="hunt-update-actions">
                <button
                  type="submit"
                  className="hunt-update-inline-button"
                  disabled={isSavingUpdate}
                >
                  {isSavingUpdate ? 'Saving...' : 'Save'}
                </button>
                <button
                  type="button"
                  className="hunt-update-inline-button"
                  onClick={() => setIsUpdateFormOpen(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {huntResult && (
          <BattleResultPopup
            result={huntResult}
            onClose={() => setHuntResult(null)}
          />
        )}
      </section>
    </div>
  )
}

export default HuntModal
