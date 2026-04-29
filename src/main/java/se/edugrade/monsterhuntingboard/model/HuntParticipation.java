package se.edugrade.monsterhuntingboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hunt_participations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hunter_id", "hunt_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HuntParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hunter_id", nullable = false)
    private Hunter hunter;

    @ManyToOne
    @JoinColumn(name = "hunt_id", nullable = false)
    private Hunt hunt;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean won;

    @Column(nullable = false)
    private int expChange;

    @Column(nullable = false)
    private int goldChange;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
