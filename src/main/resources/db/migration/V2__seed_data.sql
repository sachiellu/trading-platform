-- Seed Users (Password is bcrypt of 'admin123' and 'user123' respectively)
INSERT INTO users (username, password, role) VALUES 
('admin', '$2a$10$BuIotKYwQKMJZPpKUVaisOAslmXCcH0tvFRYrFNvwGGXbCREnG5na', 'ADMIN'),
('user1', '$2a$10$DSxs61UHZ240/kHSbJSi6OgJ24Kyw7UxoESSC0Pszvr/3KxC5u0yG', 'USER');

-- Seed Products
INSERT INTO products (name, price, stock, version, is_deleted) VALUES
('iPhone 15 Pro', 36900.00, 50, 0, false),
('MacBook Pro 14', 54900.00, 20, 0, false),
('AirPods Pro', 7490.00, 100, 0, false),
('iPad Air', 19900.00, 40, 0, false),
('Apple Watch Series 9', 13500.00, 75, 0, false);
