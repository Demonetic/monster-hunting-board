package se.edugrade.monsterhuntingboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonsterHuntingBoardApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonsterHuntingBoardApplication.class, args);
    }
}
