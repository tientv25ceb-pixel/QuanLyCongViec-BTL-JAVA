-- Chay file nay bang tai khoan root (phpMyAdmin / MySQL Workbench / CLI)
-- Sua mat khau o 2 dong IDENTIFIED BY neu can

-- Cach 1: Tao user rieng cho ung dung (khuyen nghi)
CREATE USER IF NOT EXISTS 'taskapp'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456789';
GRANT ALL PRIVILEGES ON task_management.* TO 'taskapp'@'localhost';
FLUSH PRIVILEGES;

-- Cach 2: Sua user root (neu van dung root trong config.properties)
-- Bo comment 2 dong duoi neu muon dung root:
-- ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456789';
-- FLUSH PRIVILEGES;