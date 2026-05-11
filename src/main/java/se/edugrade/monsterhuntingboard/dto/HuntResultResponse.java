package se.edugrade.monsterhuntingboard.dto;

import java.util.List;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.Hunter;

public record HuntResultResponse(
        Long huntId,
        String huntTitle,
        String hunterName,
        String hunterAppearance,
        int initialHunterHp,
        int initialHunterMaxHp,
        String beastName,
        String beastType,
        int initialBeastHp,
        int initialBeastMaxHp,
        boolean won,
        int expChange,
        int goldChange,
        int newExp,
        int newGold,
        int newLevel,
        int newBaseHp,
        int newCurrentHp,
        int damageTaken,
        boolean expPotionApplied,
        boolean endurancePotionApplied,
        WeatherResponse weather,
        List<ParticipantWeatherResponse> participantWeather,
        List<BattleTurnResponse> turns
) {
    public static HuntResultResponse from(
            Hunt hunt,
            Hunter hunter,
            int initialHunterHp,
            int initialHunterMaxHp,
            int initialBeastHp,
            int initialBeastMaxHp,
            boolean won,
            int expChange,
            int goldChange,
            int damageTaken,
            boolean expPotionApplied,
            boolean endurancePotionApplied,
            WeatherResponse weather,
            List<ParticipantWeatherResponse> participantWeather,
            List<BattleTurnResponse> turns
    ) {
        return new HuntResultResponse(
                hunt.getId(),
                hunt.getTitle(),
                hunter.getDisplayName(),
                hunter.getAppearance().name(),
                initialHunterHp,
                initialHunterMaxHp,
                hunt.getBeasts().isEmpty() ? "Unknown Beast" : hunt.getBeasts().getFirst().getName(),
                hunt.getBeasts().isEmpty() ? "UNKNOWN" : hunt.getBeasts().getFirst().getType().name(),
                initialBeastHp,
                initialBeastMaxHp,
                won,
                expChange,
                goldChange,
                hunter.getExp(),
                hunter.getGold(),
                hunter.getLevel(),
                hunter.getBaseHp(),
                hunter.getCurrentHp(),
                damageTaken,
                expPotionApplied,
                endurancePotionApplied,
                weather,
                participantWeather,
                turns
        );
    }
}
