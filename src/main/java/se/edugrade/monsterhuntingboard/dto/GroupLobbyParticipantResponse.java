package se.edugrade.monsterhuntingboard.dto;

import se.edugrade.monsterhuntingboard.model.Hunter;

public record GroupLobbyParticipantResponse(
        Long hunterId,
        String hunterName,
        String hunterAppearance
) {
    public static GroupLobbyParticipantResponse from(Hunter hunter) {
        return new GroupLobbyParticipantResponse(
                hunter.getId(),
                hunter.getDisplayName(),
                hunter.getAppearance().name()
        );
    }
}
