from django.db import models


class MenuItem(models.Model):
    name = models.CharField(max_length=120)
    category = models.CharField(max_length=40)
    description = models.TextField(blank=True)
    price = models.PositiveIntegerField(help_text="Price in Philippine pesos")
    is_available = models.BooleanField(default=True)

    class Meta:
        ordering = ["category", "name"]


class Order(models.Model):
    class Status(models.TextChoices):
        RECEIVED = "received", "Received"
        PREPARING = "preparing", "Preparing"
        READY = "ready", "Ready"
        COMPLETED = "completed", "Completed"
        CANCELLED = "cancelled", "Cancelled"

    queue_number = models.PositiveIntegerField(unique=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.RECEIVED)
    total = models.PositiveIntegerField(help_text="Total in Philippine pesos")
    created_at = models.DateTimeField(auto_now_add=True)


class OrderItem(models.Model):
    order = models.ForeignKey(Order, related_name="items", on_delete=models.PROTECT)
    menu_item = models.ForeignKey(MenuItem, on_delete=models.PROTECT)
    name = models.CharField(max_length=120)
    unit_price = models.PositiveIntegerField()
    quantity = models.PositiveSmallIntegerField()
