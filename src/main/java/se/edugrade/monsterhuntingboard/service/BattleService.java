package se.edugrade.monsterhuntingboard.service;

import org.springframework.stereotype.Service;

@Service
public class BattleService {

    public boolean rollWin() {
        return Math.random() < 0.7;
    }
}
