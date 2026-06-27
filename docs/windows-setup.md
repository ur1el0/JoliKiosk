# Windows setup and run guide

This guide runs the complete project on Windows:

- PostgreSQL 18 in Docker Desktop
- Django in a VS Code PowerShell terminal
- The Android app in an Android Studio emulator

The debug app connects to Django through `http://127.0.0.1:8000`. Before running it, an ADB reverse rule maps the emulator or USB-connected phone’s port `8000` to the Windows host’s port `8000`.

## 1. Project layout

```text
WebDev2/
├── app/       Android Studio and Gradle project
├── backend/   Django API and Docker Compose database
└── docs/
```

Open `WebDev2/` in VS Code for backend work. Open `WebDev2/app/` in Android Studio, because `app/` is the Gradle project root.

## 2. Install the prerequisites

Install these before opening the project:

1. [Git for Windows](https://git-scm.com/download/win)
2. [Python for Windows](https://www.python.org/downloads/windows/) — Python 3.12 or newer
3. [Visual Studio Code](https://code.visualstudio.com/download) with the Microsoft Python extension
4. [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/) using Linux containers and preferably the WSL 2 backend
5. [Android Studio](https://developer.android.com/studio/install.html) with:
   - Android SDK Platform 35
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android Emulator

Hardware virtualization must be enabled in BIOS/UEFI for Docker Desktop and the Android Emulator. Restart Windows after enabling WSL 2, virtualization features, or installing Docker Desktop if prompted.

Verify the command-line tools in PowerShell:

```powershell
git --version
py --version
docker --version
docker compose version
```

Start Docker Desktop and wait until it reports that the engine is running.

## 3. Open the repository in VS Code

In PowerShell, change to the directory containing the project:

```powershell
cd C:\path\to\WebDev2
code .
```

All following backend commands assume the terminal starts in `WebDev2`.

## 4. Create the Windows Python environment

Do not reuse a Linux or macOS virtual environment copied with the project. Create one on Windows:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r .\backend\requirements.txt
```

If PowerShell blocks `Activate.ps1`, either use the virtual environment directly:

```powershell
.\.venv\Scripts\python.exe -m pip install -r .\backend\requirements.txt
```

or review your organization’s policy before changing PowerShell execution policy. Python documents the Windows activation command as `<venv>\Scripts\Activate.ps1` in its [virtual environment documentation](https://docs.python.org/3/library/venv.html).

In VS Code, press `Ctrl+Shift+P`, select **Python: Select Interpreter**, and choose:

```text
WebDev2\.venv\Scripts\python.exe
```

## 5. Create the backend environment file

Create the local `.env` from the tracked example:

```powershell
Copy-Item .\backend\.env.example .\backend\.env
```

The default development values are:

```dotenv
DJANGO_SECRET_KEY=django-insecure-change-this-before-deployment
DJANGO_DEBUG=true
DJANGO_ALLOWED_HOSTS=127.0.0.1,localhost

POSTGRES_DB=kiosk_db
POSTGRES_USER=kiosk_user
POSTGRES_PASSWORD=change-this-local-password
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
```

`backend/.env` is ignored by Git. Do not commit it. Use a different secret and database password outside local development.

## 6. Start PostgreSQL with Docker

From the repository root:

```powershell
cd .\backend
docker compose up -d
docker compose ps
```

Wait until the `db` service reports `healthy`. If needed, inspect startup output:

```powershell
docker compose logs -f db
```

Press `Ctrl+C` to stop following the logs; the database container continues running.

The Compose file exposes PostgreSQL on Windows port `5432` and stores data in the named volume `backend_postgres_data`.

Optional direct PostgreSQL access:

```powershell
docker compose exec db psql -U kiosk_user -d kiosk_db
```

Exit `psql` with:

```text
\q
```

## 7. Initialize and run Django in VS Code

Stay in `WebDev2\backend` with the virtual environment activated:

```powershell
python manage.py migrate
python manage.py seed_menu
python manage.py check
python manage.py test orders
python manage.py runserver
```

Leave this terminal running. Django should report:

```text
Starting development server at http://127.0.0.1:8000/
```

The available API routes are:

```text
GET  http://127.0.0.1:8000/api/menu-items/
GET  http://127.0.0.1:8000/api/orders/
POST http://127.0.0.1:8000/api/orders/
GET  http://127.0.0.1:8000/api/orders/<id>/
```

### Verify the API from another PowerShell terminal

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/menu-items/
```

Use an ID returned by the menu endpoint when creating an order:

```powershell
$body = @{
    items = @(
        @{ menuItemId = 1; quantity = 2 }
    )
} | ConvertTo-Json -Depth 4

Invoke-RestMethod `
    -Uri http://127.0.0.1:8000/api/orders/ `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

The repository also contains `backend/tests.http` for VS Code REST Client or JetBrains HTTP Client testing.

## 8. Open the Android project correctly

Start Android Studio and choose **Open**. Select this folder, not the repository root:

```text
C:\path\to\WebDev2\app
```

Allow Gradle sync to finish. Android Studio creates a Windows-specific `app/local.properties` containing the local Android SDK path. Do not copy or commit another machine’s `local.properties`.

If Android Studio asks for the Gradle JDK, use its bundled **Embedded JDK/JBR**. Do not select an unsupported system JDK such as JDK 25. The setting is available under:

```text
File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK
```

Command-line build from PowerShell:

```powershell
cd C:\path\to\WebDev2\app
.\gradlew.bat assembleDebug
```

## 9. Create and start the Android emulator

In Android Studio:

1. Open **Tools > Device Manager**.
2. Select **Add a new device** or **Create Virtual Device**.
3. Choose a phone profile such as Pixel 7.
4. Select an API 35 system image and download it if necessary.
5. Finish creating the Android Virtual Device (AVD).
6. Start the AVD with its play button.
7. Select that emulator in Android Studio’s device selector.

Google recommends the emulator as the preferred general testing method in the [Android Studio installation guide](https://developer.android.com/studio/install.html).

## 10. Run the complete application

Before pressing Run in Android Studio, confirm:

1. Docker Desktop is running.
2. `docker compose ps` reports the PostgreSQL container as healthy.
3. Django is running at `http://127.0.0.1:8000`.
4. The Android emulator is fully started.

Create the reverse rule after the emulator appears in Device Manager:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list
```

Then press **Run app** in Android Studio.

The debug app is already configured with:

```text
http://127.0.0.1:8000
```

The reverse rule makes the Android device’s `127.0.0.1:8000` reach Django on the development computer. Reinstall or rerun the app after changing `KIOSK_API_URL`, because it is compiled into `BuildConfig`.

## 11. ADB commands on Windows

ADB is installed by Android Studio as part of SDK Platform-Tools. Its default path is normally:

```text
C:\Users\<your-user>\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

Android’s [ADB documentation](https://developer.android.com/tools/adb) confirms that ADB is supplied by the SDK Platform-Tools package.

Use the complete path if `adb` is not on `PATH`:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

Expected emulator output resembles:

```text
List of devices attached
emulator-5554    device
```

Useful recovery commands:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" kill-server
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" start-server
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

## 12. Optional: run on a physical Android phone

The same ADB reverse configuration works with a physical phone connected by USB.

1. Enable **Developer options** and **USB debugging** on the phone.
2. Connect it by USB and accept the RSA debugging prompt.
3. Verify it appears as `device`:

```powershell
adb devices
```

4. Reverse the phone’s port `8000` to Windows port `8000`:

```powershell
adb reverse tcp:8000 tcp:8000
```

5. Run the app on the phone. No API URL change is required.

Remove the reverse rule when finished:

```powershell
adb reverse --remove tcp:8000
```

Recreate the reverse rule whenever the device reconnects or the ADB server restarts.

## 13. Normal startup order after initial setup

After the one-time installation and migrations, use this shorter sequence.

### PowerShell terminal 1 — database

```powershell
cd C:\path\to\WebDev2\backend
docker compose up -d
docker compose ps
```

### VS Code terminal 2 — backend

```powershell
cd C:\path\to\WebDev2
.\.venv\Scripts\Activate.ps1
cd .\backend
python manage.py runserver
```

### Android Studio

1. Open `WebDev2\app`.
2. Start the configured AVD.
3. Run `adb reverse tcp:8000 tcp:8000` in the Android Studio terminal.
4. Press **Run app**.

## 14. Stop everything

Stop Django with `Ctrl+C` in its terminal.

Stop PostgreSQL while preserving its data:

```powershell
cd C:\path\to\WebDev2\backend
docker compose down
```

The following command also deletes the PostgreSQL volume and all local database data. Use it only when intentionally resetting the database:

```powershell
docker compose down -v
```

After a reset, rerun `migrate` and `seed_menu`.

## 15. Troubleshooting

### `docker` is not recognized

Install and start Docker Desktop, then open a new PowerShell terminal. Docker Desktop includes Docker Compose; Docker recommends this installation method in its [Compose installation guide](https://docs.docker.com/compose/install/).

### Docker cannot bind port 5432

Another PostgreSQL instance is using the port. Stop the other service or change the host side of the mapping in `backend/compose.yaml`, then update `POSTGRES_PORT` in `backend/.env`.

### Database authentication fails after changing `.env`

The official PostgreSQL image uses its initialization credentials only when creating a new empty data directory. Either restore the original values or intentionally reset the development volume with `docker compose down -v`, then start it again and rerun migrations.

### Django reports a missing environment variable

Confirm `backend/.env` exists and contains every key from `backend/.env.example`. Run Django from the `backend` directory.

### Django reports `DisallowedHost`

For the standard emulator setup, confirm:

```dotenv
DJANGO_ALLOWED_HOSTS=127.0.0.1,localhost
```

Restart Django after changing `.env`.

### Android reports that the kiosk service is unavailable

Check these in order:

1. `http://127.0.0.1:8000/api/menu-items/` works in the Windows browser.
2. Docker reports the database as healthy.
3. `adb reverse --list` includes `tcp:8000 tcp:8000` for the selected device.
4. The app’s debug URL is `http://127.0.0.1:8000`.
5. The app was rebuilt and reinstalled after changing the debug URL.
6. Django was restarted after `.env` changes.

### Gradle fails with a Java version such as `25.0.3`

Open `app/` in Android Studio and select the bundled Embedded JDK/JBR as the Gradle JDK. Avoid building this project with an unsupported system JDK.

### `adb` is not recognized

Use the full Platform-Tools path shown above, or add this directory to the Windows user `PATH`:

```text
%LOCALAPPDATA%\Android\Sdk\platform-tools
```

Open a new terminal after changing `PATH`.

### The menu is empty

Seed it again:

```powershell
cd C:\path\to\WebDev2\backend
python manage.py seed_menu
```

The command is safe to rerun because it updates existing seeded menu items by name.
