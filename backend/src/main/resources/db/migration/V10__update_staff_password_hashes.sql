-- =============================================
-- V10: Fix staff BCrypt password hashes (password: admin123)
-- =============================================

UPDATE staff_user
SET password_hash = '$2a$10$ekUCnu/ho87YRxyHSwBEtOGga1ygtRnmiOcc.IoK3syx6RmTb1Dqa'
WHERE username IN ('admin', 'kitchen1');
