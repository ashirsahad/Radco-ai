# RAD Consultancy (Android)

**RAD Consultancy LLC** — *Corporate Services | Tech | Advertising* — native Android business accounting and Point-of-Sale (POS) application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, and **DataStore**.

## Brand Identity

- **Palette**: Jet Black (`#0B0B0B`), Pure White (`#FFFFFF`), Tuscan Red (`#B22023`)
- **Typography**: League Spartan (Primary Display & Headings), Quicksand (Secondary Body & UI), JetBrains Mono (Monospace Codes)
- **Tagline**: *"Compliance. Growth. Trust. From setup to scale, we've got you covered."*

## Features

- **Multi-Organization Workspace & RBAC**:
  - Sign in, register a new company workspace, or use Demo Role quick access (`Admin`, `Manager`, `Accountant`).
  - Role-based navigation and permissions (`Admin`: all 10 modules + Team + Audit Log + Settings; `Accountant`: Dashboard, Invoices, Expenses, Payments, Banking, Reports + Settings; `Manager`: configurable module toggles + Settings).
- **13 Modules & Screens**:
  - **Dashboard**, **Invoices**, **Clients**, **Expenses** (with Photo Picker receipt preview), **POS**, **Payments**, **Banking** (with reconciliation), **Inventory**, **Projects**, **Reports**, **Team** *(Admin)*, **Audit Log** *(Admin)*, and **Settings** (theme, 3 languages, 9 currencies, backup share).

## Demo Accounts

All pre-seeded demo users in the default **RAD Consultancy LLC** organization share the password `password123`:
- **Admin**: `admin@radconsultancy.com`
- **Manager**: `manager@radconsultancy.com`
- **Accountant**: `accountant@radconsultancy.com`

## Building

```bash
gradle assembleDebug
```

The generated APK is output to `app/build/outputs/apk/debug/app-debug.apk`.
