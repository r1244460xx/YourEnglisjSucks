# YourEnglishSucks - 英文修飾與學習 AI 互動平台

本專案依據 [Design_Plan.md](Design_Plan.md) 規範開發，提供專為英語學習與寫作精修打造的 AI Web 應用服務。

---

## 🌟 功能亮點

1. **頂部三合一功能水平 Tabs**：
   - `✨ 英文修飾`（第一期核心功能）
   - `🌐 英翻中`（預留擴充）
   - `🔄 中翻英`（預留擴充）
2. **專屬英文修飾 Skill**：
   - **第一次輸入**：送出英文草稿，後端自動注入專業「英文修飾教練 Skill」，產出道地口語版、商務專業版、文法錯誤逐點解析與實用詞彙升級推薦。
   - **第二次及後續追加發問**：保持同一個對話上下文 (Session & Context)，不再重複帶入 Skill，直接針對修改細節進行問答討論。
3. **客製化輸入與快速鍵行為**：
   - 輸入欄位預設單行，隨文字自動向下延伸。
   - 按 **`Enter`**：跳下一行（不送出）。
   - 按 **`Shift + Enter`** 或點選「**送出 🚀**」：發送文本。
4. **雙重資料持久化 (PostgreSQL & H2)**：
   - `chat_messages`：儲存完整歷史對話串流。
   - `polish_raw_submissions`：獨立儲存使用者每一次新對話的**第一筆原始輸入**，供日後寫作習慣與語法弱點大數據分析。
5. **歷史紀錄向下無限滾動 (Infinite Scroll)**：
   - 左側導覽列預設載入最新資料，滾動到底部時自動從資料庫載入更舊紀錄。

---

## 🚀 快速啟動指南

### 1. 啟動後端 (Spring Boot 3 / Java 21+)
進入 `backend` 目錄並執行 Maven Wrapper：
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
> 後端預設運行於 `http://localhost:8080`。
> - 資料庫預設使用內嵌持久化 H2 資料庫（檔案儲存於 `backend/data/yourenglishsucks`，開箱即用免設定）。
> - 若有安裝 Docker，可執行 `docker compose up -d` 啟動 PostgreSQL 16，後端將自動支援或切換 `postgres` profile。

### 2. 啟動前端 (React + Vite + Tailwind CSS)
進入 `frontend` 目錄：
```powershell
cd frontend
npm run dev
```
> 前端運行於 `http://localhost:3000`，已配置 Vite API 代理直通後端 `http://localhost:8080`。

### 3. 設定 Gemini API Key
- **方式 A（Web 介面）**：開啟網頁後，點擊右上角「齒輪 ⚙️」設定，貼上您的 Gemini API Key（儲存於瀏覽器 LocalStorage）。
- **方式 B（後端環境變數）**：在啟動後端前設定環境變數 `$env:GEMINI_API_KEY="AIzaSy..."`。
- *若尚未填寫 Key，系統會自動切換為高品質智慧模擬展示，方便離線體驗整個互動流程。*
