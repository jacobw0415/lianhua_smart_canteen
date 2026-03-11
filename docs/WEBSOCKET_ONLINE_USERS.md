# 使用者上線狀態（WebSocket）串接說明

## 功能概述

- **REST**：`GET /api/users/online` 取得目前線上使用者清單（含自己），依上線時間排序。
- **WebSocket**：訂閱 `/topic/online-users` 可即時收到「上線（ONLINE）」與「正式登出離線（OFFLINE）」事件。僅在**正式登出**時會廣播 OFFLINE，單純 WebSocket 斷線不會廣播，避免閒置或網路中斷被誤判為離線。

## 1. REST API

- **URL**：`GET /api/users/online`
- **認證**：Bearer JWT（與現有登入 Token 相同）
- **回應**：`ApiResponseDto<List<OnlineUserDto>>`

```json
{
  "status": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "username": "admin",
      "fullName": "管理員",
      "onlineAt": "2025-03-11T10:00:00"
    }
  ],
  "timestamp": "2025-03-11T10:00:00+08:00"
}
```

- 僅會列出「目前有 WebSocket 連線」的使用者；未連線者不會出現在 `data` 中。

## 2. WebSocket（STOMP over SockJS）

### 端點

- **連線 URL**：`/ws`（需與前端同源或 CORS 允許，例如 `wss://your-api-host/ws`）
- **SockJS**：支援，建議前端使用 SockJS + STOMP 以相容各瀏覽器。

### 認證方式（二擇一）

1. **CONNECT frame 的 header 帶 Token（建議）**  
   - Header 名稱：`Authorization: Bearer <access_token>` 或 `token: <access_token>`
2. **連線 URL query**  
   - 例如：`/ws?token=<access_token>`  
   - 需後端 HandshakeHandler 將 query 帶入 session，CONNECT 時由 ChannelInterceptor 讀取（已實作）。

未帶有效 JWT 或 Token 已黑名單時，連線會被拒絕。

### 訂閱即時事件

- **Topic**：`/topic/online-users`
- **訊息格式**：`UserOnlineEventDto`

```json
{
  "eventType": "ONLINE",
  "userId": 1,
  "username": "admin",
  "fullName": "管理員",
  "at": "2025-03-11T10:00:00"
}
```

- `eventType`：`"ONLINE"`（WebSocket 連線成功時）、`"OFFLINE"`（**僅正式登出時**，含 `POST /api/auth/logout` 或管理員強制登出）。
- WebSocket 單純斷線（關閉分頁、閒置、網路中斷）時**不會**廣播 OFFLINE，該使用者會自 `GET /api/users/online` 列表中消失，但其他客戶端不會收到 OFFLINE 事件。

## 3. 前端串接範例（概念）

```javascript
// 使用 sockjs-client + @stomp/stompjs（或 stompjs）
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const token = 'YOUR_ACCESS_TOKEN';
const wsUrl = `${API_BASE_URL}/ws`;

const client = new Client({
  webSocketFactory: () => new SockJS(wsUrl),
  connectHeaders: {
    Authorization: `Bearer ${token}`,
    // 或 token: token,
  },
  onConnect: () => {
    client.subscribe('/topic/online-users', (msg) => {
      const event = JSON.parse(msg.body);
      if (event.eventType === 'ONLINE') {
        // 加入或更新列表中的使用者
      } else if (event.eventType === 'OFFLINE') {
        // 從列表中移除該使用者
      }
    });
  },
});

client.activate();
```

- 頁面載入時可先呼叫 `GET /api/users/online` 取得目前列表，再訂閱 `/topic/online-users` 做即時更新。
- 連線成功後，自己也會出現在線上列表中（含自己）。

## 4. 注意事項

- 線上狀態為**記憶體儲存**，重啟後端後會清空，需重新連線才會再次出現在線上列表。
- **僅正式登出才顯示離線**：`OFFLINE` 僅在以下情況廣播：
  - 使用者呼叫 `POST /api/auth/logout` 且 Token 驗證成功；
  - 管理員對該使用者執行強制登出。
- WebSocket 斷開（關閉分頁、閒置、網路中斷）時，該使用者會自線上列表移除（`GET /api/users/online` 不再列出），但**不會**廣播 OFFLINE，其他客戶端的線上列表不會即時移除該使用者，直到該使用者正式登出或他們重新整理／重新拉取列表。
- 同一使用者多裝置/多分頁會有多個 WebSocket 連線；正式登出時會一次移除該使用者所有連線並廣播一筆 OFFLINE。
- CORS：若前後端不同源，須在後端設定允許 WebSocket 的來源（`SecurityConfig` 已放行 `/ws/**`，CORS 依現有設定）。
