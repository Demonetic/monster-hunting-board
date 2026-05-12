package se.edugrade.monsterhuntingboard.util;

import se.edugrade.monsterhuntingboard.model.Appearance;
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

    public static int calculateLevelScaledExpLoss(int level) {
        return 20 + (level * 8);
    }

    public static int calculateBaseHp(int level, Appearance appearance) {
        int baseHp = 100 + ((level - 1) * 10);
        if (appearance == Appearance.PALADIN) {
            baseHp += 15;
        }
        return baseHp;
    }

    public static RewardResult applyWinReward(Hunt hunt) {
        return new RewardResult(hunt.getRewardExp(), hunt.getRewardGold());
    }

    public static int applyExpPotionBonus(int expReward) {
        if (expReward <= 0) {
            return expReward;
        }
        return expReward + Math.max(1, Math.round(expReward * 0.1f));
    }

    public static int applyAppearanceExpBonus(int expReward, Appearance appearance) {
        if (expReward <= 0 || appearance != Appearance.MAGE) {
            return expReward;
        }
        return expReward + Math.max(1, Math.round(expReward * 0.1f));
    }

    public static int applyAppearanceGoldBonus(int goldReward, Appearance appearance, boolean fortuneSongTriggered) {
        if (goldReward <= 0 || appearance != Appearance.BARD || !fortuneSongTriggered) {
            return goldReward;
        }
        return goldReward + Math.max(1, Math.round(goldReward * 0.1f));
    }
}
