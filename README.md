# 🌿 Lianhua ERP Backend
**ERP System for Lianhua Vegetarian — Supplier, Purchase, Payment, Sales, and Financial Reports**  
蓮華素食 ERP 系統後端 — 用於管理供應商、進貨、付款、銷售與財務報表

---

## 📘 Overview / 專案概述

**English:**  
Lianhua ERP is a Spring Boot–based backend system designed for a vegetarian lunchbox supplier.  
It manages purchasing, payments, receipts, expenses, and generates real-time financial reports (AR/AP Aging, Balance Sheet, Cash Flow, Profit & Loss).  
This system aims to support transparent internal accounting, supplier coordination, and accurate monthly reporting.

**中文說明：**  
蓮華 ERP 是一套以 Spring Boot 為核心的後端系統，  
專為素食便當供應企業打造，提供進貨、付款、收款、開支與財務報表管理。  
系統目標為：提升財務透明度、加強供應商協作、支援即時報表分析。

---

## 🏗️ Architecture / 系統架構

```text
lianhua-erp/
├── src/
│   ├── main/java/com/lianhua/erp/
│   │   ├── controller/        # REST API 層 (Controller layer)
│   │   ├── service/           # 業務邏輯層 (Service interfaces)
│   │   ├── service/impl/      # 業務邏輯實作層 (Service implementations)
│   │   ├── repository/        # JPA 資料存取層 (Repository layer)
│   │   ├── domain/            # 實體模型 (Entities)
│   │   ├── dto/               # 輸入輸出資料物件 (DTOs)
│   │   ├── mapper/            # MapStruct 對象映射 (Entity ↔ DTO)
│   │   ├── config/            # 系統設定 (Security, Swagger, etc.)
│   │   └── exception/         # 全域例外處理 (Global Exception Handler)
│   └── resources/
│       ├── application.yml    # 系統設定檔
│       └── schema.sql         # 初始資料表結構
├── pom.xml
└── README.md
```

---

## ⚙️ Technology Stack / 技術棧

| Category 類別 | Technology 技術 |
|----------------|----------------|
| Backend Framework 後端框架 | Spring Boot 3.5.x |
| Language 語言 | Java 21 |
| ORM / DB | JPA (Hibernate), MySQL 8.x |
| Object Mapping | MapStruct 1.5.x |
| Dependency Injection | Spring Context |
| Authentication | Spring Security + JWT |
| Documentation | SpringDoc / Swagger UI |
| Logging | SLF4J + Logback |
| Container | Docker / Docker Compose |
| Testing | JUnit 5, Mockito |

---

## 🚀 Installation & Setup / 安裝與啟動

### 1️⃣ Requirements / 系統需求
- JDK 21+
- Maven 3.9+
- MySQL 8+
- (Optional) Docker, Docker Compose

### 2️⃣ Database Setup / 建立資料庫
```sql
CREATE DATABASE lianhua
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3️⃣ Configure Application / 設定環境變數
Edit `application.yml` or use `.env`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lianhua
    username: root
    password: password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### 4️⃣ Run the Application / 啟動專案
```bash
mvn clean spring-boot:run
```

Open:  
👉 http://localhost:8080/swagger-ui/index.html

---

## 📂 Core Modules / 核心模組

| Module 模組 | Description 功能說明 |
|--------------|----------------------|
| **Suppliers** | Manage supplier information 管理供應商資料 |
| **Purchases** | Record purchase orders 登記進貨單 |
| **Payments** | Track supplier payments 追蹤付款金額 |
| **Customers / Orders** | Manage customer and sales 訂單與客戶管理 |
| **Receipts** | Record received payments 收款記錄 |
| **Expenses** | Track business expenses 開支紀錄 |
| **Reports** | Generate AR/AP, P&L, Cash Flow, and Balance Sheet 報表生成模組 |
| **Security** | JWT-based authentication 安全驗證與角色權限控制 |

---

## 📊 Reports / 財務報表模組

| Report 報表 | Description 功能 |
|--------------|------------------|
| **AR Aging Report** | Analyze overdue receivables 應收帳齡分析 |
| **AP Aging Report** | Analyze overdue payables 應付帳齡分析 |
| **Cash Flow Report** | Summarize cash inflows/outflows 現金流量分析 |
| **Profit & Loss Report** | Monthly income statement 月損益表 |
| **Balance Sheet** | Assets, liabilities, and equity overview 資產負債表 |

---

## 🧩 API Endpoints / 主要 API 路徑

| Endpoint | Description |
|-----------|--------------|
| `/api/suppliers` | Supplier management |
| `/api/purchases` | Purchase records |
| `/api/payments` | Payment transactions |
| `/api/customers` | Customer data |
| `/api/orders` | Sales orders |
| `/api/receipts` | Receipts and collections |
| `/api/reports/ar-aging` | Accounts receivable aging |
| `/api/reports/ap-aging` | Accounts payable aging |
| `/api/reports/cash-flow` | Cash flow report |
| `/api/reports/balance-sheet` | Balance sheet |
| `/api/reports/profit-loss` | Profit & loss report |

---

## 🧪 Testing / 測試

Run all tests:
```bash
mvn test
```

Integration tests:
```bash
mvn verify
```

---

## 🐳 Docker Deployment / Docker 部署

### Compose Example / docker-compose.yml 範例
```yaml
version: "3.8"
services:
  app:
    build: .
    container_name: lianhua-backend
    ports:
      - "8080:8080"
    env_file: .env
    depends_on:
      - db

  db:
    image: mysql:8.0
    container_name: mysql-lianhua
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: lianhua
    volumes:
      - ./data/mysql:/var/lib/mysql
    restart: always
```

啟動：
```bash
docker-compose up -d
```

---

## 🧭 Git Commit Guide / Git 提交指南

| Type 類型 | Example 範例 |
|------------|--------------|
| **feat** | `feat(report): 新增現金流量報表服務實作` |
| **fix** | `fix(payment): 修正付款金額檢核邏輯` |
| **refactor** | `refactor(service): 統一報表 DTO 欄位名稱` |
| **docs** | `docs(readme): 補充安裝與架構說明` |
| **test** | `test(repository): 新增報表查詢單元測試` |

---

## 🧾 License & Maintenance / 授權與維護

| 項目 | 說明 |
|------|------|
| **開發單位 / Maintainer** | Lianhua Vegetarian Tech |
| **授權方式 / License** | Internal Use Only (內部使用，非公開) |
| **版本 / Version** | v2.5 |
| **主要負責人 / Maintainer** | Jacob Huang (System Architect) |
| **聯絡方式 / Contact** | `lianhua.tech@company.local` (範例) |

---

## 💡 Future Enhancements / 後續規劃

- 🧾 Payroll management (薪資管理)
- 📅 Monthly/Quarterly tax filing reports (報稅報表自動生成)
- 📈 Grafana integration for real-time monitoring (即時數據監控)
- 🔐 Role-based Access Control (角色權限強化)

**系統定位說明**：  
本系統專注於**帳務管理與銷售紀錄**，不包含原物料庫存管理功能。  
此設計符合外燴/便當業務模式，便當為即時製作，無需庫存管理。
