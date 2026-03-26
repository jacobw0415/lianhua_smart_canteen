-- ============================================================
-- 🌿 Lianhua ERP Schema v2.7 (含通知中心擴展架構)
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
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 3. 採購主表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE purchases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_no VARCHAR(20) NOT NULL COMMENT '進貨單編號（PO-YYYYMM-XXXX）',
  supplier_id BIGINT NOT NULL,
  purchase_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  total_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '總金額（由明細表計算）',
  paid_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '已付款金額',
  balance DECIMAL(10,2) GENERATED ALWAYS AS (total_amount - paid_amount) STORED COMMENT '餘額（自動計算）',
  status ENUM('PENDING','PARTIAL','PAID') DEFAULT 'PENDING' COMMENT '付款狀態',
  record_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '記錄狀態：ACTIVE（正常進貨）, VOIDED（已作廢）',
  voided_at TIMESTAMP NULL COMMENT '作廢時間',
  void_reason VARCHAR(500) NULL COMMENT '作廢原因',
  note VARCHAR(255) COMMENT '備註',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uk_purchases_purchase_no UNIQUE (purchase_no),
  CONSTRAINT fk_purchases_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_purchases_supplier_id ON purchases(supplier_id);
CREATE INDEX idx_purchases_accounting_period ON purchases(accounting_period);
CREATE INDEX idx_purchases_record_status ON purchases(record_status);
CREATE INDEX idx_purchases_voided_at ON purchases(voided_at);
CREATE INDEX idx_purchases_purchase_date ON purchases(purchase_date);

