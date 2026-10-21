package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Chat;
import at.ac.tuwien.sepr.groupphase.backend.entity.JobRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    default void deleteChat(Chat chat, ChatMessageRepository chatMessageRepository) {
        chatMessageRepository.deleteAll(chat.getChatMessages());
        this.delete(chat);
    }

    void deleteAllByIdLessThan(long l);

    boolean existsByJobRequest(JobRequest jobRequest);

    Optional<Chat> findChatByJobRequest_Id(long jobRequestId);
}
