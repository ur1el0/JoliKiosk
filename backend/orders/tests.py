import json
from django.test import Client, TestCase
from .models import MenuItem


class OrderApiTests(TestCase):
    def setUp(self) -> None:
        self.burger = MenuItem.objects.create(name="Classic Burger", category="Burger", price=12000)

    def test_server_calculates_order_total(self) -> None:
        response = self.client.post("/api/orders/", data=json.dumps({"items": [{"menuItemId": self.burger.id, "quantity": 2, "price": 1}]}), content_type="application/json")
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["total"], 24000)
        self.assertEqual(response.json()["queueNumber"], 1)

    def test_mobile_client_can_create_order_without_csrf_cookie(self) -> None:
        csrf_enforcing_client = Client(enforce_csrf_checks=True)
        response = csrf_enforcing_client.post(
            "/api/orders/",
            data=json.dumps({"items": [{"menuItemId": self.burger.id, "quantity": 1}]}),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 201)

    def test_lists_all_orders_newest_first(self) -> None:
        first = self.client.post(
            "/api/orders/",
            data=json.dumps({"items": [{"menuItemId": self.burger.id, "quantity": 1}]}),
            content_type="application/json",
        ).json()
        second = self.client.post(
            "/api/orders/",
            data=json.dumps({"items": [{"menuItemId": self.burger.id, "quantity": 2}]}),
            content_type="application/json",
        ).json()

        response = self.client.get("/api/orders/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual([order["id"] for order in response.json()["orders"]], [second["id"], first["id"]])
        self.assertEqual(response.json()["orders"][0]["items"][0]["quantity"], 2)
