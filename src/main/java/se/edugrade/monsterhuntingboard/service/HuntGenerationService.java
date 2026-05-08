package se.edugrade.monsterhuntingboard.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntSourceType;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntTemplate;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;
import se.edugrade.monsterhuntingboard.repository.HuntTemplateRepository;

@Service
@RequiredArgsConstructor
public class HuntGenerationService {
    private static final Logger log = LoggerFactory.getLogger(HuntGenerationService.class);
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");

    private final HuntRepository huntRepository;
    private final HuntTemplateRepository huntTemplateRepository;
    private final BeastRepository beastRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generateCurrentRotationsOnStartup() {
        generateCurrentRotations();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Stockholm")
    @Transactional
    public void generateCurrentRotations() {
        ensureDefaultTemplates();

        LocalDate today = ZonedDateTime.now(STOCKHOLM_ZONE).toLocalDate();
        ensureDailyRotations(today);
        ensureWeeklyContracts(today);
    }

    @Transactional
    public void ensureDefaultTemplates() {
        if (huntTemplateRepository.count() > 0 || beastRepository.count() == 0) {
            return;
        }

        Beast basilisk = beastRepository.findFirstByType(BeastType.BASILISK).orElse(null);
        Beast griffin = beastRepository.findFirstByType(BeastType.GRIFFIN).orElse(null);
        Beast pegasus = beastRepository.findFirstByType(BeastType.PEGASUS).orElse(null);
        Beast chimera = beastRepository.findFirstByType(BeastType.CHIMERA).orElse(null);
        Beast phoenix = beastRepository.findFirstByType(BeastType.PHOENIX).orElse(null);
        Beast dragon = beastRepository.findFirstByType(BeastType.DRAGON).orElse(null);

        List<HuntTemplate> templates = new ArrayList<>();
        if (basilisk != null) {
            templates.add(buildTemplate("Basilisk Burrow Sweep", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.EASY, 60, 30, null, List.of(basilisk)));
            templates.add(buildTemplate("Stone Path Cleanup", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.EASY, 70, 35, null, List.of(basilisk)));
            templates.add(buildTemplate("Marsh Stalker Bounty", HuntSourceType.DAILY_BOUNTY, HuntType.SOLO_HUNT, Difficulty.EASY, 80, 40, null, List.of(basilisk)));
        }
        if (griffin != null) {
            templates.add(buildTemplate("Griffin Sky Trial", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 110, 70, null, List.of(griffin)));
            templates.add(buildTemplate("Ridge Recon Sweep", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 120, 75, null, List.of(griffin)));
            templates.add(buildTemplate("Feathered Menace Bounty", HuntSourceType.DAILY_BOUNTY, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 120, 80, null, List.of(griffin)));
        }
        if (pegasus != null) {
            templates.add(buildTemplate("Pegasus Sky Run", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 105, 65, null, List.of(pegasus)));
            templates.add(buildTemplate("Cloudwake Escort", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 115, 72, null, List.of(pegasus)));
            templates.add(buildTemplate("Silver Mane Bounty", HuntSourceType.DAILY_BOUNTY, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 125, 82, null, List.of(pegasus)));
        }
        if (chimera != null) {
            templates.add(buildTemplate("Chimera Fang Contract", HuntSourceType.WEEKLY_CONTRACT, HuntType.SOLO_HUNT, Difficulty.HARD, 190, 130, null, List.of(chimera)));
            templates.add(buildTemplate("Ravine Alpha Contract", HuntSourceType.WEEKLY_CONTRACT, HuntType.SOLO_HUNT, Difficulty.HARD, 210, 145, null, List.of(chimera)));
        }
        if (phoenix != null) {
            templates.add(buildTemplate("Phoenix Ember Contract", HuntSourceType.WEEKLY_CONTRACT, HuntType.SOLO_HUNT, Difficulty.HARD, 240, 180, null, List.of(phoenix)));
        }
        if (dragon != null) {
            templates.add(buildTemplate("Dragonfall Vanguard", HuntSourceType.DAILY_BOSS, HuntType.HUNT, Difficulty.BOSS, 420, 520, 4, List.of(dragon)));
            if (chimera != null) {
                templates.add(buildTemplate("Molten Crown Siege", HuntSourceType.DAILY_BOSS, HuntType.HUNT, Difficulty.BOSS, 450, 560, 4, List.of(dragon, chimera)));
            }
            if (phoenix != null) {
                templates.add(buildTemplate("Ashen Sky Cataclysm", HuntSourceType.DAILY_BOSS, HuntType.HUNT, Difficulty.BOSS, 470, 590, 4, List.of(dragon, phoenix)));
            }
        }

        if (!templates.isEmpty()) {
            huntTemplateRepository.saveAll(templates);
            log.info("Created {} default hunt templates", templates.size());
        }
    }

    private void ensureDailyRotations(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();

        ensureGeneratedHuntsForPeriod(HuntSourceType.REPEATABLE, 3, dayStart, nextDayStart, null);
        ensureGeneratedHuntsForPeriod(HuntSourceType.DAILY_BOUNTY, 2, dayStart, nextDayStart, null);
        ensureDailyBosses(date, dayStart, nextDayStart);
    }

    private void ensureWeeklyContracts(LocalDate date) {
        LocalDate weekStartDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndDate = weekStartDate.plusWeeks(1);
        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekEndDate.atStartOfDay();

        ensureGeneratedHuntsForPeriod(HuntSourceType.WEEKLY_CONTRACT, 3, weekStart, weekEnd, null);
    }

    private void ensureDailyBosses(LocalDate date, LocalDateTime dayStart, LocalDateTime nextDayStart) {
        List<Hunt> existingBosses = huntRepository
                .findByGeneratedIsTrueAndSourceTypeAndAvailableFromGreaterThanEqualAndAvailableFromLessThan(
                        HuntSourceType.DAILY_BOSS,
                        dayStart,
                        nextDayStart
                );
        if (existingBosses.size() >= 3) {
            return;
        }

        List<HuntTemplate> templates = shuffledTemplates(HuntSourceType.DAILY_BOSS);
        List<LocalTime> bossTimes = List.of(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(19, 0));

        for (int index = existingBosses.size(); index < Math.min(3, templates.size()); index++) {
            HuntTemplate template = templates.get(index);
            LocalDateTime bossStart = date.atTime(bossTimes.get(index));
            huntRepository.save(buildGeneratedHunt(
                    template,
                    dayStart,
                    nextDayStart,
                    bossStart,
                    bossStart.minusMinutes(10),
                    1
            ));
        }
    }

    private void ensureGeneratedHuntsForPeriod(
            HuntSourceType sourceType,
            int targetCount,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Integer startHour
    ) {
        List<Hunt> existing = huntRepository
                .findByGeneratedIsTrueAndSourceTypeAndAvailableFromGreaterThanEqualAndAvailableFromLessThan(
                        sourceType,
                        periodStart,
                        periodEnd
                );
        if (existing.size() >= targetCount) {
            return;
        }

        List<HuntTemplate> templates = shuffledTemplates(sourceType);
        int missingCount = Math.min(targetCount - existing.size(), templates.size());
        for (int index = 0; index < missingCount; index++) {
            HuntTemplate template = templates.get(index);
            huntRepository.save(buildGeneratedHunt(
                    template,
                    periodStart,
                    periodEnd,
                    startHour == null ? null : periodStart.withHour(startHour),
                    null,
                    sourceType == HuntSourceType.REPEATABLE ? 5 : 1
            ));
        }
    }

    private List<HuntTemplate> shuffledTemplates(HuntSourceType sourceType) {
        List<HuntTemplate> templates = new ArrayList<>(huntTemplateRepository.findByActiveTrueAndSourceType(sourceType));
        Collections.shuffle(templates);
        return templates;
    }

    private Hunt buildGeneratedHunt(
            HuntTemplate template,
            LocalDateTime availableFrom,
            LocalDateTime expiresAt,
            LocalDateTime startTime,
            LocalDateTime roomOpensAt,
            Integer winLimitPerHunter
    ) {
        return Hunt.builder()
                .title(buildGeneratedTitle(template, availableFrom, startTime))
                .type(template.getType())
                .difficulty(template.getDifficulty())
                .status(template.getSourceType() == HuntSourceType.DAILY_BOSS ? HuntStatus.SCHEDULED : HuntStatus.ACTIVE)
                .sourceType(template.getSourceType())
                .generated(true)
                .availableFrom(availableFrom)
                .startTime(startTime)
                .roomOpensAt(roomOpensAt)
                .expiresAt(expiresAt)
                .winLimitPerHunter(winLimitPerHunter)
                .maxPartySize(template.getMaxPartySize())
                .beasts(template.getBeasts())
                .rewardExp(scaleReward(template.getRewardExp(), template.getSourceType()))
                .rewardGold(scaleReward(template.getRewardGold(), template.getSourceType()))
                .build();
    }

    private int scaleReward(int baseReward, HuntSourceType sourceType) {
        float multiplier = switch (sourceType) {
            case REPEATABLE, MANUAL -> 1.0f;
            case DAILY_BOUNTY -> 1.2f;
            case WEEKLY_CONTRACT -> 1.35f;
            case DAILY_BOSS -> 1.5f;
        };
        return Math.max(1, Math.round(baseReward * multiplier));
    }

    private String buildGeneratedTitle(HuntTemplate template, LocalDateTime availableFrom, LocalDateTime startTime) {
        if (template.getSourceType() == HuntSourceType.DAILY_BOSS && startTime != null) {
            return "%s - %s".formatted(template.getTitle(), startTime.toLocalTime());
        }
        return "%s - %s".formatted(template.getTitle(), availableFrom.toLocalDate());
    }

    private HuntTemplate buildTemplate(
            String title,
            HuntSourceType sourceType,
            HuntType type,
            Difficulty difficulty,
            int rewardExp,
            int rewardGold,
            Integer maxPartySize,
            List<Beast> beasts
    ) {
        return HuntTemplate.builder()
                .title(title)
                .sourceType(sourceType)
                .type(type)
                .difficulty(difficulty)
                .rewardExp(rewardExp)
                .rewardGold(rewardGold)
                .maxPartySize(maxPartySize)
                .beasts(beasts)
                .build();
    }
}
