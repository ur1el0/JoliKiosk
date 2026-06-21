import json
from django.test import TestCase
from .models import MenuItem


class OrderApiTests(TestCase):
    def setUp(self) -> None:
        self.burger = MenuItem.objects.create(name="Classic Burger", category="Burger", price=12000)

    def test_server_calculates_order_total(self) -> None:
        response = self.client.post("/api/orders/", data=json.dumps({"items": [{"menuItemId": self.burger.id, "quantity": 2, "price": 1}]}), content_type="application/json")
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["total"], 24000)
        self.assertEqual(response.json()["queueNumber"], 1)
