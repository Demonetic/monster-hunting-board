package se.edugrade.monsterhuntingboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
}
