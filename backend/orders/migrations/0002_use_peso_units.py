from django.db import migrations, models
from django.db.models import F


def convert_centavos_to_pesos(apps, schema_editor) -> None:
    MenuItem = apps.get_model("orders", "MenuItem")
    Order = apps.get_model("orders", "Order")
    OrderItem = apps.get_model("orders", "OrderItem")

    MenuItem.objects.update(price=F("price") / 100)
    Order.objects.update(total=F("total") / 100)
    OrderItem.objects.update(unit_price=F("unit_price") / 100)


def convert_pesos_to_centavos(apps, schema_editor) -> None:
    MenuItem = apps.get_model("orders", "MenuItem")
    Order = apps.get_model("orders", "Order")
    OrderItem = apps.get_model("orders", "OrderItem")

    MenuItem.objects.update(price=F("price") * 100)
    Order.objects.update(total=F("total") * 100)
    OrderItem.objects.update(unit_price=F("unit_price") * 100)


class Migration(migrations.Migration):
    dependencies = [("orders", "0001_initial")]

    operations = [
        migrations.RunPython(convert_centavos_to_pesos, convert_pesos_to_centavos),
        migrations.AlterField(
            model_name="menuitem",
            name="price",
            field=models.PositiveIntegerField(help_text="Price in Philippine pesos"),
        ),
        migrations.AlterField(
            model_name="order",
            name="total",
            field=models.PositiveIntegerField(help_text="Total in Philippine pesos"),
        ),
    ]
