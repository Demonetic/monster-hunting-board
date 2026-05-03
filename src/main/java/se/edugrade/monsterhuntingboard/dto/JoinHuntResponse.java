package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.Hunter;

public record JoinHuntResponse(
        Long huntId,
        String huntTitle,
        Long hunterId,
        String hunterDisplayName,
        int currentPartySize,
        Integer maxPartySize,
        String message
) {
    public static JoinHuntResponse from(Hunt hunt, Hunter hunter, int currentPartySize, String message) {
        return new JoinHuntResponse(
                hunt.getId(),
                hunt.getTitle(),
                hunter.getId(),
                hunter.getDisplayName(),
                currentPartySize,
                hunt.getMaxPartySize(),
                message
        );
    }
}
