# Backend – Spring Boot Service

Real-time chat & AI integration service written in **Kotlin** using **Spring Boot 3** and **R2DBC**.

---

## ⚙️ Tech stack

• Kotlin 1.9 + Spring Boot 3.2  
• WebFlux (reactive REST & SSE streaming)  
• R2DBC + Flyway (H2 for quick-run, PostgreSQL for prod)  
• Spring Security ‑ JWT resource-server  
• Spring AI + Ollama integration  
• Micrometer / Prometheus metrics  
• Gradle Kotlin DSL build  
• Tests: JUnit 5, Mockk, PIT mutation testing, Jacoco coverage

---

## ▶️ Running

### 1. Via IDE (IntelliJ IDEA)

1. Open this folder as a project.
2. Copy `.env.example` → `.env` and tweak values. Make sure to complete the file with all required values.
3. Add the environment variables to your run configuration in IntelliJ IDEA.
4. Run `HomeAssignmentApplication.kt`.
5. Pick a Spring profile:  
   • `dev` (default) – H2 file database (persists between restarts).  
   • `prod` – PostgreSQL (make sure env vars are set).

### 2. Via CLI

```bash
# First copy and configure environment variables
cp .env.example .env
# Edit .env with your values

# Then run with desired profile
./gradlew bootRun --args="--spring.profiles.active=dev"    # H2 file database
./gradlew bootRun --args="--spring.profiles.active=prod"    # PostgreSQL
```

Production-grade JAR:

```bash
./gradlew clean bootJar
java -jar build/libs/home-assignment.jar --spring.profiles.active=prod
```

---

## 🔑 Environment variables (`.env`)

Copy `.env.example` → `.env` and tweak values. Make sure to complete the file with all required values and add them when running from IntelliJ IDEA.

---

## 🧪 Developer toolbox

| Task                     | Command                       | Results |
|--------------------------|-------------------------------|---------|
| Run tests                | `./gradlew test`              | 91% coverage |
| Mutation testing (PIT)   | `./gradlew pitest`            | High mutation score |
| Coverage report          | `./gradlew jacocoTestReport`  | Reports in build/reports/jacoco |

Reports are written to `build/reports/*`.

---

## 📄 Further reading

* [Additional documentation](https://docs.google.com/document/d/1BovFcnWqz19ikOPZGnHJbFscn5CoD2mSLtO6hYkSH20/edit?usp=sharing) – extra documentation about the project.

---

© 2025 Sorin Grecu – provided exclusively for recruiting purposes. 