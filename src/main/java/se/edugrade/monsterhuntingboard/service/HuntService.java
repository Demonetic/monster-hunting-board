package se.edugrade.monsterhuntingboard.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;
import se.edugrade.monsterhuntingboard.dto.BattleParticipantResponse;
import se.edugrade.monsterhuntingboard.dto.CompleteHuntRequest;
import se.edugrade.monsterhuntingboard.dto.CreateHuntRequest;
import se.edugrade.monsterhuntingboard.dto.GroupLobbyParticipantResponse;
import se.edugrade.monsterhuntingboard.dto.GroupLobbyResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResultResponse;
import se.edugrade.monsterhuntingboard.dto.JoinHuntResponse;
import se.edugrade.monsterhuntingboard.dto.ParticipantWeatherResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateHuntRequest;
import se.edugrade.monsterhuntingboard.dto.WeatherResponse;
import se.edugrade.monsterhuntingboard.exception.DuplicateResourceException;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.InvalidHuntStateException;
import se.edugrade.monsterhuntingboard.exception.PartyFullException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.HunterGeneratedHuntProgress;
import se.edugrade.monsterhuntingboard.model.WeatherContext;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HunterGeneratedHuntProgressRepository;
import se.edugrade.monsterhuntingboard.repository.HuntParticipationRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;
import se.edugrade.monsterhuntingboard.util.RewardResult;

@Service
@RequiredArgsConstructor
public class HuntService {
    private static final Logger log = LoggerFactory.getLogger(HuntService.class);
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");
    private static final long GROUP_LOBBY_WINDOW_MINUTES = 10L;
    private static final int SOLO_HUNT_MAX_WINS = 5;

    private final HuntRepository huntRepository;
    private final BeastRepository beastRepository;
    private final HunterRepository hunterRepository;
    private final HuntParticipationRepository huntParticipationRepository;
    private final HunterGeneratedHuntProgressRepository hunterGeneratedHuntProgressRepository;
    private final BattleService battleService;
    private final WeatherService weatherService;
    private final Map<Long, Map<Long, HuntResultResponse>> completedGroupBattleResults = new ConcurrentHashMap<>();
    private final Map<Long, Object> groupCompletionLocks = new ConcurrentHashMap<>();

    @Transactional
    public HuntResponse createHunt(CreateHuntRequest request) {
        List<Beast> beasts = resolveBeasts(request.beastIds());
        HuntConfiguration configuration = buildHuntConfiguration(request);

        Hunt hunt = Hunt.builder()
                .title(request.title())
                .type(request.type())
                .difficulty(request.difficulty())
                .status(configuration.status())
                .startTime(configuration.startTime())
                .maxPartySize(configuration.maxPartySize())
                .beasts(beasts)
                .rewardExp(request.rewardExp())
                .rewardGold(request.rewardGold())
                .build();

        Hunt savedHunt = huntRepository.save(hunt);
        log.info("Created hunt: {} (id={})", savedHunt.getTitle(), savedHunt.getId());
        return HuntResponse.from(
                savedHunt,
                Math.toIntExact(huntParticipationRepository.countByHuntId(savedHunt.getId())),
                0,
                getSoloHuntMaxWins(savedHunt),
                false
        );
    }

    @Transactional
    public List<HuntResponse> getAllHunts(String username) {
        Hunter hunter = findHunterByUsername(username);
        List<Hunt> hunts = huntRepository.findAll()
                .stream()
                .peek(this::updateHuntStatusIfNeeded)
                .filter(this::isVisibleOnBoard)
                .toList();

        return buildHuntResponses(hunts, hunter);
    }

    @Transactional
    public HuntResponse getHuntById(Long id, String username) {
        Hunt hunt = getHuntOrThrow(id);
        updateHuntStatusIfNeeded(hunt);
        Hunter hunter = findHunterByUsername(username);
        return buildHuntResponse(hunt, hunter, findProgressByHuntId(hunter, List.of(hunt)));
    }

    @Transactional
    public GroupLobbyResponse getGroupLobby(Long huntId, String username) {
        Hunt hunt = getHuntOrThrow(huntId);
        updateHuntStatusIfNeeded(hunt);

        if (hunt.getType() != HuntType.HUNT) {
            throw new InvalidGameRuleException("Only group hunts have a lobby");
        }

        Hunter hunter = getHunterOrThrow(username);
        List<HuntParticipation> participations = huntParticipationRepository.findByHuntIdOrderByJoinedAtAscIdAsc(hunt.getId());
        boolean joined = participations.stream()
                .anyMatch(participation -> participation.getHunter().getId().equals(hunter.getId()));
        if (!joined) {
            throw new InvalidHuntStateException("Join the hunt before entering the lobby");
        }
        if (hunt.getStartTime() != null) {
            LocalDateTime lobbyOpensAt = hunt.getStartTime().minusMinutes(GROUP_LOBBY_WINDOW_MINUTES);
            LocalDateTime now = getCurrentTime();
            if (hunt.getStatus() == HuntStatus.SCHEDULED && now.isBefore(lobbyOpensAt)) {
                throw new InvalidHuntStateException("Lobby opens 10 minutes before the hunt starts");
            }
        }

        List<GroupLobbyParticipantResponse> participants = participations.stream()
                .map(HuntParticipation::getHunter)
                .map(GroupLobbyParticipantResponse::from)
                .toList();

        return GroupLobbyResponse.from(hunt, participations.size(), joined, participants);
    }

