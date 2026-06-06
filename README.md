# 商品交易系統說明與實作導覽

已成功為**商品交易平台**設計並實作了高水準、安全的後端 RESTful API 服務。本專案完全以 Java 17、Spring Boot 3.3.5、PostgreSQL 及 Flyway 完成，且針對併發交易設計了樂觀鎖及退避重試控制。

---

## 實作功能明細

### 1. 專案配置與環境
- **Spring Boot 專案設定**：使用 Java 17，專案包含內置的 Maven Wrapper (`mvnw.cmd`)，不需預先安裝 Maven 即可建置。
- **多環境配置 (`application.yml`)**：
  - `dev` 環境（預設）：使用 H2 記憶體資料庫（PostgreSQL 相容模式），並開啟 H2 控制台，方便無資料庫環境下直接啟動測試。
  - `prod` 環境：連結實體 PostgreSQL，由 Docker Compose 統一調度。

### 2. 資料庫設計與 Flyway 遷移 (Migration)
- **`V1__init_schema.sql`**：建立 `users`、`products` , `orders`、`audit_logs` 四張資料表，並為模糊搜尋、價格區間、軟刪除標記及審計欄位規劃了專屬索引（Index）以應對百萬級數據的查詢。
- **`V2__seed_data.sql`**：預加載管理員帳號 (`admin` / `admin123`)、一般用戶帳號 (`user1` / `user123`) 以及多款 Apple 測試商品。

### 3. 用戶認證與權限控制 (Security & JWT)
- 整合 **Spring Security**，配置無狀態的 JWT Session 認證。
- 提供 `/api/auth/login` 登入 API，回傳經由 Base64 安全密鑰加密的 JWT Token。
- 權限管理：限制商品管理 API 僅能由 `ADMIN`（管理員）呼叫，而商品瀏覽及下單 API 開放給 `USER`（一般用戶）。未登入的請求會回傳標準的 `401 Unauthorized` 或 `403 Forbidden` JSON 錯誤。

### 4. 併發防超賣控制 (加分項 - 核心亮點)
- **樂觀鎖 (Optimistic Locking)**：商品 Entity 設定了 `@Version` 欄位。當發生併發交易時，資料庫會自動比對版本號。
- **隨機退避重試 (Randomized Backoff)**：在服務層實作了 `placeOrder` 重試機制（最多重試 8 次）。重試時引入了 20ms 到 150ms 之間的**隨機延遲**，以打散線程的喚醒時間，完美解決併發下「雷霆咆哮（Thundering Herd）」的碰撞問題。
- **非侵入式軟刪除**：商品邏輯刪除僅在服務層設為 `is_deleted = true`，未在 Entity 使用 Hibernate `@Where` 註解，從而避免了「加載歷史訂單時因找不到商品而報錯」的常見 JPA 陷阱。

### 5. API 限流機制 (Rate Limiting)
- 實作 Servlet 過濾器 `RateLimitingFilter`，使用 **Token Bucket（權杖桶）演算法**對每個客戶端 IP 限制訪問頻率（單桶容量 60，每秒恢復 2 個 Token），防止惡意請求與爬蟲。

### 6. 操作審計日誌 (Audit Log)
- 當管理員對商品進行 `CREATE`、`UPDATE` , `DELETE` 時，審計系統會利用 Jackson 將變更前與變更後的商品快照序列化成 JSON 記錄於資料庫中，並記錄操作者 ID、名稱及時間。

### 7. OpenAPI / Swagger API 文件
- 導入 `springdoc-openapi`，在本地開啟 Swagger 控制面板，並配置了 **Bearer Auth (JWT)** 安全方案，可直接在網頁上貼上 Token 測試保護端點。

---

## 測試與驗證結果

撰寫了全量的單元測試與集成測試，並在主機上執行了驗證。

### 1. 併發集成測試 (`OrderServiceConcurrencyTest`)
- **測試場景**：一商品初始庫存為 **10**，使用 `ExecutorService` 建立 **15** 個併發線程，利用 `CountDownLatch` 在同一毫秒齊發搶購該商品（每人購買 1 件）。
- **驗證結果**：
  - 成功搶購訂單：**10 筆**
  - 因庫存不足而被拒絕：**5 筆**
  - 因樂觀鎖衝突失敗（經 8 次退避重試後）：**0 筆**
  - 商品剩餘庫存：**0**（且資料庫訂單總數與成功扣減一致，庫存無負數，無超賣！）
- **結論**：樂觀鎖與退避機制成功發揮作用。

### 2. 商品 CRUD 與搜尋測試 (`ProductServiceTest`)
- 測試了商品創建、更新、詳細查詢、分頁與模糊搜尋，並驗證了邏輯軟刪除。
- 所有測試全部通過！

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
