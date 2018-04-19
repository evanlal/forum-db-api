CREATE USER 'student'@'localhost';
CREATE DATABASE bb;
GRANT ALL ON bb.* TO 'student'@'localhost';
FLUSH PRIVILEGES;
