package se.edugrade.monsterhuntingboard.model;

import java.util.List;
import java.util.Map;

public record WeatherEffect(
        WeatherCategory category,
        String displayName,
        List<String> descriptions,
        double rewardMultiplier,
        double expMultiplier,
        double goldMultiplier,
        double defeatPenaltyMultiplier,
        double hunterDamageTakenMultiplier,
        double enduranceCostMultiplier,
        double beastResistanceMultiplier,
        double beastDamageMultiplier,
        double enemyDamageMultiplier,
        double attackRollMultiplier,
        double defaultHunterAttackRollMultiplier,
        Map<Appearance, Double> hunterAttackRollMultipliers,
        Map<Difficulty, Double> beastDamageMultipliersByDifficulty
) {
    public static WeatherEffect neutral() {
        return fromCategory(WeatherCategory.CLOUDY_OVERCAST);
    }

    public static WeatherEffect fromCategory(WeatherCategory category) {
        return switch (category) {
            case SUNNY_CLEAR -> new WeatherEffect(
                    category,
                    "Sunny / Clear",
                    List.of("+10% gold earned"),
                    1.0,
                    1.0,
                    1.10,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case CLOUDY_OVERCAST -> new WeatherEffect(
                    category,
                    "Cloudy / Overcast",
                    List.of("No weather effects"),
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case FOG_MIST -> new WeatherEffect(
                    category,
                    "Fog / Mist",
                    List.of("Hunter attack rolls -10%"),
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    0.90,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case DRIZZLE -> new WeatherEffect(
                    category,
                    "Drizzle",
                    List.of("+5% EXP", "All beasts deal +5% damage"),
                    1.0,
                    1.05,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.05,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case RAIN -> new WeatherEffect(
                    category,
                    "Rain",
                    List.of("+10% EXP", "Medium / Hard / Boss beasts deal +10% damage"),
                    1.0,
                    1.10,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of(
                            Difficulty.MEDIUM, 1.10,
                            Difficulty.HARD, 1.10,
                            Difficulty.BOSS, 1.10
                    )
            );
            case HEAVY_RAIN -> new WeatherEffect(
                    category,
                    "Heavy Rain",
                    List.of("+15% EXP", "Hard / Boss beasts deal +15% damage"),
                    1.0,
                    1.15,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of(
                            Difficulty.HARD, 1.15,
                            Difficulty.BOSS, 1.15
                    )
            );
            case THUNDERSTORM -> new WeatherEffect(
                    category,
                    "Thunderstorm",
                    List.of("+20% gold", "Defeat penalty +15%"),
                    1.0,
                    1.0,
                    1.20,
                    1.15,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case SNOW -> new WeatherEffect(
                    category,
                    "Snow",
                    List.of("Hunter takes +10% damage", "Endurance cost +5%"),
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.10,
                    1.05,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case HEAVY_SNOW -> new WeatherEffect(
                    category,
                    "Heavy Snow",
                    List.of("Beasts gain +5% resistance", "Endurance cost +10%"),
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.10,
                    1.05,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
            case WINDY -> new WeatherEffect(
                    category,
                    "Windy",
                    List.of("Ranger attack roll +10%", "Other hunters -5%", "Beasts deal -5% damage"),
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    0.95,
                    1.0,
                    1.0,
                    0.95,
                    Map.of(Appearance.RANGER, 1.10),
                    Map.of()
            );
            case EXTREME_WEATHER -> new WeatherEffect(
                    category,
                    "Extreme Weather",
                    List.of("+25% rewards", "Enemies deal +20% damage"),
                    1.25,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.0,
                    1.20,
                    1.0,
                    1.0,
                    Map.of(),
                    Map.of()
            );
        };
    }

    public double getHunterAttackRollMultiplier(Appearance appearance) {
        return attackRollMultiplier
                * hunterAttackRollMultipliers.getOrDefault(appearance, defaultHunterAttackRollMultiplier);
    }

    public double getBeastDamageMultiplier(Difficulty difficulty) {
        return beastDamageMultiplier
                * beastDamageMultipliersByDifficulty.getOrDefault(difficulty, 1.0)
                * enemyDamageMultiplier;
    }

    public double getExpRewardMultiplier() {
        return rewardMultiplier * expMultiplier;
    }

    public double getGoldRewardMultiplier() {
        return rewardMultiplier * goldMultiplier;
    }
}
