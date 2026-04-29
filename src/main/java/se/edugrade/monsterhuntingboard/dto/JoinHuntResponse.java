package se.edugrade.monsterhuntingboard.dto;

public record JoinHuntResponse(
        Long huntId,
        String huntTitle,
        Long hunterId,
        String hunterDisplayName,
        int currentPartySize,
        Integer maxPartySize,
        String message
) {
}
