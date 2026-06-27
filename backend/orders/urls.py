from django.urls import path

from . import views

urlpatterns = [
    path("menu-items/", views.menu_items, name="menu-items"),
    path("orders/", views.orders_collection, name="orders"),
    path("orders/<int:order_id>/", views.order_detail, name="order-detail"),
]
