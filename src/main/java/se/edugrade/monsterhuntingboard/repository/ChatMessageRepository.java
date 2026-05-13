package se.edugrade.monsterhuntingboard.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import se.edugrade.monsterhuntingboard.model.ChatMessage;
import se.edugrade.monsterhuntingboard.model.ChatType;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatTypeOrderByCreatedAtDescIdDesc(ChatType chatType, Pageable pageable);

    List<ChatMessage> findByChatTypeAndLobbyIdOrderByCreatedAtDescIdDesc(
            ChatType chatType,
            Long lobbyId,
            Pageable pageable
    );
}
