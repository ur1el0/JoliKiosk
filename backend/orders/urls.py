from django.urls import path

from . import views

urlpatterns = [
    path("menu-items/", views.menu_items, name="menu-items"),
    path("orders/", views.create_order, name="create-order"),
    path("orders/<int:order_id>/", views.order_detail, name="order-detail"),
]
