CREATE DATABASE IF NOT EXISTS products;
CREATE DATABASE IF NOT EXISTS users;
CREATE DATABASE IF NOT EXISTS inventories;

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

INSERT INTO product (id, name, brand, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-0000111122220001', '-', '')),
  "ProductA", "brand1",
  UNHEX(REPLACE('12341234-0000-0000-0000-000011112222acc1', '-', '')),
  5.0
);
INSERT INTO product (id, name, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-0000111122220002', '-', '')),
  "ProductB",
  UNHEX(REPLACE('12341234-0000-0000-0000-000011112222acc1', '-', '')),
  8.5
);
INSERT INTO product (id, name, brand, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-0000111122220003', '-', '')),
  "ProductA", "brand2",
  UNHEX(REPLACE('12341234-0000-0000-0000-000011112222acc1', '-', '')),
  7.0
);

INSERT INTO product (id, name, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-0000111122220004', '-', '')),
  "ProductA",
  UNHEX(REPLACE('12341234-0000-0000-0000-000011112222acc2', '-', '')),
  6.0
);
INSERT INTO product (id, name, account_id, unit_price) VALUES (
  UNHEX(REPLACE('abc12300-0000-0000-0000-0000111122220005', '-', '')),
  "ProductB",
  UNHEX(REPLACE('12341234-0000-0000-0000-000011112222acc2', '-', '')),
  7.0
);

-- Iniciar base de datos de usuarios
USE users;
CREATE TABLE IF NOT EXISTS account (
  id BINARY(16) PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS users (
  id BINARY(16) PRIMARY KEY,
  associated_account_id BINARY(16) NOT NULL
);
CREATE TABLE IF NOT EXISTS inventory_reference;
CREATE TABLE IF NOT EXISTS permissions_for_inventory;


