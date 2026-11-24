# ShopMart Backend — Phase 1 MVP

Spring Boot 3 / Java 17 REST API for the ShopMart e-commerce platform. This scaffold
covers the Phase 1 MVP surface: authentication, catalog (products, categories, brands),
cart, wishlist, orders, and payments.

## Stack

- **Spring Boot 3.2.5**, Java 17, Maven
- **Spring Security 6** with stateless JWT (access + rotating refresh tokens)
- **Spring Data JPA** (Hibernate) on **PostgreSQL**
- **Spring Data Redis**; transactional email via the Resend HTTPS API
- **springdoc-openapi** (Swagger UI)
- Lombok; manual DTO mappers (no MapStruct)

## Project layout

```
com.shopmart
├── config/         Security, CORS, OpenAPI, admin seeder
├── common/         BaseEntity, ApiResponse, PageResponse, exceptions, notifications
├── security/       JWT provider + filter, UserPrincipal, SecurityUtils
├── util/           SlugUtils
└── module/
    ├── auth/       register, login, refresh, OTP, password reset, profile
    ├── user/       User, Address, Role
    ├── category/   CRUD
    ├── brand/      CRUD
    ├── product/    CRUD + filtering, pagination, search, featured
    ├── cart/       add / update / remove / clear
    ├── wishlist/   add / remove / move-to-cart / clear
    ├── order/      create from cart, list, details, cancel, track, admin status
    └── payment/    initiate (COD + gateway), verify, history
```

Each module follows the same internal shape: `entity / repository / dto / service /
service/impl / controller / mapper`.

## Prerequisites

- JDK 17+
- Maven 3.9+
- PostgreSQL 14+ (a database named `shopmart`)
- Redis (optional for Phase 1; the app will still start if the Redis auto-config is present and a server is reachable)

## Configuration

All settings live in `src/main/resources/application.yml` and are overridable via
environment variables:

Nothing sensitive has a committed default. Set these in the process environment or your
host's secret store:

| Variable | Required | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | yes | JDBC URL, e.g. `jdbc:postgresql://host:5432/db` |
| `SPRING_DATASOURCE_USERNAME` | yes | Database user |
| `SPRING_DATASOURCE_PASSWORD` | yes | Database password |
| `JWT_SECRET` | yes | **Base64-encoded** HMAC key, at least 32 bytes after decoding |
| `JWT_ACCESS_EXP` | no | Access token TTL (ms); default 15 min |
| `JWT_REFRESH_EXP` | no | Refresh token TTL (ms); default 7 days |
| `CORS_ORIGINS` | production | Comma-separated frontend origins |
| `RAZORPAY_KEY_ID` | for payments | Razorpay key id (publishable) |
| `RAZORPAY_KEY_SECRET` | for payments | Razorpay key secret |
| `RAZORPAY_WEBHOOK_SECRET` | for webhooks | Separate Razorpay webhook signing secret |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | if `STORAGE_PROVIDER=cloudinary` | Cloudinary credentials |
| `RESEND_API_KEY` / `RESEND_FROM_EMAIL` | for email | Resend API key and verified sender |
| `ADMIN_PASSWORD` | optional | Seeds the admin account only on first run; blank skips seeding |
| `SUPERADMIN_PASSWORD` / `ENGINEER_PASSWORD` | optional | Same, for the other staff roles |
| `ADMIN_EMAIL` / `SUPERADMIN_EMAIL` / `ENGINEER_EMAIL` | optional | Seed email overrides |

Generate a production JWT secret:

```bash
openssl rand -base64 48
```

## Database schema

Flyway owns the schema and runs automatically on startup (migrations live in
`src/main/resources/db/migration`). Hibernate runs with `ddl-auto=validate`, so an entity /
migration mismatch fails startup instead of silently changing tables. A fresh database is
built by `V1__baseline_schema.sql`; existing databases are baselined and receive later
migrations only.

The SQL files under `db/` are reference snapshots for manual inspection, not part of the
deployment path. `db/seed.sql` contains sample catalog data only; it does not create users.

## Run

```bash
# create the database
createdb shopmart

# required secrets (use your own values; never commit them)
export SPRING_DATASOURCE_PASSWORD='your-local-db-password'
export JWT_SECRET="$(openssl rand -base64 48)"
# optional: seed a staff account on first run
export ADMIN_PASSWORD='choose-a-strong-password'

mvn spring-boot:run
```

