INSERT INTO users (username, password_hash, role)
VALUES
    ('admin', '$2a$10$OqcnZmrOOksxMUZMsjtSXu3DQ/bNILKtp9BermtUOwaGADs7XFTO6', 'ROLE_ADMIN'),
    ('user', '$2a$10$dxnPULESmrBxOi8zVGbyEO4UbIkayE45k7RtX/RJFfX2EfbHJ6oNK', 'ROLE_USER')
ON CONFLICT (username) DO NOTHING;
