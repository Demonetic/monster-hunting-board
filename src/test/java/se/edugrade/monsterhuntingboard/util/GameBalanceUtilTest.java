package se.edugrade.monsterhuntingboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Difficulty;

class GameBalanceUtilTest {

    @Test
    void levelThresholdsScaleGradually() {
        assertThat(GameBalanceUtil.getExpRequiredForNextLevel(1)).isEqualTo(200);
        assertThat(GameBalanceUtil.getExpRequiredForNextLevel(2)).isEqualTo(250);
        assertThat(GameBalanceUtil.getExpRequiredForNextLevel(3)).isEqualTo(300);

        assertThat(GameBalanceUtil.getLevelFloorExp(1)).isEqualTo(0);
        assertThat(GameBalanceUtil.getLevelFloorExp(2)).isEqualTo(200);
        assertThat(GameBalanceUtil.getLevelFloorExp(3)).isEqualTo(450);
        assertThat(GameBalanceUtil.getLevelFloorExp(4)).isEqualTo(750);

        assertThat(GameBalanceUtil.calculateLevel(0)).isEqualTo(1);
        assertThat(GameBalanceUtil.calculateLevel(199)).isEqualTo(1);
        assertThat(GameBalanceUtil.calculateLevel(200)).isEqualTo(2);
        assertThat(GameBalanceUtil.calculateLevel(449)).isEqualTo(2);
        assertThat(GameBalanceUtil.calculateLevel(450)).isEqualTo(3);
        assertThat(GameBalanceUtil.calculateLevel(750)).isEqualTo(4);
    }

    @Test
    void appearanceBonusesApplyToHpAndRewards() {
        assertThat(GameBalanceUtil.calculateBaseHp(1, Appearance.KNIGHT)).isEqualTo(100);
        assertThat(GameBalanceUtil.calculateBaseHp(1, Appearance.PALADIN)).isEqualTo(115);
        assertThat(GameBalanceUtil.applyAppearanceExpBonus(100, Appearance.MAGE)).isEqualTo(110);
        assertThat(GameBalanceUtil.applyAppearanceExpBonus(100, Appearance.RANGER)).isEqualTo(100);
        assertThat(GameBalanceUtil.applyAppearanceGoldBonus(75, Appearance.BARD, true)).isEqualTo(83);
        assertThat(GameBalanceUtil.applyAppearanceGoldBonus(75, Appearance.BARD, false)).isEqualTo(75);
    }

    @Test
    void huntBeastHpScalesByDifficultyAndHunterLevel() {
        assertThat(GameBalanceUtil.calculateSoloBeastBattleHp(Difficulty.EASY, 1)).isEqualTo(80);
        assertThat(GameBalanceUtil.calculateSoloBeastBattleHp(Difficulty.EASY, 4)).isEqualTo(110);
        assertThat(GameBalanceUtil.calculateSoloBeastBattleHp(Difficulty.MEDIUM, 3)).isEqualTo(160);
        assertThat(GameBalanceUtil.calculateSoloBeastBattleHp(Difficulty.HARD, 3)).isEqualTo(234);
    }

    @Test
    void bossHpScalesByAverageLevelAndParticipantCount() {
        assertThat(GameBalanceUtil.calculateBeastBattleHp(Difficulty.BOSS, 2, 1)).isEqualTo(330);
        assertThat(GameBalanceUtil.calculateBeastBattleHp(Difficulty.BOSS, 2, 2)).isEqualTo(479);
        assertThat(GameBalanceUtil.calculateBeastBattleHp(Difficulty.BOSS, 2, 4)).isEqualTo(776);
    }
}
