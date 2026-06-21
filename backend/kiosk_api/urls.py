from django.contrib import admin
from django.urls import path
from orders import views

urlpatterns = [path("admin/", admin.site.urls), path("api/menu-items/", views.menu_items), path("api/orders/", views.create_order), path("api/orders/<int:order_id>/", views.order_detail)]
