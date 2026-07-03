# Bank Cards Management API

![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-green?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-7-green?logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)
![Liquibase](https://img.shields.io/badge/Liquibase-4.x-red?logo=liquibase)
![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-red)
![Caffeine](https://img.shields.io/badge/Caffeine-Cache-yellow)
![Bucket4j](https://img.shields.io/badge/Bucket4j-Rate%20Limiting-orange)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3-green?logo=swagger)
![Maven](https://img.shields.io/badge/Maven-3.9+-blue?logo=apachemaven)
![CI](https://github.com/charset-8utf/bank-rest/actions/workflows/ci.yml/badge.svg)
[![Release](https://img.shields.io/github/v/release/charset-8utf/bank-rest?logo=github&cacheSeconds=0)](https://github.com/charset-8utf/bank-rest/releases)
[![GHCR](https://img.shields.io/badge/GHCR-latest-2496ED?logo=docker&logoColor=white)](https://github.com/charset-8utf/bank-rest/pkgs/container/bank-rest)

REST API для управления банковскими картами с JWT-аутентификацией (access + refresh tokens), ролевой моделью, AES-GCM шифрованием номеров карт и rate limiting.

---

## Возможности

| Область                      | Что реализовано                                                                                                                |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| **Аутентификация**           | Регистрация / вход / выход, JWT access token (15 мин) + refresh token (7 дней, rotation), JTI blacklist, роли `ADMIN` / `USER` |
| **Rate limiting**            | Bucket4j: 10 запросов/мин с IP на auth-эндпоинты (`/login`, `/register`, `/refresh`); возвращает 429 с JSON-ошибкой            |
| **Карты (USER)**             | Просмотр своих карт с пагинацией и фильтрацией, запрос блокировки, переводы между своими картами, история транзакций           |
| **Карты (ADMIN)**            | Создание, блокировка, активация, удаление любых карт, просмотр всех карт с фильтрацией                                         |
| **Пользователи (ADMIN)**     | Просмотр всех пользователей с пагинацией, получение по ID, удаление                                                            |
| **Профиль (USER)**           | Просмотр и обновление своего профиля (имя, фамилия, телефон)                                                                   |
| **Безопасность карт**        | Номера шифруются AES-256-GCM (random IV на каждое шифрование), в ответах — маска `**** **** **** 1234`; валидация Луна         |
| **Кеширование**              | Caffeine: `users` (TTL 10 мин) — снимает нагрузку с БД при каждом JWT-запросе; `cards` (TTL 2 мин) — список карт пользователя  |
| **Оптимистичная блокировка** | `@Version` на `Card` + `@Retryable` (3 попытки, backoff 100ms×2) защищают от двойного списания при конкурентных переводах      |
| **Пагинация**                | Детерминированная: вторичная сортировка по `id` предотвращает дубли на границах страниц при одинаковом `createdAt`             |
| **XSS-защита ответов**       | URI в теле ошибок санитизируется перед отдачей клиенту                                                                         |

---

## Архитектура

```
Client
  └─ HTTP ──► RateLimitingFilter ──► JwtAuthenticationFilter ──► Controller ──► Service (interface)
                 (Bucket4j)              (blacklist check)                         │
                                     TokenBlacklist                            ServiceImpl
                                      (Caffeine JTI)                               │
                                                          ┌────────────────────────┼─────────────────┐
                                                       Cache (Caffeine)        Repository         EncryptionUtil
                                                         users / cards             │               (AES-256-GCM)
                                                                               PostgreSQL
                                                                          refresh_tokens table
```

### Refresh token flow

```
POST /api/auth/login  →  { accessToken, refreshToken }
        │
        └─ access token (JWT, 15 мин)  →  Authorization: Bearer <accessToken>
        └─ refresh token (UUID, 7 дней, хранится в БД)

POST /api/auth/refresh  { refreshToken }  →  { accessToken, refreshToken }  (rotation: старый удаляется)

POST /api/auth/logout   Bearer <accessToken>  +  { refreshToken }
        └─ JTI добавляется в Caffeine blacklist
        └─ refresh token удаляется из БД
```

### Структура проекта

```
src/main/java/com/example/bankcards/
├── config/          # SecurityConfig, CacheConfig, RetryConfig, OpenApiConfig, SecurityProperties
├── controller/      # AuthController, CardController, AdminCardController, AdminUserController, UserController
├── dto/
│   ├── request/     # RegisterRequest, LoginRequest, RefreshTokenRequest, CardCreateRequest, TransferRequest
│   └── response/    # AuthResponse, CardResponse, UserResponse, PageResponse, ErrorResponse
├── entity/          # BaseEntity, User (UserDetails), Card (@Version), Role, RefreshToken, CardStatus, Transaction
├── exception/       # BankCardsException, ResourceNotFoundException, UserAlreadyExistsException,
│                    # InsufficientFundsException, CardNotActiveException, InvalidRefreshTokenException,
│                    # GlobalExceptionHandler
├── mapper/          # UserMapper, CardMapper (MapStruct)
├── repository/      # UserRepository (@EntityGraph), CardRepository, TransactionRepository,
│                    # RoleRepository, RefreshTokenRepository, UserProfileRepository
├── security/        # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl,
│                    # TokenBlacklistService, RateLimitingFilter
├── service/         # Интерфейсы + Impl: AuthService, CardService, UserService, RefreshTokenService
└── util/            # CardEncryptionUtil (AES-256-GCM), CardMaskUtil, ApiOutputSanitizer,
                     # CardNumberValidator (@ValidCardNumber, алгоритм Луна)
```

---

## Быстрый старт

### Требования

- Docker Desktop (запущен)
- Java 21+ и Maven 3.9+ — только для локального запуска без Docker

### Вариант A: полный Docker (рекомендуется)

```bash
docker compose up -d --build
```

Собирает образ приложения, поднимает PostgreSQL 17, ждёт готовности БД, запускает приложение.  
API доступен на `http://localhost:8080`, Swagger UI — `http://localhost:8080/swagger-ui.html`.

### Вариант B: локальный запуск

**1. Секреты:**

```bash
cp .env.example .env
```

Все чувствительные параметры читаются из переменных окружения (`${VAR:fallback}`).
Значения по умолчанию в `application.yml` — только для локальной разработки.

| Переменная                     | Описание                                                |
|--------------------------------|---------------------------------------------------------|
| `JWT_SECRET`                   | Секрет для подписи JWT (Base64, ≥32 байта)              |
| `JWT_EXPIRATION`               | Время жизни access token в мс (по умолчанию 900000)     |
| `JWT_REFRESH_EXPIRATION`       | Время жизни refresh token в мс (по умолчанию 604800000) |
| `CARD_ENCRYPTION_SECRET`       | Ключ AES-256-GCM (Base64, ровно 32 байта)               |
| `RATE_LIMIT_CAPACITY`          | Ёмкость bucket на IP (по умолчанию 10)                  |
| `RATE_LIMIT_REFILL_PER_MINUTE` | Пополнение bucket в минуту (по умолчанию 10)            |

**2. База данных:**

```bash
docker compose up -d postgres
```

**3. Запуск:**

```bash
mvn spring-boot:run
```

---

## API

### Аутентификация

```bash
# Регистрация — возвращает access + refresh token
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "password": "secret123"}'

# Вход
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'

# Обновление токенов (refresh token rotation)
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-..."}'

# Выход — инвалидирует оба токена
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-..."}'
```

> **Rate limit**: `/login`, `/register`, `/refresh` — 10 запросов/мин с одного IP. При превышении — HTTP 429.

### Карты пользователя (роль USER)

```bash
# Свои карты с пагинацией
curl "http://localhost:8080/api/cards?page=0&size=10" \
  -H "Authorization: Bearer <accessToken>"

# Свои карты с фильтрацией по статусу
curl "http://localhost:8080/api/cards?status=ACTIVE" \
  -H "Authorization: Bearer <accessToken>"

# Перевод между своими картами
curl -X POST http://localhost:8080/api/cards/transfer \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"fromCardId": 1, "toCardId": 2, "amount": 100.00}'

# Запросить блокировку своей карты
curl -X PUT http://localhost:8080/api/cards/1/block \
  -H "Authorization: Bearer <accessToken>"
```

### Администрирование (роль ADMIN)

```bash
# Создать карту для пользователя
curl -X POST http://localhost:8080/api/admin/cards \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"ownerId": 5, "cardNumber": "4111111111111111", "expiryDate": "2028-12-31"}'

# Заблокировать / активировать карту
curl -X PUT http://localhost:8080/api/admin/cards/3/block -H "Authorization: Bearer <admin-token>"
curl -X PUT http://localhost:8080/api/admin/cards/3/activate -H "Authorization: Bearer <admin-token>"

# Удалить пользователя (удаляет refresh tokens и профиль в правильном порядке)
curl -X DELETE http://localhost:8080/api/admin/users/7 -H "Authorization: Bearer <admin-token>"
```

### Таблица эндпоинтов

| Метод  | Путь                             | Роль  | Описание                                       |
|--------|----------------------------------|-------|------------------------------------------------|
| POST   | `/api/auth/register`             | —     | Регистрация, возвращает access + refresh token |
| POST   | `/api/auth/login`                | —     | Вход, возвращает access + refresh token        |
| POST   | `/api/auth/refresh`              | —     | Обновление токенов (rotation)                  |
| POST   | `/api/auth/logout`               | USER  | Выход, инвалидирует оба токена                 |
| GET    | `/api/cards`                     | USER  | Свои карты (пагинация, фильтр по статусу)      |
| PUT    | `/api/cards/{id}/block`          | USER  | Запросить блокировку своей карты               |
| GET    | `/api/cards/{id}/transactions`   | USER  | История транзакций по своей карте              |
| POST   | `/api/cards/transfer`            | USER  | Перевод между своими картами                   |
| GET    | `/api/users/profile`             | USER  | Получить свой профиль                          |
| PUT    | `/api/users/profile`             | USER  | Обновить свой профиль                          |
| POST   | `/api/admin/cards`               | ADMIN | Создать карту для пользователя                 |
| GET    | `/api/admin/cards`               | ADMIN | Все карты (пагинация, фильтр по статусу)       |
| PUT    | `/api/admin/cards/{id}/block`    | ADMIN | Заблокировать карту                            |
| PUT    | `/api/admin/cards/{id}/activate` | ADMIN | Активировать карту                             |
| DELETE | `/api/admin/cards/{id}`          | ADMIN | Удалить карту                                  |
| GET    | `/api/admin/users`               | ADMIN | Все пользователи (пагинация)                   |
| GET    | `/api/admin/users/{id}`          | ADMIN | Пользователь по ID                             |
| DELETE | `/api/admin/users/{id}`          | ADMIN | Удалить пользователя                           |

> Роль `ADMIN` назначается напрямую в БД. Новые пользователи получают роль `USER` автоматически.
>
> Перевод (`/api/cards/transfer`) работает только между картами одного пользователя — осознанное ограничение дизайна для управления личными картами.

---

## Тесты и покрытие

```bash
mvn test     # тесты + JaCoCo отчёт
mvn verify   # тесты + JaCoCo + проверка порогов + генерация docs/openapi.yaml
```

**Типы тестов:**
- **Unit (Mockito)** — сервисы (`AuthService`, `CardService`, `UserService`, `RefreshTokenService`), утилиты (Luhn, AES-GCM), JWT, маппинг
- **Controller (`@SpringBootTest` + H2)** — все контроллеры, включая rate limit 429, 401, 403
- **Integration (Testcontainers + реальный PostgreSQL)** — auth flow (register → login → refresh → logout → 401 со старым refresh token), полный жизненный цикл карты

Пороги JaCoCo: **LINE ≥ 80%, BRANCH ≥ 80%** для проекта в целом; **LINE ≥ 90%, BRANCH ≥ 90%** для пакета `service`.

---

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`):

| Job        | Шаги                                                         |
|------------|--------------------------------------------------------------|
| **test**   | Checkout → JDK 21 → `mvn test jacoco:report` → upload JaCoCo |
| **docker** | Checkout → JDK 21 → `mvn package -DskipTests` → Docker build |

Testcontainers использует Docker-in-Docker (доступен на `ubuntu-latest`).  
Docker job запускается только после успешного прохождения тестов.

---

## Автор

[charset-8utf](https://github.com/charset-8utf)