> To add a committed Maven wrapper (`./mvnw`), run `mvn -N wrapper:wrapper` once and commit the generated `.mvn/` directory and `mvnw`/`mvnw.cmd` scripts.

The API is served under the `/api` context path on port `8080`.

- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/v3/api-docs

Staff accounts are no longer created with built-in passwords. Set `ADMIN_PASSWORD`,
`SUPERADMIN_PASSWORD`, or `ENGINEER_PASSWORD` before the first start if you want those
accounts seeded; otherwise no staff account is created. Existing accounts are never reset
or modified by the application.

## Auth flow

1. `POST /api/auth/register` → creates a customer and sends the OTP through Resend (requires `RESEND_API_KEY` and `RESEND_FROM_EMAIL`).
2. `POST /api/auth/verify-otp` → marks the email verified.
3. `POST /api/auth/login` → returns `{ accessToken, refreshToken }`.
4. Send `Authorization: Bearer <accessToken>` on protected routes.
5. `POST /api/auth/refresh-token` → rotates the refresh token and issues a new pair.

OTP and order-confirmation emails are sent through Resend. SMS remains a separate, optional channel controlled by `SMS_ENABLED` and its SMS provider configuration. Without Resend credentials, email delivery fails with a safe service-unavailable response; OTPs are not written to logs.

## Endpoint overview (~45 routes)

| Area | Routes |
|---|---|
| Auth | `POST /auth/{register,login,refresh-token,logout,verify-otp,resend-otp,forgot-password,reset-password}`, `GET/PUT /auth/me`, `PUT /auth/change-password` |
| Categories | `GET /categories`, `GET /categories/{id}`, `POST/PUT/DELETE` (ADMIN) |
| Brands | `GET /brands`, `GET /brands/{id}`, `POST/PUT/DELETE` (ADMIN) |
| Products | `GET /products` (filter/paginate/search), `GET /products/featured`, `GET /products/{id}`, `GET /products/slug/{slug}`, `POST/PUT/DELETE` + `PATCH /products/{id}/stock` (ADMIN) |
| Cart | `GET /cart`, `POST /cart/items`, `PUT /cart/items/{id}`, `DELETE /cart/items/{id}`, `DELETE /cart` |
| Wishlist | `GET /wishlist`, `POST /wishlist/{productId}`, `DELETE /wishlist/{productId}`, `POST /wishlist/{productId}/move-to-cart`, `DELETE /wishlist` |
| Orders | `POST /orders`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel`, `GET /orders/{id}/track`, `PATCH /orders/{id}/status` (ADMIN) |
| Payments | `POST /payments/initiate`, `POST /payments/verify`, `GET /payments/order/{orderId}` |
| Coupons | `POST /coupons/validate`; admin `GET /coupons`, `GET /coupons/{id}`, `POST/PUT/DELETE /coupons` |
| Reviews | `GET /products/{id}/reviews`, `GET /products/{id}/reviews/summary`, `POST /products/{id}/reviews`, `PUT/DELETE /reviews/{id}`, admin `PATCH /reviews/{id}/moderate` |
| Blogs | `GET /blogs`, `GET /blogs/{slug}`; admin `GET /blogs/admin/all`, `GET /blogs/admin/{id}`, `POST/PUT/DELETE /blogs` |
| Notifications | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`, `DELETE /notifications/{id}`, admin `POST /notifications/send` |
| Analytics (admin) | `GET /admin/analytics/{dashboard,sales,top-products,order-status,low-stock}` |
| Search | `GET /search`, `GET /search/suggest`, `GET /search/trending`, `GET /search/history`, `DELETE /search/history` |
| Recommendations | `GET /products/{id}/similar`, `GET /products/{id}/frequently-bought-together`, `POST /products/{id}/view`, `GET /recommendations/{trending,recently-viewed,for-you}` |
| Vendors | `POST /vendors/register`, `GET/PUT /vendors/me`, `GET /vendors/me/{products,orders,earnings,payouts}`, `GET /vendors/store/{slug}`; admin `GET /vendors`, `GET /vendors/{id}`, `PATCH /vendors/{id}/status`, `POST/GET /vendors/{id}/payouts`, `PATCH /vendors/payouts/{id}/paid` |
| Warehouses (admin) | `GET/POST/PUT/DELETE /warehouses`, `GET/PUT /warehouses/{id}/inventory`, `GET /warehouses/inventory/product/{id}`, `GET /warehouses/{id}/low-stock`, `POST/GET /warehouses/transfers` |
| Reports (admin) | `GET /admin/reports/{sales,revenue-by-category,revenue-by-brand,revenue-by-vendor,top-customers,inventory,orders}` (each supports `?format=csv`) |

