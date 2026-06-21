from django.contrib import admin
from .models import MenuItem, Order, OrderItem


class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0
    readonly_fields = ("menu_item", "name", "unit_price", "quantity")


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ("queue_number", "status", "total", "created_at")
    list_filter = ("status",)
    inlines = (OrderItemInline,)


admin.site.register(MenuItem)
