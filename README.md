# Sorin Grecu – Home-Assignment  
Full-stack real-time chat app (Kotlin with Spring Boot + Typescript with React and Next.js)

---

## 🚀 Quick start

```bash
# clone and start everything
./run.sh
```

• API → http://localhost:8080  
• Web UI → http://localhost:3000

> Requires **Java 21**, **Node 20** and a POSIX-compatible shell (the bundled `run.sh` works on macOS / Linux / WSL / Git-bash).

---

## 📂 Repository structure

```
├── run.sh           # one-command bootstrap script
├── backend/         # Kotlin + Spring Boot 3 service
└── frontend/        # Next.js 15 (Edge runtime) client
```

---

## 🛠 Run from your IDE instead

• **Backend** – open `backend/` in IntelliJ IDEA > Run `HomeAssignmentApplication.kt` (profile: `dev`).  
• **Frontend** – open `frontend/` in WebStorm > `npm run dev`.

Environment variables live in `.env` files inside each sub-folder.
Copy the provided `.env.example` files to `.env`, then include them in your project.
The `run.sh` script automatically does this through a setup wizard.

---

## ✅ Tests & Quality gates

| Layer     | Command                | Tooling                           | Coverage |
|-----------|------------------------|-----------------------------------|----------|
| Backend   | `./gradlew test`       | JUnit 5, Mockk, PIT mutation testing, Jacoco | 91% |
| Frontend  | `npm test`             | Jest, React Testing-Library       | Core components: 95%+ |
| Lint FE   | `npm run lint`         | ESLint (Next.js shareable config) | - |

Coverage reports generate under `/backend/build/reports` and `/frontend/coverage`.

---

## 📄 Further reading

* backend/README.md & frontend/README.md – stack details and dev guides.
* [Additional documentation](https://docs.google.com/document/d/1BovFcnWqz19ikOPZGnHJbFscn5CoD2mSLtO6hYkSH20/edit?usp=sharing) – extra documentation about the project.

---

© 2025 Sorin Grecu – provided exclusively for recruiting purposes. 