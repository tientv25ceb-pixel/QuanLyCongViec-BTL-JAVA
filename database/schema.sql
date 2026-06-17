-- Tao database
CREATE DATABASE IF NOT EXISTS task_management;
USE task_management;

-- 1. Bang luu tru nguoi dung
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    full_name VARCHAR(100),
    encrypted_phone VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 2. Bang quan ly nhom lam viec
CREATE TABLE IF NOT EXISTS `groups` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 3. Bang trung gian quan ly thanh vien trong nhom
CREATE TABLE IF NOT EXISTS group_members (
    group_id INT,
    user_id INT,
    role_in_group VARCHAR(20) DEFAULT 'MEMBER',
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Bang quan ly cong viec (Ho tro ca viec ca nhan va viec nhom)
CREATE TABLE IF NOT EXISTS tasks (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'TODO',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    due_date DATE,
    creator_id INT,
    group_id INT DEFAULT NULL,
    assignee_id INT DEFAULT NULL,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 5. Bang ghi log he thong (Dap ung yeu cau nang cao 6.1)
CREATE TABLE IF NOT EXISTS system_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(50),
    action VARCHAR(50),
    status VARCHAR(20),
    request_id VARCHAR(50) DEFAULT NULL
);

-- Chen du lieu mau (Mat khau da duoc hash SHA-256 tuong ung voi chuoi '123456')
INSERT INTO users (username, password_hash, role, full_name) VALUES
('admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Admin', 'He Thong Quan Tri'),
('user1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'User', 'Nguyen Van A'),
('user2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'User', 'Tran Thi B');

-- ============================================================
-- TAI KHOAN MYSQL CHO UNG DUNG (chay bang quyen root)
-- User: taskapp | Password: 123456789
-- Dung mysql_native_password de tranh loi "Public Key Retrieval"
-- ============================================================
CREATE USER IF NOT EXISTS 'taskapp'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456789';
GRANT ALL PRIVILEGES ON task_management.* TO 'taskapp'@'localhost';
FLUSH PRIVILEGES;
