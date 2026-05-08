package se.edugrade.monsterhuntingboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "beasts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
    private BeastType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
    private Difficulty difficulty;

    @Column(nullable = false)
    private int hp;

    @Column(nullable = false)
    private int attackPower;

    @Column(nullable = false)
    private int rewardExp;

    @Column(nullable = false)
    private int rewardGold;
}
