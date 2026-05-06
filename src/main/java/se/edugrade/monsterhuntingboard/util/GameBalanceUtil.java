package se.edugrade.monsterhuntingboard.util;

import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;

public final class GameBalanceUtil {

    private GameBalanceUtil() {
    }

    public static int calculateLevel(int exp) {
        return 1 + (exp / 100);
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
