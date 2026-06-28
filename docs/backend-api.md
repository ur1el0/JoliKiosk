# Backend, database, and Android API guide

This document explains the complete backend, how Android communicates with it, which endpoints exist, and how to inspect data submitted by users.

## Backend components

```text
Android app
    │ HTTP/JSON
    ▼
Django development server — 127.0.0.1:8000
    │ Psycopg
    ▼
PostgreSQL 18 — Docker container on port 5432
```

The backend uses plain Django views and `JsonResponse`; Django REST Framework is not required for the current small API.

## Important backend files

| File | Responsibility |
|---|---|
| `backend/kiosk_api/settings.py` | Environment and PostgreSQL configuration |
| `backend/kiosk_api/urls.py` | Project routes and `/api/` prefix |
| `backend/orders/urls.py` | Menu and order endpoint routes |
| `backend/orders/models.py` | PostgreSQL table definitions |
| `backend/orders/views.py` | JSON parsing, validation, database operations, and responses |
| `backend/orders/admin.py` | Django Admin menu and order views |
| `backend/orders/tests.py` | API regression tests |
| `backend/orders/management/commands/seed_menu.py` | Mock menu importer |
| `backend/compose.yaml` | PostgreSQL Docker service and volume |
| `backend/.env` | Local secrets and database connection values; never committed |

## Database models

### `MenuItem`

Represents a product that Android can display and order.

| Field | Meaning |
|---|---|
| `id` | Database product ID used by Android |
| `name` | Product name |
| `category` | Burger, Meal, Fries, Drink, and similar categories |
| `description` | Product description |
| `price` | Price in Philippine centavos; `12000` means ₱120.00 |
| `is_available` | Controls whether the item appears in the API menu |

### `Order`

Represents one completed checkout request.

| Field | Meaning |
|---|---|
| `id` | Internal database order ID |
| `queue_number` | Number displayed to the kiosk user |
| `status` | `received`, `preparing`, `ready`, `completed`, or `cancelled` |
| `total` | Server-calculated total in centavos |
| `created_at` | Time the order was saved |

The backend currently creates orders with status `received`. There is no public API endpoint for changing status, but staff can change it through Django Admin.

### `OrderItem`

Represents one product line inside an order.

| Field | Meaning |
|---|---|
| `order` | Parent `Order` foreign key |
| `menu_item` | Original `MenuItem` foreign key |
| `name` | Product-name snapshot at checkout |
| `unit_price` | Price snapshot at checkout |
| `quantity` | Number ordered |

Saving name and price snapshots preserves what was ordered even if the menu product changes later.

## Android connection

The debug API address is compiled into `BuildConfig.KIOSK_API_URL` from `app/build.gradle.kts`:

```text
http://127.0.0.1:8000
```

`KioskApi.kt` uses `HttpURLConnection` on an IO coroutine dispatcher.

During local development, create the port mapping after starting the emulator or connecting a phone:

```powershell
adb reverse tcp:8000 tcp:8000
adb reverse --list
```

The mapping makes Android’s `127.0.0.1:8000` reach Django on the development computer.