    @Transactional
    public List<HuntResponse> getScheduledHunts(String username) {
        Hunter hunter = findHunterByUsername(username);
        List<Hunt> hunts = huntRepository.findByStatus(HuntStatus.SCHEDULED)
                .stream()
                .peek(this::updateHuntStatusIfNeeded)
                .filter(hunt -> hunt.getStatus() == HuntStatus.SCHEDULED && isVisibleOnBoard(hunt))
                .toList();
        return buildHuntResponses(hunts, hunter);
    }

    @Transactional
    public JoinHuntResponse joinHunt(Long huntId, String username) {
        Hunt hunt = getHuntOrThrow(huntId);
        Hunter hunter = getHunterOrThrow(username);
        updateHuntStatusIfNeeded(hunt);

        if (hunt.getType() != HuntType.HUNT) {
            throw new InvalidGameRuleException("Only HUNT missions can be joined");
        }
        validateGeneratedHuntAvailability(hunt);
        validateHunterHasRemainingGeneratedEligibility(hunt, hunter);
        validateHunterHasRemainingSoloWins(hunt, hunter);
        validateHuntHasNotStarted(hunt);
        if (hunt.getStatus() != HuntStatus.SCHEDULED && hunt.getStatus() != HuntStatus.ACTIVE) {
            throw new InvalidHuntStateException("This hunt cannot be joined in its current state");
        }
        if (huntParticipationRepository.existsByHunterIdAndHuntId(hunter.getId(), hunt.getId())) {
            throw new DuplicateResourceException("Hunter already joined this hunt");
        }

        long currentPartySize = huntParticipationRepository.countByHuntId(hunt.getId());
        if (hunt.getMaxPartySize() != null && currentPartySize >= hunt.getMaxPartySize()) {
            throw new PartyFullException("Hunt is already full");
        }

        huntParticipationRepository.save(createParticipation(hunter, hunt));
        log.info("Hunter {} joined hunt {}", hunter.getDisplayName(), hunt.getTitle());

        return JoinHuntResponse.from(hunt, hunter, (int) (currentPartySize + 1), "Hunter joined the hunt successfully");
    }

    @Transactional
    public HuntResultResponse startSoloHunt(Long huntId, String username, CompleteHuntRequest request) {
        Hunt hunt = getHuntOrThrow(huntId);
        Hunter hunter = getHunterOrThrow(username);
        updateHuntStatusIfNeeded(hunt);

        if (hunt.getType() != HuntType.SOLO_HUNT) {
            throw new InvalidGameRuleException("Only SOLO_HUNT missions can be started here");
        }
        validateGeneratedHuntAvailability(hunt);
        validateHunterHasRemainingGeneratedEligibility(hunt, hunter);
        validateHunterHasRemainingSoloWins(hunt, hunter);
        validateSoloHuntIsActive(hunt);
        validateHunterCanFight(hunter);

        HuntParticipation savedParticipation = huntParticipationRepository.save(getOrCreateSoloParticipation(hunter, hunt));
        WeatherContext weatherContext = weatherService.getCurrentWeatherForHunter(hunter);
        SoloBattleSimulation simulation = battleService.simulateSoloBattle(hunt, hunter, weatherContext.effect());
        return applySoloResult(hunt, hunter, savedParticipation, simulation, weatherContext);
    }

    @Transactional
    public HuntResultResponse completeHuntForCurrentHunter(Long huntId, String username, CompleteHuntRequest request) {
        Hunt hunt = getHuntOrThrow(huntId);
        Hunter hunter = getHunterOrThrow(username);
        updateHuntStatusIfNeeded(hunt);

        HuntParticipation participation = huntParticipationRepository
                .findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found for this hunter and hunt"));

        if (hunt.getType() != HuntType.HUNT) {
            throw new InvalidGameRuleException("Only HUNT missions can be completed here");
        }

        synchronized (groupCompletionLocks.computeIfAbsent(hunt.getId(), ignored -> new Object())) {
            HuntResultResponse cachedResult = findCachedGroupBattleResult(hunt.getId(), hunter.getId());
            if (cachedResult != null) {
                return cachedResult;
            }

            if ((hunt.getStatus() == HuntStatus.COMPLETED || hunt.getStatus() == HuntStatus.FAILED)
                    && participation.isCompleted()) {
                return getCachedGroupBattleResult(hunt.getId(), hunter.getId());
            }

            validateHuntCanBeCompleted(hunt);
            if (participation.isCompleted()) {
                return getCachedGroupBattleResult(hunt.getId(), hunter.getId());
            }
            validateGeneratedHuntAvailability(hunt);
            validateHunterHasRemainingGeneratedEligibility(hunt, hunter);
            validateHunterCanFight(hunter);

            List<HuntParticipation> participations = huntParticipationRepository.findByHuntIdOrderByJoinedAtAscIdAsc(hunt.getId());
            Map<Long, GroupParticipantBattleContext> participantWeatherContexts = buildParticipantWeatherContexts(participations);
            GroupBattleSimulation simulation = battleService.simulateGroupBossBattle(hunt, participations, participantWeatherContexts);
            return applyGroupResult(hunt, participations, hunter.getId(), simulation, participantWeatherContexts);
        }
    }

