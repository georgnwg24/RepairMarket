package at.ac.tuwien.sepr.groupphase.backend.entity;

import at.ac.tuwien.sepr.groupphase.backend.type.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a report in the persistent data store.
 */
@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private ApplicationUser reporter;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private ApplicationUser target;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_request_id")
    private JobRequest jobRequest;

    @ManyToOne
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage;

    @Enumerated(EnumType.STRING)
    private ReportType type;

    @Column(length = 1023)
    private String reason;

    @Builder.Default
    private boolean isOpen = true;

    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();

}
