# 🚐 My Matatu App

My Matatu is a smart transport application designed to help commuters in Kenya easily track matatus, plan their routes, and get real-time updates — all through a mobile-first experience. The system also provides tools for conductors to share their matatu’s location and status, and for SACCOs/admins to monitor fleet operations via a web panel.

---

## 📱 App Features

### Commuter App
- 🔍 Search and select destinations
- 🗺️ View nearest matatu stations
- 🟢🟥 Track incoming matatus with availability indicators
- 🔁 Get smart route suggestions including multi-leg trips
- 🔐 Role-based login for commuters

### Conductor App
- 🚦 Toggle between “Has Space” and “Full” status
- 📍 Share real-time matatu location using phone GPS
- 🔐 Role-based login for conductors

### Admin Web Panel
- 📊 Monitor matatu activity
- 👥 Manage users and routes
- 📍 View active matatus and route history

---

## 🔐 Authentication & Roles

Implemented using **Firebase Authentication**, the system supports three user roles:

- `commuter` – Access commuter features and tracking UI
- `conductor` – Start trips and share location/status
- `admin` – Access the web dashboard for route/fleet monitoring

Role-based routing is handled post-login by checking the user's stored role in Firebase Realtime Database.

---

## 🛠️ Tech Stack

| Layer        | Technology                  |
|--------------|-----------------------------|
| Mobile App   | Android (Java), XML UI      |
| Backend      | Firebase (Auth, Realtime DB)|
| Maps         | Google Maps SDK             |
| Admin Panel  | HTML/CSS/JS or React (planned)|
| Deployment   | GitHub + Firebase Hosting   |

---

## 📁 Repository Structure

```plaintext
MyMatatuApp/
├── commuter-app/          # Android app for passengers
├── conductor-app/         # Android app for matatu conductors
├── admin-panel/           # Web dashboard (to be developed)
├── docs/                  # Requirements, wireframes, planning
└── README.md              # This file
