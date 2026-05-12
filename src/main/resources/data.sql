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

INSERT INTO hunters (display_name, appearance, city, country, latitude, longitude, level, exp, gold, base_hp, current_hp, exp_potion_active, endurance_potion_active, created_at, user_account_id)
SELECT 'Aria', 'MAGE', 'Stockholm', 'Sweden', 59.3293, 18.0686, 1, 0, 0, 100, 100, false, false, NOW(6),
       (SELECT id FROM user_accounts WHERE username = 'hunter1')
WHERE NOT EXISTS (
    SELECT 1
    FROM hunters
    WHERE user_account_id = (SELECT id FROM user_accounts WHERE username = 'hunter1')
);

INSERT INTO hunters (display_name, appearance, city, country, latitude, longitude, level, exp, gold, base_hp, current_hp, exp_potion_active, endurance_potion_active, created_at, user_account_id)
SELECT 'Rowan', 'RANGER', 'Stockholm', 'Sweden', 59.3293, 18.0686, 1, 0, 0, 100, 100, false, false, NOW(6),
       (SELECT id FROM user_accounts WHERE username = 'hunter2')
WHERE NOT EXISTS (
    SELECT 1
    FROM hunters
    WHERE user_account_id = (SELECT id FROM user_accounts WHERE username = 'hunter2')
);

SET @schema_name = DATABASE();

SET @add_beast_name_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'beasts'
              AND COLUMN_NAME = 'name'
        ),
        'SELECT 1',
        'ALTER TABLE beasts ADD COLUMN name VARCHAR(80) NOT NULL DEFAULT ''Unknown Beast'''
    )
);
PREPARE add_beast_name_stmt FROM @add_beast_name_sql;
EXECUTE add_beast_name_stmt;
DEALLOCATE PREPARE add_beast_name_stmt;

SET @drop_beast_difficulty_sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'beasts'
              AND COLUMN_NAME = 'difficulty'
        ),
        'ALTER TABLE beasts DROP COLUMN difficulty',
        'SELECT 1'
    )
);
PREPARE drop_beast_difficulty_stmt FROM @drop_beast_difficulty_sql;
EXECUTE drop_beast_difficulty_stmt;
DEALLOCATE PREPARE drop_beast_difficulty_stmt;

INSERT INTO beasts (id, name, type, hp, attack_power, reward_exp, reward_gold)
VALUES
    (1, 'Basilisk', 'BASILISK', 110, 18, 60, 30),
    (2, 'Griffin', 'GRIFFIN', 180, 32, 110, 70),
    (3, 'Pegasus', 'PEGASUS', 165, 28, 100, 60),
    (4, 'Chimera', 'CHIMERA', 260, 48, 190, 130),
    (5, 'Phoenix', 'PHOENIX', 320, 58, 240, 180),
    (6, 'Dragon', 'DRAGON', 520, 82, 420, 520)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    hp = VALUES(hp),
    attack_power = VALUES(attack_power),
    reward_exp = VALUES(reward_exp),
    reward_gold = VALUES(reward_gold);
