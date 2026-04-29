INSERT INTO user_accounts (username, password, role)
VALUES ('gamemaster', '$2a$10$LlTwsbWJpy9XqwhWIYvTA.rKfKZpq550UTrGlpW73iev7H4phZTbq', 'GAME_MASTER')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role);

INSERT INTO user_accounts (username, password, role)
VALUES ('hunter1', '$2a$10$LlTwsbWJpy9XqwhWIYvTA.rKfKZpq550UTrGlpW73iev7H4phZTbq', 'HUNTER')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role);

INSERT INTO user_accounts (username, password, role)
VALUES ('hunter2', '$2a$10$LlTwsbWJpy9XqwhWIYvTA.rKfKZpq550UTrGlpW73iev7H4phZTbq', 'HUNTER')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    role = VALUES(role);

INSERT INTO hunters (display_name, appearance, level, exp, gold, base_hp, current_hp, created_at, user_account_id)
SELECT 'Aria', 'MAGE', 1, 0, 0, 100, 100, NOW(6),
       (SELECT id FROM user_accounts WHERE username = 'hunter1')
WHERE NOT EXISTS (
    SELECT 1
    FROM hunters
    WHERE user_account_id = (SELECT id FROM user_accounts WHERE username = 'hunter1')
);

INSERT INTO hunters (display_name, appearance, level, exp, gold, base_hp, current_hp, created_at, user_account_id)
SELECT 'Rowan', 'RANGER', 1, 0, 0, 100, 100, NOW(6),
       (SELECT id FROM user_accounts WHERE username = 'hunter2')
WHERE NOT EXISTS (
    SELECT 1
    FROM hunters
    WHERE user_account_id = (SELECT id FROM user_accounts WHERE username = 'hunter2')
);

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM hunt_participations;
DELETE FROM hunt_beasts;
DELETE FROM hunts;
DELETE FROM beasts;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE beasts AUTO_INCREMENT = 1;
ALTER TABLE hunts AUTO_INCREMENT = 1;

INSERT INTO beasts (id, type, difficulty, hp, attack_power, reward_exp, reward_gold)
VALUES
    (1, 'BASILISK', 'EASY', 110, 18, 60, 30),
    (2, 'GRIFFIN', 'MEDIUM', 180, 32, 110, 70),
    (3, 'CHIMERA', 'HARD', 260, 48, 190, 130),
    (4, 'PHOENIX', 'HARD', 320, 58, 240, 180),
    (5, 'DRAGON', 'BOSS', 520, 82, 420, 520);

INSERT INTO hunts (id, title, type, difficulty, status, start_time, max_party_size, reward_exp, reward_gold, created_at)
VALUES
    -- 3 regular group hunts
    (1, 'Griffin Watch Patrol', 'HUNT', 'MEDIUM', 'SCHEDULED', DATE_ADD(NOW(6), INTERVAL 1 YEAR), 4, 110, 70, NOW(6)),
    (2, 'Basilisk Marsh Escort', 'HUNT', 'EASY', 'SCHEDULED', DATE_ADD(DATE_ADD(NOW(6), INTERVAL 1 YEAR), INTERVAL 2 DAY), 3, 60, 30, NOW(6)),
    (3, 'Chimera Ravine Expedition', 'HUNT', 'HARD', 'ACTIVE', DATE_SUB(NOW(6), INTERVAL 30 MINUTE), 4, 190, 130, NOW(6)),

    -- 5 regular solo hunts
    (4, 'Basilisk Burrow Sweep', 'SOLO_HUNT', 'EASY', 'ACTIVE', NULL, NULL, 60, 30, NOW(6)),
    (5, 'Griffin Sky Trial', 'SOLO_HUNT', 'MEDIUM', 'ACTIVE', NULL, NULL, 110, 70, NOW(6)),
    (6, 'Chimera Fang Trial', 'SOLO_HUNT', 'HARD', 'ACTIVE', NULL, NULL, 190, 130, NOW(6)),
    (7, 'Phoenix Ember Duel', 'SOLO_HUNT', 'MEDIUM', 'ACTIVE', NULL, NULL, 140, 90, NOW(6)),
    (8, 'Basilisk Stone Path', 'SOLO_HUNT', 'EASY', 'ACTIVE', NULL, NULL, 70, 35, NOW(6)),

    -- 2 boss fights: one solo, one group
    (9, 'Dragon King Solo Trial', 'SOLO_HUNT', 'BOSS', 'ACTIVE', NULL, NULL, 420, 520, NOW(6)),
    (10, 'Dragonfall Vanguard', 'HUNT', 'BOSS', 'SCHEDULED', DATE_ADD(DATE_ADD(NOW(6), INTERVAL 1 YEAR), INTERVAL 7 DAY), 6, 420, 520, NOW(6));

INSERT INTO hunt_beasts (hunt_id, beast_id)
VALUES
    (1, 2),
    (1, 1),
    (2, 1),
    (3, 3),
    (3, 2),
    (4, 1),
    (5, 2),
    (6, 3),
    (7, 4),
    (8, 1),
    (9, 5),
    (10, 5),
    (10, 3);