    @Transactional
    public HuntResponse updateHunt(Long id, UpdateHuntRequest request) {
        Hunt hunt = getHuntOrThrow(id);

        if (request.title() != null) {
            hunt.setTitle(request.title());
        }
        if (request.difficulty() != null) {
            hunt.setDifficulty(request.difficulty());
        }
        if (request.status() != null) {
            hunt.setStatus(request.status());
        }
        if (request.rewardExp() != null) {
            hunt.setRewardExp(request.rewardExp());
        }
        if (request.rewardGold() != null) {
            hunt.setRewardGold(request.rewardGold());
        }
        if (request.beastIds() != null) {
            if (request.beastIds().isEmpty()) {
                throw new InvalidGameRuleException("A hunt must contain at least one beast");
            }
            hunt.setBeasts(resolveBeasts(request.beastIds()));
        }

        applyTypeSpecificUpdates(hunt, request);
        validateUpdatedHunt(hunt);
        updateHuntStatusIfNeeded(hunt);

        Hunt savedHunt = huntRepository.save(hunt);
        log.info("Updated hunt: {} (id={})", savedHunt.getTitle(), savedHunt.getId());
        return HuntResponse.from(
                savedHunt,
                Math.toIntExact(huntParticipationRepository.countByHuntId(savedHunt.getId())),
                0,
                getSoloHuntMaxWins(savedHunt),
                false
        );
    }

    @Transactional
    public void deleteHunt(Long id) {
        Hunt hunt = getHuntOrThrow(id);

        if (huntParticipationRepository.existsByHuntId(hunt.getId()) && !canDeleteHuntWithParticipations(hunt)) {
            throw new InvalidGameRuleException("Cannot delete hunt because hunters have already joined it");
        }

        if (canDeleteHuntWithParticipations(hunt)) {
            huntParticipationRepository.deleteAll(huntParticipationRepository.findByHuntId(hunt.getId()));
        }
        huntRepository.delete(hunt);
        log.info("Deleted hunt id={}", id);
    }

