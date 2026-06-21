from django.core.management.base import BaseCommand
from orders.models import MenuItem

MENU = [("Classic Burger", 12000, "Burger", "Beef patty, lettuce, tomato, and house sauce"), ("Crispy Fries", 8000, "Sides", "Golden potato fries"), ("Sweet Spaghetti", 9000, "Meals", "Sweet-style spaghetti with a drink"), ("Chicken with Rice", 15000, "Meals", "Crispy chicken, steamed rice, and gravy"), ("Cola Float", 18000, "Drinks", "Cola topped with vanilla ice cream")]


class Command(BaseCommand):
    help = "Create or update the demo kiosk menu."

    def handle(self, *_: object, **__: object) -> None:
        for name, price, category, description in MENU:
            MenuItem.objects.update_or_create(name=name, defaults={"price": price, "category": category, "description": description, "is_available": True})
        self.stdout.write(self.style.SUCCESS("Demo kiosk menu is ready."))
