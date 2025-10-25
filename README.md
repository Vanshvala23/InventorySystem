## 🏬 Inventory Management System (Java + MySQL + Swing)

### 📋 Overview

A simple yet functional **Inventory Management System** built using **Java Swing** for the user interface and **MySQL** as the database. It helps manage items, update stock, and generate e-receipts for purchases.

---

### ⚙️ Features

* 🧾 Generate digital e-receipts
* 💾 Save receipts locally
* 📦 Add, edit, and remove products
* 📉 Automatic stock updates after sales
* ⏱️ Real-time statistics and clock display
* 🖥️ Simple and modern Swing UI
* 💡 Works perfectly on **VS Code** with **MySQL Workbench**

---

### 🧰 Technologies Used

* **Java (Swing GUI)**
* **MySQL Database**
* **JDBC Driver (Connector/J)**

---

### 🗃️ Database Setup

1. Open **MySQL Workbench**
2. Create a new database:

```sql
CREATE DATABASE inventory_db;
USE inventory_db;
```

3. Create a table:

```sql
CREATE TABLE items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    quantity INT,
    price DOUBLE
);
```

4. Insert sample data:

```sql
INSERT INTO items (name, quantity, price) VALUES
('Notebook', 50, 30.00),
('Pen', 100, 10.00),
('Pencil Box', 40, 50.00);
```

---

### 🚀 How to Run

1. Install **MySQL JDBC Driver** (Connector/J).
2. In **VS Code**, add the JDBC `.jar` to your classpath.
3. Run the `InventoryManagementSystem.java` file.

---

### 🧾 Receipt Example

```
🏬 Aadhar MART
----------------------------------------
Item                     Qty     Total
----------------------------------------
Notebook                 2       60.00
Pen                      3       30.00
----------------------------------------
TOTAL AMOUNT:                    90.00
Date: 25-10-2025 22:15:47
Thank you for shopping with us!
```

---

### 💡 Developer

**Developed by:** Vansh Vala
🖥️ *Made with Java, MySQL, and a lot of ☕*