## Phase 2 modules

Added on top of the Phase 1 MVP:

- **Coupons** — `PERCENTAGE` / `FIXED` discounts with min-order, max-discount cap, total and
  per-user usage limits, and validity windows. `POST /coupons/validate` previews the discount;
  order creation applies the code, clamps the total at zero, and records a redemption.
- **Reviews** — one review per user per product, auto-flagged `verifiedPurchase` when the user
  has bought the item. Every change recomputes `products.rating_average` / `rating_count`.
  Admins moderate via `PATCH /reviews/{id}/moderate`.
- **Blogs** — draft/published posts with slugs and tags; public reads return published only.
- **Notifications** — in-app notification centre (`notifications` table). Order placement raises
  one automatically; admins can target a user or broadcast to all via `POST /notifications/send`.
  This is separate from the email/SMS dispatch abstraction (`NotificationService`).
- **Analytics** (admin) — dashboard totals, daily sales series, top products, order-status
  breakdown, and low-stock list, all derived from existing tables.

Phase 2 tables live in `db/phase2.sql` (run after `db/schema.sql`).

## Phase 3 modules

A marketplace + intelligence layer on top of Phases 1–2:

- **AI Search** — relevance-ranked product search with category/brand facets, autocomplete
  suggestions, trending queries, and per-user search history. Search runs behind a
  `SearchProvider` interface; the default `LexicalSearchProvider` does keyword/relevance
  matching, and an embedding/vector provider can be dropped in later by implementing the
  same interface and marking it `@Primary` — no controller or service changes needed
  (same swap pattern as `PaymentGateway`).
- **Recommendations** — similar products, frequently-bought-together (co-purchase from order
  history), trending, recently-viewed, and a personalized "for you" feed derived from viewing
  history. Product views are tracked via `POST /products/{id}/view`.
- **Vendor system** — vendor registration (grants `ROLE_VENDOR`), profile, approval workflow
  (`PENDING/APPROVED/SUSPENDED/REJECTED`), per-vendor product and order views, commission-based
  earnings, and payouts. Products gain a nullable `vendorId`; order items snapshot the vendor
  at purchase time, so existing data stays valid.
- **Multi-warehouse** — warehouses, per-warehouse inventory (with reserved quantity), stock
  transfers between warehouses, and low-stock views. Order fulfilment calls an
  `InventoryAllocator` that decrements warehouse stock when warehouses exist; otherwise it is a
  no-op and `product.stock` remains the authoritative availability gate (backward-compatible).
- **Advanced reports** — sales over a date range, revenue by category / brand / vendor, top
  customers, inventory valuation + low stock, and an order-status report. Every report endpoint
  also serves CSV via `?format=csv`.

Phase 3 tables live in `db/phase3.sql` (run after `db/schema.sql` and `db/phase2.sql`); it also
adds the `vendor_id` columns to `products` and `order_items`.

## Notes & Phase 2 hooks

- **Payments** use a `PaymentGateway` abstraction with a `StubPaymentGateway` so the
  initiate → verify flow is testable without live keys. Provide a Razorpay/Stripe-backed
  implementation and verify the HMAC signature for real before going live.
- **Coupons / discounts** are stubbed (`discount = 0`) in order creation.
- **Notifications** use Resend for email; SMS remains an optional separately configured provider.
- **RBAC** uses a `Role` enum (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_VENDOR`). A full
  roles/permissions table can replace it later without touching controllers.
- `ddl-auto` is `validate`; Flyway is the only component that changes the schema.
Configure `RESEND_API_KEY` and `RESEND_FROM_EMAIL` before running email flows. The sender address must be authorized in Resend (verify your domain in Resend for production delivery). No SMTP server configuration or Spring Mail dependency is used.

> Build note: this project was scaffolded without running `mvn compile` (Maven Central
> was unreachable in the authoring environment). Dependencies resolve on your first build.
