package se.edugrade.monsterhuntingboard.util;

import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;

public final class GameBalanceUtil {
    private static final int BASE_EXP_TO_LEVEL_UP = 200;
    private static final int EXP_INCREMENT_PER_LEVEL = 50;

    private GameBalanceUtil() {
    }

    public static int calculateLevel(int exp) {
        int level = 1;

        while (exp >= getLevelFloorExp(level + 1)) {
            level++;
        }

        return level;
    }

    public static int getLevelFloorExp(int level) {
        if (level <= 1) {
            return 0;
        }

        int totalExp = 0;
        for (int currentLevel = 1; currentLevel < level; currentLevel++) {
            totalExp += getExpRequiredForNextLevel(currentLevel);
        }
        return totalExp;
    }

    public static int getExpRequiredForNextLevel(int currentLevel) {
        if (currentLevel < 1) {
            throw new IllegalArgumentException("currentLevel must be at least 1");
        }

        return BASE_EXP_TO_LEVEL_UP + ((currentLevel - 1) * EXP_INCREMENT_PER_LEVEL);
    }

    public static int calculateBaseHp(int level) {
        return 100 + ((level - 1) * 10);
    }

    public static int calculateLossExp(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> -10;
            case MEDIUM -> -25;
            case HARD -> -50;
            case BOSS -> -100;
        };
    }

    public static RewardResult applyWinReward(Hunt hunt) {
        return new RewardResult(hunt.getRewardExp(), hunt.getRewardGold());
    }

    public static RewardResult applyLoss(Difficulty difficulty) {
        return new RewardResult(calculateLossExp(difficulty), 0);
    }

    public static int applyExpPotionBonus(int expReward) {
        if (expReward <= 0) {
            return expReward;
        }
        return expReward + Math.max(1, Math.round(expReward * 0.1f));
    }
}
