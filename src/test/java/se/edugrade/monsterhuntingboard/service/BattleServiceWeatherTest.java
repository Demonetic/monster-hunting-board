package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;
import se.edugrade.monsterhuntingboard.model.HuntStatus;
import se.edugrade.monsterhuntingboard.model.HuntType;
import se.edugrade.monsterhuntingboard.model.WeatherCategory;
import se.edugrade.monsterhuntingboard.model.WeatherEffect;

class BattleServiceWeatherTest {

    private final BattleService battleService = new BattleService();

    @Test
    void rainIncreasesDamageTakenForHardAndBossBeasts() {
        Hunt hardHunt = huntWithDifficulty(Difficulty.HARD);

        int neutralDamage = battleService.calculateDamageTaken(
                hardHunt,
                true,
                false,
                WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
        );
        int rainyDamage = battleService.calculateDamageTaken(
                hardHunt,
                true,
                false,
                WeatherEffect.fromCategory(WeatherCategory.RAIN)
        );

        assertThat(rainyDamage).isGreaterThan(neutralDamage);
    }

    @Test
    void windyReducesBeastDamage() {
        Hunt easyHunt = huntWithDifficulty(Difficulty.EASY);

        int neutralDamage = battleService.calculateDamageTaken(
                easyHunt,
                true,
                false,
                WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
        );
        int windyDamage = battleService.calculateDamageTaken(
                easyHunt,
                true,
                false,
                WeatherEffect.fromCategory(WeatherCategory.WINDY)
        );

        assertThat(windyDamage).isLessThan(neutralDamage);
    }

    @Test
    void extremeWeatherIncreasesEnemyDamage() {
        Hunt bossHunt = huntWithDifficulty(Difficulty.BOSS);

        int neutralDamage = battleService.calculateDamageTaken(
                bossHunt,
                true,
                false,
                WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
        );
        int extremeDamage = battleService.calculateDamageTaken(
                bossHunt,
                true,
                false,
                WeatherEffect.fromCategory(WeatherCategory.EXTREME_WEATHER)
        );

        assertThat(extremeDamage).isGreaterThan(neutralDamage);
    }

    @Test
    void personalRainOnlyIncreasesDamageForRainTarget() {
        int baseDamage = 20;

        int rainyTargetDamage = battleService.adjustBeastDamageAgainstHunter(
                baseDamage,
                Difficulty.BOSS,
                1,
                false,
                Appearance.MAGE,
                WeatherEffect.fromCategory(WeatherCategory.RAIN)
        );
        int sunnyTargetDamage = battleService.adjustBeastDamageAgainstHunter(
                baseDamage,
                Difficulty.BOSS,
                1,
                false,
                Appearance.MAGE,
                WeatherEffect.fromCategory(WeatherCategory.SUNNY_CLEAR)
        );

        assertThat(rainyTargetDamage).isGreaterThan(sunnyTargetDamage);
    }

    @Test
    void snowyWeatherIncreasesDamageAgainstSameHunter() {
        int baseDamage = 25;

        int snowyDamage = battleService.adjustBeastDamageAgainstHunter(
                baseDamage,
                Difficulty.MEDIUM,
                1,
                false,
                Appearance.MAGE,
                WeatherEffect.fromCategory(WeatherCategory.SNOW)
        );
        int cloudyDamage = battleService.adjustBeastDamageAgainstHunter(
                baseDamage,
                Difficulty.MEDIUM,
                1,
                false,
                Appearance.MAGE,
                WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
        );

        assertThat(snowyDamage).isGreaterThan(cloudyDamage);
    }

    @Test
    void knightPassiveReducesDamageUnderSameWeather() {
        int baseDamage = 25;

        int knightDamage = battleService.adjustBeastDamageAgainstHunter(
                baseDamage,
                Difficulty.MEDIUM,
                1,
                false,
                Appearance.KNIGHT,
                WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
        );
        int mageDamage = battleService.adjustBeastDamageAgainstHunter(
                baseDamage,
                Difficulty.MEDIUM,
                1,
                false,
                Appearance.MAGE,
                WeatherEffect.fromCategory(WeatherCategory.CLOUDY_OVERCAST)
        );

        assertThat(knightDamage).isLessThan(mageDamage);
    }

    private Hunt huntWithDifficulty(Difficulty difficulty) {
        return Hunt.builder()
                .title("Weather Hunt")
                .type(HuntType.SOLO_HUNT)
                .difficulty(difficulty)
                .status(HuntStatus.ACTIVE)
                .beasts(List.of(Beast.builder()
                        .type(BeastType.GRIFFIN)
                        .difficulty(difficulty == Difficulty.BOSS ? Difficulty.HARD : difficulty)
                        .hp(180)
                        .attackPower(40)
                        .rewardExp(100)
                        .rewardGold(80)
                        .build()))
                .rewardExp(100)
                .rewardGold(80)
                .build();
    }
}