-- ------------------------------------------------------------
-- 3.1. 採購明細表
-- ------------------------------------------------------------
CREATE TABLE purchase_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_id BIGINT NOT NULL COMMENT '採購單ID',
  item VARCHAR(120) NOT NULL COMMENT '進貨項目',
  unit VARCHAR(20) NOT NULL COMMENT '顯示用單位（斤、箱、盒）',
  qty INT UNSIGNED NOT NULL COMMENT '數量',
  unit_price DECIMAL(10,2) UNSIGNED NOT NULL COMMENT '單價',
  subtotal DECIMAL(10,2) NOT NULL COMMENT '小計（不含稅）',
  note VARCHAR(255) COMMENT '備註',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_purchase_items_purchase
    FOREIGN KEY (purchase_id) REFERENCES purchases(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_purchase_items_purchase_id ON purchase_items(purchase_id);
CREATE INDEX idx_purchase_items_item ON purchase_items(item);

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
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '狀態：ACTIVE（正常付款）, VOIDED（已作廢）',
  voided_at TIMESTAMP NULL COMMENT '作廢時間',
  void_reason VARCHAR(500) NULL COMMENT '作廢原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (purchase_id) REFERENCES purchases(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (reference_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payments_purchase_id ON payments(purchase_id);
CREATE INDEX idx_payments_accounting_period ON payments(accounting_period);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_voided_at ON payments(voided_at);

-- ------------------------------------------------------------
-- 5. 商品分類表
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
-- 5.1 商品表
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
-- 7. 費用類別主檔
-- ------------------------------------------------------------
CREATE TABLE expense_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  account_code VARCHAR(20) NOT NULL UNIQUE,
  description VARCHAR(255),
  active BOOLEAN DEFAULT TRUE,
  is_salary BOOLEAN DEFAULT FALSE COMMENT '是否為薪資類別',
  frequency_type ENUM('DAILY','WEEKLY','BIWEEKLY','MONTHLY','UNLIMITED') DEFAULT 'DAILY' COMMENT '費用頻率類型',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_expense_categories_account_code ON expense_categories(account_code);

-- ------------------------------------------------------------
-- 8. 開支表 (含會計期間)
-- ------------------------------------------------------------
CREATE TABLE expenses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  expense_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  category_id BIGINT NOT NULL,
  amount DECIMAL(10,2) UNSIGNED NOT NULL,
  note VARCHAR(255),
  employee_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '狀態：ACTIVE（正常支出）, VOIDED（已作廢）',
  voided_at TIMESTAMP NULL COMMENT '作廢時間',
  void_reason VARCHAR(500) NULL COMMENT '作廢原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 【新增虛擬列】用於控制唯一性：僅在有效時保留 ID，作廢時變為 NULL
  active_employee_id BIGINT GENERATED ALWAYS AS (IF(status = 'ACTIVE', employee_id, NULL)) VIRTUAL,

  FOREIGN KEY (category_id) REFERENCES expense_categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  FOREIGN KEY (employee_id) REFERENCES employees(id)
    ON DELETE SET NULL ON UPDATE CASCADE,

  -- 【修正唯一索引】使用虛擬列替代原本的 employee_id + status
  -- 這能確保：有效單據不能重複，但作廢單據可以有無數個
  UNIQUE INDEX uk_expenses_active_repeat (active_employee_id, expense_date, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 其他一般索引保持不變
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_employee_id ON expenses(employee_id);
CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_expenses_accounting_period ON expenses(accounting_period);
CREATE INDEX idx_expenses_status ON expenses(status);
CREATE INDEX idx_expenses_voided_at ON expenses(voided_at);

-- ------------------------------------------------------------
-- 9. 使用者表 (加強版：增加員工關聯與電子郵件)
-- ------------------------------------------------------------
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) UNIQUE NOT NULL COMMENT '登入帳號',
  password VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密碼',
  full_name VARCHAR(100) COMMENT '顯示姓名',
  email VARCHAR(100) UNIQUE COMMENT '用於找回密碼或通知',
  employee_id BIGINT UNIQUE COMMENT '關聯員工表 ID',
  enabled BOOLEAN DEFAULT TRUE COMMENT '帳號是否啟用',
  last_login_at TIMESTAMP NULL COMMENT '最後登入時間',
  mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否啟用多因子認證',
  mfa_secret VARCHAR(512) NULL COMMENT 'TOTP 密鑰（建議以 AES 加密儲存）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_users_employee
    FOREIGN KEY (employee_id) REFERENCES employees(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 10. 角色表 (加強版：增加描述)
-- ------------------------------------------------------------
CREATE TABLE roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE NOT NULL COMMENT '角色代碼 (如 ROLE_ADMIN, ROLE_MANAGER)',
  description VARCHAR(100) COMMENT '角色中文名稱/描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 10.1 權限定義表 (新增：實作顆粒度權限控制)
-- ------------------------------------------------------------
CREATE TABLE permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE NOT NULL COMMENT '權限識別碼 (如 purchase:view, order:create)',
  description VARCHAR(100) COMMENT '權限描述',
  module VARCHAR(50) COMMENT '所屬模組 (如 進貨, 銷售, 財務)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 11. 使用者角色關聯表 (保持不變，標準多對多)
-- ------------------------------------------------------------
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY(user_id, role_id),
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 11.1 角色權限關聯表 (新增：角色與權限的多對多)
-- ------------------------------------------------------------
CREATE TABLE role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY(role_id, permission_id),
  FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY(permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 11.2 登入審核日誌 (新增：安全性稽核用)
-- ------------------------------------------------------------
CREATE TABLE login_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  login_ip VARCHAR(45),
  login_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status ENUM('SUCCESS', 'FAILED') DEFAULT 'SUCCESS',
  user_agent VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 11.3 使用者管理操作稽核日誌 (與 UserAuditLog 實體對齊)
-- ------------------------------------------------------------
CREATE TABLE user_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurred_at TIMESTAMP(6) NOT NULL COMMENT '操作發生時間（UTC）',
  operator_id BIGINT NOT NULL COMMENT '操作者使用者 id',
  target_user_id BIGINT NOT NULL COMMENT '被操作的使用者 id',
  action VARCHAR(50) NOT NULL COMMENT 'USER_CREATE, USER_UPDATE, USER_RESET_PASSWORD, USER_DELETE 等',
  details TEXT NULL COMMENT '變更摘要（JSON 或文字），不得含密碼明文',
  KEY idx_audit_occurred_at (occurred_at),
  KEY idx_audit_operator (operator_id),
  KEY idx_audit_target (target_user_id),
  KEY idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);

-- ------------------------------------------------------------
-- 12. 訂單商家表 (合作單位)
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
-- 13. 訂單表 (🚀 v2.8 核心修改：同步作廢欄位以解決閃跳問題)
-- ------------------------------------------------------------
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(20) NOT NULL COMMENT '訂單編號（SO-YYYYMM-XXXX）',
  customer_id BIGINT NOT NULL,
  order_date DATE NOT NULL,
  delivery_date DATE NOT NULL,
  accounting_period VARCHAR(7) NOT NULL DEFAULT (DATE_FORMAT(CURRENT_DATE(), '%Y-%m')),
  order_status ENUM('PENDING','CONFIRMED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  payment_status ENUM('UNPAID','PARTIAL','PAID') NOT NULL DEFAULT 'UNPAID',
  total_amount DECIMAL(10,2) UNSIGNED NOT NULL,

  -- 🚀 新增同步作廢欄位：直接將收款作廢狀態存入主表
  record_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '記錄狀態：ACTIVE（正常）, VOIDED（已作廢）',
  voided_at TIMESTAMP NULL COMMENT '作廢時間',
  void_reason VARCHAR(500) NULL COMMENT '作廢原因',

  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY(customer_id) REFERENCES order_customers(id) ON DELETE CASCADE ON UPDATE CASCADE,
  UNIQUE (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_accounting_period ON orders(accounting_period);
CREATE INDEX idx_orders_record_status ON orders(record_status); -- 新增索引
CREATE INDEX idx_orders_voided_at ON orders(voided_at);         -- 新增索引

-- ------------------------------------------------------------
-- 14. 訂單明細表
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
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
  UNIQUE (order_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 15. 收款表 Receipts
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
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '狀態：ACTIVE（正常收款）, VOIDED（已作廢）',
  voided_at TIMESTAMP NULL COMMENT '作廢時間',
  void_reason VARCHAR(500) NULL COMMENT '作廢原因',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_receipts_order_id ON receipts(order_id);
CREATE INDEX idx_receipts_accounting_period ON receipts(accounting_period);
CREATE INDEX idx_receipts_received_date ON receipts(received_date);
CREATE INDEX idx_receipts_status ON receipts(status);
CREATE INDEX idx_receipts_voided_at ON receipts(voided_at);

-- ------------------------------------------------------------
-- 16. 通知中心系統 (Notification Center v2.7)
-- ------------------------------------------------------------

-- A. 通知中心主表 (儲存訊息內容與關聯，支援多型關聯)
CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(50) NOT NULL COMMENT '用於識別通知類型，如 PURCHASE_VOIDED',
  actor_id BIGINT COMMENT '觸發通知的用戶 ID (NULL 則代表系統自動發送)',
  target_type VARCHAR(50) NOT NULL COMMENT '關聯模組類型，如 purchases, orders, expenses',
  target_id BIGINT NOT NULL COMMENT '關聯單據的 ID',
  payload JSON NULL COMMENT '儲存動態參數，如 {"no": "PO-001", "reason": "輸入錯誤"}',
  priority TINYINT DEFAULT 1 COMMENT '1:一般, 2:重要, 3:緊急',
  action_url VARCHAR(255) NULL COMMENT '點擊後跳轉的前端路徑',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_notifications_target (target_type, target_id),
  INDEX idx_notifications_created (created_at DESC),
  FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- B. 使用者通知關聯表 (儲存每個用戶的閱讀狀態)
CREATE TABLE user_notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  notification_id BIGINT NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  read_at TIMESTAMP NULL,
  is_archived BOOLEAN DEFAULT FALSE COMMENT '是否封存 (用戶手動刪除/隱藏)',

  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_notifications_status ON user_notifications(user_id, is_read);

-- ------------------------------------------------------------
-- 17. 密碼重設權杖表 (新增：支援忘記密碼功能)
-- ------------------------------------------------------------
CREATE TABLE password_reset_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(100) NOT NULL UNIQUE COMMENT '加密後的重設權杖',
  user_id BIGINT NOT NULL COMMENT '關聯的使用者',
  expiry_date TIMESTAMP NOT NULL COMMENT '權杖過期時間',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_password_reset_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);

-- ------------------------------------------------------------
-- 18. Refresh Token 表（儲存不透明 Token，可撤銷）
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL COMMENT 'Token 的 SHA-256 雜湊，不存明文',
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  revoked_at TIMESTAMP NULL,

  CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ------------------------------------------------------------
-- 19. MFA 待驗證階段暫存（登入成功但尚未通過 MFA 時使用）
-- ------------------------------------------------------------
CREATE TABLE mfa_pending_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_mfa_pending_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_mfa_pending_token ON mfa_pending_sessions(token);
CREATE INDEX idx_mfa_pending_expires_at ON mfa_pending_sessions(expires_at);

-- ------------------------------------------------------------
-- 20. 財務操作稽核日誌表
-- ------------------------------------------------------------
CREATE TABLE financial_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurred_at TIMESTAMP NOT NULL COMMENT '操作發生時間（UTC）',
  operator_id BIGINT NULL COMMENT '操作者使用者 ID，系統自動動作可為 NULL',
  entity_type VARCHAR(50) NOT NULL COMMENT '實體類型：如 PAYMENT, PURCHASE, ORDER, RECEIPT',
  entity_id BIGINT NOT NULL COMMENT '被操作實體的 ID',
  action VARCHAR(50) NOT NULL COMMENT '操作類型：如 PAYMENT_VOID, PAYMENT_DELETE_ALL_FOR_PURCHASE',
  details TEXT NULL COMMENT '變更摘要（JSON 或文字），不得含密碼或 Token 明文',

  CONSTRAINT fk_fin_audit_operator
    FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_fin_audit_occurred_at ON financial_audit_logs(occurred_at);
CREATE INDEX idx_fin_audit_operator ON financial_audit_logs(operator_id);
CREATE INDEX idx_fin_audit_entity ON financial_audit_logs(entity_type, entity_id);

-- ------------------------------------------------------------
-- 21. 全系統活動稽核（HTTP 層自動記錄 + 可查詢）
-- ------------------------------------------------------------
CREATE TABLE activity_audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  occurred_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  operator_id BIGINT NOT NULL COMMENT '操作者使用者 ID',
  operator_username VARCHAR(60) NULL COMMENT '操作者使用者名稱快照',
  action VARCHAR(32) NOT NULL COMMENT 'CREATE / UPDATE / DELETE / EXPORT / PATCH',
  resource_type VARCHAR(64) NOT NULL COMMENT '依 API 路徑推斷之資源類型',
  resource_id BIGINT NULL COMMENT '路徑中第一個數字 ID（若有）',
  http_method VARCHAR(10) NOT NULL,
  request_path VARCHAR(1024) NOT NULL,
  query_string VARCHAR(512) NULL,
  ip_address VARCHAR(45) NULL,
  user_agent VARCHAR(512) NULL,
  details TEXT NULL COMMENT 'JSON：補充資訊（不含密碼）',
  INDEX idx_activity_occurred_at (occurred_at),
  INDEX idx_activity_operator (operator_id),
  INDEX idx_activity_operator_username (operator_username),
  INDEX idx_activity_resource (resource_type, resource_id),
  INDEX idx_activity_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 22. 全系統活動稽核保留期：歸檔再刪除
-- ------------------------------------------------------------
-- 過期資料（目前預設保留 180 天）會被排程批次搬移到 archive 表，
-- 再刪除主表資料，以降低 activity_audit_logs 的長期膨脹並保留可追溯紀錄。
CREATE TABLE activity_audit_logs_archive (
  id BIGINT PRIMARY KEY,
  occurred_at TIMESTAMP(3) NOT NULL,
  operator_id BIGINT NOT NULL,
  operator_username VARCHAR(60) NULL,
  action VARCHAR(32) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id BIGINT NULL,
  http_method VARCHAR(10) NOT NULL,
  request_path VARCHAR(1024) NOT NULL,
  query_string VARCHAR(512) NULL,
  ip_address VARCHAR(45) NULL,
  user_agent VARCHAR(512) NULL,
  details TEXT NULL COMMENT 'JSON：補充資訊（不含密碼）',
  archived_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  INDEX idx_activity_occurred_at (occurred_at),
  INDEX idx_activity_operator (operator_id),
  INDEX idx_activity_operator_username (operator_username),
  INDEX idx_activity_resource (resource_type, resource_id),
  INDEX idx_activity_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--    Schema v2.7 完成：
--    1. 整合 v2.6 所有修正（作廢機制、費用類別更新）。
--    2. 新增通知中心三表架構 (16. A, B)。
--    3. 支援 JSON Payload 與 Template 解耦，為產品化做準備。
--    4. 新增全系統活動稽核表 activity_audit_logs (21)。
--    5. 新增活動稽核歸檔表 activity_audit_logs_archive (22)，配合歸檔後刪除策略。
-- ============================================================