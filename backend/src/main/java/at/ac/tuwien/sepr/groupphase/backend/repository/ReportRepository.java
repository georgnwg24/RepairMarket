package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.ChatMessage;
import at.ac.tuwien.sepr.groupphase.backend.entity.Report;
import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByReporter(ApplicationUser reporter);

    List<Report> findAllByTarget(ApplicationUser target);

    Optional<Report> findById(Long id);

    Page<Report> findAllByIsOpen(boolean isOpen, Pageable pageable);

    @Query("""
        SELECT r FROM Report r
        WHERE r.isOpen = :isOpen
          AND (
                LOWER(r.reporter.username) LIKE LOWER(CONCAT('%', :username, '%')) OR
                LOWER(r.target.username) LIKE LOWER(CONCAT('%', :username, '%'))
              )
        """)
    Page<Report> findAllByIsOpenAndUsernameContainingIgnoreCase(boolean isOpen, String username, Pageable pageable);

    List<Report> findAllByType(ReportType type);

    /**
     * Deletes all Licenses with an ID less than the specified value.
     *
     * @param l the threshold ID; all Licenses with an ID less than this value will be deleted
     */
    void deleteAllByIdLessThan(long l);

    Optional<Report> findByReporterAndChatMessage(ApplicationUser reporter, ChatMessage chatMessage);

    @Modifying
    @Query("UPDATE Report r SET r.chatMessage = null WHERE r.chatMessage = :message")
    void nullifyChatMessageReferences(@Param("message") ChatMessage message);

    boolean existsByJobRequest_Id(Long jobRequestId);

}
