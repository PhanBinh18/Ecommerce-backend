# Hợp đồng Giao tiếp API (API Contracts) - Identity Service

**Base URL (thông qua Gateway):** `http://localhost:8080`

| HTTP Method | Endpoint | Dữ liệu gửi lên (Payload) | Kết quả trả về (Response) | Phân quyền |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | `{ "username": "...", "password": "...", "email": "..." }` | `201 Created` | Mọi người (Public) |
| `POST` | `/api/auth/login` | `{ "username": "...", "password": "..." }` | `{ "accessToken": "ey...", "tokenType": "Bearer" }` | Mọi người (Public) |
| `GET` | `/api/users/me` | *Không (Gửi kèm Token ở Header)* | `{ "id": 1, "username": "...", "roles": ["USER"] }` | USER, ADMIN |