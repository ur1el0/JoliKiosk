import json
from django.db import transaction
from django.db.models import Max
from django.http import HttpRequest, JsonResponse
from django.views.decorators.http import require_GET, require_POST
from .models import MenuItem, Order, OrderItem


def menu_payload(item: MenuItem) -> dict:
    return {"id": item.id, "name": item.name, "category": item.category, "description": item.description, "price": item.price}


def order_payload(order: Order) -> dict:
    return {"id": order.id, "queueNumber": order.queue_number, "status": order.status, "total": order.total, "items": [{"menuItemId": item.menu_item_id, "name": item.name, "unitPrice": item.unit_price, "quantity": item.quantity} for item in order.items.all()]}


@require_GET
def menu_items(_: HttpRequest) -> JsonResponse:
    return JsonResponse({"items": [menu_payload(item) for item in MenuItem.objects.filter(is_available=True)]})


@require_POST
def create_order(request: HttpRequest) -> JsonResponse:
    try:
        requested_items = json.loads(request.body)["items"]
    except (json.JSONDecodeError, KeyError, TypeError):
        return JsonResponse({"error": "Send a JSON body with a non-empty items array."}, status=400)
    if not isinstance(requested_items, list) or not requested_items:
        return JsonResponse({"error": "An order must contain at least one item."}, status=400)
    quantities: dict[int, int] = {}
    try:
        for item in requested_items:
            item_id, quantity = int(item["menuItemId"]), int(item["quantity"])
            if not 1 <= quantity <= 20:
                raise ValueError
            quantities[item_id] = quantities.get(item_id, 0) + quantity
    except (KeyError, TypeError, ValueError):
        return JsonResponse({"error": "Each item needs a valid menuItemId and quantity from 1 to 20."}, status=400)
    with transaction.atomic():
        menu = {item.id: item for item in MenuItem.objects.select_for_update().filter(id__in=quantities, is_available=True)}
        if len(menu) != len(quantities):
            return JsonResponse({"error": "One or more menu items are unavailable."}, status=409)
        total = sum(menu[item_id].price * quantity for item_id, quantity in quantities.items())
        order = Order.objects.create(queue_number=(Order.objects.aggregate(last=Max("queue_number"))["last"] or 0) + 1, total=total)
        OrderItem.objects.bulk_create([OrderItem(order=order, menu_item=menu[item_id], name=menu[item_id].name, unit_price=menu[item_id].price, quantity=quantity) for item_id, quantity in quantities.items()])
    return JsonResponse(order_payload(Order.objects.prefetch_related("items").get(pk=order.pk)), status=201)


@require_GET
def order_detail(_: HttpRequest, order_id: int) -> JsonResponse:
    try:
        return JsonResponse(order_payload(Order.objects.prefetch_related("items").get(pk=order_id)))
    except Order.DoesNotExist:
        return JsonResponse({"error": "Order not found."}, status=404)
