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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Table(name = "hunters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hunter {
    public static final String DEFAULT_CITY = "Stockholm";
    public static final String DEFAULT_COUNTRY = "Sweden";
    public static final double DEFAULT_LATITUDE = 59.3293;
    public static final double DEFAULT_LONGITUDE = 18.0686;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String displayName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Appearance appearance;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int exp;

    @Column(nullable = false)
    private int gold;

    @Column(nullable = false)
    private int baseHp;

    @Column(nullable = false)
    private int currentHp;

    @Column(nullable = false)
    @Builder.Default
    private String city = DEFAULT_CITY;

    @Column(nullable = false)
    @Builder.Default
    private String country = DEFAULT_COUNTRY;

    @Column(nullable = false)
    @Builder.Default
    private double latitude = DEFAULT_LATITUDE;

    @Column(nullable = false)
    @Builder.Default
    private double longitude = DEFAULT_LONGITUDE;

    @Column(nullable = false)
    @Builder.Default
    private boolean expPotionActive = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean endurancePotionActive = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Builder.Default
    @OneToMany(mappedBy = "hunter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HuntParticipation> participations = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "hunter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HunterInventoryItem> inventoryItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (city == null || city.isBlank()) {
            city = DEFAULT_CITY;
        }
        if (country == null || country.isBlank()) {
            country = DEFAULT_COUNTRY;
        }
        if (latitude == 0d) {
            latitude = DEFAULT_LATITUDE;
        }
        if (longitude == 0d) {
            longitude = DEFAULT_LONGITUDE;
        }
    }
}
