package se.edugrade.monsterhuntingboard.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hunts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hunt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HuntType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HuntStatus status;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HuntSourceType sourceType = HuntSourceType.MANUAL;

    @Column(name = "is_generated", nullable = false)
    @Builder.Default
    private boolean generated = false;

    private LocalDateTime availableFrom;

    private LocalDateTime startTime;

    private LocalDateTime roomOpensAt;

    private LocalDateTime expiresAt;

    private Integer winLimitPerHunter;

    private Integer maxPartySize;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "hunt_beasts",
            joinColumns = @JoinColumn(name = "hunt_id"),
            inverseJoinColumns = @JoinColumn(name = "beast_id")
    )
    private List<Beast> beasts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "hunt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HuntParticipation> participations = new ArrayList<>();

    @Column(nullable = false)
    private int rewardExp;

    @Column(nullable = false)
    private int rewardGold;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        normalizeBeastsCollection();
        this.createdAt = LocalDateTime.now();
    }

    private void normalizeBeastsCollection() {
        this.beasts = beasts == null ? new ArrayList<>() : new ArrayList<>(beasts);
    }
}
