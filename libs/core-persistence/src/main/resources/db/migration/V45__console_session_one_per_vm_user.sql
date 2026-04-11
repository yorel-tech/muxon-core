-- At most one console_session row per VM and user (prevents unbounded duplicate rows).

DELETE FROM console_session c
WHERE NOT EXISTS (
    SELECT 1
    FROM (
        SELECT DISTINCT ON (vm_id, user_id) id
        FROM console_session
        ORDER BY vm_id, user_id, expires_at DESC, created_at DESC
    ) keep
    WHERE keep.id = c.id
);

ALTER TABLE console_session
    ADD CONSTRAINT uq_console_session_vm_user UNIQUE (vm_id, user_id);
