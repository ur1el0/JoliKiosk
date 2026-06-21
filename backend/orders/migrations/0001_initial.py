import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):
    initial = True
    dependencies = []
    operations = [
        migrations.CreateModel(name="MenuItem", fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")), ("name", models.CharField(max_length=120)), ("category", models.CharField(max_length=40)), ("description", models.TextField(blank=True)), ("price", models.PositiveIntegerField(help_text="Price in Philippine centavos")), ("is_available", models.BooleanField(default=True))], options={"ordering": ["category", "name"]}),
        migrations.CreateModel(name="Order", fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")), ("queue_number", models.PositiveIntegerField(unique=True)), ("status", models.CharField(choices=[("received", "Received"), ("preparing", "Preparing"), ("ready", "Ready"), ("completed", "Completed"), ("cancelled", "Cancelled")], default="received", max_length=16)), ("total", models.PositiveIntegerField(help_text="Total in Philippine centavos")), ("created_at", models.DateTimeField(auto_now_add=True))]),
        migrations.CreateModel(name="OrderItem", fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")), ("name", models.CharField(max_length=120)), ("unit_price", models.PositiveIntegerField()), ("quantity", models.PositiveSmallIntegerField()), ("menu_item", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, to="orders.menuitem")), ("order", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="items", to="orders.order"))]),
    ]
