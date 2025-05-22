# Sorin Grecu – Home-Assignment  
Full-stack real-time chat app (Kotlin with Spring Boot + Typescript with React and Next.js)

---

## 🚀 Quick start

> **📋 Prerequisites:** Java 21, Node 20+
> 
> **💻 Windows Users:** Use WSL (Windows Subsystem for Linux) or Git Bash to run the scripts. Standard Command Prompt/PowerShell won't work with the bash scripts.

### Option 1: Bootstrap script (Recommended)
```bash
# clone and start everything
./run.sh
```
> **Requirements:** Java 21, Node 20 and a POSIX-compatible shell (macOS / Linux / WSL / Git-bash).

The script will automatically:
- Check prerequisites (Java 21, Node.js 20+)
- Configure environment variables (via wizard if needed)  
- Build and install all dependencies (Gradle + npm/yarn/pnpm)
- Start the selected services

> ✨ **New:** The script now handles all dependency building automatically, eliminating common setup issues for reviewers.

> ⚠️ **Note:** This option requires **Ollama to be pre-installed** with the `gemma3:1b` model. If you don't have Ollama set up, use **Option 2 (Docker Compose)** instead, which handles everything automatically.
> 
> To set up Ollama manually:
> ```bash
> # Install Ollama from https://ollama.com
> ollama pull gemma3:1b
> ollama serve  # runs on port 11434
> ```

### Option 2: Docker Compose
```bash
# copy environment template and configure
cp .env.example .env
# edit .env with your Google OAuth secret (IMPORTANT: check email from Sorin Grecu!)
# then start all services
docker-compose up --build
```
> ⚠️ **Can't create `.env` files?** See troubleshooting tips in the Troubleshooting section below.
> 
> 🔑 **CRITICAL:** You must add the Google Client Secret from the email sent by Sorin Grecu to `.env` before running Docker Compose, otherwise authentication will fail.

> **Requirements:** Docker & Docker Compose.
> 
> ✨ **Advantage:** This option automatically installs and configures Ollama with the `gemma3:1b` model - no manual setup required!

**What gets started:**
- 🗄️ **PostgreSQL 16** (port 5432) - Local database container
- 🤖 **Ollama AI** (port 11434) - Local AI model server (gemma3:1b)
- ⚙️ **Backend API** (port 8080) - Kotlin Spring Boot service
- 🌐 **Frontend Web** (port 3000) - Next.js React application

### Option 3: Run from your IDE instead
```bash
# copy environment template and configure
cp .env.example .env
# edit .env with your Google OAuth secret (check email from Sorin Grecu!)
# install frontend dependencies
cd frontend && npm install && cd ..
```
> ⚠️ **Can't create `.env` files?** See troubleshooting tips in the Troubleshooting section below.

• **Backend** – open `backend/` in IntelliJ IDEA > Run `HomeAssignmentApplication.kt` (profile: `dev`).  
• **Frontend** – open `frontend/` in WebStorm > `npm run dev`.

> **Requirements:** Java 21, Node 20. Copy the provided `.env.example` files to `.env` in each sub-folder as needed.

• API → http://localhost:8080  
• Web UI → http://localhost:3000

---

## ⚙️ Environment Setup

### Google OAuth Configuration (Required)
1. **Copy the environment template:**
   ```bash
   cp .env.example .env
   ```
   
   > 💡 **Having trouble with dotfiles?** If you can't create files starting with `.` (common on Windows):
   > - Use WSL, Git Bash, or PowerShell (not Command Prompt)
   > - Alternatively, create the file in a text editor and save as `.env` (include the quotes)
   > - Or rename `env.example` to `env` and edit it directly

2. **Configure Google OAuth:**
   - The Google Client ID is already provided in `.env.example`
   - **IMPORTANT:** You must add the Google Client Secret that will be sent to you via email from Sorin Grecu
   - Edit `.env` and set `JBHA_GOOGLE_CLIENT_SECRET` with the secret from the email

3. **Other settings:**
   - Database credentials (default values work for development)
   - Shared secret for JWT signing (default provided for development)
   - AI model configuration (defaults to `gemma3:1b`)

> ⚠️ **Note:** Without the correct Google Client Secret, authentication will not work. Make sure to check your email for the secret from Sorin Grecu.

---

## 🛠️ Troubleshooting

### Common Authentication Issues

**Error: `invalid_client` during Google OAuth login**
- **Cause:** The `JBHA_GOOGLE_CLIENT_SECRET` was not set to the value supplied in the email from Sorin Grecu
- **Solution:** 
  1. Check your email for the Google Client Secret from Sorin Grecu
  2. Edit your `.env` file and set `JBHA_GOOGLE_CLIENT_SECRET=<secret-from-email>`
  3. Restart the application

### Common Setup Issues

**Can't create `.env` files on Windows?**
- Use WSL, Git Bash, or PowerShell (not Command Prompt)
- Alternatively, create the file in a text editor and save as `.env` (include the quotes)
- Or rename `env.example` to `env` and edit it directly

**Bash script won't run on Windows?**
- Make sure you're using WSL (Windows Subsystem for Linux) or Git Bash
- Standard Command Prompt/PowerShell won't work with bash scripts
- Consider using Docker Compose as an alternative

**Backend build fails?**
- Ensure Java 21 is installed and `JAVA_HOME` is set correctly
- Check network connectivity for downloading dependencies
- Verify all environment variables are configured properly

**Frontend dependencies fail to install?**
- Ensure Node.js 20+ is installed
- Check network connectivity for downloading packages
- Try clearing npm cache: `npm cache clean --force`

**AI features not working?**
- If using `./run.sh`: Make sure Ollama is installed and running with `gemma3:1b` model
- If using Docker Compose: Check that the Ollama container started successfully
- Verify `JBHA_OLLAMA_URL` is set correctly in your environment

---

## 📂 Repository structure

```
├── run.sh             # one-command bootstrap script
├── docker-compose.yml # local development environment
├── .env.example       # local development variables template
├── backend/           # Kotlin + Spring Boot 3 service
└── frontend/          # Next.js 15 (Edge runtime) client
```

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