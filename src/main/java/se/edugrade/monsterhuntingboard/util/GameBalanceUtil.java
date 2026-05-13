package se.edugrade.monsterhuntingboard.util;

import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.model.Hunt;

public final class GameBalanceUtil {
    private static final int BASE_EXP_TO_LEVEL_UP = 200;
    private static final int EXP_INCREMENT_PER_LEVEL = 50;
    private static final double BOSS_PARTICIPANT_HP_MULTIPLIER = 0.45;

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

    public static int calculateSoloBeastBattleHp(Difficulty difficulty, int hunterLevel) {
        return calculateBeastBattleHp(difficulty, hunterLevel, 1);
    }

    public static int calculateBeastBattleHp(Difficulty difficulty, int averageHunterLevel, int participantCount) {
        int safeLevel = Math.max(1, averageHunterLevel);
        int effectiveHp = getHuntBeastBaseHp(difficulty)
                + ((safeLevel - 1) * getHuntBeastHpPerLevel(difficulty));

        if (difficulty != Difficulty.BOSS) {
            return effectiveHp;
        }

        int safeParticipantCount = Math.max(1, participantCount);
        double participantMultiplier = 1.0 + (BOSS_PARTICIPANT_HP_MULTIPLIER * (safeParticipantCount - 1));
        return Math.max(1, (int) Math.round(effectiveHp * participantMultiplier));
    }

    public static int calculateSoloBeastBattleAttack(Difficulty difficulty, int hunterLevel) {
        return calculateBeastBattleAttack(difficulty, hunterLevel, 1);
    }

    public static int calculateBeastBattleAttack(Difficulty difficulty, int averageHunterLevel, int participantCount) {
        int safeLevel = Math.max(1, averageHunterLevel);
        int effectiveAttack = getHuntBeastBaseAttack(difficulty)
                + ((safeLevel - 1) * getHuntBeastAttackPerLevel(difficulty));

        if (difficulty != Difficulty.BOSS) {
            return effectiveAttack;
        }

        int safeParticipantCount = Math.max(1, participantCount);
        return effectiveAttack + ((safeParticipantCount - 1) * 1);
    }

    private static int getHuntBeastBaseHp(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 80;
            case MEDIUM -> 130;
            case HARD -> 190;
            case BOSS -> 300;
        };
    }

    private static int getHuntBeastHpPerLevel(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 10;
            case MEDIUM -> 15;
            case HARD -> 22;
            case BOSS -> 30;
        };
    }

    private static int getHuntBeastBaseAttack(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 8;
            case MEDIUM -> 12;
            case HARD -> 10;
            case BOSS -> 16;
        };
    }

    private static int getHuntBeastAttackPerLevel(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 0;
            case BOSS -> 2;
        };
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
