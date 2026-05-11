# RentAll — Property & Services Rental Platform

A JavaFX desktop application built as part of an academic project at **ESPRIT** (3A39 — 2025/2026).  
RentAll is a multi-role rental platform covering **property stays**, **services**, and **tool rentals** across Tunisia.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Modules](#modules)
- [API Integrations](#api-integrations)
- [Authors](#authors)

---

## Overview

RentAll allows three types of users to interact with the platform:

| Role | Permissions |
|------|-------------|
| **Admin** | Full CRUD on all listings, user management |
| **Host** | Full CRUD on own listings (stays, services, tools) |
| **Guest** | Browse and favorite listings, book tools |

---

## Features

### 🏠 Stays (Logement)
- Full CRUD for property listings mapped to the `logement` table
- Role-aware views — admins see all, hosts see only their own
- Live search + price range + room count filters
- **Automatic 15% price reduction** for listings older than 7 days
- AI-generated property descriptions via **Groq API (LLaMA 3)**
- **Price suggestion engine** based on similar listings in the DB
- **Interactive tile map** built from scratch with pure JavaFX Canvas
  - OpenStreetMap tiles (no WebView, no JavaScript)
  - Pan, zoom, pin clustering, zone-colored markers
- Property detail popup with **3-day weather forecast** (Open-Meteo API)
- Geographic zone tracking across 5 Tunisian regions

### 🔧 Tools
- Tool listings with stock management and category assignment
- Tool booking with date range availability checking
- Quality score system (0–100) with improvement suggestions

### 🛠️ Services
- Service listings with duration, location, and pricing
- Similar services recommendation engine
- Quality score system matching the tool module

### 👤 User Management
- Registration, login, role assignment
- Host pending approval flow
- Admin dashboard with user CRUD

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17+ |
| UI Framework | JavaFX |
| Database | MySQL (Aiven Cloud) |
| Build Tool | Maven |
| Map Tiles | OpenStreetMap / CartoCDN |
| Geocoding | Nominatim (OpenStreetMap) |
| Weather | Open-Meteo API |
| AI Descriptions | Groq API (LLaMA 3.1 8B) |
| JSON Parsing | org.json |
| Testing | JUnit 5 + jqwik (property-based testing) |

---

## Project Structure

```
src/main/java/tn/piapp/
├── dao/
│   ├── ManzelCRUDManager.java   # Stays CRUD — logement table
│   ├── ZoneDAO.java             # Geographic zone counters
│   ├── ServiceDao.java
│   ├── ToolDao.java
│   ├── ToolBookingDao.java
│   ├── FavoriteDao.java
│   └── ...
├── db/
│   └── DbConnection.java        # MySQL singleton connection
├── model/
│   ├── Manzel.java              # Property listing model
│   ├── Service.java
│   ├── Tool.java
│   ├── User.java
│   └── ...
├── service/
│   ├── GeocodingService.java    # Nominatim geocoding + DB cache
│   ├── DescriptionGeneratorService.java  # Groq AI descriptions
│   ├── PriceSuggestionService.java
│   ├── QualityScoreService.java
│   └── ...
├── ui/
│   ├── ManzelController.java    # Stays screen (grid + form + map)
│   ├── MapCanvas.java           # Pure JavaFX Canvas tile map
│   ├── MapCanvasContainer.java  # Map wrapper with zoom controls
│   ├── TileCache.java           # Async OSM tile fetcher (LRU cache)
│   ├── GuestBrowseController.java
│   ├── HomeController.java
│   └── ...
└── util/
    ├── TunisiaZoneClassifier.java  # Address → zone mapping
    ├── PriceSuggestionEngine.java
    └── WeatherService.java
```

---

## Database Schema

### Key tables

```sql
-- Property listings
CREATE TABLE logement (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         LONGTEXT,
    address             VARCHAR(255),
    city                VARCHAR(100),
    country             VARCHAR(100),
    price_per_night     DECIMAL(10,2) NOT NULL,
    number_of_rooms     INT,
    number_of_beds      INT,
    number_of_bathrooms INT,
    max_guests          INT,
    square_meters       INT,
    is_active           TINYINT NOT NULL DEFAULT 1,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL,
    image_name          VARCHAR(255),
    image_size          INT,
    image_updated_at    DATETIME,
    host_id             INT NOT NULL,   -- FK → user(id)
    category_id         INT            -- FK → category(id)
);

-- Geographic zone counters (single-row table)
CREATE TABLE zone (
    id         INT PRIMARY KEY DEFAULT 1,
    GrandTunis INT NOT NULL DEFAULT 0,
    CapBon     INT NOT NULL DEFAULT 0,
    NordOuest  INT NOT NULL DEFAULT 0,
    Centre     INT NOT NULL DEFAULT 0,
    Sud        INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_single_row CHECK (id = 1)
);

INSERT INTO zone VALUES (1, 0, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE id = id;
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL database (or use the provided Aiven cloud connection)

### Setup

1. **Clone the repository**
```bash
git clone https://github.com/your-username/rentall.git
cd rentall/project_java/pi_java
```

2. **Configure the database**  
   Edit `src/main/java/tn/piapp/db/DbConnection.java` with your DB credentials:
```java
private static final String URL  = "jdbc:mysql://your-host:port/your-db";
private static final String USER = "your-user";
private static final String PASSWORD = "your-password";
```

3. **Add the `org.json` dependency** to `pom.xml` if not already present:
```xml
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20240303</version>
</dependency>
```

4. **Build and run**
```bash
mvn clean javafx:run
```

---

## Modules

### Stays Module — Price Reduction Logic

Listings automatically receive a **15% discount** after being listed for 7+ days.  
This is triggered every time the grid loads:

```
App opens → showGridView() → applyPriceReductions()
    → for each listing: if daysListed() >= 7
        → price_per_night × 0.85 saved to DB
    → UI shows red "🏷 -15%" badge on discounted cards
```

### Map Module — Pure JavaFX Canvas

The interactive map uses no WebView or browser engine.  
It fetches OpenStreetMap tiles directly as `Image` objects, draws them on a `Canvas`, and handles:
- Pan (mouse drag)
- Zoom (scroll wheel + buttons)
- Pin clustering (nearby pins collapse into a count badge)
- Zone-colored teardrop markers

### Zone Tracking

Every property is classified into one of 5 Tunisian geographic zones based on its address keywords:

| Zone | Example cities |
|------|---------------|
| Grand Tunis | Tunis, Ariana, Ben Arous, La Marsa |
| Cap Bon | Nabeul, Hammamet, Kelibia |
| Nord Ouest | Bizerte, Béja, Jendouba, Tabarka |
| Centre | Sousse, Sfax, Kairouan, Monastir |
| Sud | Gabès, Djerba, Tozeur, Tataouine |

---

## API Integrations

| API | Usage | Docs |
|-----|-------|------|
| [Groq](https://groq.com) | AI property description generation (LLaMA 3.1 8B) | [docs](https://console.groq.com/docs) |
| [Nominatim](https://nominatim.org) | Address → lat/lng geocoding | [docs](https://nominatim.org/release-docs/latest/) |
| [Open-Meteo](https://open-meteo.com) | 3-day weather forecast per property | [docs](https://open-meteo.com/en/docs) |
| [CartoCDN / OSM](https://carto.com) | Map tile images | [OSM](https://www.openstreetmap.org) |

---

## Authors

Built by the **ESPRIT 3A39 — RentAll team** (2025/2026).

> Stays module developed by **[Your Name]**

---

## License

This project was developed for academic purposes at ESPRIT School of Engineering.
