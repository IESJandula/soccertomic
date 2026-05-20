FROM node:22-alpine

WORKDIR /app

# Fijar pnpm en major 10 para evitar que el build use pnpm 11
RUN corepack enable && corepack prepare pnpm@10 --activate

# Copiar todo el repo
COPY . .

# Entrar en Front y construir
WORKDIR /app/Front

RUN pnpm install --frozen-lockfile && \
    pnpm build

# Servir la app construida
EXPOSE 5173

CMD ["pnpm", "dlx", "serve", "-s", "dist", "-l", "tcp://0.0.0.0:5173"]
