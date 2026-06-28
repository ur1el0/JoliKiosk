import json
from io import StringIO

from django.core.management import call_command
from django.test import Client, TestCase

from .models import MenuItem


class OrderApiTests(TestCase):
    def setUp(self) -> None:
        self.burger = MenuItem.objects.create(name="Classic Burger", category="Burger", price=120)

    def test_server_calculates_order_total(self) -> None:
        response = self.client.post("/api/orders/", data=json.dumps({"items": [{"menuItemId": self.burger.id, "quantity": 2, "price": 1}]}), content_type="application/json")
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["total"], 240)
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


class SeedMenuTests(TestCase):
    expected_prices = {
        "Yum Burger": 42,
        "Small Fries": 50,
        "Spaghetti with Drink": 86,
        "Coke Float": 57,
        "1pc Chicken with Rice": 95,
        "2pc Burgersteak": 138,
        "8pc Chicken Joy Bucket": 707,
    }

    def test_seed_menu_creates_expected_peso_prices(self) -> None:
        call_command("seed_menu", stdout=StringIO())

        prices = dict(MenuItem.objects.values_list("name", "price"))
        self.assertEqual(prices, self.expected_prices)

    def test_seed_menu_repairs_an_existing_price(self) -> None:
        MenuItem.objects.create(name="Yum Burger", category="Burger", price=120)

        call_command("seed_menu", stdout=StringIO())

        self.assertEqual(MenuItem.objects.get(name="Yum Burger").price, 42)
