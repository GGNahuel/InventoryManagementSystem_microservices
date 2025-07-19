CREATE DATABASE IF NOT EXISTS products;
CREATE DATABASE IF NOT EXISTS users;
CREATE DATABASE IF NOT EXISTS inventories;

-- ----------------------------
-- Iniciar base de datos de productos
USE products;
CREATE TABLE IF NOT EXISTS product (
  id BINARY(16) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  brand VARCHAR(255),
  model VARCHAR(255),
  description VARCHAR(255),
  account_id BINARY(16) NOT NULL,
  unit_price DOUBLE NOT NULL,
  categories VARBINARY(255)
);

-- Productos iniciales
INSERT INTO product (id, name, brand, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-000011110001', '-', '')),
  "ProductA", "brand1",
  UNHEX(REPLACE('12341234-0000-0000-0000-00001111acc1', '-', '')),
  5.0
);
INSERT INTO product (id, name, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-000011110002', '-', '')),
  "ProductB",
  UNHEX(REPLACE('12341234-0000-0000-0000-00001111acc1', '-', '')),
  8.5
);
INSERT INTO product (id, name, brand, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-000011110003', '-', '')),
  "ProductA", "brand2",
  UNHEX(REPLACE('12341234-0000-0000-0000-00001111acc1', '-', '')),
  7.0
);

INSERT INTO product (id, name, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-000011110004', '-', '')),
  "ProductA",
  UNHEX(REPLACE('12341234-0000-0000-0000-00001111acc2', '-', '')),
  6.0
);
INSERT INTO product (id, name, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-000011110005', '-', '')),
  "ProductB",
  UNHEX(REPLACE('12341234-0000-0000-0000-00001111acc2', '-', '')),
  7.0
);

-- Iniciar base de datos de usuarios
USE users;
CREATE TABLE IF NOT EXISTS account (
  id BINARY(16) PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS user (
  id BINARY(16) PRIMARY KEY,
  associated_account_id BINARY(16) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL,
  is_admin BOOLEAN NOT NULL,

  CONSTRAINT fk_users_account FOREIGN KEY (associated_account_id) REFERENCES account(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS inventory_reference (
  id BINARY(16) PRIMARY KEY,
  inventory_id_reference BINARY(16) NOT NULL UNIQUE,
  associated_account_id BINARY(16),

  CONSTRAINT fk_inventory_account FOREIGN KEY (associated_account_id) REFERENCES account(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS permission_for_inventory (
  id BINARY(16) PRIMARY KEY,
  permissions VARCHAR(255) NOT NULL,
  inventory_reference_id BINARY(16) NOT NULL,
  associated_user_id BINARY(16) NOT NULL,

  CONSTRAINT fk_perm_inventory FOREIGN KEY (inventory_reference_id) REFERENCES inventory_reference(id) ON DELETE CASCADE,
  CONSTRAINT fk_perm_user FOREIGN KEY (associated_user_id) REFERENCES user(id) ON DELETE CASCADE
);

/* -- Entidades iniciales
INSERT INTO account (id, username, password) VALUES (
  UNHEX(REPLACE("12341234-0000-0000-0000-000011112222acc1", "-", "")), "account1", "password"
); */