# Kiosk ordering demo

An Android kiosk-style ordering app with a small Django API. It uses generic demo menu data and is not affiliated with or connected to Jollibee.

## Run the API

Use Python 3.12+ and install the backend dependencies:

```bash
cd backend
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py seed_menu
python manage.py runserver
```

The API is available at `http://127.0.0.1:8000`:

- `GET /api/menu-items/`
- `POST /api/orders/`
- `GET /api/orders/<id>/`

For the Android emulator, the debug app targets `http://10.0.2.2:8000`. A physical device needs the computer's LAN IP instead. Configure a real HTTPS API host for release builds in `app/build.gradle.kts`.

## Verification

```bash
cd backend
python manage.py test
```
