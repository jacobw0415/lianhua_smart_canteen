-- ============================================================
-- 🌿 Lianhua ERP Schema v2.5
-- ============================================================

CREATE DATABASE IF NOT EXISTS lianhua
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE lianhua;

-- ------------------------------------------------------------
-- 1. 員工表
-- ------------------------------------------------------------
CREATE TABLE employees (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  position VARCHAR(50),
  salary DECIMAL(10,2) UNSIGNED,
  hire_date DATE,
  status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 2. 供應商表
-- ------------------------------------------------------------
CREATE TABLE suppliers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  contact VARCHAR(100),
  phone VARCHAR(50),
  billing_cycle ENUM('WEEKLY','BIWEEKLY','MONTHLY') DEFAULT 'MONTHLY',
  note TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 3. 採購表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE purchases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  supplier_id BIGINT NOT NULL,
  purchase_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  item VARCHAR(120) NOT NULL,
  qty INT UNSIGNED NOT NULL,
  unit_price DECIMAL(10,2) UNSIGNED NOT NULL,
  tax_rate DECIMAL(5,2) DEFAULT 0.00,
  tax_amount DECIMAL(10,2) DEFAULT 0.00,
  total_amount DECIMAL(10,2) DEFAULT 0.00,
  paid_amount DECIMAL(10,2) DEFAULT 0.00,
  balance DECIMAL(10,2) GENERATED ALWAYS AS (total_amount - paid_amount) STORED,
  status ENUM('PENDING','PARTIAL','PAID') DEFAULT 'PENDING',
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (supplier_id, purchase_date, item)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_purchases_supplier_id ON purchases(supplier_id);
CREATE INDEX idx_purchases_accounting_period ON purchases(accounting_period);

-- ------------------------------------------------------------
-- 4. 付款表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_id BIGINT NOT NULL,
  amount DECIMAL(10,2) UNSIGNED NOT NULL DEFAULT 0.00,
  pay_date DATE DEFAULT (CURRENT_DATE),
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  method ENUM('CASH','TRANSFER','CARD','CHECK') DEFAULT 'CASH',
  reference_no VARCHAR(100),
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (purchase_id) REFERENCES purchases(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (reference_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payments_purchase_id ON payments(purchase_id);
CREATE INDEX idx_payments_accounting_period ON payments(accounting_period);

-- ------------------------------------------------------------
-- 5A. 商品分類表
-- ------------------------------------------------------------
CREATE TABLE product_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  code VARCHAR(20) UNIQUE,
  description VARCHAR(255),
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_product_categories_code ON product_categories(code);
CREATE INDEX idx_product_categories_active ON product_categories(active);

-- ------------------------------------------------------------
-- 5B. 商品表
-- ------------------------------------------------------------
CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  category_id BIGINT NOT NULL,
  unit_price DECIMAL(10,2) UNSIGNED NOT NULL,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES product_categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active ON products(active);

-- ------------------------------------------------------------
-- 6. 銷售表 (零售，含會計期間)
-- ------------------------------------------------------------
CREATE TABLE sales (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sale_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  product_id BIGINT NOT NULL,
  qty INT UNSIGNED NOT NULL,
  amount DECIMAL(10,2) UNSIGNED NOT NULL,
  pay_method ENUM('CASH','CARD','MOBILE') NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY(product_id) REFERENCES products(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (sale_date, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sales_product_id ON sales(product_id);
CREATE INDEX idx_sales_accounting_period ON sales(accounting_period);

-- ------------------------------------------------------------
-- 7A. 費用類別主檔
-- ------------------------------------------------------------
CREATE TABLE expense_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  account_code VARCHAR(20) NOT NULL,
  parent_id BIGINT NULL,
  description VARCHAR(255),
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (parent_id) REFERENCES expense_categories(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_expense_categories_account_code ON expense_categories(account_code);
CREATE INDEX idx_expense_categories_parent_id ON expense_categories(parent_id);

-- ------------------------------------------------------------
-- 7B. 開支表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE expenses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  expense_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  category_id BIGINT NOT NULL,
  amount DECIMAL(10,2) UNSIGNED NOT NULL,
  note VARCHAR(255),
  employee_id BIGINT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES expense_categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (employee_id) REFERENCES employees(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  UNIQUE (employee_id, expense_date, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_employee_id ON expenses(employee_id);
CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_expenses_accounting_period ON expenses(accounting_period);

-- ------------------------------------------------------------
-- 8. 使用者表
-- ------------------------------------------------------------
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100),
  enabled BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 9. 角色表
-- ------------------------------------------------------------
CREATE TABLE roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 10. 使用者角色關聯表
-- ------------------------------------------------------------
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY(user_id, role_id),
  FOREIGN KEY(user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY(role_id) REFERENCES roles(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- ------------------------------------------------------------
-- 11. 訂單商家表 (合作單位)
-- ------------------------------------------------------------
CREATE TABLE order_customers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL UNIQUE,
  contact_person VARCHAR(100),
  phone VARCHAR(50),
  address VARCHAR(255),
  billing_cycle ENUM('WEEKLY','BIWEEKLY','MONTHLY') DEFAULT 'MONTHLY',
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 12. 訂單表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  order_date DATE NOT NULL,
  delivery_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  status ENUM('PENDING','CONFIRMED','DELIVERED','CANCELLED','PAID') DEFAULT 'PENDING',
  total_amount DECIMAL(10,2) UNSIGNED NOT NULL,
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY(customer_id) REFERENCES order_customers(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (customer_id, order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_accounting_period ON orders(accounting_period);

-- ------------------------------------------------------------
-- 13. 訂單明細表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  qty INT UNSIGNED NOT NULL,
  unit_price DECIMAL(10,2) UNSIGNED NOT NULL,
  subtotal DECIMAL(10,2) UNSIGNED NOT NULL,
  discount DECIMAL(10,2) UNSIGNED DEFAULT 0,
  tax DECIMAL(10,2) UNSIGNED DEFAULT 0,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY(order_id) REFERENCES orders(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY(product_id) REFERENCES products(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (order_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_order_items_accounting_period ON order_items(accounting_period);

-- ------------------------------------------------------------
-- 14. 收款表 Receipts
-- ------------------------------------------------------------
CREATE TABLE receipts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  received_date DATE DEFAULT (CURRENT_DATE),
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  amount DECIMAL(10,2) UNSIGNED NOT NULL,
  method ENUM('CASH','TRANSFER','CARD','CHECK') DEFAULT 'CASH',
  reference_no VARCHAR(100),
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (order_id),
  FOREIGN KEY (order_id) REFERENCES orders(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_receipts_order_id ON receipts(order_id);
CREATE INDEX idx_receipts_accounting_period ON receipts(accounting_period);

-- ============================================================
--    Schema v2.5 完成：
--    加入 Product Categories 模組，強化分類與報表彈性
-- ============================================================
