from django.core.management.base import BaseCommand

from orders.models import MenuItem


MOCK_MENU = [
    (
        "Yum Burger",
        12000,
        "Burger",
        "Juicy beef patty with fresh lettuce, tomato, and special sauce.",
    ),
    (
        "Small Fries",
        8000,
        "Fries",
        "Crispy golden potato fries seasoned with a touch of salt.",
    ),
    (
        "Spaghetti with Drink",
        9000,
        "Meal",
        "Sweet Filipino-style spaghetti served with a refreshing drink.",
    ),
    (
        "Coke Float",
        18000,
        "Drink",
        "Ice-cold cola topped with creamy vanilla ice cream.",
    ),
    (
        "1pc Chicken with Rice",
        15000,
        "Meal",
        "Crispy fried chicken served with steamed rice and savory gravy.",
    ),
    (
        "2pc Burgersteak",
        17000,
        "Meal",
        "Two savory beef patties served with rice and mushroom gravy.",
    ),
    (
        "8pc Chicken Joy Bucket",
        69900,
        "Meal",
        "Eight pieces of crispy fried chicken made for sharing.",
    ),
]

SUPERSEDED_SEED_NAMES = {
    "Classic Burger",
    "Crispy Fries",
    "Sweet Spaghetti",
    "Chicken with Rice",
    "Cola Float",
}


class Command(BaseCommand):
    help = "Create or update the mock kiosk menu."

    def handle(self, *_: object, **__: object) -> None:
        MenuItem.objects.filter(name__in=SUPERSEDED_SEED_NAMES).update(is_available=False)

        for name, price, category, description in MOCK_MENU:
            MenuItem.objects.update_or_create(
                name=name,
                defaults={
                    "price": price,
                    "category": category,
                    "description": description,
                    "is_available": True,
                },
            )

        self.stdout.write(self.style.SUCCESS(f"Mock kiosk menu is ready ({len(MOCK_MENU)} items)."))
