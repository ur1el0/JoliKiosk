# Simple application flow

This document explains what the kiosk application does from the user’s point of view. For backend details and API examples, see [backend-api.md](backend-api.md).

## Complete flow

```text
Open app
   ↓
Select pickup branch
   ↓
Android requests the available menu from Django
   ↓
Django reads menu items from PostgreSQL and returns JSON
   ↓
User searches the menu and adds items to the cart
   ↓
User changes quantities and reviews the total
   ↓
User presses Place Order
   ↓
Android sends menu item IDs and quantities to Django
   ↓
Django validates the items and calculates the real total
   ↓
Django saves the order and its line items in PostgreSQL
   ↓
Android receives and displays the queue number
```

## 1. Branch selection

When the app opens, the user selects a pickup branch.

The branch name, address, and image currently exist only in the Android app. The selected branch changes the text shown on the menu screen, but it is not sent to or saved by the backend.

Relevant Android functions:

- `AppNavigation()` decides whether to show branch selection or the main kiosk.
- `BranchSelectionScreen()` displays the available branches.
- `BranchCard()` displays one selectable branch.

## 2. Menu loading

After branch selection, Android requests:

```http
GET /api/menu-items/
```

Django returns only menu items whose `is_available` field is `true`.

Android converts the JSON response into `MenuItem` objects and displays their name, description, category, price, and matching local image.

Relevant functions:

- Android `KioskApi.menu()` sends the HTTP request and parses JSON.
- Android `MenuScreen()` displays loading, error, empty, and menu states.
- Android `MenuCard()` displays an individual product.
- Django `menu_items()` reads available products from PostgreSQL.

## 3. Search and cart

Searching happens locally on the phone. The search text is not sent to Django.

When the user adds an item, Android stores its quantity in an in-memory map:

```text
menu item ID → quantity
```

For example:

```text
1 → 2
4 → 1
```

This means two units of menu item `1` and one unit of menu item `4` are in the cart.

The cart is not stored in PostgreSQL before checkout. Closing or recreating the app can clear this in-memory state.

Relevant functions:

- `KioskContent()` owns the cart quantity map.
- `CartScreen()` calculates and displays cart lines and the current total.
- `CartRow()` adds or removes one unit.

## 4. Placing an order

When the user presses **Place Order**, Android sends only item IDs and quantities:

```json
{
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    }
  ]
}
```

Android does not send a trusted total or unit price. Django loads the current prices from PostgreSQL and calculates the authoritative total itself.

Relevant functions:

- Android `KioskApi.submitOrder()` builds and sends the JSON request.
- Django `orders_collection()` validates and stores the order.

## 5. Backend validation

Before saving an order, Django checks:

- The request contains a non-empty `items` array.
- Every line has a valid `menuItemId` and `quantity`.
- Each quantity is between 1 and 20.
- Every requested menu item exists and is available.
- The total uses prices from the database, not values supplied by Android.

If validation succeeds, Django creates one `Order` and one `OrderItem` for each distinct menu item.

## 6. Order confirmation

Django responds with the saved order, including:

- Database order ID
- Queue number
- Status
- Server-calculated total
- Saved order items

Android clears the cart and displays the queue number in `OrderConfirmation()`.

The initial status is:

```text
received
```

## What is stored and what is not

| Data | Location | Saved in PostgreSQL? |
|---|---|---:|
| Menu products and prices | Backend | Yes |
| Order ID and queue number | Backend | Yes |
| Order total and status | Backend | Yes |
| Ordered item IDs, names, prices, and quantities | Backend | Yes |
| Selected branch | Android only | No |
| Search query | Android only | No |
| Cart before checkout | Android memory | No |
| Customer identity or login | Not implemented | No |
| Payment information | Not implemented | No |

## Main screens

| Screen | Purpose |
|---|---|
| Branch selection | Select the displayed pickup branch |
| Menu | Load, search, and add available products |
| Cart | Review quantities and total |
| Order confirmation | Show the returned queue number and status |

## Network connection during development

The Android debug build calls:

```text
http://127.0.0.1:8000
```

Because `127.0.0.1` inside Android refers to the Android device itself, ADB reverse maps Android port `8000` to the computer’s port `8000`:

```powershell
adb reverse tcp:8000 tcp:8000
```

Django must remain running while the app is used. See [windows-runbook.md](windows-runbook.md) for startup commands.