## Endpoint summary

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/menu-items/` | List available menu items |
| `GET` | `/api/orders/` | List all orders, newest first |
| `POST` | `/api/orders/` | Validate and create an order |
| `GET` | `/api/orders/<id>/` | Retrieve one order |

All endpoints use trailing slashes.

## `GET /api/menu-items/`

Request:

```http
GET http://127.0.0.1:8000/api/menu-items/
```

Example response:

```json
{
  "items": [
    {
      "id": 1,
      "name": "Yum Burger",
      "category": "Burger",
      "description": "Juicy beef patty with fresh lettuce, tomato, and special sauce.",
      "price": 12000
    }
  ]
}
```

Only available products are returned.

## `POST /api/orders/`

Request:

```http
POST http://127.0.0.1:8000/api/orders/
Content-Type: application/json
```

```json
{
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    },
    {
      "menuItemId": 4,
      "quantity": 1
    }
  ]
}
```

Example successful response (`201 Created`):

```json
{
  "id": 12,
  "queueNumber": 12,
  "status": "received",
  "total": 42000,
  "items": [
    {
      "menuItemId": 1,
      "name": "Yum Burger",
      "unitPrice": 12000,
      "quantity": 2
    },
    {
      "menuItemId": 4,
      "name": "Coke Float",
      "unitPrice": 18000,
      "quantity": 1
    }
  ]
}
```

The server ignores any client-supplied price and calculates the total from current database prices.

The order endpoint is CSRF-exempt because the Android kiosk is not a browser and does not use Django session authentication. Django Admin remains protected by CSRF middleware.

## `GET /api/orders/`

Request:

```http
GET http://127.0.0.1:8000/api/orders/
```

Response:

```json
{
  "orders": [
    {
      "id": 12,
      "queueNumber": 12,
      "status": "received",
      "total": 42000,
      "items": [
        {
          "menuItemId": 1,
          "name": "Yum Burger",
          "unitPrice": 12000,
          "quantity": 2
        }
      ]
    }
  ]
}
```

Orders are sorted newest first.

## `GET /api/orders/<id>/`

Request:

```http
GET http://127.0.0.1:8000/api/orders/12/
```

Returns the same order structure without the outer `orders` array. An unknown ID returns `404`.

## Validation and error responses

| Status | Meaning |
|---:|---|
| `201` | Order created successfully |
| `400` | Missing/invalid JSON, empty items, or invalid quantity |
| `404` | Requested order does not exist |
| `405` | HTTP method is not supported by that endpoint |
| `409` | One or more products do not exist or are unavailable |

## What user input reaches the database

Android submits:

```text
menuItemId
quantity
```

Django then adds database-controlled values:

```text
Order.queue_number
Order.status
Order.total
Order.created_at
OrderItem.name
OrderItem.unit_price
```

The following app data is not submitted or stored:

- Selected branch
- Search query
- Cart contents before checkout
- Customer identity
- Payment details

## How to inspect actual submitted data

Start PostgreSQL and Django first:

```powershell
cd backend
docker compose up -d
python manage.py runserver
```

### Option 1: View JSON in a browser

Open:

```text
http://127.0.0.1:8000/api/orders/
```

To inspect one order:

```text
http://127.0.0.1:8000/api/orders/1/
```

### Option 2: Use PowerShell

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/orders/
```

Show nested order items clearly:

```powershell
$response = Invoke-RestMethod http://127.0.0.1:8000/api/orders/
$response.orders | ConvertTo-Json -Depth 6
```

### Option 3: Use `backend/tests.http`

Open `backend/tests.http` in an HTTP-client extension and run:

```http
GET http://localhost:8000/api/orders/
```

The same file contains requests for creating and retrieving orders.

### Option 4: Use Django Admin

Create an admin account once:

```powershell
cd backend
python manage.py createsuperuser
```

Start Django and open:

```text
http://127.0.0.1:8000/admin/
```

The Orders page displays queue number, status, total, creation time, and inline order items. The Menu Items page displays database products and availability.

Admin login is only for backend management; kiosk customers do not authenticate.

### Option 5: Use the Django shell

List orders:

```powershell
python manage.py shell -c "from orders.models import Order; print(list(Order.objects.values()))"
```

List order items:

```powershell
python manage.py shell -c "from orders.models import OrderItem; print(list(OrderItem.objects.values()))"
```

Print orders with their lines:

```powershell
python manage.py shell -c "from orders.models import Order; [(print(o.id, o.queue_number, o.total, list(o.items.values('name','unit_price','quantity')))) for o in Order.objects.prefetch_related('items')]"
```

### Option 6: Query PostgreSQL directly

Enter `psql` inside the Docker container:

```powershell
docker compose exec db psql -U kiosk_user -d kiosk_db
```

Useful SQL queries:

```sql
SELECT id, queue_number, status, total, created_at
FROM orders_order
ORDER BY created_at DESC;
```

```sql
SELECT
    oi.order_id,
    oi.name,
    oi.unit_price,
    oi.quantity
FROM orders_orderitem AS oi
ORDER BY oi.order_id DESC, oi.id;
```

Join orders and line items:

```sql
SELECT
    o.queue_number,
    o.status,
    oi.name,
    oi.quantity,
    oi.unit_price,
    o.total
FROM orders_order AS o
JOIN orders_orderitem AS oi ON oi.order_id = o.id
ORDER BY o.created_at DESC, oi.id;
```

Exit PostgreSQL with:

```text
\q
```

## Testing the data flow

Run the backend tests:

```powershell
cd backend
python manage.py test orders
```

The tests currently verify:

- Django calculates totals using database prices.
- Android-style POST requests work without a CSRF cookie.
- `GET /api/orders/` returns all orders newest first.

## Current scope and limitations

- No customer authentication
- No payment processing
- No public order-status update endpoint
- No branch table or branch field on orders
- No pagination on the all-orders endpoint
- Cart state is not persisted before checkout
- The API is intended for local development and coursework, not public deployment
