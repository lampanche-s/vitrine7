DROP INDEX IF EXISTS uq_users_username_active;

WITH prepared_users AS (
    SELECT
        id,
        LOWER(
            REGEXP_REPLACE(
                COALESCE(
                    NULLIF(TRIM(username), ''),
                    NULLIF(SPLIT_PART(TRIM(COALESCE(email, '')), '@', 1), ''),
                    'usuario-' || id
                ),
                '[^A-Za-z0-9._-]+',
                '-',
                'g'
            )
        ) AS base_username,
        deleted_at
    FROM users
),
normalized_users AS (
    SELECT
        id,
        CASE
            WHEN LENGTH(TRIM(BOTH '.-_' FROM base_username)) >= 3
                THEN LEFT(TRIM(BOTH '.-_' FROM base_username), 80)
            ELSE 'usuario-' || id
        END AS normalized_username,
        deleted_at
    FROM prepared_users
),
deduplicated_users AS (
    SELECT
        id,
        CASE
            WHEN deleted_at IS NULL
                 AND COUNT(*) FILTER (WHERE deleted_at IS NULL)
                     OVER (PARTITION BY normalized_username) > 1
                 AND ROW_NUMBER() OVER (
                     PARTITION BY normalized_username, (deleted_at IS NULL)
                     ORDER BY id
                 ) > 1
                THEN LEFT(
                    normalized_username,
                    80 - LENGTH('-' || id::TEXT)
                ) || '-' || id::TEXT
            ELSE normalized_username
        END AS final_username
    FROM normalized_users
)
UPDATE users target
SET username = deduplicated_users.final_username
FROM deduplicated_users
WHERE target.id = deduplicated_users.id;

DROP INDEX IF EXISTS uq_users_email_active;
DROP INDEX IF EXISTS uq_users_email_not_null;

CREATE UNIQUE INDEX uq_users_username_active
    ON users (LOWER(username))
    WHERE deleted_at IS NULL;
