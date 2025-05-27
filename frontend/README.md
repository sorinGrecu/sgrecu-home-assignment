# Frontend – Next.js Client

React-based web client for the real-time chat app, built with **Next.js 15** and **TypeScript**.

---

## ⚙️ Tech stack

• Next.js 15 (App Router, Server & Client components, Edge runtime)  
• React 19 + TypeScript 5  
• Tailwind CSS 4 for utility-first styling  
• Radix-UI Primitives and Lucide-React icons  
• Next-Auth v5 – Google OAuth → JWT exchange with the backend  
• TanStack React Query 5 – data-fetching / caching  
• Zustand – local UI state  
• Axios – REST client with typed wrappers  
• Server-Sent Events (SSE) for real-time chat streaming via POST requests  
• Jest + Testing-Library for unit / integration tests  
• ESLint (Next.js shareable config) + SWC for compile-time checks

---

## 🚀 Quick start

1. Install dependencies (Node ≥ 20 LTS recommended):

   ```bash
   npm ci              # or pnpm i / yarn
   ```

2. Copy `env.example` to `.env` (used by run.sh) or `.env.local` (picked up by `npm run dev`). 
   
   **IMPORTANT:** Make sure to add the Google Client Secret from Sorin Grecu's email to make authentication work.

3. Start the development server:

   ```bash
   npm run dev   # http://localhost:3000
   ```

   Hot-reloading is powered by **Turbopack**.

---

## 📦 Useful NPM scripts

| command              | purpose                                   |
| -------------------- | ----------------------------------------- |
| `npm run dev`        | Start Next.js in dev mode (Turbopack)     |
| `npm run build`      | Production build                          |
| `npm start`          | Run the compiled app                      |
| `npm test`           | Run Jest test-suite                       |
| `npm run test:watch` | Jest in watch mode                        |
| `npm run lint`       | ESLint source check                       |
| `npm run test:coverage` | Generate coverage report (`/coverage`) |

---

## 📂 Project structure (high-level)

```
frontend/
├── app/               # Next.js App-Router pages, layouts & components
│   ├── api/           # Route-handlers (Next-Auth lives here)
│   └── components/    # UI building blocks (chat, auth, layout…)
├── lib/               # Shared libraries (hooks, services, utils, config)
├── types/             # Shared TS type definitions
├── __tests__/         # Jest tests & mocks
├── public/            # Static assets
└── tailwind.config.js # Styling configuration
```

---

## 🛡️ Authentication flow (TL;DR)

1. Frontend triggers `next-auth` Google OAuth.
2. After Google callback the **`jwt` callback** performs a token exchange with `/api/auth/google` on the backend, persisting a **backend JWT** inside the Next-Auth session.
3. `lib/services/apiClient.ts` attaches that JWT to every request and handles 401/403 globally.
4. SSE chat stream is authenticated the same way via an `Authorization: Bearer <JWT>` header.

---

## 💬 Real-time chat

The chat system uses **POST requests with Server-Sent Events (SSE)** for real-time streaming:

- **`/app/components/chat/chatApiClient.ts`** handles POST requests to `/api/chat/stream` with JSON request bodies
- **SSE parsing** is done directly using `fetch()` and `ReadableStream` for better control over the request/response cycle
- **Streaming chunks** are forwarded to the UI via the `useChat` hook, enabling token-by-token streaming similar to ChatGPT
- **Authentication** is handled via JWT tokens in the `Authorization` header

**Benefits of POST-based approach:**
- No URL length limitations for large messages
- Better security (no sensitive data in URLs/logs)
- Structured request bodies with validation
- RESTful API design

---

## 🧪 Testing & Coverage

Tests live under `__tests__/`. Run `npm test` or `npm run test:coverage` to get a report—HTML output is placed in `coverage/`.

**Current coverage:**
- Overall project: 45% (includes untested boilerplate and generated files)
- Core chat components: 97%
- Auth components: 56%
- UI components: Most critical components at 100%

---

## ✅ Linting & code-style

* ESLint config is located in `eslint.config.mjs` and runs with `npm run lint`.
* Tailwind code uses [class-variance-authority](https://github.com/joe-bell/cva) for variant handling.

---

## 📄 Further reading

* [Additional documentation](https://docs.google.com/document/d/1BovFcnWqz19ikOPZGnHJbFscn5CoD2mSLtO6hYkSH20/edit?usp=sharing) – extra documentation about the project.

---

© 2025 Sorin Grecu – provided exclusively for recruiting purposes. 