package se.edugrade.monsterhuntingboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.BattleTurnResponse;
import se.edugrade.monsterhuntingboard.dto.CompleteHuntRequest;
import se.edugrade.monsterhuntingboard.dto.CreateHuntRequest;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResultResponse;
import se.edugrade.monsterhuntingboard.dto.JoinHuntResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateHuntRequest;
import se.edugrade.monsterhuntingboard.exception.DuplicateResourceException;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.InvalidHuntStateException;
import se.edugrade.monsterhuntingboard.exception.PartyFullException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntParticipation;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntParticipationRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;
import se.edugrade.monsterhuntingboard.util.RewardResult;

@Service
@RequiredArgsConstructor
public class HuntService {
    private static final Logger log = LoggerFactory.getLogger(HuntService.class);

    private final HuntRepository huntRepository;
    private final BeastRepository beastRepository;
    private final HunterRepository hunterRepository;
    private final HuntParticipationRepository huntParticipationRepository;
    private final BattleService battleService;

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
        return HuntResponse.from(savedHunt, Math.toIntExact(huntParticipationRepository.countByHuntId(savedHunt.getId())));
    }

    @Transactional
    public List<HuntResponse> getAllHunts() {
        return huntRepository.findAll()
                .stream()
                .peek(this::updateHuntStatusIfNeeded)
                .map(hunt -> HuntResponse.from(
                        hunt,
                        Math.toIntExact(huntParticipationRepository.countByHuntId(hunt.getId()))
                ))
                .toList();
    }

    @Transactional
    public HuntResponse getHuntById(Long id) {
        Hunt hunt = getHuntOrThrow(id);
        updateHuntStatusIfNeeded(hunt);
        return HuntResponse.from(hunt, Math.toIntExact(huntParticipationRepository.countByHuntId(hunt.getId())));
    }

    @Transactional
    public List<HuntResponse> getScheduledHunts() {
        return huntRepository.findByStatus(HuntStatus.SCHEDULED)
                .stream()
                .peek(this::updateHuntStatusIfNeeded)
                .filter(hunt -> hunt.getStatus() == HuntStatus.SCHEDULED)
                .map(hunt -> HuntResponse.from(
                        hunt,
                        Math.toIntExact(huntParticipationRepository.countByHuntId(hunt.getId()))
                ))
                .toList();
    }

    @Transactional
    public JoinHuntResponse joinHunt(Long huntId, String username) {
        Hunt hunt = getHuntOrThrow(huntId);
        Hunter hunter = getHunterOrThrow(username);
        updateHuntStatusIfNeeded(hunt);

        if (hunt.getType() != HuntType.HUNT) {
            throw new InvalidGameRuleException("Only HUNT missions can be joined");
        }
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
        validateSoloHuntIsActive(hunt);
        validateHunterCanFight(hunter);

        HuntParticipation savedParticipation = huntParticipationRepository.save(createParticipation(hunter, hunt));
        SoloBattleSimulation simulation = battleService.simulateSoloBattle(hunt, hunter);
        return applySoloResult(hunt, hunter, savedParticipation, simulation);
    }

    @Transactional
    public HuntResultResponse completeHuntForCurrentHunter(Long huntId, String username, CompleteHuntRequest request) {
        Hunt hunt = getHuntOrThrow(huntId);
        Hunter hunter = getHunterOrThrow(username);
        updateHuntStatusIfNeeded(hunt);

        HuntParticipation participation = huntParticipationRepository
                .findByHunterIdAndHuntId(hunter.getId(), hunt.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found for this hunter and hunt"));

        validateHuntCanBeCompleted(hunt);
        if (participation.isCompleted()) {
            throw new InvalidHuntStateException("Hunt participation has already been completed");
        }
        validateHunterCanFight(hunter);
        if (hunt.getType() != HuntType.HUNT) {
            throw new InvalidGameRuleException("Only HUNT missions can be completed here");
        }

        List<HuntParticipation> participations = huntParticipationRepository.findByHuntIdOrderByJoinedAtAscIdAsc(hunt.getId());
        GroupBattleSimulation simulation = battleService.simulateGroupBossBattle(hunt, participations);
        return applyGroupResult(hunt, participations, hunter.getId(), simulation);
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
        return HuntResponse.from(savedHunt, Math.toIntExact(huntParticipationRepository.countByHuntId(savedHunt.getId())));
    }

    @Transactional
    public void deleteHunt(Long id) {
        Hunt hunt = getHuntOrThrow(id);

        if (huntParticipationRepository.existsByHuntId(hunt.getId())) {
            throw new InvalidGameRuleException("Cannot delete hunt because hunters have already joined it");
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
            SoloBattleSimulation simulation
    ) {
        boolean won = simulation.hunterWon();
        boolean expPotionApplied = hunter.isExpPotionActive() && won;
        boolean endurancePotionApplied = hunter.isEndurancePotionActive();
        int previousLevel = hunter.getLevel();
        int currentLevelFloorExp = GameBalanceUtil.getLevelFloorExp(previousLevel);

        RewardResult rewardResult = won
                ? calculateWinRewardForHunter(hunt, hunter, expPotionApplied)
                : new RewardResult(-GameBalanceUtil.calculateLevelScaledExpLoss(previousLevel), 0);

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

        participation.setCompleted(true);
        participation.setWon(won);
        participation.setExpChange(rewardResult.expChange());
        participation.setGoldChange(rewardResult.goldChange());
        participation.setCompletedAt(LocalDateTime.now());
        log.info("Completed solo hunt {} for hunter {} with result {}", hunt.getTitle(), hunter.getDisplayName(), won ? "WIN" : "LOSS");

        return HuntResultResponse.from(
                hunt,
                hunter,
                won,
                rewardResult.expChange(),
                rewardResult.goldChange(),
                simulation.damageTaken(),
                expPotionApplied,
                endurancePotionApplied,
                simulation.turns()
        );
    }

    private HuntResultResponse applyGroupResult(
            Hunt hunt,
            List<HuntParticipation> participations,
            Long currentHunterId,
            GroupBattleSimulation simulation
    ) {
        boolean won = simulation.huntersWon();
        LocalDateTime completedAt = LocalDateTime.now();
        Map<Long, HunterBattleOutcome> outcomes = simulation.hunterOutcomes();
        Map<Long, HuntParticipation> participationsByHunterId = participations.stream()
                .collect(Collectors.toMap(participation -> participation.getHunter().getId(), Function.identity()));

        HuntResultResponse currentHunterResponse = null;
        for (HuntParticipation partyParticipation : participations) {
            Hunter partyHunter = partyParticipation.getHunter();
            HunterBattleOutcome outcome = outcomes.get(partyHunter.getId());
            if (outcome == null) {
                throw new InvalidGameRuleException("Missing battle outcome for hunter " + partyHunter.getDisplayName());
            }

            HuntResultResponse hunterResult = applyGroupHunterOutcome(
                    hunt,
                    partyHunter,
                    participationsByHunterId.get(partyHunter.getId()),
                    outcome,
                    won,
                    completedAt,
                    simulation.turns()
            );
            if (partyHunter.getId().equals(currentHunterId)) {
                currentHunterResponse = hunterResult;
            }
        }

        hunt.setStatus(won ? HuntStatus.COMPLETED : HuntStatus.FAILED);
        if (currentHunterResponse == null) {
            throw new ResourceNotFoundException("Current hunter result not found for this hunt");
        }
        log.info("Completed group hunt {} with result {}", hunt.getTitle(), won ? "WIN" : "LOSS");
        return currentHunterResponse;
    }

    private HuntResultResponse applyGroupHunterOutcome(
            Hunt hunt,
            Hunter hunter,
            HuntParticipation participation,
            HunterBattleOutcome outcome,
            boolean won,
            LocalDateTime completedAt,
            List<BattleTurnResponse> turns
    ) {
        boolean expPotionApplied = hunter.isExpPotionActive() && won;
        boolean endurancePotionApplied = hunter.isEndurancePotionActive();
        int previousLevel = hunter.getLevel();
        int currentLevelFloorExp = GameBalanceUtil.getLevelFloorExp(previousLevel);

        RewardResult rewardResult = won
                ? calculateWinRewardForHunter(hunt, hunter, expPotionApplied)
                : new RewardResult(-GameBalanceUtil.calculateLevelScaledExpLoss(previousLevel), 0);

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

        participation.setCompleted(true);
        participation.setWon(won);
        participation.setExpChange(rewardResult.expChange());
        participation.setGoldChange(rewardResult.goldChange());
        participation.setCompletedAt(completedAt);

        return HuntResultResponse.from(
                hunt,
                hunter,
                won,
                rewardResult.expChange(),
                rewardResult.goldChange(),
                outcome.damageTaken(),
                expPotionApplied,
                endurancePotionApplied,
                turns
        );
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

    private void validateHuntHasNotStarted(Hunt hunt) {
        if (hunt.getStartTime() != null && LocalDateTime.now().isAfter(hunt.getStartTime())) {
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

    private RewardResult calculateWinRewardForHunter(Hunt hunt, Hunter hunter, boolean expPotionApplied) {
        RewardResult rewardResult = GameBalanceUtil.applyWinReward(hunt);
        int expReward = GameBalanceUtil.applyAppearanceExpBonus(rewardResult.expChange(), hunter.getAppearance());
        if (expPotionApplied) {
            expReward = GameBalanceUtil.applyExpPotionBonus(expReward);
        }

        boolean fortuneSongTriggered = hunter.getAppearance() == se.edugrade.monsterhuntingboard.model.Appearance.BARD
                && ThreadLocalRandom.current().nextInt(100) < 20;
        int goldReward = GameBalanceUtil.applyAppearanceGoldBonus(
                rewardResult.goldChange(),
                hunter.getAppearance(),
                fortuneSongTriggered
        );

        return new RewardResult(expReward, goldReward);
    }

    private void validateDifficultyMatchesType(HuntType type, Difficulty difficulty) {
        if (type == HuntType.HUNT && difficulty != Difficulty.BOSS) {
            throw new InvalidGameRuleException("HUNT missions must use BOSS difficulty");
        }
        if (type == HuntType.SOLO_HUNT && difficulty == Difficulty.BOSS) {
            throw new InvalidGameRuleException("SOLO_HUNT missions cannot use BOSS difficulty");
        }
    }

    private void updateHuntStatusIfNeeded(Hunt hunt) {
        if (hunt.getStatus() == HuntStatus.SCHEDULED
                && hunt.getStartTime() != null
                && LocalDateTime.now().isAfter(hunt.getStartTime())) {
            hunt.setStatus(HuntStatus.ACTIVE);
        }
    }

    private record HuntConfiguration(
            LocalDateTime startTime,
            Integer maxPartySize,
            HuntStatus status
    ) {
    }
}
