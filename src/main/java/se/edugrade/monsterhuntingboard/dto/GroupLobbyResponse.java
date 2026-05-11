package se.edugrade.monsterhuntingboard.dto;

import java.time.LocalDateTime;
import java.util.List;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;

public record GroupLobbyResponse(
        Long huntId,
        String huntTitle,
        HuntStatus status,
        LocalDateTime startTime,
        int currentPartySize,
        Integer maxPartySize,
        String beastName,
        String beastType,
        boolean joined,
        List<GroupLobbyParticipantResponse> participants
) {
    public static GroupLobbyResponse from(
            Hunt hunt,
            int currentPartySize,
            boolean joined,
            List<GroupLobbyParticipantResponse> participants
    ) {
        return new GroupLobbyResponse(
                hunt.getId(),
                hunt.getTitle(),
                hunt.getStatus(),
                hunt.getStartTime(),
                currentPartySize,
                hunt.getMaxPartySize(),
                hunt.getBeasts().isEmpty() ? "Unknown Beast" : hunt.getBeasts().getFirst().getName(),
                hunt.getBeasts().isEmpty() ? "UNKNOWN" : hunt.getBeasts().getFirst().getType().name(),
                joined,
                participants
        );
    }
}
