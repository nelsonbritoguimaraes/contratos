# ContractOps AI — Production Deployment Guide

## Current Status (as of latest session)

The system is **feature-complete for core financial + operational flows** and has been prepared for containerized deployment.

**What is production-ready:**
- Full backend (Kotlin + Spring Boot 3.4 + Java 21)
- PostgreSQL + Flyway migrations
- Multi-tenant architecture
- Authentication (Keycloak ready)
- Rich financial module (AR, AP, NFS-e, bank reconciliation, cash flow, accounting integration)
- Automatic folha → contas a pagar + accounting
- Real OFX/CSV bank statement import
- CNAB 240 export
- CFO Dashboard

---

## Recommended Production Deployment Options

### Option 1: Simple VPS + Docker (Recommended for start)

**Requirements:**
- Ubuntu 22.04+ or Debian 12+ VPS (minimum 4 vCPU / 8GB RAM recommended)
- Docker + Docker Compose installed

**Steps:**

1. **On the VPS:**
   ```bash
   # Install Docker
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER
   # Log out and back in
   ```

2. **Clone the project**
   ```bash
   git clone <your-repo-url>
   cd Contratos
   ```

3. **Create environment file**
   ```bash
   cp .env.example .env.prod
   nano .env.prod
   ```

   Example `.env.prod`:
   ```env
   POSTGRES_PASSWORD=StrongRandomPassword123!
   REDIS_PASSWORD=AnotherStrongPassword
   KEYCLOAK_ADMIN_PASSWORD=SuperSecretKeycloakPass
   KEYCLOAK_ISSUER_URI=https://your-domain.com/realms/contractops
   ```

4. **Build and start**
   ```bash
   docker compose -f docker-compose.prod.yml build --no-cache
   docker compose -f docker-compose.prod.yml up -d
   ```

5. **Setup reverse proxy + SSL** (strongly recommended)
   - Use **Traefik** or **Nginx Proxy Manager**
   - Expose only 80/443 publicly
   - Terminate TLS before reaching the backend

---

### Option 2: Managed Platforms (Easiest)

- **Railway.app** or **Render.com** — Good for starting
- **Fly.io**
- **AWS ECS + RDS** (more enterprise)
- **DigitalOcean App Platform** or **Kubernetes**

For these platforms you mainly need:
- The improved `backend/Dockerfile`
- Proper environment variables

---

## Important Production Checklist

- [ ] Change all default passwords (Postgres, Keycloak, etc.)
- [ ] Use managed PostgreSQL in real production (AWS RDS, DigitalOcean Managed DB, etc.)
- [ ] Enable HTTPS + proper domain
- [ ] Set `contractops.security.jwt.allow-demo-fallback=false`
- [ ] Configure proper Keycloak realm (not dev mode)
- [ ] Set up monitoring (Prometheus + Grafana or managed)
- [ ] Configure backups for database
- [ ] Set resource limits (already in docker-compose.prod.yml)
- [ ] Use secrets management (Docker secrets, Kubernetes secrets, or platform vault)
- [ ] Run with `SPRING_PROFILES_ACTIVE=prod`
- [ ] Review and harden `application-prod.yml`

---

## Building the Backend Image Locally (for testing)

```bash
cd backend
docker build -t contractops-backend:latest .
```

---

## Common Commands

```bash
# View logs
docker compose -f docker-compose.prod.yml logs -f backend

# Scale backend (if behind load balancer)
docker compose -f docker-compose.prod.yml up -d --scale backend=2

# Database backup
docker exec contractops-postgres-prod pg_dump -U contractops contractops > backup.sql
```

---

## Next Production Improvements (recommended)

1. Replace local Keycloak with a managed identity provider or hardened Keycloak cluster.
2. Add OpenTelemetry + centralized logging.
3. Implement proper secret rotation.
4. Add database migration approval process in CI.
5. Set up blue/green or canary deployments.

---

**Current state:** The application is in a strong position to be deployed. The main remaining blocker for a real production launch is **infrastructure + operational hardening** (SSL, secrets, monitoring, backups).

After you have Java 21 configured locally, you can also run:
```bash
cd backend
./gradlew bootJar -x test
```

Then build the Docker image.

Good luck with the deployment! Let me know if you want me to prepare Traefik config, Kubernetes manifests, or CI/CD pipelines next.

---

## Deploying the Frontend (Production)

The frontend is a Vite + React application. In production it is built into static files and served by Nginx.

### 1. Build the frontend Docker image

```bash
cd frontend
docker build -t contractops-frontend:latest .
```

### 2. Run it together with the backend (recommended)

Use the production compose file we prepared:

```bash
# From the project root
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d frontend
```

The frontend will be available on port **80** by default (you can change this in `docker-compose.prod.yml`).

### 3. Important notes for frontend in production

- The Nginx config (`frontend/nginx.conf`) automatically proxies `/api` calls to the backend service (`http://backend:8080`).
- Client-side routing (React Router) is properly handled with `try_files`.
- Static assets are cached for 1 year.
- If you put a reverse proxy in front (Traefik, Nginx Proxy Manager, Cloudflare, etc.), expose the frontend container on a different internal port and let the reverse proxy handle port 80/443 + SSL.

### 4. Environment variables / Configuration

The frontend currently has **no build-time environment variables** for the API URL (it uses relative `/api` paths, which is the best practice when served from the same domain).

If you need to change the backend URL in the future, you can:
- Update `frontend/nginx.conf` (proxy_pass line), or
- Add Vite `define` config + environment variables at build time.

### Quick local production test (without full compose)

```bash
cd frontend
npm run build
npm run preview
```

This uses Vite's preview server (good for testing the production bundle locally).

---

**Full production stack command:**

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

This will start:
- Postgres
- Redis
- Keycloak
- Backend (Kotlin)
- Frontend (Nginx serving React)

Access the app at `http://your-server-ip` (or your domain after configuring the reverse proxy).
