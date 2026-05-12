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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.model.Beast;
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

        List<HuntTemplate> templates = new ArrayList<>();
        templates.add(buildTemplate("Wayfinder Trial", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.EASY, 60, 30, null, List.of()));
        templates.add(buildTemplate("Stonewatch Sweep", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.EASY, 70, 35, null, List.of()));
        templates.add(buildTemplate("Wildpath Trial", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 105, 65, null, List.of()));
        templates.add(buildTemplate("Skyline Sweep", HuntSourceType.REPEATABLE, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 115, 72, null, List.of()));

        templates.add(buildTemplate("Daily Bounty I", HuntSourceType.DAILY_BOUNTY, HuntType.SOLO_HUNT, Difficulty.EASY, 80, 40, null, List.of()));
        templates.add(buildTemplate("Daily Bounty II", HuntSourceType.DAILY_BOUNTY, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 120, 80, null, List.of()));
        templates.add(buildTemplate("Daily Bounty III", HuntSourceType.DAILY_BOUNTY, HuntType.SOLO_HUNT, Difficulty.MEDIUM, 125, 82, null, List.of()));

        templates.add(buildTemplate("Weekly Contract I", HuntSourceType.WEEKLY_CONTRACT, HuntType.SOLO_HUNT, Difficulty.HARD, 190, 130, null, List.of()));
        templates.add(buildTemplate("Weekly Contract II", HuntSourceType.WEEKLY_CONTRACT, HuntType.SOLO_HUNT, Difficulty.HARD, 210, 145, null, List.of()));
        templates.add(buildTemplate("Weekly Contract III", HuntSourceType.WEEKLY_CONTRACT, HuntType.SOLO_HUNT, Difficulty.HARD, 240, 180, null, List.of()));

        templates.add(buildTemplate("Daily Boss I", HuntSourceType.DAILY_BOSS, HuntType.HUNT, Difficulty.BOSS, 420, 520, 4, List.of()));
        templates.add(buildTemplate("Daily Boss II", HuntSourceType.DAILY_BOSS, HuntType.HUNT, Difficulty.BOSS, 450, 560, 4, List.of()));
        templates.add(buildTemplate("Daily Boss III", HuntSourceType.DAILY_BOSS, HuntType.HUNT, Difficulty.BOSS, 470, 590, 4, List.of()));

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

        List<Beast> beastPool = shuffledBeastPool(HuntSourceType.DAILY_BOSS, dayStart, existingBosses);
        if (beastPool.isEmpty()) {
            return;
        }

        List<HuntTemplate> templates = shuffledTemplates(HuntSourceType.DAILY_BOSS, dayStart);
        List<LocalTime> bossTimes = List.of(LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(19, 0));

        for (int index = existingBosses.size(); index < Math.min(3, templates.size()); index++) {
            HuntTemplate template = templates.get(index);
            LocalDateTime bossStart = date.atTime(bossTimes.get(index));
            List<Beast> selectedBeasts = selectGeneratedBeasts(beastPool, index - existingBosses.size(), 1);
            huntRepository.save(buildGeneratedHunt(
                    template,
                    dayStart,
                    nextDayStart,
                    bossStart,
                    bossStart.minusMinutes(10),
                    1,
                    selectedBeasts
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

        List<Beast> beastPool = shuffledBeastPool(sourceType, periodStart, existing);
        if (beastPool.isEmpty()) {
            return;
        }

        List<HuntTemplate> templates = shuffledTemplates(sourceType, periodStart);
        int missingCount = Math.min(targetCount - existing.size(), templates.size());
        for (int index = 0; index < missingCount; index++) {
            HuntTemplate template = templates.get(index);
            List<Beast> selectedBeasts = selectGeneratedBeasts(beastPool, index, 1);
            huntRepository.save(buildGeneratedHunt(
                    template,
                    periodStart,
                    periodEnd,
                    startHour == null ? null : periodStart.withHour(startHour),
                    null,
                    sourceType == HuntSourceType.REPEATABLE ? 5 : 1,
                    selectedBeasts
            ));
        }
    }

    private List<HuntTemplate> shuffledTemplates(HuntSourceType sourceType, LocalDateTime rotationStart) {
        List<HuntTemplate> templates = new ArrayList<>(huntTemplateRepository.findByActiveTrueAndSourceType(sourceType));
        Collections.shuffle(templates, new Random(buildRotationSeed(sourceType, rotationStart, "templates")));
        return templates;
    }

    private List<Beast> shuffledBeastPool(HuntSourceType sourceType, LocalDateTime rotationStart, List<Hunt> existingHunts) {
        List<Beast> beastPool = new ArrayList<>(beastRepository.findAll());
        Collections.shuffle(beastPool, new Random(buildRotationSeed(sourceType, rotationStart, "beasts")));
        if (beastPool.isEmpty()) {
            return List.of();
        }

        Set<Long> usedBeastIds = existingHunts.stream()
                .flatMap(hunt -> hunt.getBeasts().stream())
                .map(Beast::getId)
                .collect(Collectors.toSet());

        List<Beast> availableBeasts = beastPool.stream()
                .filter(beast -> !usedBeastIds.contains(beast.getId()))
                .toList();

        return availableBeasts.isEmpty() ? beastPool : new ArrayList<>(availableBeasts);
    }

    private List<Beast> selectGeneratedBeasts(List<Beast> beastPool, int offset, int count) {
        List<Beast> selectedBeasts = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            selectedBeasts.add(beastPool.get((offset + index) % beastPool.size()));
        }
        return selectedBeasts;
    }

    private Hunt buildGeneratedHunt(
            HuntTemplate template,
            LocalDateTime availableFrom,
            LocalDateTime expiresAt,
            LocalDateTime startTime,
            LocalDateTime roomOpensAt,
            Integer winLimitPerHunter,
            List<Beast> selectedBeasts
    ) {
        return Hunt.builder()
                .title(buildGeneratedTitle(template, availableFrom, startTime, selectedBeasts))
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
                .beasts(selectedBeasts)
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

    private String buildGeneratedTitle(
            HuntTemplate template,
            LocalDateTime availableFrom,
            LocalDateTime startTime,
            List<Beast> selectedBeasts
    ) {
        String beastLabel = selectedBeasts.stream()
                .map(Beast::getName)
                .collect(Collectors.joining(" / "));
        String rotationLabel = switch (template.getSourceType()) {
            case REPEATABLE -> "Repeatable Hunt";
            case DAILY_BOUNTY -> "Daily Bounty";
            case WEEKLY_CONTRACT -> "Weekly Contract";
            case DAILY_BOSS -> "Daily Boss";
            case MANUAL -> template.getTitle();
        };

        if (template.getSourceType() == HuntSourceType.DAILY_BOSS && startTime != null) {
            return "%s: %s - %s".formatted(rotationLabel, beastLabel, startTime.toLocalTime());
        }
        return "%s: %s - %s".formatted(rotationLabel, beastLabel, availableFrom.toLocalDate());
    }

    private long buildRotationSeed(HuntSourceType sourceType, LocalDateTime rotationStart, String scope) {
        long daySeed = rotationStart.toLocalDate().toEpochDay();
        long sourceSeed = (long) sourceType.ordinal() * 9_973L;
        long scopeSeed = scope.hashCode();
        return daySeed * 1_003L + sourceSeed + scopeSeed;
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
