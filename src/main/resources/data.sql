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

INSERT INTO hunters (display_name, appearance, city, country, latitude, longitude, level, exp, gold, base_hp, current_hp, created_at, user_account_id)
SELECT 'Aria', 'MAGE', 'Stockholm', 'Sweden', 59.3293, 18.0686, 1, 0, 0, 100, 100, NOW(6),
       (SELECT id FROM user_accounts WHERE username = 'hunter1')
WHERE NOT EXISTS (
    SELECT 1
    FROM hunters
    WHERE user_account_id = (SELECT id FROM user_accounts WHERE username = 'hunter1')
);

INSERT INTO hunters (display_name, appearance, city, country, latitude, longitude, level, exp, gold, base_hp, current_hp, created_at, user_account_id)
SELECT 'Rowan', 'RANGER', 'Stockholm', 'Sweden', 59.3293, 18.0686, 1, 0, 0, 100, 100, NOW(6),
       (SELECT id FROM user_accounts WHERE username = 'hunter2')
WHERE NOT EXISTS (
    SELECT 1
    FROM hunters
    WHERE user_account_id = (SELECT id FROM user_accounts WHERE username = 'hunter2')
);

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM hunt_participations;
DELETE FROM hunter_generated_hunt_progress;
DELETE FROM hunt_beasts;
DELETE FROM hunt_template_beasts;
DELETE FROM hunts;
DELETE FROM hunt_templates;
DELETE FROM beasts;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE beasts AUTO_INCREMENT = 1;
ALTER TABLE hunts AUTO_INCREMENT = 1;
ALTER TABLE hunt_templates AUTO_INCREMENT = 1;

INSERT INTO beasts (id, type, difficulty, hp, attack_power, reward_exp, reward_gold)
VALUES
    (1, 'BASILISK', 'EASY', 110, 18, 60, 30),
    (2, 'GRIFFIN', 'MEDIUM', 180, 32, 110, 70),
    (3, 'PEGASUS', 'MEDIUM', 165, 28, 100, 60),
    (4, 'CHIMERA', 'HARD', 260, 48, 190, 130),
    (5, 'PHOENIX', 'HARD', 320, 58, 240, 180),
    (6, 'DRAGON', 'BOSS', 520, 82, 420, 520);
