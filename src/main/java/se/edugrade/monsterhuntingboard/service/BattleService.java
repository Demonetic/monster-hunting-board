package se.edugrade.monsterhuntingboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BattleService {
    private static final Logger log = LoggerFactory.getLogger(BattleService.class);

    public boolean rollWin() {
        boolean won = Math.random() < 0.7;
        log.debug("Battle roll result: {}", won ? "WIN" : "LOSS");
        return won;
    }
}