    private List<Beast> resolveBeasts(List<Long> beastIds) {
        Set<Long> uniqueIds = new HashSet<>(beastIds);
        if (uniqueIds.size() != beastIds.size()) {
            throw new InvalidGameRuleException("A hunt cannot contain duplicate beasts");
        }

        List<Beast> foundBeasts = beastRepository.findAllById(beastIds);
        Map<Long, Beast> beastsById = foundBeasts.stream()
                .collect(Collectors.toMap(Beast::getId, Function.identity()));

        return beastIds.stream()
                .map(beastId -> {
                    Beast beast = beastsById.get(beastId);
                    if (beast == null) {
                        throw new ResourceNotFoundException("Beast not found with id: " + beastId);
                    }
                    return beast;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private HuntConfiguration buildHuntConfiguration(CreateHuntRequest request) {
        if (request.type() == HuntType.HUNT) {
            return buildPartyHuntConfiguration(request);
        }
        return buildSoloHuntConfiguration(request);
    }

    private HuntConfiguration buildPartyHuntConfiguration(CreateHuntRequest request) {
        validateDifficultyMatchesType(request.type(), request.difficulty());
        if (request.startTime() == null) {
            throw new InvalidGameRuleException("startTime is required for HUNT");
        }
        if (request.maxPartySize() == null) {
            throw new InvalidGameRuleException("maxPartySize is required for HUNT");
        }
        if (request.maxPartySize() < 1) {
            throw new InvalidGameRuleException("maxPartySize must be at least 1");
        }

        HuntStatus status = request.status() != null ? request.status() : HuntStatus.SCHEDULED;
        return new HuntConfiguration(request.startTime(), request.maxPartySize(), status);
    }

    private HuntConfiguration buildSoloHuntConfiguration(CreateHuntRequest request) {
        validateDifficultyMatchesType(request.type(), request.difficulty());
        HuntStatus status = request.status() != null ? request.status() : HuntStatus.ACTIVE;
        return new HuntConfiguration(null, null, status);
    }

    private void applyTypeSpecificUpdates(Hunt hunt, UpdateHuntRequest request) {
        if (hunt.getType() == HuntType.SOLO_HUNT) {
            if (request.maxPartySize() != null) {
                throw new InvalidGameRuleException("Cannot set maxPartySize for SOLO_HUNT");
            }
            if (request.startTime() != null) {
                throw new InvalidGameRuleException("Cannot set startTime for SOLO_HUNT");
            }
            hunt.setMaxPartySize(null);
            hunt.setStartTime(null);
            return;
        }

        if (request.startTime() != null) {
            hunt.setStartTime(request.startTime());
        }
        if (request.maxPartySize() != null) {
            hunt.setMaxPartySize(request.maxPartySize());
        }
    }

    private void validateUpdatedHunt(Hunt hunt) {
        validateDifficultyMatchesType(hunt.getType(), hunt.getDifficulty());
        if (hunt.getType() == HuntType.SOLO_HUNT) {
            if (hunt.getMaxPartySize() != null || hunt.getStartTime() != null) {
                throw new InvalidGameRuleException("SOLO_HUNT cannot have maxPartySize or startTime");
            }
            return;
        }

        long participantCount = huntParticipationRepository.countByHuntId(hunt.getId());
        if ((hunt.getStatus() == HuntStatus.SCHEDULED || hunt.getStatus() == HuntStatus.ACTIVE)
                && hunt.getMaxPartySize() == null) {
            throw new InvalidGameRuleException("maxPartySize is required for HUNT when status is SCHEDULED or ACTIVE");
        }
        if (hunt.getMaxPartySize() != null && hunt.getMaxPartySize() < participantCount) {
            throw new InvalidGameRuleException("maxPartySize cannot be less than current number of participants");
        }
        if (hunt.getStatus() == HuntStatus.SCHEDULED && hunt.getStartTime() == null) {
            throw new InvalidGameRuleException("startTime is required when hunt status is SCHEDULED");
        }
    }

    private Hunt getHuntOrThrow(Long id) {
        return huntRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hunt not found with id: " + id));
    }

    private Hunter getHunterOrThrow(String username) {
        return hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));
    }

    private HuntResultResponse applySoloResult(
            Hunt hunt,
            Hunter hunter,
            HuntParticipation participation,
            SoloBattleSimulation simulation,
            WeatherContext weatherContext
    ) {
        int initialHunterMaxHp = hunter.getBaseHp();
        boolean won = simulation.hunterWon();
        boolean expPotionApplied = hunter.isExpPotionActive() && won;
        boolean endurancePotionApplied = hunter.isEndurancePotionActive();
        int previousLevel = hunter.getLevel();
        int currentLevelFloorExp = GameBalanceUtil.getLevelFloorExp(previousLevel);
        WeatherEffect weatherEffect = weatherContext.effect();

        RewardResult rewardResult = won
                ? calculateWinRewardForHunter(hunt, hunter, expPotionApplied, weatherEffect)
                : new RewardResult(calculateLossPenalty(previousLevel, weatherEffect), 0);

        int adjustedExp = hunter.getExp() + rewardResult.expChange();
        int newExp = won ? Math.max(0, adjustedExp) : Math.max(currentLevelFloorExp, adjustedExp);
        int newGold = won ? hunter.getGold() + rewardResult.goldChange() : hunter.getGold();
        int newLevel = GameBalanceUtil.calculateLevel(newExp);
        int newBaseHp = GameBalanceUtil.calculateBaseHp(newLevel, hunter.getAppearance());
        int newCurrentHp = won
                ? (newLevel > previousLevel ? newBaseHp : simulation.hunterRemainingHp())
                : newBaseHp;

        hunter.setExp(newExp);
        hunter.setGold(newGold);
        hunter.setLevel(newLevel);
        hunter.setBaseHp(newBaseHp);
        hunter.setCurrentHp(newCurrentHp);
        hunter.setExpPotionActive(false);
        hunter.setEndurancePotionActive(false);
        if (won) {
            registerGeneratedHuntWin(hunter, hunt);
            registerSoloHuntWin(hunter, hunt);
        }

        participation.setCompleted(true);
        participation.setWon(won);
        participation.setExpChange(rewardResult.expChange());
        participation.setGoldChange(rewardResult.goldChange());
        participation.setCompletedAt(getCurrentTime());
        log.info("Completed solo hunt {} for hunter {} with result {}", hunt.getTitle(), hunter.getDisplayName(), won ? "WIN" : "LOSS");

        return HuntResultResponse.from(
                hunt,
                hunter,
                simulation.initialHunterHp(),
                initialHunterMaxHp,
                simulation.initialMonsterHp(),
                simulation.initialMonsterHp(),
                won,
                rewardResult.expChange(),
                rewardResult.goldChange(),
                newLevel > previousLevel,
                simulation.damageTaken(),
                expPotionApplied,
                endurancePotionApplied,
                WeatherResponse.from(weatherContext),
                List.of(ParticipantWeatherResponse.from(new GroupParticipantBattleContext(
                        hunter.getId(),
                        hunter.getDisplayName(),
                        weatherContext
                ))),
                List.of(BattleParticipantResponse.from(
                        hunter,
                        simulation.initialHunterHp(),
                        initialHunterMaxHp,
                        simulation.hunterRemainingHp(),
                        simulation.hunterRemainingHp() > 0,
                        simulation.damageTaken(),
                        calculateSoloDamageDealt(simulation.turns(), hunter.getId()),
                        rewardResult.expChange(),
                        rewardResult.goldChange()
                )),
                simulation.turns()
        );
    }

    private HuntResultResponse applyGroupResult(
            Hunt hunt,
            List<HuntParticipation> participations,
            Long currentHunterId,
            GroupBattleSimulation simulation,
            Map<Long, GroupParticipantBattleContext> participantWeatherContexts
    ) {
        boolean won = simulation.huntersWon();
        LocalDateTime completedAt = getCurrentTime();
        Map<Long, HunterBattleOutcome> outcomes = simulation.hunterOutcomes();
        Map<Long, InitialParticipantState> initialStates = participations.stream()
                .collect(Collectors.toMap(
                        participation -> participation.getHunter().getId(),
                        participation -> new InitialParticipantState(
                                participation.getHunter().getCurrentHp(),
                                participation.getHunter().getBaseHp(),
                                participation.getHunter().getLevel(),
                                participation.getHunter().isExpPotionActive(),
                                participation.getHunter().isEndurancePotionActive()
                        ),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        Map<Long, HuntParticipation> participationsByHunterId = participations.stream()
                .collect(Collectors.toMap(participation -> participation.getHunter().getId(), Function.identity()));
        List<ParticipantWeatherResponse> participantWeather = participantWeatherContexts.values().stream()
                .map(ParticipantWeatherResponse::from)
                .toList();
        for (HuntParticipation partyParticipation : participations) {
            Hunter partyHunter = partyParticipation.getHunter();
            HunterBattleOutcome outcome = outcomes.get(partyHunter.getId());
            if (outcome == null) {
                throw new InvalidGameRuleException("Missing battle outcome for hunter " + partyHunter.getDisplayName());
            }
            GroupParticipantBattleContext participantWeatherContext = participantWeatherContexts.get(partyHunter.getId());
            if (participantWeatherContext == null) {
                throw new InvalidGameRuleException("Missing weather context for hunter " + partyHunter.getDisplayName());
            }

            applyGroupHunterOutcome(
                    hunt,
                    partyHunter,
                    participationsByHunterId.get(partyHunter.getId()),
                    outcome,
                    won,
                    completedAt,
                    participantWeatherContext
            );
        }

        hunt.setStatus(won ? HuntStatus.COMPLETED : HuntStatus.FAILED);
        List<BattleParticipantResponse> battleParticipants = buildGroupBattleParticipants(
                participations,
                initialStates,
                outcomes
        );
        Map<Long, HuntResultResponse> battleResultsByHunterId = new java.util.LinkedHashMap<>();
        for (HuntParticipation partyParticipation : participations) {
            Hunter partyHunter = partyParticipation.getHunter();
            battleResultsByHunterId.put(
                    partyHunter.getId(),
                    buildGroupHuntResultResponse(
                            hunt,
                            partyHunter,
                            participationsByHunterId.get(partyHunter.getId()),
                            outcomes.get(partyHunter.getId()),
                            initialStates.get(partyHunter.getId()),
                            participantWeatherContexts.get(partyHunter.getId()),
                            participantWeather,
                            battleParticipants,
                            simulation
                    )
            );
        }
        completedGroupBattleResults.put(hunt.getId(), battleResultsByHunterId);
        log.info("Completed group hunt {} with result {}", hunt.getTitle(), won ? "WIN" : "LOSS");
        HuntResultResponse currentHunterResult = battleResultsByHunterId.get(currentHunterId);
        if (currentHunterResult == null) {
            throw new ResourceNotFoundException("Current hunter result not found for this hunt");
        }
        return currentHunterResult;
    }

    private HuntResultResponse buildGroupHuntResultResponse(
            Hunt hunt,
            Hunter currentHunter,
            HuntParticipation currentParticipation,
            HunterBattleOutcome currentOutcome,
            InitialParticipantState currentInitialState,
            GroupParticipantBattleContext currentWeatherContext,
            List<ParticipantWeatherResponse> participantWeather,
            List<BattleParticipantResponse> battleParticipants,
            GroupBattleSimulation simulation
    ) {
        if (currentHunter == null || currentParticipation == null || currentOutcome == null
                || currentWeatherContext == null || currentInitialState == null) {
            throw new ResourceNotFoundException("Current hunter result not found for this hunt");
        }

        return HuntResultResponse.from(
                hunt,
                currentHunter,
                currentInitialState.initialHp(),
                currentInitialState.initialMaxHp(),
                simulation.initialBossHp(),
                simulation.initialBossHp(),
                simulation.huntersWon(),
                currentParticipation.getExpChange(),
                currentParticipation.getGoldChange(),
                currentHunter.getLevel() > currentInitialState.initialLevel(),
                currentOutcome.damageTaken(),
                currentInitialState.expPotionActive() && simulation.huntersWon(),
                currentInitialState.endurancePotionActive(),
                WeatherResponse.from(currentWeatherContext.weatherContext()),
                participantWeather,
                battleParticipants,
                simulation.turns()
        );
    }

    private HuntResultResponse getCachedGroupBattleResult(Long huntId, Long hunterId) {
        HuntResultResponse result = findCachedGroupBattleResult(huntId, hunterId);
        if (result == null) {
            throw new InvalidHuntStateException("This hunt has already been completed and the battle result is no longer available");
        }
        return result;
    }

    private HuntResultResponse findCachedGroupBattleResult(Long huntId, Long hunterId) {
        return completedGroupBattleResults
                .getOrDefault(huntId, Map.of())
                .get(hunterId);
    }

    private void applyGroupHunterOutcome(
            Hunt hunt,
            Hunter hunter,
            HuntParticipation participation,
            HunterBattleOutcome outcome,
            boolean won,
            LocalDateTime completedAt,
            GroupParticipantBattleContext participantWeatherContext
    ) {
        boolean expPotionApplied = hunter.isExpPotionActive() && won;
        boolean endurancePotionApplied = hunter.isEndurancePotionActive();
        int previousLevel = hunter.getLevel();
        int currentLevelFloorExp = GameBalanceUtil.getLevelFloorExp(previousLevel);
        WeatherEffect weatherEffect = participantWeatherContext.weatherEffect();

        RewardResult rewardResult = won
                ? calculateWinRewardForHunter(hunt, hunter, expPotionApplied, weatherEffect)
                : new RewardResult(calculateLossPenalty(previousLevel, weatherEffect), 0);

        int adjustedExp = hunter.getExp() + rewardResult.expChange();
        int newExp = won ? Math.max(0, adjustedExp) : Math.max(currentLevelFloorExp, adjustedExp);
        int newGold = won ? hunter.getGold() + rewardResult.goldChange() : hunter.getGold();
        int newLevel = GameBalanceUtil.calculateLevel(newExp);
        int newBaseHp = GameBalanceUtil.calculateBaseHp(newLevel, hunter.getAppearance());
        int newCurrentHp = won
                ? (newLevel > previousLevel ? newBaseHp : outcome.remainingHp())
                : newBaseHp;

        hunter.setExp(newExp);
        hunter.setGold(newGold);
        hunter.setLevel(newLevel);
        hunter.setBaseHp(newBaseHp);
        hunter.setCurrentHp(newCurrentHp);
        hunter.setExpPotionActive(false);
        hunter.setEndurancePotionActive(false);
        if (won) {
            registerGeneratedHuntWin(hunter, hunt);
        }

        participation.setCompleted(true);
        participation.setWon(won);
        participation.setExpChange(rewardResult.expChange());
        participation.setGoldChange(rewardResult.goldChange());
        participation.setCompletedAt(completedAt);
    }

    private int calculateSoloDamageDealt(List<BattleTurnResponse> turns, Long hunterId) {
        String combatantId = "hunter-" + hunterId;
        return turns.stream()
                .filter(turn -> combatantId.equals(turn.attackerCombatantId()))
                .mapToInt(BattleTurnResponse::damage)
                .sum();
    }

    private List<BattleParticipantResponse> buildGroupBattleParticipants(
            List<HuntParticipation> participations,
            Map<Long, InitialParticipantState> initialStates,
            Map<Long, HunterBattleOutcome> outcomes
    ) {
        return participations.stream()
                .map(HuntParticipation::getHunter)
                .map(hunter -> {
                    InitialParticipantState initialState = initialStates.get(hunter.getId());
                    HunterBattleOutcome outcome = outcomes.get(hunter.getId());
                    if (initialState == null || outcome == null) {
                        throw new InvalidGameRuleException("Missing group battle participant state for hunter " + hunter.getDisplayName());
                    }
                    return BattleParticipantResponse.from(
                            hunter,
                            initialState.initialHp(),
                            initialState.initialMaxHp(),
                            hunter.getCurrentHp(),
                            outcome.remainingHp() > 0,
                            outcome.damageTaken(),
                            outcome.damageDealt(),
                            findParticipationExpChange(participations, hunter.getId()),
                            findParticipationGoldChange(participations, hunter.getId())
                    );
                })
                .toList();
    }

    private int findParticipationExpChange(List<HuntParticipation> participations, Long hunterId) {
        return participations.stream()
                .filter(participation -> participation.getHunter().getId().equals(hunterId))
                .mapToInt(HuntParticipation::getExpChange)
                .findFirst()
                .orElse(0);
    }

    private int findParticipationGoldChange(List<HuntParticipation> participations, Long hunterId) {
        return participations.stream()
                .filter(participation -> participation.getHunter().getId().equals(hunterId))
                .mapToInt(HuntParticipation::getGoldChange)
                .findFirst()
                .orElse(0);
    }

    private Map<Long, GroupParticipantBattleContext> buildParticipantWeatherContexts(List<HuntParticipation> participations) {
        return participations.stream()
                .map(HuntParticipation::getHunter)
                .collect(Collectors.toMap(
                        Hunter::getId,
                        hunter -> new GroupParticipantBattleContext(
                                hunter.getId(),
                                hunter.getDisplayName(),
                                weatherService.getCurrentWeatherForHunter(hunter)
                        ),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
    }

    private HuntParticipation createParticipation(Hunter hunter, Hunt hunt) {
        return HuntParticipation.builder()
                .hunter(hunter)
                .hunt(hunt)
                .completed(false)
                .won(false)
                .expChange(0)
                .goldChange(0)
                .build();
    }

    private HuntParticipation getOrCreateSoloParticipation(Hunter hunter, Hunt hunt) {
        return huntParticipationRepository.findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .map(existingParticipation -> {
                    existingParticipation.setCompleted(false);
                    existingParticipation.setWon(false);
                    existingParticipation.setExpChange(0);
                    existingParticipation.setGoldChange(0);
                    existingParticipation.setCompletedAt(null);
                    return existingParticipation;
                })
                .orElseGet(() -> createParticipation(hunter, hunt));
    }

    private void validateHuntHasNotStarted(Hunt hunt) {
        if (hunt.getStartTime() != null && getCurrentTime().isAfter(hunt.getStartTime())) {
            throw new InvalidHuntStateException("Hunt has already started");
        }
    }

    private void validateHuntCanBeCompleted(Hunt hunt) {
        if (hunt.getStatus() != HuntStatus.ACTIVE) {
            throw new InvalidHuntStateException("Hunt must be ACTIVE to be completed");
        }
    }

    private void validateSoloHuntIsActive(Hunt hunt) {
        if (hunt.getStatus() != HuntStatus.ACTIVE) {
            throw new InvalidHuntStateException("Solo hunt must be ACTIVE");
        }
    }

    private void validateHunterCanFight(Hunter hunter) {
        if (hunter.getCurrentHp() <= 0) {
            throw new InvalidGameRuleException("Hunter has no HP left and must recover before fighting");
        }
    }

    private void validateGeneratedHuntAvailability(Hunt hunt) {
        if (!hunt.isGenerated()) {
            return;
        }

        LocalDateTime now = getCurrentTime();
        if (hunt.getAvailableFrom() != null && now.isBefore(hunt.getAvailableFrom())) {
            throw new InvalidHuntStateException("This generated hunt is not available yet");
        }
        if (hunt.getExpiresAt() != null && !now.isBefore(hunt.getExpiresAt())) {
            throw new InvalidHuntStateException("This generated hunt is no longer available");
        }
    }

    private void validateHunterHasRemainingGeneratedEligibility(Hunt hunt, Hunter hunter) {
        if (!hunt.isGenerated() || hunt.getWinLimitPerHunter() == null) {
            return;
        }

        int currentWins = hunterGeneratedHuntProgressRepository.findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .map(HunterGeneratedHuntProgress::getWinCount)
                .orElse(0);
        if (currentWins >= hunt.getWinLimitPerHunter()) {
            throw new InvalidGameRuleException("Hunter has reached the win limit for this hunt");
        }
    }

    private void validateHunterHasRemainingSoloWins(Hunt hunt, Hunter hunter) {
        Integer maxWins = getSoloHuntMaxWins(hunt);
        if (maxWins == null) {
            return;
        }

        int currentWins = hunterGeneratedHuntProgressRepository.findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .map(HunterGeneratedHuntProgress::getWinCount)
                .orElse(0);
        if (currentWins >= maxWins) {
            throw new InvalidGameRuleException("Hunter has reached the win limit for this solo hunt");
        }
    }

    private void registerGeneratedHuntWin(Hunter hunter, Hunt hunt) {
        if (!hunt.isGenerated()) {
            return;
        }

        HunterGeneratedHuntProgress progress = hunterGeneratedHuntProgressRepository
                .findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .orElseGet(() -> HunterGeneratedHuntProgress.builder()
                        .hunter(hunter)
                        .hunt(hunt)
                        .winCount(0)
                        .build());
        progress.setWinCount(progress.getWinCount() + 1);
        progress.setLastWinAt(getCurrentTime());
        hunterGeneratedHuntProgressRepository.save(progress);
    }

    private void registerSoloHuntWin(Hunter hunter, Hunt hunt) {
        Integer maxWins = getSoloHuntMaxWins(hunt);
        if (maxWins == null || hunt.isGenerated()) {
            return;
        }

        HunterGeneratedHuntProgress progress = hunterGeneratedHuntProgressRepository
                .findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .orElseGet(() -> HunterGeneratedHuntProgress.builder()
                        .hunter(hunter)
                        .hunt(hunt)
                        .winCount(0)
                        .build());
        progress.setWinCount(Math.min(maxWins, progress.getWinCount() + 1));
        progress.setLastWinAt(getCurrentTime());
        hunterGeneratedHuntProgressRepository.save(progress);
    }

    private List<HuntResponse> buildHuntResponses(List<Hunt> hunts, Hunter hunter) {
        Map<Long, HunterGeneratedHuntProgress> progressByHuntId = findProgressByHuntId(hunter, hunts);
        return hunts.stream()
                .map(hunt -> buildHuntResponse(hunt, hunter, progressByHuntId))
                .toList();
    }

    private HuntResponse buildHuntResponse(
            Hunt hunt,
            Hunter hunter,
            Map<Long, HunterGeneratedHuntProgress> progressByHuntId
    ) {
        Integer maxWins = getSoloHuntMaxWins(hunt);
        int winCount = 0;
        boolean completed = hunt.getType() == HuntType.HUNT
                && (hunt.getStatus() == HuntStatus.COMPLETED || hunt.getStatus() == HuntStatus.FAILED);

        if (hunter != null && maxWins != null) {
            winCount = progressByHuntId.getOrDefault(hunt.getId(), null) != null
                    ? progressByHuntId.get(hunt.getId()).getWinCount()
                    : 0;
            completed = winCount >= maxWins;
        }

        return HuntResponse.from(
                hunt,
                Math.toIntExact(huntParticipationRepository.countByHuntId(hunt.getId())),
                winCount,
                maxWins,
                completed
        );
    }

    private Map<Long, HunterGeneratedHuntProgress> findProgressByHuntId(Hunter hunter, List<Hunt> hunts) {
        if (hunter == null || hunts.isEmpty()) {
            return Map.of();
        }

        List<Long> huntIds = hunts.stream().map(Hunt::getId).toList();
        return hunterGeneratedHuntProgressRepository.findByHunterIdAndHuntIdIn(hunter.getId(), huntIds)
                .stream()
                .collect(Collectors.toMap(progress -> progress.getHunt().getId(), Function.identity()));
    }

    private Integer getSoloHuntMaxWins(Hunt hunt) {
        if (hunt.getType() != HuntType.SOLO_HUNT) {
            return null;
        }
        return hunt.getWinLimitPerHunter() != null ? hunt.getWinLimitPerHunter() : SOLO_HUNT_MAX_WINS;
    }

    private Hunter findHunterByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return hunterRepository.findByUserAccountUsername(username).orElse(null);
    }

    private RewardResult calculateWinRewardForHunter(
            Hunt hunt,
            Hunter hunter,
            boolean expPotionApplied,
            WeatherEffect weatherEffect
    ) {
        RewardResult rewardResult = GameBalanceUtil.applyWinReward(hunt);
        int expReward = applyMultiplier(rewardResult.expChange(), weatherEffect.getExpRewardMultiplier());
        expReward = GameBalanceUtil.applyAppearanceExpBonus(expReward, hunter.getAppearance());
        if (expPotionApplied) {
            expReward = GameBalanceUtil.applyExpPotionBonus(expReward);
        }

        boolean fortuneSongTriggered = hunter.getAppearance() == se.edugrade.monsterhuntingboard.model.Appearance.BARD
                && ThreadLocalRandom.current().nextInt(100) < 20;
        int goldReward = applyMultiplier(rewardResult.goldChange(), weatherEffect.getGoldRewardMultiplier());
        goldReward = GameBalanceUtil.applyAppearanceGoldBonus(
                goldReward,
                hunter.getAppearance(),
                fortuneSongTriggered
        );

        return new RewardResult(expReward, goldReward);
    }

    private int calculateLossPenalty(int previousLevel, WeatherEffect weatherEffect) {
        int basePenalty = GameBalanceUtil.calculateLevelScaledExpLoss(previousLevel);
        return -applyMultiplier(basePenalty, weatherEffect.defeatPenaltyMultiplier());
    }

    private int applyMultiplier(int baseValue, double multiplier) {
        if (baseValue <= 0) {
            return baseValue;
        }
        return Math.max(1, (int) Math.round(baseValue * multiplier));
    }

    private void validateDifficultyMatchesType(HuntType type, Difficulty difficulty) {
        if (difficulty == null) {
            throw new InvalidGameRuleException("difficulty is required");
        }
    }

    private void updateHuntStatusIfNeeded(Hunt hunt) {
        LocalDateTime now = getCurrentTime();
        if (hunt.isGenerated() && hunt.getExpiresAt() != null && !now.isBefore(hunt.getExpiresAt())) {
            if (hunt.getStatus() == HuntStatus.SCHEDULED || hunt.getStatus() == HuntStatus.ACTIVE) {
                hunt.setStatus(HuntStatus.FAILED);
            }
            return;
        }

        if (hunt.getStatus() == HuntStatus.SCHEDULED
                && hunt.getStartTime() != null
                && now.isAfter(hunt.getStartTime())) {
            hunt.setStatus(HuntStatus.ACTIVE);
        } else if (hunt.isGenerated()
                && hunt.getType() == HuntType.SOLO_HUNT
                && hunt.getStatus() == HuntStatus.SCHEDULED
                && (hunt.getAvailableFrom() == null || !now.isBefore(hunt.getAvailableFrom()))) {
            hunt.setStatus(HuntStatus.ACTIVE);
        }
    }

    private boolean isVisibleOnBoard(Hunt hunt) {
        if (!hunt.isGenerated()) {
            return true;
        }

        LocalDateTime now = getCurrentTime();
        boolean afterAvailability = hunt.getAvailableFrom() == null || !now.isBefore(hunt.getAvailableFrom());
        boolean beforeExpiry = hunt.getExpiresAt() == null || now.isBefore(hunt.getExpiresAt());
        return afterAvailability
                && beforeExpiry
                && (hunt.getStatus() == HuntStatus.SCHEDULED || hunt.getStatus() == HuntStatus.ACTIVE);
    }

    private boolean canDeleteHuntWithParticipations(Hunt hunt) {
        return hunt.getType() == HuntType.HUNT
                && (hunt.getStatus() == HuntStatus.COMPLETED || hunt.getStatus() == HuntStatus.FAILED);
    }

    private LocalDateTime getCurrentTime() {
        return ZonedDateTime.now(STOCKHOLM_ZONE).toLocalDateTime();
    }

    private record InitialParticipantState(
            int initialHp,
            int initialMaxHp,
            int initialLevel,
            boolean expPotionActive,
            boolean endurancePotionActive
    ) {
    }

    private record HuntConfiguration(
            LocalDateTime startTime,
            Integer maxPartySize,
            HuntStatus status
    ) {
    }
}
